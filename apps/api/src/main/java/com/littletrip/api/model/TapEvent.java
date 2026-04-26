package com.littletrip.api.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tap_events")
public class TapEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tap_type", nullable = false, length = 4)
    private TapType tapType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stop_id", nullable = false)
    private TransitStop stop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_id", nullable = false)
    private Operator operator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private OperatorDevice device;

    @Column(name = "card_token", nullable = false, length = 64)
    private String cardToken;

    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public TapEvent() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public TapType getTapType() { return tapType; }
    public void setTapType(TapType tapType) { this.tapType = tapType; }
    public TransitStop getStop() { return stop; }
    public void setStop(TransitStop stop) { this.stop = stop; }
    public Operator getOperator() { return operator; }
    public void setOperator(Operator operator) { this.operator = operator; }
    public OperatorDevice getDevice() { return device; }
    public void setDevice(OperatorDevice device) { this.device = device; }
    public String getCardToken() { return cardToken; }
    public void setCardToken(String cardToken) { this.cardToken = cardToken; }
    public UUID getRequestId() { return requestId; }
    public void setRequestId(UUID requestId) { this.requestId = requestId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}