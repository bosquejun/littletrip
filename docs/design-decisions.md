# Design Decisions

This document explains where the implementation intentionally diverges from the original challenge spec and the reasoning behind each change. It also captures assumptions, current limitations, and future architectural directions.

---

## Assumptions

- **Single-instance deployment**: The current implementation runs as a single-node service. Horizontal scaling via multiple instances is not yet implemented.
- **Synchronous processing**: All fare transactions are processed synchronously—request comes in, fare is calculated, response is returned inline.
- **Direct database access**: The API communicates directly with PostgreSQL. No intermediate message broker in the current architecture.
- **Trusted internal network**: Assumes API is deployed behind a gateway/load balancer in a controlled environment. No mTLS between services.
- **Idempotent operations**: Duplicate transaction requests with the same `deviceId` + timestamp window are considered idempotent (handled at DB constraint level).

---

## Current Limitations

### Horizontal Scalability

The current architecture is **vertically scaled only**:

```
[Load Balancer] → [API Instance] → [PostgreSQL]
```

- Single API instance handles all requests
- Database becomes the bottleneck under high load
- No way to scale read/write independently
- Connection pool limits constrain concurrency

### Reliability

- **No message durability**: If the API crashes mid-request, in-flight transactions are lost
- **No retry logic**: Failed requests are not automatically retried
- **No dead-letter handling**: Poison messages have nowhere to go
- **Single point of failure**: Database downtime = total service outage

---

## Future Architecture: Message Queue

To achieve distributed architecture and robustness, the system should evolve toward:

```
[Device] → [API] → [Message Queue] → [Processor] → [Database]
                    ↓
              [Dead Letter Queue]
```

### Recommended: Apache Kafka

| Aspect | Rationale |
|--------|-----------|
| **Durability** | Messages persisted to disk, survive broker restarts |
| **Ordering** | Per-partition ordering enables exactly-once semantics |
| **Replayability** | Consumer groups can replay from offsets for reprocessing |
| **Scalability** | Partition-based horizontal scaling |
| **Ecosystem** | Rich connector ecosystem for analytics/pipelines |

### Alternative: RabbitMQ

| Aspect | Rationale |
|--------|-----------|
| **Simpler ops** | Easier to operate for smaller scale |
| **Flexible routing** | Complex routing patterns via exchanges |
| **Lower latency** | Better for low-latency requirements |
| **Smart consumers** | Push-based delivery model |

---

## Original Spec vs. Implementation

The challenge defined the input/output data shapes (using JSON examples) with fields like `CompanyId`, `BusID`, `PAN`, and `ChargeAmount`. The implementation renames several of these fields. Each change is documented below.

---

### `CompanyId` → `operatorId`

| Spec | Implementation |
|------|---------------|
| `CompanyId: "Company1"` | `operatorId: <UUID>` |

**Why:** “Company” is too vague. “Operator” matches transit terminology and aligns with how we scope devices + API keys.

---

### `BusID` → `deviceId`

| Spec | Implementation |
|------|---------------|
| `BusID: "Bus37"` | `deviceId: <UUID>` |

**Why:** “Bus” is too specific. The identifier actually represents the tap device, not the vehicle.
Using deviceId keeps it flexible across different transport modes and avoids schema changes later.

---

### `PAN` → `cardToken`

| Spec | Implementation |
|------|---------------|
| `PAN: "5500005555555559"` | `cardToken: "tok_abc123"` |

**Why:** Using raw card numbers directly introduces unnecessary risk and tight coupling to payment-specific formats. cardToken keeps the system decoupled by treating the value as a simple identifier. The API doesn’t need to know anything about the underlying card—only that it consistently represents the same user/payment source. This also lets upstream systems handle any sensitive data without affecting this service.

---

### `ChargeAmount: "$3.25"` → `fareAmount: 325` (cents)

| Spec | Implementation |
|------|---------------|
| `"ChargeAmount": "$3.25"` | `"fareAmount": 325` |

**Why:** Amounts shouldn’t be stored as formatted strings.
Using an integer (in cents) avoids rounding issues and makes calculations and comparisons reliable.
Formatting is handled at the UI level.
