package com.littletrip.api.model;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "transit_fares")
public class TransitFare implements Serializable {

    private static final long serialVersionUID = 1L;

    @EmbeddedId
    private TransitFareId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("stopAId")
    @JoinColumn(name = "stop_a_id")
    private TransitStop stopA;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("stopBId")
    @JoinColumn(name = "stop_b_id")
    private TransitStop stopB;

    @Column(name = "base_fare_cents", nullable = false)
    private int baseFareCents;

    public TransitFare() {}

    public TransitFareId getId() { return id; }
    public void setId(TransitFareId id) { this.id = id; }
    public TransitStop getStopA() { return stopA; }
    public void setStopA(TransitStop stopA) { this.stopA = stopA; }
    public TransitStop getStopB() { return stopB; }
    public void setStopB(TransitStop stopB) { this.stopB = stopB; }
    public int getBaseFareCents() { return baseFareCents; }
    public void setBaseFareCents(int baseFareCents) { this.baseFareCents = baseFareCents; }
}
