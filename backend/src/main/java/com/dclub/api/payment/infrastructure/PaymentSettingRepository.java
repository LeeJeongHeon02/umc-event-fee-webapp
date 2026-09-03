package com.dclub.api.payment.infrastructure;

import com.dclub.api.payment.domain.PaymentSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PaymentSettingRepository extends JpaRepository<PaymentSetting, Long> {
    Optional<PaymentSetting> findFirstByActiveTrueOrderByCreatedAtDesc();
    List<PaymentSetting> findAllByOrderByCreatedAtDesc();
}
