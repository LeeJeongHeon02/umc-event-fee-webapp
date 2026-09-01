package com.dclub.api.repository;

import com.dclub.api.domain.ClubEvent;
import com.dclub.api.domain.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClubEventRepository extends JpaRepository<ClubEvent, Long> {
    List<ClubEvent> findAllByStatusOrderByStartsAtAsc(EventStatus status);
}
