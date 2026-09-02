package com.dclub.api.payment.infrastructure;

import com.dclub.api.payment.domain.PaymentStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentStatusHistoryRepository extends JpaRepository<PaymentStatusHistory, Long> {
    List<PaymentStatusHistory> findAllByPaymentObligationIdOrderByChangedAtAsc(Long paymentObligationId);
}
