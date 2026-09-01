package com.dclub.api.domain;

import com.dclub.api.common.ApiException;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "payment_obligations", uniqueConstraints = @UniqueConstraint(name = "uk_payment_member_source", columnNames = {"member_id", "source_type", "source_id"}))
public class PaymentObligation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "member_id", nullable = false)
    private Long memberId;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private PaymentType type;
    @Column(nullable = false)
    private long amount;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private PaymentStatus status;
    @Enumerated(EnumType.STRING) @Column(name = "source_type", nullable = false)
    private PaymentSourceType sourceType;
    @Column(name = "source_id", nullable = false)
    private Long sourceId;
    @Column(name = "source_title", nullable = false)
    private String sourceTitle;
    @Column(name = "due_at")
    private Instant dueAt;
    @Column(name = "bank_name")
    private String bankName;
    @Column(name = "account_number")
    private String accountNumber;
    @Column(name = "account_holder")
    private String accountHolder;
    @Column(name = "kakao_pay_receive_url")
    private String kakaoPayReceiveUrl;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    private long version;

    protected PaymentObligation() {}

    public PaymentObligation(Long memberId, PaymentType type, long amount, PaymentSourceType sourceType,
                             Long sourceId, String sourceTitle, Instant dueAt, Instant now) {
        if (amount < 0) throw new IllegalArgumentException("납부 금액은 음수일 수 없습니다.");
        this.memberId = memberId;
        this.type = type;
        this.amount = amount;
        this.status = amount == 0 ? PaymentStatus.NOT_REQUIRED : PaymentStatus.UNPAID;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.sourceTitle = sourceTitle;
        this.dueAt = dueAt;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void setDestination(String bankName, String accountNumber, String accountHolder, String kakaoPayReceiveUrl) {
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
        this.kakaoPayReceiveUrl = kakaoPayReceiveUrl;
    }

    public void report(Instant now) {
        if (status != PaymentStatus.UNPAID && status != PaymentStatus.REJECTED) {
            throw ApiException.conflict("PAYMENT_STATE_CONFLICT", "현재 상태에서는 송금을 신고할 수 없습니다.");
        }
        status = PaymentStatus.REPORTED;
        updatedAt = now;
    }

    public void confirm(Instant now) {
        requireReported();
        status = PaymentStatus.CONFIRMED;
        updatedAt = now;
    }

    public void reject(Instant now) {
        requireReported();
        status = PaymentStatus.REJECTED;
        updatedAt = now;
    }

    public void cancelForEvent(Instant now) {
        status = switch (status) {
            case CONFIRMED -> PaymentStatus.REFUND_PENDING;
            case REFUND_PENDING, REFUNDED -> throw ApiException.conflict(
                    "PAYMENT_STATE_CONFLICT", "이미 환불 처리가 진행 중이거나 완료되었습니다.");
            default -> PaymentStatus.VOID;
        };
        updatedAt = now;
    }

    private void requireReported() {
        if (status != PaymentStatus.REPORTED) {
            throw ApiException.conflict("PAYMENT_STATE_CONFLICT", "확인 대기 상태의 납부만 처리할 수 있습니다.");
        }
    }

    public Long getId() { return id; }
    public Long getMemberId() { return memberId; }
    public PaymentType getType() { return type; }
    public long getAmount() { return amount; }
    public PaymentStatus getStatus() { return status; }
    public PaymentSourceType getSourceType() { return sourceType; }
    public Long getSourceId() { return sourceId; }
    public String getSourceTitle() { return sourceTitle; }
    public Instant getDueAt() { return dueAt; }
    public String getBankName() { return bankName; }
    public String getAccountNumber() { return accountNumber; }
    public String getAccountHolder() { return accountHolder; }
    public String getKakaoPayReceiveUrl() { return kakaoPayReceiveUrl; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
