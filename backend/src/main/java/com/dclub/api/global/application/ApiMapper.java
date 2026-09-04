package com.dclub.api.global.application;

import com.dclub.api.global.presentation.ApiDtos.*;
import com.dclub.api.member.domain.*;
import com.dclub.api.event.domain.*;
import com.dclub.api.dues.domain.*;
import com.dclub.api.payment.domain.*;
import com.dclub.api.member.infrastructure.*;
import com.dclub.api.event.infrastructure.*;
import com.dclub.api.dues.infrastructure.*;
import com.dclub.api.payment.infrastructure.*;
import org.springframework.stereotype.Component;

@Component
public class ApiMapper {
    private final PaymentReportRepository reportRepository;
    private final PaymentStatusHistoryRepository historyRepository;

    public ApiMapper(PaymentReportRepository reportRepository, PaymentStatusHistoryRepository historyRepository) {
        this.reportRepository = reportRepository;
        this.historyRepository = historyRepository;
    }

    public MeResponse member(Member member) {
        return new MeResponse(member.getId(), member.getKakaoProfileName(), member.getLoginId(),
                member.getName(), member.getPart(),
                member.displayNickname(), member.getRole(), member.getStatus(),
                member.isOnboardingCompleted(), member.getApprovedAt());
    }

    public ParticipationSummary participation(Participation participation) {
        return new ParticipationSummary(participation.getId(), participation.getStatus(),
                participation.getJoinedAt(), participation.getCanceledAt(), participation.getVersion());
    }

    public PaymentSummary payment(PaymentObligation payment) {
        return new PaymentSummary(payment.getId(), payment.getType(), payment.getAmount(), payment.getStatus(),
                payment.getDueAt(), source(payment), payment.getUpdatedAt(), payment.getVersion());
    }

    public PaymentDetail paymentDetail(PaymentObligation payment) {
        var latestReport = reportRepository.findFirstByPaymentObligationIdOrderByReportedAtDesc(payment.getId())
                .map(this::report).orElse(null);
        var history = historyRepository.findAllByPaymentObligationIdOrderByChangedAtAsc(payment.getId()).stream()
                .map(item -> new PaymentStatusHistoryItem(item.getFromStatus(), item.getToStatus(),
                        item.getReason(), item.getChangedAt()))
                .toList();
        PaymentDestination destination = payment.getBankName() == null ? null :
                new PaymentDestination(payment.getBankName(), payment.getAccountNumber(),
                        payment.getAccountHolder(), payment.getKakaoPayReceiveUrl());
        return new PaymentDetail(payment.getId(), payment.getType(), payment.getAmount(), payment.getStatus(),
                payment.getDueAt(), source(payment), payment.getUpdatedAt(), payment.getVersion(),
                destination, latestReport, history);
    }

    public PaymentReportResponseItem report(PaymentReport report) {
        return new PaymentReportResponseItem(report.getId(), report.getMethod(), report.getSenderName(),
                report.getTransferredAt(), report.getNote(), report.getReportedAt());
    }

    public PaymentSource source(PaymentObligation payment) {
        return new PaymentSource(payment.getSourceType(), payment.getSourceId(),
                payment.getSourceTitle(), payment.getDueAt());
    }
}
