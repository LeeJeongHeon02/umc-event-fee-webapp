package com.dclub.api.event.infrastructure;

import com.dclub.api.event.domain.ClubEvent;
import com.dclub.api.event.domain.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClubEventRepository extends JpaRepository<ClubEvent, Long> {
    List<ClubEvent> findAllByStatusOrderByStartsAtAsc(EventStatus status);
    List<ClubEvent> findAllByOrderByStartsAtDesc();
}
