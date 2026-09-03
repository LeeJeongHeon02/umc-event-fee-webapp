package com.dclub.api.dues.application;

import com.dclub.api.dues.domain.DuesRound;
import com.dclub.api.dues.infrastructure.DuesRoundRepository;
import com.dclub.api.global.common.ApiException;
import com.dclub.api.global.presentation.ApiDtos.*;
import com.dclub.api.global.security.CurrentMemberProvider;
import com.dclub.api.member.domain.MemberStatus;
import com.dclub.api.member.infrastructure.MemberRepository;
import com.dclub.api.payment.domain.PaymentObligation;
import com.dclub.api.payment.domain.PaymentSourceType;
import com.dclub.api.payment.domain.PaymentType;
import com.dclub.api.payment.infrastructure.PaymentObligationRepository;
import com.dclub.api.payment.infrastructure.PaymentSettingRepository;
import com.dclub.api.notification.domain.Notification;
import com.dclub.api.notification.infrastructure.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class AdminDuesApplicationService {
    private final DuesRoundRepository duesRoundRepository;
    private final MemberRepository memberRepository;
    private final PaymentObligationRepository paymentRepository;
    private final CurrentMemberProvider currentMemberProvider;
    private final PaymentSettingRepository paymentSettingRepository;
    private final NotificationRepository notificationRepository;
    private final Clock clock;

    public AdminDuesApplicationService(DuesRoundRepository duesRoundRepository, MemberRepository memberRepository,
                                       PaymentObligationRepository paymentRepository,
                                       PaymentSettingRepository paymentSettingRepository,
                                       NotificationRepository notificationRepository,
                                       CurrentMemberProvider currentMemberProvider, Clock clock) {
        this.duesRoundRepository = duesRoundRepository;
        this.memberRepository = memberRepository;
        this.paymentRepository = paymentRepository;
        this.paymentSettingRepository = paymentSettingRepository;
        this.notificationRepository = notificationRepository;
        this.currentMemberProvider = currentMemberProvider;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<AdminDuesRoundResponse> list() {
        currentMemberProvider.requireStaff();
        return duesRoundRepository.findAllByOrderByCreatedAtDesc().stream().map(this::response).toList();
    }

    @Transactional
    public AdminDuesRoundResponse create(AdminDuesRoundRequest request) {
        currentMemberProvider.requireStaff();
        Instant now = Instant.now(clock);
        DuesRound round = new DuesRound(request.title(), request.amount(), request.dueAt(), request.bankName(),
                request.accountNumber(), request.accountHolder(), request.kakaoPayReceiveUrl(), now);
        return response(duesRoundRepository.saveAndFlush(round));
    }

    @Transactional
    public AdminDuesRoundResponse update(long id, AdminDuesRoundRequest request) {
        currentMemberProvider.requireStaff();
        DuesRound round = find(id);
        round.updateDraft(request.title(), request.amount(), request.dueAt(), request.bankName(),
                request.accountNumber(), request.accountHolder(), request.kakaoPayReceiveUrl(), request.version(),
                Instant.now(clock));
        return response(duesRoundRepository.saveAndFlush(round));
    }

    @Transactional
    public AdminDuesPublishResponse publish(long id, AdminEventVersionRequest request) {
        currentMemberProvider.requireStaff();
        DuesRound round = find(id);
        Instant now = Instant.now(clock);
        var targetMembers = memberRepository.findAllByStatusOrderByCreatedAtAsc(MemberStatus.ACTIVE);
        if (targetMembers.isEmpty()) {
            throw ApiException.conflict("DUES_TARGET_EMPTY", "회비를 부과할 활성 회원이 없습니다.");
        }
        round.publish(request.version(), now);
        round = duesRoundRepository.saveAndFlush(round);

        DuesRound published = round;
        var activeSetting = published.getAmount() > 0 && (published.getAccountNumber() == null || published.getAccountNumber().isBlank())
                ? paymentSettingRepository.findFirstByActiveTrueOrderByCreatedAtDesc()
                    .orElseThrow(() -> ApiException.conflict("ACTIVE_PAYMENT_SETTING_REQUIRED", "유료 회비에 사용할 활성 송금정보가 없습니다."))
                : null;
        List<PaymentObligation> obligations = targetMembers.stream()
                .map(member -> {
                    PaymentObligation payment = new PaymentObligation(member.getId(), PaymentType.MEMBERSHIP_DUE,
                            published.getAmount(), PaymentSourceType.DUES_ROUND, published.getId(),
                            published.getTitle(), published.getDueAt(), now);
                    payment.setDestination(activeSetting == null ? published.getBankName() : activeSetting.getBankName(),
                            activeSetting == null ? published.getAccountNumber() : activeSetting.getAccountNumber(),
                            activeSetting == null ? published.getAccountHolder() : activeSetting.getAccountHolder(),
                            activeSetting == null ? published.getKakaoPayReceiveUrl() : activeSetting.getKakaoPayReceiveUrl());
                    return payment;
                }).toList();
        paymentRepository.saveAll(obligations);
        notificationRepository.saveAll(obligations.stream().map(payment -> new Notification(payment.getMemberId(),
                "회비 납부 요청이 도착했어요", published.getTitle(), "/payments/" + payment.getId(), now)).toList());
        return new AdminDuesPublishResponse(response(published), obligations.size());
    }

    @Transactional
    public void delete(long id, long version) {
        currentMemberProvider.requireStaff();
        DuesRound round = find(id);
        round.requireDeletable(version);
        if (paymentRepository.countBySourceTypeAndSourceId(PaymentSourceType.DUES_ROUND, id) > 0) {
            throw ApiException.conflict("DUES_ROUND_HAS_PAYMENTS", "납부 항목이 생성된 회비 차수는 삭제할 수 없습니다.");
        }
        duesRoundRepository.delete(round);
    }

    private DuesRound find(long id) {
        return duesRoundRepository.findById(id).orElseThrow(() -> ApiException.notFound("회비 차수를 찾을 수 없습니다."));
    }

    private AdminDuesRoundResponse response(DuesRound round) {
        long targetCount = paymentRepository.countBySourceTypeAndSourceId(PaymentSourceType.DUES_ROUND, round.getId());
        return new AdminDuesRoundResponse(round.getId(), round.getTitle(), round.getAmount(), round.getDueAt(),
                round.getStatus(), round.getBankName(), round.getAccountNumber(), round.getAccountHolder(),
                round.getKakaoPayReceiveUrl(), targetCount, round.getCreatedAt(), round.getUpdatedAt(), round.getVersion());
    }
}
