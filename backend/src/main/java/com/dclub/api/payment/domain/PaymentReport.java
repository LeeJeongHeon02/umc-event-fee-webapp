package com.dclub.api.payment.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "payment_reports")
public class PaymentReport {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "payment_obligation_id", nullable = false)
    private Long paymentObligationId;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private PaymentMethod method;
    @Column(name = "sender_name", nullable = false)
    private String senderName;
    @Column(name = "transferred_at")
    private Instant transferredAt;
    private String note;
    @Column(name = "reported_at", nullable = false)
    private Instant reportedAt;

    protected PaymentReport() {}

    public PaymentReport(Long paymentObligationId, PaymentMethod method, String senderName,
                         Instant transferredAt, String note, Instant reportedAt) {
        this.paymentObligationId = paymentObligationId;
        this.method = method;
        this.senderName = senderName;
        this.transferredAt = transferredAt;
        this.note = note;
        this.reportedAt = reportedAt;
    }

    public Long getId() { return id; }
    public Long getPaymentObligationId() { return paymentObligationId; }
    public PaymentMethod getMethod() { return method; }
    public String getSenderName() { return senderName; }
    public Instant getTransferredAt() { return transferredAt; }
    public String getNote() { return note; }
    public Instant getReportedAt() { return reportedAt; }
}
