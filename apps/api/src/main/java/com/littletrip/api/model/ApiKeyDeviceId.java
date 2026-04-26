package com.littletrip.api.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class ApiKeyDeviceId implements Serializable {

    private UUID apiKeyId;
    private UUID deviceId;

    public ApiKeyDeviceId() {}

    public ApiKeyDeviceId(UUID apiKeyId, UUID deviceId) {
        this.apiKeyId = apiKeyId;
        this.deviceId = deviceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApiKeyDeviceId that = (ApiKeyDeviceId) o;
        return Objects.equals(apiKeyId, that.apiKeyId) && Objects.equals(deviceId, that.deviceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(apiKeyId, deviceId);
    }
}