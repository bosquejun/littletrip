package com.littletrip.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.littletrip.api.model.TapEvent;
import com.littletrip.api.model.TapType;

import java.time.Instant;
import java.util.UUID;

public class TapEventDto {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("tapType")
    private TapType tapType;

    @JsonProperty("stopId")
    private UUID stopId;

    @JsonProperty("stopName")
    private String stopName;

    @JsonProperty("operatorId")
    private UUID operatorId;

    @JsonProperty("operatorName")
    private String operatorName;

    @JsonProperty("deviceId")
    private UUID deviceId;

    @JsonProperty("cardToken")
    private String cardToken;

    @JsonProperty("requestId")
    private UUID requestId;

    @JsonProperty("createdAt")
    private Instant createdAt;

    public TapEventDto() {}

    public static TapEventDto from(TapEvent tapEvent) {
        TapEventDto dto = new TapEventDto();
        dto.id = tapEvent.getId();
        dto.tapType = tapEvent.getTapType();
        dto.cardToken = tapEvent.getCardToken();
        dto.requestId = tapEvent.getRequestId();
        dto.createdAt = tapEvent.getCreatedAt();
        if (tapEvent.getStop() != null) {
            dto.stopId = tapEvent.getStop().getId();
            dto.stopName = tapEvent.getStop().getName();
        }
        if (tapEvent.getOperator() != null) {
            dto.operatorId = tapEvent.getOperator().getId();
            dto.operatorName = tapEvent.getOperator().getName();
        }
        if (tapEvent.getDevice() != null) {
            dto.deviceId = tapEvent.getDevice().getId();
        }
        return dto;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public TapType getTapType() { return tapType; }
    public void setTapType(TapType tapType) { this.tapType = tapType; }
    public UUID getStopId() { return stopId; }
    public void setStopId(UUID stopId) { this.stopId = stopId; }
    public String getStopName() { return stopName; }
    public void setStopName(String stopName) { this.stopName = stopName; }
    public UUID getOperatorId() { return operatorId; }
    public void setOperatorId(UUID operatorId) { this.operatorId = operatorId; }
    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
    public UUID getDeviceId() { return deviceId; }
    public void setDeviceId(UUID deviceId) { this.deviceId = deviceId; }
    public String getCardToken() { return cardToken; }
    public void setCardToken(String cardToken) { this.cardToken = cardToken; }
    public UUID getRequestId() { return requestId; }
    public void setRequestId(UUID requestId) { this.requestId = requestId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}