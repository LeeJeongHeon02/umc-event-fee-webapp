package com.dclub.api.repository;

import com.dclub.api.domain.Participation;
import com.dclub.api.domain.ParticipationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ParticipationRepository extends JpaRepository<Participation, Long> {
    Optional<Participation> findByEventIdAndMemberId(Long eventId, Long memberId);
    long countByEventIdAndStatus(Long eventId, ParticipationStatus status);
    List<Participation> findAllByEventIdAndStatusOrderByJoinedAtAsc(Long eventId, ParticipationStatus status);
}
