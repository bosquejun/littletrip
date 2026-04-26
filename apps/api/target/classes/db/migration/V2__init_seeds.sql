-- Ensure required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =========================================
-- Insert Operators + Devices in one flow
-- =========================================
WITH inserted_operators AS (
    INSERT INTO operators (id, name, created_at)
    VALUES
        (uuid_generate_v4(), 'Metro Transit Corp', NOW()),
        (uuid_generate_v4(), 'CityLink Transport Services', NOW()),
        (uuid_generate_v4(), 'UrbanMove Operators Inc.', NOW()),
        (uuid_generate_v4(), 'RapidBus Co.', NOW()),
        (uuid_generate_v4(), 'GreenLine Transit', NOW()),
        (uuid_generate_v4(), 'NorthStar Mobility', NOW()),
        (uuid_generate_v4(), 'BlueRoute Transport', NOW()),
        (uuid_generate_v4(), 'Sunrise Commuter Services', NOW())
    RETURNING id, name
)

INSERT INTO operator_devices (
    id,
    operator_id,
    name,
    status,
    last_seen_at,
    created_at
)
SELECT
    uuid_generate_v4(),
    io.id,
    d.name,
    d.status,
    NOW(),
    NOW()
FROM inserted_operators io
JOIN (
    VALUES
        ('Metro Transit Corp', 'METRO-DEV-1', 'active'),
        ('Metro Transit Corp', 'METRO-DEV-2', 'active'),

        ('CityLink Transport Services', 'CITY-DEV-1', 'active'),
        ('CityLink Transport Services', 'CITY-DEV-2', 'inactive'),

        ('UrbanMove Operators Inc.', 'URBAN-DEV-1', 'active'),
        ('RapidBus Co.', 'RAPID-DEV-1', 'active'),
        ('GreenLine Transit', 'GREEN-DEV-1', 'active'),
        ('NorthStar Mobility', 'NORTH-DEV-1', 'active'),
        ('BlueRoute Transport', 'BLUE-DEV-1', 'active'),
        ('Sunrise Commuter Services', 'SUN-DEV-1', 'active')
) AS d(operator_name, name, status)
ON io.name = d.operator_name;


-- =========================================
-- Insert API Keys
-- =========================================
INSERT INTO api_keys (
    id,
    key_hash,
    operator_id,
    name,
    active,
    created_at,
    updated_at
)
SELECT
    uuid_generate_v4(),
    encode(gen_random_bytes(32), 'hex'),
    o.id,
    o.name || ' Key',
    TRUE,
    NOW(),
    NOW()
FROM operators o;


-- =========================================
-- Link API Keys to Devices
-- =========================================
INSERT INTO api_key_devices (api_key_id, device_id)
SELECT
    ak.id,
    od.id
FROM api_keys ak
JOIN operator_devices od
    ON od.operator_id = ak.operator_id;


-- =========================================
-- Insert Transit Stops and Fares
-- =========================================
WITH inserted_stops AS (
    INSERT INTO transit_stops (id, name)
    VALUES
        (uuid_generate_v4(), 'North Station'),
        (uuid_generate_v4(), 'Central Avenue'),
        (uuid_generate_v4(), 'South Terminal'),
        (uuid_generate_v4(), 'Airport Hub')
    RETURNING id, name
),
s AS (
    SELECT
        (SELECT id FROM inserted_stops WHERE name = 'North Station') as north,
        (SELECT id FROM inserted_stops WHERE name = 'Central Avenue') as central,
        (SELECT id FROM inserted_stops WHERE name = 'South Terminal') as south,
        (SELECT id FROM inserted_stops WHERE name = 'Airport Hub') as airport
)
INSERT INTO transit_fares (stop_a_id, stop_b_id, base_fare_cents)
VALUES
    -- Original 3 pairs
    (LEAST((SELECT north FROM s), (SELECT central FROM s)), GREATEST((SELECT north FROM s), (SELECT central FROM s)), 325),
    (LEAST((SELECT central FROM s), (SELECT south FROM s)),   GREATEST((SELECT central FROM s), (SELECT south FROM s)),   550),
    (LEAST((SELECT north FROM s), (SELECT south FROM s)),     GREATEST((SELECT north FROM s), (SELECT south FROM s)),     730),

    -- Airport Hub Connections
    (LEAST((SELECT north FROM s), (SELECT airport FROM s)),   GREATEST((SELECT north FROM s), (SELECT airport FROM s)),   1200), -- North to Airport
    (LEAST((SELECT central FROM s), (SELECT airport FROM s)), GREATEST((SELECT central FROM s), (SELECT airport FROM s)), 850),  -- Central to Airport
    (LEAST((SELECT south FROM s), (SELECT airport FROM s)),   GREATEST((SELECT south FROM s), (SELECT airport FROM s)),   450);  -- South to Airport
