package com.dclub.api.notification.infrastructure;

import com.dclub.api.notification.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findAllByMemberIdOrderByCreatedAtDesc(Long memberId);
    Optional<Notification> findByIdAndMemberId(Long id, Long memberId);
    long countByMemberIdAndReadAtIsNull(Long memberId);
}
