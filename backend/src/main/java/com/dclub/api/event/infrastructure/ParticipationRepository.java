package com.dclub.api.event.infrastructure;

import com.dclub.api.event.domain.Participation;
import com.dclub.api.event.domain.ParticipationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ParticipationRepository extends JpaRepository<Participation, Long> {
    Optional<Participation> findByEventIdAndMemberId(Long eventId, Long memberId);
    long countByEventIdAndStatus(Long eventId, ParticipationStatus status);
    long countByEventId(Long eventId);
    List<Participation> findAllByEventIdAndStatusOrderByJoinedAtAsc(Long eventId, ParticipationStatus status);
}
