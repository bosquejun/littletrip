package com.littletrip.api.dto;

import com.littletrip.api.model.TransitFare;
import com.littletrip.api.model.TransitFareId;
import java.util.UUID;

public record TransitFareDto(
    UUID stopAId,
    String stopAName,
    UUID stopBId,
    String stopBName,
    int baseFareCents
) {
    public static TransitFareDto from(TransitFare fare) {
        return new TransitFareDto(
            fare.getId().getStopAId(),
            fare.getStopA().getName(),
            fare.getId().getStopBId(),
            fare.getStopB().getName(),
            fare.getBaseFareCents()
        );
    }
}