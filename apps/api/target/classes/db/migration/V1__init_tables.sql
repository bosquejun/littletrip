CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE operators (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE operator_devices (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    operator_id UUID NOT NULL REFERENCES operators(id) ON DELETE CASCADE,
    name VARCHAR(20) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'active'
        CHECK (status IN ('active', 'inactive', 'revoked')),
    last_seen_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


CREATE TABLE transit_stops (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE tap_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tap_type VARCHAR(4) NOT NULL
        CHECK (tap_type IN ('ON', 'OFF')),
    stop_id UUID NOT NULL
        REFERENCES transit_stops(id),
    operator_id UUID NOT NULL
        REFERENCES operators(id),
    device_id UUID NOT NULL
        REFERENCES operator_devices(id),
    card_token VARCHAR(64) NOT NULL,
    request_id UUID NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Core query: reconstruct journeys per card
CREATE INDEX idx_tap_events_card_time
ON tap_events(card_token, created_at DESC);

-- Filter by time range (analytics, cleanup, batching)
CREATE INDEX idx_tap_events_created_at
ON tap_events(created_at DESC);

-- Tap type filtering (optional but cheap)
CREATE INDEX idx_tap_events_tap_type
ON tap_events(tap_type);

-- Stop + operator queries (route/operator analytics)
CREATE INDEX idx_tap_events_stop_operator
ON tap_events(stop_id, operator_id);


CREATE TABLE journeys (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    card_token VARCHAR(64) NOT NULL,
    operator_id UUID NOT NULL
        REFERENCES operators(id),
    device_id UUID NOT NULL
        REFERENCES operator_devices(id),
    origin_stop_id UUID
        REFERENCES transit_stops(id),
    destination_stop_id UUID
        REFERENCES transit_stops(id),
    started_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL CHECK (
        status IN ('IN_PROGRESS', 'COMPLETED', 'INCOMPLETE', 'CANCELLED', 'FAILED')
    ),
    fare_amount INTEGER NOT NULL CHECK (fare_amount >= 0),
    request_id UUID NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- reconstruct journeys per card
CREATE INDEX idx_journeys_card_time
ON journeys(card_token, started_at DESC);

-- filter active journeys
CREATE INDEX idx_journeys_status
ON journeys(status);

-- operator-level queries
CREATE INDEX idx_journeys_operator
ON journeys(operator_id);

-- optional: fast lookup for open journeys
CREATE INDEX idx_journeys_active
ON journeys(card_token)
WHERE status = 'in_progress';



CREATE TABLE api_keys (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    key_hash VARCHAR(128) UNIQUE NOT NULL,
    operator_id UUID NOT NULL
        REFERENCES operators(id)
        ON DELETE CASCADE,
    name VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


CREATE TABLE api_key_devices (
    api_key_id UUID NOT NULL
        REFERENCES api_keys(id)
        ON DELETE CASCADE,
    device_id UUID NOT NULL
        REFERENCES operator_devices(id)
        ON DELETE CASCADE,
    PRIMARY KEY (api_key_id, device_id)
);

CREATE TABLE transit_fares (
    stop_a_id UUID NOT NULL REFERENCES transit_stops(id),
    stop_b_id UUID NOT NULL REFERENCES transit_stops(id),

    base_fare_cents INTEGER NOT NULL CHECK (base_fare_cents >= 0),

    PRIMARY KEY (stop_a_id, stop_b_id),

    -- Ensure stop_a_id is always 'smaller' than stop_b_id for consistency
    CONSTRAINT ordered_pair CHECK (stop_a_id < stop_b_id)
);

-- Index for lookup performance
CREATE INDEX idx_transit_fares_lookup ON transit_fares(stop_a_id, stop_b_id);
