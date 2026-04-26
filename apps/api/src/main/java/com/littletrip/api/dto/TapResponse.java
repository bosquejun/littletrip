package com.littletrip.api.dto;

import com.littletrip.api.model.JourneyStatus;
import java.util.UUID;

public class TapResponse {

    private UUID tripId;
    private JourneyStatus status;
    private Integer charge;

    public TapResponse() {}

    public TapResponse(UUID tripId, JourneyStatus status, Integer charge) {
        this.tripId = tripId;
        this.status = status;
        this.charge = charge;
    }

    public UUID getTripId() { return tripId; }
    public void setTripId(UUID tripId) { this.tripId = tripId; }
    public JourneyStatus getStatus() { return status; }
    public void setStatus(JourneyStatus status) { this.status = status; }
    public Integer getCharge() { return charge; }
    public void setCharge(Integer charge) { this.charge = charge; }
}