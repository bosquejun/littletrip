# API Reference

Base URL: `http://localhost:8081/api/v1`

Interactive docs: `http://localhost:8081/swagger-ui.html`

---

## Authentication

### Operator endpoints — `X-API-Key` header

All operator-facing endpoints (`/taps`, `/trips`, `/operator/*`) require a valid API key passed as an HTTP header:

```
X-API-Key: <keyHash>
```

The `keyHash` is a 64-character hex string seeded into the `api_keys` table (see [setup](setup.md)). Each API key is scoped to a single operator — queries automatically return only that operator's data.

**How it works internally:**

1. `ApiKeyAuthFilter` intercepts every request and reads the `X-API-Key` header.
2. The filter looks up the hash in the `api_keys` table and resolves the associated `Operator`.
3. A Spring Security `ApiKeyAuthentication` principal is set on the `SecurityContext`.
4. Controllers extract `auth.getPrincipal()` as an `ApiKey` and scope queries to `apiKey.getOperator().getId()`.

If the header is missing or the key is not found, the API returns `403 Forbidden`.

### Admin endpoints — HTTP Basic auth

Endpoints under `/admin/*` require HTTP Basic auth:

```
Authorization: Basic <base64(username:password)>
```

Credentials are configured via `ADMIN_USERNAME` / `ADMIN_PASSWORD` environment variables.

### Device authorization

When a tap event arrives, `TapService` performs a second authorization check: it verifies that the submitting API key is linked to the specific device in the `api_key_devices` join table. This prevents one operator's key from submitting events for another operator's device. If the check fails, the API returns `400 InvalidDeviceException`.

---

## Operator Endpoints

All require `X-API-Key`.

### `POST /taps`

Ingest a tap ON or OFF event from a transit device.

**Rate limit:** 20 requests / second per `(deviceId, cardToken)` pair.

**Request body:**

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "deviceId": "550e8400-e29b-41d4-a716-446655440001",
  "stopId": "550e8400-e29b-41d4-a716-446655440002",
  "cardToken": "card-abc-123",
  "tapType": "ON",
  "dateTimeUtc": "2024-01-15T09:30:00Z"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Idempotency key — duplicate IDs are rejected with `409` |
| `deviceId` | UUID | Must be linked to the authenticating API key |
| `stopId` | UUID | Must be a known transit stop |
| `cardToken` | string | Transit card identifier (opaque token, max 64 chars) |
| `tapType` | `ON` \| `OFF` | Direction of the tap |
| `dateTimeUtc` | ISO-8601 | Event timestamp |

**Response `201`:**

```json
{
  "journeyId": "550e8400-e29b-41d4-a716-446655440003",
  "status": "IN_PROGRESS",
  "chargeAmount": 0
}
```

`chargeAmount` is in **cents** (divide by 100 for display).

---

### `GET /taps`

List tap events for the authenticated operator.

**Query params:** `cardToken`, `deviceId`, `page` (default 0), `size` (default 20).

---

### `GET /trips`

List journeys for the authenticated operator.

**Query params:** `cardToken`, `page` (default 1), `size` (default 20).

---

### `GET /trips/{id}`

Get a single journey by UUID.

---

### `GET /operator`

Get the operator profile for the authenticated API key.

---

### `GET /operator/devices`

List devices belonging to the authenticated operator.

**Query params:** `page` (default 1), `size` (default 20).

---

### `PUT /operator`

Update the operator's name.

**Request body:** `{ "name": "New Name" }`

---

## Admin Endpoints

All require HTTP Basic auth. Base path: `/admin`.

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/admin/api-keys` | List all API keys |
| `POST` | `/admin/api-keys` | Create an API key |
| `DELETE` | `/admin/api-keys/{id}` | Revoke an API key |
| `GET` | `/admin/operators` | List all operators |
| `POST` | `/admin/operators` | Create an operator |
| `PUT` | `/admin/operators/{id}` | Update an operator |
| `DELETE` | `/admin/operators/{id}` | Delete an operator |
| `GET` | `/admin/devices` | List all devices |
| `POST` | `/admin/devices` | Create a device |
| `PUT` | `/admin/devices/{id}` | Update a device |
| `DELETE` | `/admin/devices/{id}` | Delete a device |
| `GET` | `/admin/fares` | List transit fares |
| `POST` | `/admin/fares` | Create a fare pair |
| `PUT` | `/admin/fares/{id}` | Update a fare |
| `DELETE` | `/admin/fares/{id}` | Delete a fare |

---

## Rate Limiting

Rate limiting is implemented with a Redis-backed sliding window counter via the `@RateLimit` annotation and `RateLimitAspect`.

**Current limits:**

| Endpoint | Limit | Window | Key |
|----------|-------|--------|-----|
| `POST /taps` | 20 req | per second | `(deviceId, cardToken)` |
| `GET /trips` | 100 req | per minute | API key |
| `GET /taps` | 100 req | per minute | API key |
| `GET /operator/*` | 100 req | per minute | API key |

**Response headers on every request:**

```
X-RateLimit-Limit:     100
X-RateLimit-Remaining: 87
X-RateLimit-Reset:     1705312860
```

When the limit is exceeded the API returns:

```
HTTP 429 Too Many Requests
Retry-After: 60

{"error":"Rate limit exceeded","retryAfter":60}
```

**Fail-open:** If Redis is unavailable, the aspect logs a warning and allows the request rather than rejecting it.

---

## Error Responses

All errors follow a standard JSON envelope:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Device not authorized for this API key"
}
```

| HTTP status | Cause |
|-------------|-------|
| `400` | Validation failure, unknown device/stop, or bad request body |
| `403` | Missing or invalid `X-API-Key` |
| `404` | Resource not found |
| `409` | Duplicate tap event (`request_id` already processed) |
| `429` | Rate limit exceeded |
| `500` | Unexpected server error |

---

## Idempotency

The `id` field in `POST /taps` is stored as a unique `request_id` in the database. Submitting the same UUID twice returns `409 Conflict`, preventing double-charging.
