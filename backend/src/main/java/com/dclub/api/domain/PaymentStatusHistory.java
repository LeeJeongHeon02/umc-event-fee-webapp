package com.dclub.api.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "payment_status_history")
public class PaymentStatusHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "payment_obligation_id", nullable = false)
    private Long paymentObligationId;
    @Enumerated(EnumType.STRING) @Column(name = "from_status")
    private PaymentStatus fromStatus;
    @Enumerated(EnumType.STRING) @Column(name = "to_status", nullable = false)
    private PaymentStatus toStatus;
    private String reason;
    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    protected PaymentStatusHistory() {}

    public PaymentStatusHistory(Long paymentObligationId, PaymentStatus fromStatus, PaymentStatus toStatus,
                                String reason, Instant changedAt) {
        this.paymentObligationId = paymentObligationId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.reason = reason;
        this.changedAt = changedAt;
    }

    public PaymentStatus getFromStatus() { return fromStatus; }
    public PaymentStatus getToStatus() { return toStatus; }
    public String getReason() { return reason; }
    public Instant getChangedAt() { return changedAt; }
}
