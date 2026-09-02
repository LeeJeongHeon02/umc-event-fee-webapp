package com.dclub.api.payment.infrastructure;

import com.dclub.api.payment.domain.PaymentObligation;
import com.dclub.api.payment.domain.PaymentSourceType;
import com.dclub.api.payment.domain.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaymentObligationRepository extends JpaRepository<PaymentObligation, Long> {
    List<PaymentObligation> findAllByMemberIdOrderByDueAtAsc(Long memberId);
    Optional<PaymentObligation> findByIdAndMemberId(Long id, Long memberId);
    Optional<PaymentObligation> findByMemberIdAndSourceTypeAndSourceId(Long memberId, PaymentSourceType sourceType, Long sourceId);
    List<PaymentObligation> findAllBySourceTypeAndSourceIdOrderByMemberIdAsc(PaymentSourceType sourceType, Long sourceId);
    long countByStatus(PaymentStatus status);
    long countByStatusIn(Collection<PaymentStatus> statuses);
}
