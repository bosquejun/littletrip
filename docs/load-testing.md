# Load Testing & Observability

## Prerequisites

- [k6](https://k6.io/docs/get-started/installation/) installed locally
- Full stack running (`./docker-start.sh` or `docker compose up -d`)

---

## Running the Stress Test

```sh
cd k6
./run.sh
```

The script sets two environment variables before invoking k6:

| Variable | Default | Description |
|----------|---------|-------------|
| `BASE_URL` | `http://localhost:8081` | API base URL (without `/api/v1`) |
| `K6_PROMETHEUS_RW_SERVER_URL` | `http://localhost:9090/api/v1/write` | Prometheus remote-write endpoint |
| `ADMIN_USER` | `admin` | Basic auth username for setup phase |
| `ADMIN_PASS` | `admin` | Basic auth password for setup phase |

Override at runtime:

```sh
BASE_URL=http://staging.example.com ADMIN_PASS=secret ./run.sh
```

k6 metrics are pushed to Prometheus via `--out experimental-prometheus-rw` and are immediately queryable in Grafana while the test runs.

---

## Setup Phase

Before any scenario runs, k6 executes a `setup()` function that bootstraps real data from the live API:

1. Calls `GET /admin/api-keys` to fetch all operator API keys
2. Calls `GET /admin/fares` to resolve all valid transit stop IDs
3. Calls `GET /admin/devices` per operator to collect active device IDs

Each scenario receives an `operators` array where every entry has an `apiKey` and a list of `devices`. This means the test runs against real seeded data — no hard-coded UUIDs.

---

## Scenarios

The test models a **compressed transit day** across five concurrent scenarios. Total wall-clock time: ~16 minutes.

```
 0:00 ──────────────────────────────────────────────────────► 16:00
 │
 ├─ [Warmup]       0:00 – 1:00    5 VUs    baseline, seeds journey data
 ├─ [Morning Rush] 1:00 – 6:00   80 VUs   commuters boarding
 ├─ [Dashboard]    2:00 – 10:00   5 VUs   operators polling (read-only)
 ├─ [Evening Rush] 6:30 – 12:00  100 VUs  commuters alighting
 └─ [Major Event]  12:30 – 15:30 150 VUs  spike at a single hub
```

### Warmup (0:00 – 1:00)

5 VUs each simulate a **complete round trip**: tap ON → sleep 1–5 s (compressed journey) → tap OFF at a different stop. Primes the database with realistic open and completed journeys before the rush scenarios start.

### Morning Rush (1:00 – 6:00)

```
VUs:  0 ──(1m)──► 50 ──(2m)──► 80 ──(1m30s)──► 30 ──(30s)──► 0
```

Models the morning commute: **80% tap ON** (commuters boarding at residential stops), **20% tap OFF** (early arrivals completing journeys at work hubs). Minimal sleep between iterations (0.2 s) — platforms are busy with trains arriving every few minutes.

### Dashboard (2:00 – 10:00)

5 constant VUs simulate operators polling the dashboard throughout the working day, independently of tap load. Each iteration randomly picks one of:

| Probability | Request |
|-------------|---------|
| 50% | `GET /trips?page=1&size=20` |
| 25% | `GET /taps?page=0&size=20` |
| 25% | `GET /operator` |

Sleep: 2–5 s between polls (realistic auto-refresh cadence).

### Evening Rush (6:30 – 12:00)

```
VUs:  0 ──(1m)──► 60 ──(2m30s)──► 100 ──(1m)──► 30 ──(30s)──► 0
```

Reverse commute: **70% tap OFF** (workers alighting at residential stops), **30% tap ON** (errands, late departures). Slightly higher peak than morning (100 vs 80 VUs) — evening rush is typically denser in transit systems.

### Major Event Spike (12:30 – 15:30)

```
VUs:  0 ──(30s)──► 150 ──(2m)──► 150 ──(30s)──► 0
```

A stadium or concert ends — 150 VUs all hammer the same hub stop simultaneously. **75% tap OFF** (crowd heading home), 25% tap ON. Sleep is 0.05 s — simulating a crowd of people scanning at once.

This scenario deliberately triggers the per-`(deviceId, cardToken)` rate limiter (20 req/s). `429 Too Many Requests` is expected and treated as a valid response — the goal is to confirm the limiter fires correctly and the journey state machine stays consistent under extreme concurrency.

---

## Card Token Pool

The test uses **500 unique card tokens** (`card-000001` … `card-000500`). Each VU is assigned a slice of the pool proportional to its VU index, minimising cross-VU card collisions. This avoids a single card being concurrently hammered by multiple VUs (which would produce `INCOMPLETE` journeys instead of `COMPLETED` ones and skew the `journey_completion_rate` metric).

---

## VU-local Journey State

Each VU maintains a local `openJourneys` map tracking which of its cards currently have an `IN_PROGRESS` journey. This enforces proper tap ON → tap OFF pairing — the same guarantee a physical reader provides. A VU will only send tap OFF for a card it previously sent tap ON for, and vice versa.

---

## Thresholds

The test fails if any of the following are breached:

| Threshold | Limit | Scope |
|-----------|-------|-------|
| Error rate | < 2% | Morning rush, Evening rush |
| Error rate | < 1% | Dashboard reads |
| p95 latency | < 800 ms | Morning rush, Evening rush |
| p99 latency | < 2 000 ms | Morning rush, Evening rush |
| p95 latency | < 500 ms | Dashboard reads |
| p99 latency | < 1 000 ms | Dashboard reads |
| `tap_success_rate` | > 95% | All tap submissions (201 or 409) |
| `journey_completion_rate` | > 80% | Tap OFFs that return 201 |

The Major Event scenario has **no hard thresholds** — `429` responses are expected and valid.

---

## Custom Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `tap_success_rate` | Rate | % of tap requests that returned 201 or 409 (success or idempotent) |
| `journey_completion_rate` | Rate | % of tap OFFs that returned 201 (journey completed) |
| `rate_limit_hits` | Counter | Total 429 responses across all scenarios |
| `tap_latency_ms` | Trend | End-to-end latency for every tap submission |

---

## Monitoring with Grafana

### 1. Start the observability stack

```sh
docker compose up -d prometheus grafana redis-exporter
```

Prometheus scrapes:

| Target | Endpoint | Interval |
|--------|----------|----------|
| Spring Boot API | `/actuator/prometheus` | 15 s |
| Redis Exporter | `:9121` | 15 s |

### 2. Open Grafana

Go to **http://localhost:4000** and log in with `admin` / `admin`.

The **LittleTrip API** dashboard is provisioned automatically — no manual import needed.

### 3. Run the test and watch live

```sh
cd k6 && ./run.sh
```

k6 pushes metrics to Prometheus as the test runs. Panels update in near real time.

### Key panels to watch

| Panel | What to look for |
|-------|-----------------|
| **Request rate** | Rises with each scenario's ramp-up; dips between scenarios |
| **p95 / p99 latency** | Should stay under 800 ms during rush scenarios |
| **Error rate** | Should be near 0% during commute scenarios; spikes to ~60–80% during Major Event (expected `429`s) |
| **`rate_limit_hits` counter** | Climbs sharply during Major Event spike, flat during commute scenarios |
| **Redis ops/sec** | Spikes with rate-limit key writes during Major Event |
| **JVM heap / GC** | Watch for heap pressure at 100+ VUs during Evening Rush |

### Useful PromQL queries

```promql
# Request rate by scenario tag
rate(http_server_requests_seconds_count[30s])

# p95 tap latency
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket{uri="/api/v1/taps"}[1m]))

# Rate limit decisions (allowed vs throttled)
rate(api_rate_limit_requests_total[30s])

# 429 rate
rate(http_server_requests_seconds_count{status="429"}[30s])

# Redis connected clients
redis_connected_clients
```
