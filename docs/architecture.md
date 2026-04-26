# Architecture

## Overview

```
Transit Device
    │
    ▼ POST /taps  (X-API-Key)
┌───────────────────────────────┐        ┌──────────────┐
│  Spring Boot API  (port 8081) │◄──────►│  PostgreSQL  │
│  - Tap ingestion              │        └──────────────┘
│  - Journey state machine      │        ┌──────────────┐
│  - Fare calculation           │◄──────►│    Redis     │
│  - Redis caching              │        │  (cache +    │
│  - Rate limiting              │        │  rate limit) │
└───────────────────────────────┘        └──────────────┘
    ▲
    │ HTTP (X-API-Key / Basic auth)
    ▼
┌───────────────────────────────┐
│  Next.js Dashboard (port 3000)│
│  - Trip viewer                │
│  - Tap simulator              │
│  - Merchant KPI cards         │
└───────────────────────────────┘
    │
    ▼ metrics scrape
┌──────────────┐    ┌─────────────────┐
│  Prometheus  │───►│  Grafana :4000  │
│  :9090       │    │                 │
└──────────────┘    └─────────────────┘
```

---

## Monorepo Layout

```
little-trip/
├── apps/
│   ├── api/          Spring Boot 3.2 (Java 17)
│   └── web/          Next.js 16 (React 19, Tailwind v4)
├── packages/
│   ├── eslint-config/
│   └── typescript-config/
├── docker/
│   ├── grafana/      Grafana provisioning
│   └── prometheus/   prometheus.yml
├── k6/               Load / stress tests
├── docker-compose.yml
├── docker-start.sh   One-command full-stack startup
└── .env.example
```

---

## Spring Boot API

### Layer structure

```
controller/        HTTP layer — validates auth, delegates to services
  admin/           Basic-auth endpoints for platform management
service/           Business logic (TapService, TripService, FareService, …)
repository/        Spring Data JPA repositories
model/             JPA entities
dto/               Request/response shapes
annotation/        @RateLimit, @RequiresApiKey, @RequiresAdmin
config/            Security, WebConfig, RateLimitAspect, OpenAPI
exception/         Domain exceptions (InvalidDeviceException, DuplicateEventException)
```

### Key data flow

```
POST /taps
  → ApiKeyAuthFilter       resolves X-API-Key → ApiKey principal
  → RateLimitAspect        checks (deviceId, cardToken) sliding window in Redis
  → TapController          extracts ApiKey, delegates to TapService
  → TapService             validates device linkage, persists TapEvent
  → TripService            runs journey state machine, calculates fare
  → JourneyRepository      saves Journey, evicts "trips" cache
```

### Database schema (simplified)

```
operators
  └─ operator_devices
  └─ api_keys
       └─ api_key_devices ──► operator_devices

transit_stops
  └─ transit_fares (stop_a_id, stop_b_id, base_fare_cents)

tap_events   (card_token, stop_id, device_id, operator_id, tap_type, request_id UNIQUE)
journeys     (card_token, origin_stop_id, destination_stop_id, status, fare_amount, request_id UNIQUE)
```

Migrations are managed by **Flyway** (`V1__init_tables.sql`, `V2__init_seeds.sql`). Schema is validated on startup (`ddl-auto: validate`).

### Security

Spring Security is configured with two authentication providers:

| Path pattern | Mechanism | Principal type |
|--------------|-----------|----------------|
| `/taps/**`, `/trips/**`, `/operator/**` | `X-API-Key` header | `ApiKey` |
| `/admin/**` | HTTP Basic | Spring `UserDetails` |
| `/actuator/**` | None (internal) | — |
| `/swagger-ui.html`, `/docs/**` | None | — |

---

## Next.js Dashboard

### Patterns

- **Server Components** call `lib/api.ts` directly (no client-side fetch), using Next.js `"use cache"` + `cacheTag` / `cacheLife` for data caching.
- **Client Components** are suffixed `*Client.tsx`; server wrappers `*Server.tsx`.
- **Feature slices** live in `features/` — `trips/`, `simulator/`, `merchant/` — each containing both server and client components.

### Caching strategy

| Data | `cacheLife` |
|------|------------|
| Trips list | `"minutes"` |
| Device list | `"hours"` |

### Server actions

`app/actions/taps.ts` wraps `POST /taps` as a Next.js Server Action used by the tap simulator. It calls `revalidateTag("trips")` after a successful tap to keep the trip list fresh.

---

## Observability

### Metrics

Spring Boot Actuator exposes Prometheus metrics at `/actuator/prometheus`. Custom counters:

| Metric | Tags | Description |
|--------|------|-------------|
| `api.rate.limit.requests` | `endpoint`, `result` (allowed/throttled) | Rate limit decisions |

Standard Micrometer metrics (JVM, HTTP request latency histograms, connection pool) are also exported.

### Prometheus

Config in `docker/prometheus/prometheus.yml`. Scrapes the API every 15 s.

### Grafana

Provisioned dashboards and datasources in `docker/grafana/provisioning/`. Access at `http://localhost:4000` (admin / admin).

---

## Load Testing

k6 scripts in `k6/`:

```sh
cd k6
./run.sh          # runs stress-test.js against http://localhost:8081
```

The stress test simulates concurrent tap ON/OFF sequences against the `/taps` endpoint to validate rate limiting and journey state machine behaviour under load.
