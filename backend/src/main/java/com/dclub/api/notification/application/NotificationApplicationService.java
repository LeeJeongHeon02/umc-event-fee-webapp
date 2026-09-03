package com.dclub.api.notification.application;

import com.dclub.api.global.common.ApiException;
import com.dclub.api.global.presentation.ApiDtos.NotificationItem;
import com.dclub.api.global.presentation.ApiDtos.NotificationResponse;
import com.dclub.api.global.security.CurrentMemberProvider;
import com.dclub.api.notification.domain.Notification;
import com.dclub.api.notification.infrastructure.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Instant;

@Service
public class NotificationApplicationService {
    private final NotificationRepository repository;
    private final CurrentMemberProvider currentMemberProvider;
    private final Clock clock;
    public NotificationApplicationService(NotificationRepository repository, CurrentMemberProvider currentMemberProvider, Clock clock) {
        this.repository = repository; this.currentMemberProvider = currentMemberProvider; this.clock = clock;
    }
    @Transactional(readOnly = true)
    public NotificationResponse list() {
        Long memberId = currentMemberProvider.requireActive().getId();
        var items = repository.findAllByMemberIdOrderByCreatedAtDesc(memberId).stream().map(this::item).toList();
        return new NotificationResponse(items, repository.countByMemberIdAndReadAtIsNull(memberId));
    }
    @Transactional
    public NotificationItem read(long id) {
        Long memberId = currentMemberProvider.requireActive().getId();
        Notification notification = repository.findByIdAndMemberId(id, memberId)
                .orElseThrow(() -> ApiException.notFound("알림을 찾을 수 없습니다."));
        notification.markRead(Instant.now(clock));
        return item(notification);
    }
    @Transactional
    public void readAll() {
        Long memberId = currentMemberProvider.requireActive().getId();
        Instant now = Instant.now(clock);
        repository.findAllByMemberIdOrderByCreatedAtDesc(memberId).forEach(notification -> notification.markRead(now));
    }
    private NotificationItem item(Notification n) { return new NotificationItem(n.getId(), n.getTitle(), n.getBody(), n.getLinkUrl(), n.getReadAt(), n.getCreatedAt()); }
}
