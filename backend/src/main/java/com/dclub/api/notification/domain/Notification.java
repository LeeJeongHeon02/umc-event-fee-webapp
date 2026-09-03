package com.dclub.api.notification.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "member_id", nullable = false)
    private Long memberId;
    @Column(nullable = false)
    private String title;
    @Column(nullable = false, length = 500)
    private String body;
    @Column(name = "link_url", length = 500)
    private String linkUrl;
    @Column(name = "read_at")
    private Instant readAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Notification() {}
    public Notification(Long memberId, String title, String body, String linkUrl, Instant now) {
        this.memberId = memberId; this.title = title; this.body = body; this.linkUrl = linkUrl; this.createdAt = now;
    }
    public void markRead(Instant now) { if (readAt == null) readAt = now; }
    public Long getId() { return id; }
    public Long getMemberId() { return memberId; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getLinkUrl() { return linkUrl; }
    public Instant getReadAt() { return readAt; }
    public Instant getCreatedAt() { return createdAt; }
}
