import http from "k6/http";
import { check, sleep } from "k6";
import { uuidv4 } from "https://jslib.k6.io/k6-utils/1.4.0/index.js";
import encoding from "k6/encoding";
import { Counter, Rate, Trend } from "k6/metrics";

// ---------------------------------------------------------------------------
// Config
// ---------------------------------------------------------------------------

const BASE_URL = __ENV.BASE_URL || "http://localhost:8081/api/v1";
const ADMIN_USER = __ENV.ADMIN_USER || "admin";
const ADMIN_PASS = __ENV.ADMIN_PASS || "admin";
const encodedAdminCred = encoding.b64encode(`${ADMIN_USER}:${ADMIN_PASS}`);

// ---------------------------------------------------------------------------
// Custom metrics
// ---------------------------------------------------------------------------

const tapSuccessRate = new Rate("tap_success_rate");
const journeyCompletionRate = new Rate("journey_completion_rate");
const rateLimitHits = new Counter("rate_limit_hits");
const tapLatency = new Trend("tap_latency_ms", true);

// ---------------------------------------------------------------------------
// Scenarios
//
// Models a compressed transit day:
//
//  0:00 –  1:00   Warmup          Baseline traffic — system stabilises
//  1:00 –  6:00   Morning Rush    Commuters board: 80% tap ON, 20% tap OFF
//  2:00 – 10:00   Dashboard       Operators poll trips/taps dashboard (read-only)
//  6:30 – 12:00   Evening Rush    Commuters alight: 70% tap OFF, 30% tap ON
// 12:30 – 15:30   Major Event     Extreme spike — stadium/concert crowd at a hub
//
// Total wall-clock: ~16 minutes
// ---------------------------------------------------------------------------

export const options = {
  scenarios: {
    warmup: {
      executor: "ramping-vus",
      exec: "normalCommute",
      startVUs: 1,
      stages: [
        { duration: "30s", target: 5 },
        { duration: "30s", target: 5 },
      ],
      tags: { scenario: "warmup" },
    },

    morning_rush: {
      executor: "ramping-vus",
      exec: "morningRush",
      startTime: "1m",
      startVUs: 0,
      stages: [
        { duration: "1m", target: 50 }, // commuters pile in
        { duration: "2m", target: 80 }, // peak — trains arriving every 2 min
        { duration: "1m30s", target: 30 }, // tail-off as offices fill up
        { duration: "30s", target: 0 },
      ],
      tags: { scenario: "morning_rush" },
    },

    // Operator dashboards are polled throughout the day regardless of tap load
    dashboard: {
      executor: "constant-vus",
      exec: "operatorDashboard",
      startTime: "2m",
      duration: "8m",
      vus: 5,
      tags: { scenario: "dashboard" },
    },

    evening_rush: {
      executor: "ramping-vus",
      exec: "eveningRush",
      startTime: "6m30s",
      startVUs: 0,
      stages: [
        { duration: "1m", target: 60 }, // workers start leaving
        { duration: "2m30s", target: 100 }, // peak homeward flow
        { duration: "1m", target: 30 },
        { duration: "30s", target: 0 },
      ],
      tags: { scenario: "evening_rush" },
    },

    // A major event (concert, match) causes a sudden crowd at one hub.
    // All passengers tap ON simultaneously — validates rate limiting and
    // journey state machine under extreme concurrency.
    major_event: {
      executor: "ramping-vus",
      exec: "majorEventSpike",
      startTime: "12m30s",
      startVUs: 0,
      stages: [
        { duration: "30s", target: 150 }, // crowd floods the gate
        { duration: "2m", target: 150 }, // sustained surge
        { duration: "30s", target: 0 },
      ],
      tags: { scenario: "major_event" },
    },
  },

  thresholds: {
    // Core commute scenarios must stay reliable
    "http_req_failed{scenario:morning_rush}": ["rate<0.02"],
    "http_req_failed{scenario:evening_rush}": ["rate<0.02"],
    "http_req_failed{scenario:dashboard}": ["rate<0.01"],

    // Latency SLOs — tighter for read endpoints, relaxed for tap ingestion
    "http_req_duration{scenario:morning_rush}": ["p(95)<800", "p(99)<2000"],
    "http_req_duration{scenario:evening_rush}": ["p(95)<800", "p(99)<2000"],
    "http_req_duration{scenario:dashboard}": ["p(95)<500", "p(99)<1000"],

    // At least 95% of tap submissions should succeed (201 or idempotent 409)
    tap_success_rate: ["rate>0.95"],

    // At least 80% of opened journeys should be properly closed with a tap OFF
    journey_completion_rate: ["rate>0.80"],
  },
};

