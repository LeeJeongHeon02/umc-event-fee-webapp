package com.dclub.api.dues.domain;

import com.dclub.api.global.common.ApiException;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "dues_rounds")
public class DuesRound {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String title;
    @Column(nullable = false)
    private long amount;
    @Column(name = "due_at", nullable = false)
    private Instant dueAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private DuesRoundStatus status;
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

    protected DuesRound() {}

    public DuesRound(String title, long amount, Instant dueAt, String bankName, String accountNumber,
                     String accountHolder, String kakaoPayReceiveUrl, Instant now) {
        if (amount < 0) throw new IllegalArgumentException("회비는 음수일 수 없습니다.");
        this.title = title;
        this.amount = amount;
        this.dueAt = dueAt;
        this.status = DuesRoundStatus.DRAFT;
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
        this.kakaoPayReceiveUrl = kakaoPayReceiveUrl;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void updateDraft(String title, long amount, Instant dueAt, String bankName, String accountNumber,
                            String accountHolder, String kakaoPayReceiveUrl, long expectedVersion, Instant now) {
        requireVersion(expectedVersion);
        requireDraft();
        if (amount < 0) throw new IllegalArgumentException("회비는 음수일 수 없습니다.");
        this.title = title;
        this.amount = amount;
        this.dueAt = dueAt;
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
        this.kakaoPayReceiveUrl = kakaoPayReceiveUrl;
        this.updatedAt = now;
    }

    public void publish(long expectedVersion, Instant now) {
        requireVersion(expectedVersion);
        requireDraft();
        status = DuesRoundStatus.PUBLISHED;
        updatedAt = now;
    }

    public void requireDeletable(long expectedVersion) {
        requireVersion(expectedVersion);
        requireDraft();
    }

    private void requireDraft() {
        if (status != DuesRoundStatus.DRAFT) {
            throw ApiException.conflict("DUES_ROUND_STATE_CONFLICT", "초안 상태의 회비 차수만 변경할 수 있습니다.");
        }
    }

    private void requireVersion(long expectedVersion) {
        if (version != expectedVersion) {
            throw ApiException.conflict("DUES_ROUND_STATE_CONFLICT", "회비 차수가 변경되었습니다. 새로고침 후 다시 시도해 주세요.");
        }
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public long getAmount() { return amount; }
    public Instant getDueAt() { return dueAt; }
    public DuesRoundStatus getStatus() { return status; }
    public String getBankName() { return bankName; }
    public String getAccountNumber() { return accountNumber; }
    public String getAccountHolder() { return accountHolder; }
    public String getKakaoPayReceiveUrl() { return kakaoPayReceiveUrl; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
