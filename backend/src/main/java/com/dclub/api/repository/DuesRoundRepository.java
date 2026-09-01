package com.dclub.api.repository;

import com.dclub.api.domain.DuesRound;
import com.dclub.api.domain.DuesRoundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DuesRoundRepository extends JpaRepository<DuesRound, Long> {
    List<DuesRound> findAllByStatusOrderByDueAtAsc(DuesRoundStatus status);
}
