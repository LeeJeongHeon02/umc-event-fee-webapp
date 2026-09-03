package com.dclub.api.dues.infrastructure;

import com.dclub.api.dues.domain.DuesRound;
import com.dclub.api.dues.domain.DuesRoundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DuesRoundRepository extends JpaRepository<DuesRound, Long> {
    List<DuesRound> findAllByStatusOrderByDueAtAsc(DuesRoundStatus status);
    List<DuesRound> findAllByOrderByCreatedAtDesc();
}