// ---------------------------------------------------------------------------
// Card token pool
//
// A real transit system serves hundreds of thousands of cards.
// Using 500 unique tokens distributes journeys across many open/close pairs
// and avoids the state-machine edge case of a single card being hammered by
// concurrent VUs (which would produce INCOMPLETE journeys, not COMPLETED ones).
// ---------------------------------------------------------------------------

const CARD_POOL_SIZE = 500;
const cardPool = Array.from(
  { length: CARD_POOL_SIZE },
  (_, i) =>
    `${randomItem(["5", "4", "3", "0x"])}${String(i + 1).padStart(6, "0")}`,
);

// ---------------------------------------------------------------------------
// VU-local journey state
//
// Each VU tracks which of its cards currently has an open IN_PROGRESS journey.
// This ensures tap ON is always followed by tap OFF for the same card — the
// same guarantee a physical reader provides.
// ---------------------------------------------------------------------------
const openJourneys = {}; // cardToken -> true (open) | undefined (closed)

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function randomItem(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

function randomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function pickCard() {
  // Each VU owns a slice of the card pool to minimise cross-VU card collisions
  const sliceSize = Math.ceil(CARD_POOL_SIZE / 50);
  const offset = ((__VU - 1) * sliceSize) % CARD_POOL_SIZE;
  return cardPool[(offset + randomInt(0, sliceSize - 1)) % CARD_POOL_SIZE];
}

function tapPayload(tapType, stopId, deviceId, cardToken) {
  return JSON.stringify({
    id: uuidv4(),
    dateTimeUtc: new Date().toISOString(),
    tapType,
    stopId,
    deviceId,
    cardToken,
  });
}

function postTap(tapType, stopId, deviceId, cardToken, headers) {
  const res = http.post(
    `${BASE_URL}/taps`,
    tapPayload(tapType, stopId, deviceId, cardToken),
    { headers },
  );
  tapLatency.add(res.timings.duration);
  if (res.status === 429) rateLimitHits.add(1);
  return res;
}

// ---------------------------------------------------------------------------
// Setup — runs once before any scenario starts
// ---------------------------------------------------------------------------

export function setup() {
  // Fetch all operator API keys
  const keysRes = http.get(`${BASE_URL}/admin/api-keys?page=1&size=50`, {
    headers: { Authorization: `Basic ${encodedAdminCred}` },
  });
  if (keysRes.status !== 200)
    throw new Error(`Failed to fetch API keys: HTTP ${keysRes.status}`);

  const keys = keysRes.json().content;
  if (!keys || keys.length === 0)
    throw new Error(
      "No API keys found — ensure V2__init_seeds.sql was applied",
    );

  // Fetch transit stop IDs from the fare table
  const faresRes = http.get(`${BASE_URL}/admin/fares?page=1&size=50`, {
    headers: { Authorization: `Basic ${encodedAdminCred}` },
  });

  if (faresRes.status !== 200)
    throw new Error(`Failed to fetch fares: HTTP ${faresRes.status}`);

  const fares = faresRes.json().content;
  if (!fares || fares.length === 0)
    throw new Error("No fares found — ensure V2__init_seeds.sql was applied");

  const stopIdSet = new Set();
  fares.forEach((f) => {
    stopIdSet.add(f.stopAId);
    stopIdSet.add(f.stopBId);
  });
  const stopIds = Array.from(stopIdSet);

  // Build operator contexts: each holds an API key + list of active device IDs
  const operators = [];
  for (const key of keys) {
    const devRes = http.get(
      `${BASE_URL}/admin/devices?page=1&size=20&operatorId=${key.operator.id}`,
      { headers: { Authorization: `Basic ${encodedAdminCred}` } },
    );
    if (devRes.status !== 200) continue;
    const activeDevices = devRes
      .json()
      .content.filter((d) => d.status === "active")
      .map((d) => d.id);
    if (activeDevices.length === 0) continue;
    operators.push({ apiKey: key.keyHash, devices: activeDevices });
  }

  if (operators.length === 0)
    throw new Error("No operators with active devices found");

  console.log(
    `Setup: ${operators.length} operators, ${stopIds.length} stops, ${fares.length} fare pairs, ${CARD_POOL_SIZE} card tokens`,
  );

  return { operators, stopIds };
}

// ---------------------------------------------------------------------------
// Scenario: Normal Commute (warmup)
//
// Each iteration simulates one complete journey:
//   tap ON → travel (1–5 s) → tap OFF
// Used during warmup to seed the database with realistic journey data.
// ---------------------------------------------------------------------------

export function normalCommute(data) {
  const op = randomItem(data.operators);
  const headers = {
    "Content-Type": "application/json",
    "X-API-Key": op.apiKey,
  };
  const deviceId = randomItem(op.devices);
  const cardToken = pickCard();
  const stopIds = data.stopIds;

  const origin = randomItem(stopIds);
  const destination =
    randomItem(stopIds.filter((s) => s !== origin)) || randomItem(stopIds);

  // Tap ON
  const onRes = postTap("ON", origin, deviceId, cardToken, headers);
  tapSuccessRate.add(onRes.status === 201 || onRes.status === 409);
  check(onRes, { "tap ON": (r) => r.status === 201 || r.status === 409 });

  if (onRes.status === 201) openJourneys[cardToken] = true;

  // Simulate journey travel time (compressed: 1–5 s represents 5–30 min)
  sleep(randomInt(1, 5));

  // Tap OFF
  const offRes = postTap("OFF", destination, deviceId, cardToken, headers);
  journeyCompletionRate.add(offRes.status === 201);
  check(offRes, { "tap OFF": (r) => r.status === 201 || r.status === 409 });

  if (offRes.status === 201) delete openJourneys[cardToken];

  sleep(randomInt(1, 3));
}

// ---------------------------------------------------------------------------
// Scenario: Morning Rush
//
// 80% of interactions are tap ONs (commuters boarding at residential stops).
// 20% are tap OFFs (early commuters completing journeys at destination hubs).
// Minimal sleep between iterations — platforms are busy.
// ---------------------------------------------------------------------------

export function morningRush(data) {
  const op = randomItem(data.operators);
  const headers = {
    "Content-Type": "application/json",
    "X-API-Key": op.apiKey,
  };
  const deviceId = randomItem(op.devices);
  const cardToken = pickCard();
  const stopId = randomItem(data.stopIds);

  const isBoarding = Math.random() < 0.8;

  if (isBoarding && !openJourneys[cardToken]) {
    const res = postTap("ON", stopId, deviceId, cardToken, headers);
    tapSuccessRate.add(res.status === 201 || res.status === 409);
    check(res, { "morning tap ON": (r) => [201, 409, 429].includes(r.status) });
    if (res.status === 201) openJourneys[cardToken] = true;
  } else if (openJourneys[cardToken]) {
    const destination =
      randomItem(data.stopIds.filter((s) => s !== stopId)) || stopId;
    const res = postTap("OFF", destination, deviceId, cardToken, headers);
    journeyCompletionRate.add(res.status === 201);
    check(res, {
      "morning tap OFF": (r) => [201, 409, 429].includes(r.status),
    });
    if (res.status === 201) delete openJourneys[cardToken];
  }

  sleep(0.2); // High-frequency — trains arriving every few minutes
}

// ---------------------------------------------------------------------------
// Scenario: Evening Rush
//
// 70% tap OFFs (workers alighting at residential stops).
// 30% tap ONs (errands, late departures).
// ---------------------------------------------------------------------------

export function eveningRush(data) {
  const op = randomItem(data.operators);
  const headers = {
    "Content-Type": "application/json",
    "X-API-Key": op.apiKey,
  };
  const deviceId = randomItem(op.devices);
  const cardToken = pickCard();
  const stopId = randomItem(data.stopIds);

  const isAlighting = Math.random() < 0.7;

  if (isAlighting && openJourneys[cardToken]) {
    const destination =
      randomItem(data.stopIds.filter((s) => s !== stopId)) || stopId;
    const res = postTap("OFF", destination, deviceId, cardToken, headers);
    journeyCompletionRate.add(res.status === 201);
    check(res, {
      "evening tap OFF": (r) => [201, 409, 429].includes(r.status),
    });
    if (res.status === 201) delete openJourneys[cardToken];
  } else if (!openJourneys[cardToken]) {
    const res = postTap("ON", stopId, deviceId, cardToken, headers);
    tapSuccessRate.add(res.status === 201 || res.status === 409);
    check(res, { "evening tap ON": (r) => [201, 409, 429].includes(r.status) });
    if (res.status === 201) openJourneys[cardToken] = true;
  }

  sleep(0.15);
}

// ---------------------------------------------------------------------------
// Scenario: Operator Dashboard
//
// Simulates operators continuously polling the dashboard throughout the day.
// Read-only — hits /trips, /taps, and /operator endpoints.
// Runs concurrently with all tap scenarios.
// ---------------------------------------------------------------------------

export function operatorDashboard(data) {
  const op = randomItem(data.operators);
  const headers = { "X-API-Key": op.apiKey };

  const roll = Math.random();
  let res;
  if (roll < 0.5) {
    res = http.get(`${BASE_URL}/trips?page=1&size=20`, { headers });
    check(res, { "trips list 200": (r) => r.status === 200 });
  } else if (roll < 0.75) {
    res = http.get(`${BASE_URL}/taps?page=0&size=20`, { headers });
    check(res, { "taps list 200": (r) => r.status === 200 });
  } else {
    res = http.get(`${BASE_URL}/operator`, { headers });
    check(res, { "operator 200": (r) => r.status === 200 });
  }

  // Dashboards auto-refresh every few seconds
  sleep(randomInt(2, 5));
}

// ---------------------------------------------------------------------------
// Scenario: Major Event Spike
//
// A stadium/concert ends — 150 VUs all tap simultaneously at the same stop.
// Tests whether the per-(deviceId, cardToken) rate limiter (20 req/s) fires
// correctly and the journey state machine stays consistent under concurrency.
// 429s are expected and valid here — the system is protecting itself.
// ---------------------------------------------------------------------------

export function majorEventSpike(data) {
  const op = randomItem(data.operators);
  const headers = {
    "Content-Type": "application/json",
    "X-API-Key": op.apiKey,
  };
  const deviceId = randomItem(op.devices);

  // Crowd all exits through the same hub stop
  const eventStop = data.stopIds[0];
  const cardToken = pickCard();

  // Post-event: most people tapping OFF to complete their journey home
  const tapType = Math.random() < 0.75 ? "OFF" : "ON";
  const stopId =
    tapType === "OFF"
      ? randomItem(data.stopIds.filter((s) => s !== eventStop)) || eventStop
      : eventStop;

  const res = postTap(tapType, stopId, deviceId, cardToken, headers);

  // During a spike, 429 is a valid and expected response
  check(res, {
    "event tap handled": (r) => [201, 409, 429].includes(r.status),
  });

  sleep(0.05); // Minimal delay — simulating a crowd all scanning at once
}
