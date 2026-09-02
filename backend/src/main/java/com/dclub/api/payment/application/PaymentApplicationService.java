package com.dclub.api.payment.application;

import com.dclub.api.global.presentation.ApiDtos.*;
import com.dclub.api.global.application.ApiMapper;
import com.dclub.api.global.common.ApiException;
import com.dclub.api.member.domain.*;
import com.dclub.api.event.domain.*;
import com.dclub.api.dues.domain.*;
import com.dclub.api.payment.domain.*;
import com.dclub.api.member.infrastructure.*;
import com.dclub.api.event.infrastructure.*;
import com.dclub.api.dues.infrastructure.*;
import com.dclub.api.payment.infrastructure.*;
import com.dclub.api.global.security.CurrentMemberProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Instant;

@Service
public class PaymentApplicationService {
    private final PaymentObligationRepository paymentRepository;
    private final PaymentReportRepository reportRepository;
    private final PaymentStatusHistoryRepository historyRepository;
    private final CurrentMemberProvider currentMemberProvider;
    private final ApiMapper mapper;
    private final Clock clock;

    public PaymentApplicationService(PaymentObligationRepository paymentRepository,
                                     PaymentReportRepository reportRepository,
                                     PaymentStatusHistoryRepository historyRepository,
                                     CurrentMemberProvider currentMemberProvider,
                                     ApiMapper mapper, Clock clock) {
        this.paymentRepository = paymentRepository;
        this.reportRepository = reportRepository;
        this.historyRepository = historyRepository;
        this.currentMemberProvider = currentMemberProvider;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<PaymentSummary> getMyPayments() {
        Long memberId = currentMemberProvider.current().getId();
        return PageResponse.of(paymentRepository.findAllByMemberIdOrderByDueAtAsc(memberId).stream()
                .map(mapper::payment).toList());
    }

    @Transactional(readOnly = true)
    public PaymentDetail getMyPayment(long paymentId) {
        Long memberId = currentMemberProvider.current().getId();
        return mapper.paymentDetail(paymentRepository.findByIdAndMemberId(paymentId, memberId)
                .orElseThrow(() -> ApiException.notFound("납부 항목을 찾을 수 없습니다.")));
    }

    @Transactional
    public PaymentReportResponse report(long paymentId, PaymentReportRequest request) {
        Long memberId = currentMemberProvider.current().getId();
        var payment = paymentRepository.findByIdAndMemberId(paymentId, memberId)
                .orElseThrow(() -> ApiException.notFound("납부 항목을 찾을 수 없습니다."));
        if (payment.getVersion() != request.version()) {
            throw ApiException.conflict("PAYMENT_STATE_CONFLICT", "납부 상태가 변경되었습니다. 새로고침 후 다시 시도해 주세요.");
        }
        PaymentStatus previous = payment.getStatus();
        Instant now = Instant.now(clock);
        payment.report(now);
        payment = paymentRepository.saveAndFlush(payment);
        var report = reportRepository.save(new PaymentReport(payment.getId(), request.method(),
                request.senderName().trim(), request.transferredAt(), request.note(), now));
        historyRepository.save(new PaymentStatusHistory(payment.getId(), previous, payment.getStatus(), null, now));
        return new PaymentReportResponse(mapper.report(report), payment.getStatus(), payment.getVersion());
    }
}
