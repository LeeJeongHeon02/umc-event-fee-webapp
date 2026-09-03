package com.dclub.api.payment.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "payment_settings")
public class PaymentSetting {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "bank_name", nullable = false)
    private String bankName;
    @Column(name = "account_number", nullable = false)
    private String accountNumber;
    @Column(name = "account_holder", nullable = false)
    private String accountHolder;
    @Column(name = "kakao_pay_receive_url")
    private String kakaoPayReceiveUrl;
    @Column(nullable = false)
    private boolean active;
    @Column(name = "created_by", nullable = false)
    private Long createdBy;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PaymentSetting() {}

    public PaymentSetting(String bankName, String accountNumber, String accountHolder,
                          String kakaoPayReceiveUrl, Long createdBy, Instant now) {
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
        this.kakaoPayReceiveUrl = kakaoPayReceiveUrl;
        this.active = true;
        this.createdBy = createdBy;
        this.createdAt = now;
    }

    public void deactivate() { active = false; }
    public Long getId() { return id; }
    public String getBankName() { return bankName; }
    public String getAccountNumber() { return accountNumber; }
    public String getAccountHolder() { return accountHolder; }
    public String getKakaoPayReceiveUrl() { return kakaoPayReceiveUrl; }
    public boolean isActive() { return active; }
    public Long getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
