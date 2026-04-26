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

**Why:** "Company" is ambiguous — it could refer to the transit authority, the software vendor, or the device manufacturer. "Operator" is the standard term in transit systems for the entity that runs a service on a network. It also better reflects the data model: operators own devices and API keys, and all queries are scoped to an operator.

---

### `BusID` → `deviceId`

| Spec | Implementation |
|------|---------------|
| `BusID: "Bus37"` | `deviceId: <UUID>` |

**Why:** Coupling the identifier to a specific vehicle type (`Bus`) makes the model brittle. The same tap infrastructure applies to trams, ferries, light rail, and any other transit mode. `deviceId` is mode-agnostic and refers to the physical tap reader, not the vehicle. This also opens the door to one device serving multiple routes or being redeployed across vehicle types without a schema change.

---

### `PAN` → `cardToken`

| Spec | Implementation |
|------|---------------|
| `PAN: "5500005555555559"` | `cardToken: "tok_abc123"` |

**Why:** PAN (Primary Account Number) is regulated card data. Storing or logging raw PANs without PCI DSS compliance controls is a liability. By accepting an opaque `cardToken` instead, the system never touches raw card numbers — tokenisation is the responsibility of the tap reader hardware or a payment gateway upstream. The API treats `cardToken` as an arbitrary string used only for grouping tap events and journeys; it never interprets or validates the format.

---

### `ChargeAmount: "$3.25"` → `fareAmount: 325` (cents)

| Spec | Implementation |
|------|---------------|
| `"ChargeAmount": "$3.25"` | `"fareAmount": 325` |

**Why:** Formatted currency strings like `"$3.25"` are a display concern, not a storage concern. Storing amounts as strings creates several problems:

- **Floating-point risk** — parsing `"$3.25"` into a `float` and doing arithmetic on it can produce rounding errors that compound across many transactions.
- **Currency symbol coupling** — the `$` symbol hardcodes a single currency and locale; internationalisation requires a schema change.
- **Sorting and aggregation** — string-stored amounts cannot be summed or compared correctly at the database level.

Storing as an unsigned integer in the **smallest unit (cents)** is the standard approach used by payment processors (Stripe, Adyen, etc.). The web dashboard divides by 100 purely for display in `mapTripDto`, keeping the conversion in one place.

