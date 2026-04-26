package com.littletrip.api.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "api_key_devices")
@IdClass(ApiKeyDeviceId.class)
public class ApiKeyDevice {

    @Id
    @Column(name = "api_key_id")
    private UUID apiKeyId;

    @Id
    @Column(name = "device_id")
    private UUID deviceId;

    public ApiKeyDevice() {}

    public ApiKeyDevice(UUID apiKeyId, UUID deviceId) {
        this.apiKeyId = apiKeyId;
        this.deviceId = deviceId;
    }

    public UUID getApiKeyId() { return apiKeyId; }
    public void setApiKeyId(UUID apiKeyId) { this.apiKeyId = apiKeyId; }
    public UUID getDeviceId() { return deviceId; }
    public void setDeviceId(UUID deviceId) { this.deviceId = deviceId; }
}