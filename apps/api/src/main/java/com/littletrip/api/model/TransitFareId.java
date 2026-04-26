package com.littletrip.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class TransitFareId implements Serializable {

    @Column(name = "stop_a_id")
    private UUID stopAId;

    @Column(name = "stop_b_id")
    private UUID stopBId;

    public TransitFareId() {}

    public TransitFareId(UUID stopAId, UUID stopBId) {
        this.stopAId = stopAId;
        this.stopBId = stopBId;
    }

    public UUID getStopAId() { return stopAId; }
    public void setStopAId(UUID stopAId) { this.stopAId = stopAId; }
    public UUID getStopBId() { return stopBId; }
    public void setStopBId(UUID stopBId) { this.stopBId = stopBId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TransitFareId)) return false;
        TransitFareId that = (TransitFareId) o;
        return Objects.equals(stopAId, that.stopAId) && Objects.equals(stopBId, that.stopBId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stopAId, stopBId);
    }
}
