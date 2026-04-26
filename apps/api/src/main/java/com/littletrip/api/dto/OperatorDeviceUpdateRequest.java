package com.littletrip.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OperatorDeviceUpdateRequest(
    UUID id,
    String name,
    DeviceStatus status,
    OffsetDateTime lastSeenAt
) {
    public enum DeviceStatus {
        active, inactive, revoked
    }
}
