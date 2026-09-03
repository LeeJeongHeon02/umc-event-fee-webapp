package com.dclub.api.payment.application;

import com.dclub.api.global.common.ApiException;
import com.dclub.api.global.presentation.ApiDtos.PaymentSettingRequest;
import com.dclub.api.global.presentation.ApiDtos.PaymentSettingResponse;
import com.dclub.api.global.security.CurrentMemberProvider;
import com.dclub.api.payment.domain.PaymentSetting;
import com.dclub.api.payment.infrastructure.PaymentSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class PaymentSettingApplicationService {
    private final PaymentSettingRepository repository;
    private final CurrentMemberProvider currentMemberProvider;
    private final Clock clock;

    public PaymentSettingApplicationService(PaymentSettingRepository repository,
                                            CurrentMemberProvider currentMemberProvider, Clock clock) {
        this.repository = repository;
        this.currentMemberProvider = currentMemberProvider;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PaymentSettingResponse active() {
        currentMemberProvider.requireAdmin();
        return repository.findFirstByActiveTrueOrderByCreatedAtDesc().map(this::response)
                .orElseThrow(() -> ApiException.notFound("활성 송금정보가 없습니다."));
    }

    @Transactional(readOnly = true)
    public List<PaymentSettingResponse> list() {
        currentMemberProvider.requireAdmin();
        return repository.findAllByOrderByCreatedAtDesc().stream().map(this::response).toList();
    }

    @Transactional
    public PaymentSettingResponse create(PaymentSettingRequest request) {
        var admin = currentMemberProvider.requireAdmin();
        repository.findFirstByActiveTrueOrderByCreatedAtDesc().ifPresent(PaymentSetting::deactivate);
        PaymentSetting setting = new PaymentSetting(request.bankName().trim(), request.accountNumber().trim(),
                request.accountHolder().trim(), blankToNull(request.kakaoPayReceiveUrl()), admin.getId(), Instant.now(clock));
        return response(repository.saveAndFlush(setting));
    }

    private PaymentSettingResponse response(PaymentSetting setting) {
        return new PaymentSettingResponse(setting.getId(), setting.getBankName(), setting.getAccountNumber(),
                setting.getAccountHolder(), setting.getKakaoPayReceiveUrl(), setting.isActive(),
                setting.getCreatedBy(), setting.getCreatedAt());
    }

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
