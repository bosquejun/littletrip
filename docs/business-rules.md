# Business Rules

## Journey State Machine

When a tap event arrives at `POST /taps`, `TripService` runs the following logic to create or update a journey.

### States

| Status | Description |
|--------|-------------|
| `IN_PROGRESS` | Tap ON received, journey started, awaiting tap OFF |
| `COMPLETED` | Normal journey — tap OFF at a different stop from tap ON |
| `CANCELLED` | Tap ON and tap OFF at the same stop — no charge |
| `INCOMPLETE` | Tap ON followed by another tap ON (or end of session) — max fare charged |
| `FAILED` | Reserved for system-level errors (not produced by normal tap flow) |

### Transition Table

| Tap type received | Open journey exists? | Result |
|-------------------|----------------------|--------|
| **ON** | No | Create new `IN_PROGRESS` journey from current stop |
| **ON** | Yes, different stop | Close existing as `INCOMPLETE` (charge max fare), start new `IN_PROGRESS` journey |
| **ON** | Yes, same stop as origin | Close existing as `CANCELLED` (no charge), start new `IN_PROGRESS` journey |
| **OFF** | Yes, different stop from origin | Close as `COMPLETED`, calculate and charge fare |
| **OFF** | Yes, same stop as origin | Close as `CANCELLED` (no charge) |
| **OFF** | No | Event is silently ignored, returns `null` |

### Visual flow

```
Tap ON  ──► [IN_PROGRESS]
              │
              ├─ Tap OFF at different stop ──► [COMPLETED]  (fare charged)
              │
              ├─ Tap OFF at same stop      ──► [CANCELLED]  (no charge)
              │
              └─ Another Tap ON            ──► [INCOMPLETE] (max fare charged)
                                               + new [IN_PROGRESS] journey starts
```

---

## Fare Calculation

Fares are stored in the `transit_fares` table as **integer cents** to avoid floating-point rounding issues. The web dashboard divides by 100 for display.

### Normal fare (COMPLETED journey)

`TransitFareService.getFareCents(originStopId, destinationStopId)` looks up the `transit_fares` table for the matching stop pair. The pair is stored with `stop_a_id < stop_b_id` (ordered by UUID lexicographically), so the lookup always normalises the order.

### Maximum fare (INCOMPLETE journey)

`TransitFareService.getMaxFareFrom(originStopId)` returns the highest fare available from the origin stop across all destinations. This is charged when a journey is closed by a second tap ON before a tap OFF arrives.

### Seed fare table

The database is seeded with four stops and six fare pairs:

| From | To | Fare |
|------|----|------|
| North Station | Central Avenue | $3.25 |
| Central Avenue | South Terminal | $5.50 |
| North Station | South Terminal | $7.30 |
| North Station | Airport Hub | $12.00 |
| Central Avenue | Airport Hub | $8.50 |
| South Terminal | Airport Hub | $4.50 |

Admin users can add/update/delete fares via `POST /admin/fares`.

---

## Device Authorization Model

```
operators
   └─ operator_devices  (many per operator)
   └─ api_keys          (many per operator)
         └─ api_key_devices  (many-to-many: key ↔ device)
```

An operator may have multiple API keys and multiple devices. A tap event is only accepted if the authenticating API key is explicitly linked to the submitting device in `api_key_devices`. This means:

- You can create a restricted key that can only receive taps from a subset of devices.
- A key from Operator A cannot submit taps for Operator B's devices.

---

## Duplicate Event Detection

Every tap request includes a client-generated `id` (UUID). This is persisted as `request_id` with a `UNIQUE` constraint on the `tap_events` table. A second request with the same UUID returns `409 Conflict` immediately, before any journey state changes are made. This makes tap submission safe to retry.

---

## Card Tokens

`cardToken` is an opaque string (max 64 chars) representing a transit card. The API never interprets or validates the token format — it is used only as a grouping key to associate tap events and journeys with a single card.

---

## Tap Simulator Debounce

The tap ON/OFF buttons in the simulator enforce a client-side cooldown to prevent accidental double-taps.

After a button is pressed, it is disabled for a configurable number of milliseconds before it can be pressed again. The duration is controlled by the `NEXT_PUBLIC_TAP_DEBOUNCE_MS` environment variable (default: **1000 ms**).

```env
# apps/web/.env.local
NEXT_PUBLIC_TAP_DEBOUNCE_MS=1000   # 1 second cooldown (default)
NEXT_PUBLIC_TAP_DEBOUNCE_MS=500    # faster for testing
NEXT_PUBLIC_TAP_DEBOUNCE_MS=2000   # slower for kiosk deployments
```

This is a UX guard only — it does not replace the server-side rate limit (`20 req/s` per `deviceId + cardToken`). Rapid programmatic requests via the API are still subject to the rate limiter.

---

## Caching

| Layer | What | Backend | TTL |
|-------|------|---------|-----|
| Spring Boot | Trips, tap counts, operator details, devices | Redis | 10 min |
| Next.js | Trips list | Next.js Data Cache | Minutes |
| Next.js | Device list | Next.js Data Cache | Hours |

`CacheEvict` on `processTap` ensures the trips cache is invalidated whenever a new journey is created or updated.
