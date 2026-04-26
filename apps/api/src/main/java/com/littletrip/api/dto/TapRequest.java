package com.littletrip.api.dto;

import com.littletrip.api.model.TapType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;

public class TapRequest {

    private static final String UUID_REGEX =
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

    @NotBlank(message = "id is required")
    @Pattern(regexp = UUID_REGEX, message = "id must be a valid UUID")
    private String id;

    @NotNull(message = "dateTimeUtc is required")
    private Instant dateTimeUtc;

    @NotNull(message = "tapType is required")
    private TapType tapType;

    @NotBlank(message = "stopId is required")
    @Pattern(regexp = UUID_REGEX, message = "stopId must be a valid UUID")
    private String stopId;

    @NotBlank(message = "deviceId is required")
    private String deviceId;

    @NotBlank(message = "cardToken is required")
    private String cardToken;

    public TapRequest() {}

    public TapRequest(String id, Instant dateTimeUtc, TapType tapType, String stopId,
                  String deviceId, String cardToken) {
        this.id = id;
        this.dateTimeUtc = dateTimeUtc;
        this.tapType = tapType;
        this.stopId = stopId;
        this.deviceId = deviceId;
        this.cardToken = cardToken;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Instant getDateTimeUtc() { return dateTimeUtc; }
    public void setDateTimeUtc(Instant dateTimeUtc) { this.dateTimeUtc = dateTimeUtc; }
    public TapType getTapType() { return tapType; }
    public void setTapType(TapType tapType) { this.tapType = tapType; }
    public String getStopId() { return stopId; }
    public void setStopId(String stopId) { this.stopId = stopId; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getCardToken() { return cardToken; }
    public void setCardToken(String cardToken) { this.cardToken = cardToken; }
}
