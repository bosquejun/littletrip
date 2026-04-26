# Design Decisions

This document explains where the implementation intentionally diverges from the original challenge spec and the reasoning behind each change.

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
