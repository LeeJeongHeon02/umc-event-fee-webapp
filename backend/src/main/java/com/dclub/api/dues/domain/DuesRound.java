package com.dclub.api.dues.domain;

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
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    private long version;

    protected DuesRound() {}

    public DuesRound(String title, long amount, Instant dueAt, DuesRoundStatus status, Instant now) {
        if (amount < 0) throw new IllegalArgumentException("회비는 음수일 수 없습니다.");
        this.title = title;
        this.amount = amount;
        this.dueAt = dueAt;
        this.status = status;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public long getAmount() { return amount; }
    public Instant getDueAt() { return dueAt; }
    public DuesRoundStatus getStatus() { return status; }
}
