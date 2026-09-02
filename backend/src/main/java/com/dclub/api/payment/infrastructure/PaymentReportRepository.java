package com.dclub.api.payment.infrastructure;

import com.dclub.api.payment.domain.PaymentReport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentReportRepository extends JpaRepository<PaymentReport, Long> {
    Optional<PaymentReport> findFirstByPaymentObligationIdOrderByReportedAtDesc(Long paymentObligationId);
}
