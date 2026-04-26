package com.littletrip.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.littletrip.api.model.Journey;
import com.littletrip.api.model.JourneyStatus;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class TripDto {


    private Instant started;

    private Instant finished;

    private long durationSecs;

    private String fromStopId;

    private String toStopId;

    private long chargeAmount;

    private String operatorId;

    private String cardToken;

    private UUID deviceId;

    private String status;

    private UUID id;

    public TripDto() {}

    public static TripDto from(Journey journey) {
        TripDto dto = new TripDto();
        dto.id = journey.getId();
        dto.started = journey.getStartedAt();
        dto.finished = journey.getEndedAt();
        dto.durationSecs = calculateDuration(journey.getStartedAt(), journey.getEndedAt());
        dto.fromStopId = journey.getOriginStop() != null ? journey.getOriginStop().getName() : null;
        dto.toStopId = journey.getDestinationStop() != null ? journey.getDestinationStop().getName() : null;
        dto.chargeAmount = journey.getFareAmount();
        dto.operatorId = journey.getOperator() != null ? journey.getOperator().getName() : null;
        dto.cardToken = journey.getCardToken();
        dto.status = journey.getStatus().name();
        dto.deviceId = journey.getDevice() != null ? journey.getDevice().getId() : null;
        return dto;
    }

    private static long calculateDuration(Instant start, Instant end) {
        if (start == null || end == null) return 0;
        long secs = end.getEpochSecond() - start.getEpochSecond();
        return Math.max(0, secs);
    }

    private static String formatCents(int cents) {
        return String.format("$%.2f", cents / 100.0);
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Instant getStarted() { return started; }
    public void setStarted(Instant started) { this.started = started; }
    public Instant getFinished() { return finished; }
    public void setFinished(Instant finished) { this.finished = finished; }
    public long getDurationSecs() { return durationSecs; }
    public void setDurationSecs(long durationSecs) { this.durationSecs = durationSecs; }
    public String getFromStopId() { return fromStopId; }
    public void setFromStopId(String fromStopId) { this.fromStopId = fromStopId; }
    public String getToStopId() { return toStopId; }
    public void setToStopId(String toStopId) { this.toStopId = toStopId; }
    public long getChargeAmount() { return chargeAmount; }
    public void setChargeAmount(long chargeAmount) { this.chargeAmount = chargeAmount; }
    public String getOperatorId() { return operatorId; }
    public void setOperatorId(String operatorId) { this.operatorId = operatorId; }
    public String getCardToken() { return cardToken; }
    public void setCardToken(String cardToken) { this.cardToken = cardToken; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public UUID getDeviceId() { return deviceId; }
    public void setDeviceId(UUID deviceId) { this.deviceId = deviceId; }
}
