package com.dclub.api.repository;

import com.dclub.api.domain.PaymentStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentStatusHistoryRepository extends JpaRepository<PaymentStatusHistory, Long> {
    List<PaymentStatusHistory> findAllByPaymentObligationIdOrderByChangedAtAsc(Long paymentObligationId);
}
