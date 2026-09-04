package com.dclub.api.global.presentation;

import com.dclub.api.member.domain.*;
import com.dclub.api.event.domain.*;
import com.dclub.api.dues.domain.*;
import com.dclub.api.payment.domain.*;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;

public final class ApiDtos {
    private ApiDtos() {}

    public record MeResponse(Long id, String kakaoProfileName, String loginId, String phoneNumber, String name, MemberPart part,
                             String displayNickname, MemberRole role, MemberStatus status,
                             boolean onboardingCompleted, Instant approvedAt) {}

    public record LocalRegisterRequest(
            @NotBlank @Pattern(regexp = "^[a-z0-9._-]{4,30}$",
                    message = "아이디는 영문 소문자, 숫자, 마침표, 밑줄, 하이픈으로 4~30자여야 합니다.") String loginId,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Pattern(regexp = "^01[016789]-?[0-9]{3,4}-?[0-9]{4}$",
                    message = "휴대전화 번호 형식을 확인해 주세요.") String phoneNumber) {}

    public record LocalRegisterResponse(Long memberId, String loginId) {}

    public record LocalLoginRequest(@NotBlank String loginId, @NotBlank String password) {}

    public record LocalLoginResponse(MeResponse member, String redirectPath) {}

    public record OnboardingRequest(@NotBlank @Size(max = 50) String name, @NotNull MemberPart part) {}

    public record EventListItem(Long id, String title, String summary, String location, Instant startsAt,
                                Instant endsAt, Instant registrationDeadline, Integer capacity, long joinedCount,
                                long feeAmount, EventStatus status, ParticipationStatus myParticipationStatus,
                                PaymentStatus myPaymentStatus) {}

    public record PageResponse<T>(List<T> items, int page, int size, long totalElements, int totalPages) {
        public static <T> PageResponse<T> of(List<T> items) {
            return new PageResponse<>(items, 0, Math.max(items.size(), 1), items.size(), items.isEmpty() ? 0 : 1);
        }
    }

    public record ParticipationSummary(Long id, ParticipationStatus status, Instant joinedAt,
                                       Instant canceledAt, long version) {}

    public record PaymentSource(PaymentSourceType type, Long id, String title, Instant dueAt) {}

    public record PaymentSummary(Long id, PaymentType type, long amount, PaymentStatus status,
                                 Instant dueAt, PaymentSource source, Instant updatedAt, long version) {}

    public record EventDetail(Long id, String title, String summary, String description, String location,
                              Instant startsAt, Instant endsAt, Instant registrationDeadline, Integer capacity,
                              long joinedCount, long feeAmount, EventStatus status,
                              ParticipationStatus myParticipationStatus, PaymentStatus myPaymentStatus,
                              boolean allowLateCancellation, boolean canJoin, boolean canCancel,
                              ParticipationSummary myParticipation, PaymentSummary myPayment) {}

    public record JoinEventResponse(ParticipationSummary participation, PaymentSummary payment) {}

    public record CancelParticipationRequest(@NotNull @PositiveOrZero Long version, @Size(max = 300) String reason) {}

    public record CancelParticipationResponse(ParticipationStatus participationStatus,
                                              PaymentStatus paymentStatus, boolean refundRequired) {}

    public record PaymentDestination(String bankName, String accountNumber, String accountHolder,
                                     String kakaoPayReceiveUrl) {}

    public record PaymentSettingRequest(@NotBlank @Size(max = 100) String bankName,
                                        @NotBlank @Size(max = 100) String accountNumber,
                                        @NotBlank @Size(max = 100) String accountHolder,
                                        @Size(max = 500) String kakaoPayReceiveUrl) {}

    public record PaymentSettingResponse(Long id, String bankName, String accountNumber, String accountHolder,
                                         String kakaoPayReceiveUrl, boolean active, Long createdBy, Instant createdAt) {}

    public record NotificationItem(Long id, String title, String body, String linkUrl,
                                   Instant readAt, Instant createdAt) {}
    public record NotificationResponse(List<NotificationItem> items, long unreadCount) {}

    public record PaymentReportResponseItem(Long id, PaymentMethod method, String senderName,
                                            Instant transferredAt, String note, Instant reportedAt) {}

    public record PaymentStatusHistoryItem(PaymentStatus fromStatus, PaymentStatus toStatus,
                                           String reason, Instant changedAt) {}

    public record PaymentDetail(Long id, PaymentType type, long amount, PaymentStatus status, Instant dueAt,
                                PaymentSource source, Instant updatedAt, long version,
                                PaymentDestination paymentDestination,
                                PaymentReportResponseItem latestReport,
                                List<PaymentStatusHistoryItem> statusHistory) {}

    public record PaymentReportRequest(@NotNull PaymentMethod method,
                                       @NotBlank @Size(max = 100) String senderName,
                                       Instant transferredAt,
                                       @Size(max = 500) String note,
                                       @AssertTrue boolean transferConfirmed,
                                       @NotNull @PositiveOrZero Long version) {}

    public record PaymentReportResponse(PaymentReportResponseItem report, PaymentStatus paymentStatus, long version) {}

    public record AdminEventSummary(Long id, String title, Instant startsAt, Integer capacity, long joinedCount,
                                    long feeAmount, long unpaidCount, long reportedCount, long confirmedCount) {}

    public record AdminEventCreateRequest(@NotBlank @Size(max = 200) String title,
                                          @Size(max = 500) String summary,
                                          @NotBlank String description,
                                          @Size(max = 200) String location,
                                          @NotNull Instant startsAt,
                                          Instant endsAt,
                                          @NotNull Instant registrationDeadline,
                                          @Positive Integer capacity,
                                          @PositiveOrZero long feeAmount,
                                          boolean allowLateCancellation) {}

    public record AdminEventUpdateRequest(@NotBlank @Size(max = 200) String title,
                                          @Size(max = 500) String summary,
                                          @NotBlank String description,
                                          @Size(max = 200) String location,
                                          @NotNull Instant startsAt,
                                          Instant endsAt,
                                          @NotNull Instant registrationDeadline,
                                          @Positive Integer capacity,
                                          @PositiveOrZero long feeAmount,
                                          boolean allowLateCancellation,
                                          @PositiveOrZero long version) {}

    public record AdminEventVersionRequest(@PositiveOrZero long version) {}

    public record AdminEventTransitionRequest(@PositiveOrZero long version, @Size(max = 500) String reason) {}

    public record AdminEventCancelResponse(EventStatus eventStatus, long voidedPaymentCount,
                                           long refundPendingCount, long version) {}

    public record AdminEventResponse(Long id, String title, String summary, String description, String location,
                                     Instant startsAt, Instant endsAt, Instant registrationDeadline,
                                     Integer capacity, long joinedCount, long feeAmount, EventStatus status,
                                     boolean allowLateCancellation, Instant createdAt, Instant updatedAt,
                                     long version) {}

    public record AdminDuesRoundSummary(Long id, String title, long amount, Instant dueAt, long targetCount,
                                        long unpaidCount, long reportedCount, long confirmedCount,
                                        long confirmedAmount) {}

    public record AdminDuesRoundRequest(@NotBlank @Size(max = 200) String title,
                                        @PositiveOrZero long amount,
                                        @NotNull Instant dueAt,
                                        @Size(max = 100) String bankName,
                                        @Size(max = 100) String accountNumber,
                                        @Size(max = 100) String accountHolder,
                                        @Size(max = 500) String kakaoPayReceiveUrl,
                                        @PositiveOrZero long version) {}

    public record AdminDuesRoundResponse(Long id, String title, long amount, Instant dueAt,
                                         DuesRoundStatus status, String bankName, String accountNumber,
                                         String accountHolder, String kakaoPayReceiveUrl,
                                         long targetCount, Instant createdAt, Instant updatedAt, long version) {}

    public record AdminDuesPublishResponse(AdminDuesRoundResponse duesRound, long createdPaymentCount) {}

    public record AdminPaymentRow(Long paymentId, Long memberId, String nickname, String name, MemberPart part,
                                  long amount, PaymentStatus status, Instant dueAt,
                                  PaymentReportResponseItem latestReport, long version, PaymentSource source) {}

    public record AdminDashboardResponse(long memberCount, long pendingMemberCount, long upcomingEventCount,
                                         long unpaidCount, long reportedCount, long expectedAmount,
                                         long confirmedAmount, double collectionRate,
                                         List<AdminEventSummary> upcomingEvents,
                                         List<AdminDuesRoundSummary> activeDuesRounds,
                                         List<AdminPaymentRow> recentReports) {}

    public record AdminEventParticipant(Long participationId, Long memberId, String nickname, String name,
                                        MemberPart part, Instant joinedAt, Long paymentId, long amount,
                                        PaymentStatus paymentStatus, PaymentReportResponseItem latestReport,
                                        long version) {}

    public record AdminEventParticipantPage(AdminEventSummary event, List<AdminEventParticipant> items,
                                            int page, int size, long totalElements, int totalPages) {}

    public record AdminDuesPaymentPage(AdminDuesRoundSummary duesRound, List<AdminPaymentRow> items,
                                       int page, int size, long totalElements, int totalPages) {}

    public record AdminPaymentReviewRequest(@NotNull @PositiveOrZero Long version, @Size(max = 500) String note) {}

    public record AdminPaymentReviewResponse(Long paymentId, PaymentStatus status, long version, Instant reviewedAt) {}

    public record AdminMemberResponse(Long id, String kakaoProfileName, String loginId, String name, MemberPart part,
                                      String displayNickname, MemberRole role, MemberStatus status,
                                      boolean onboardingCompleted, Instant approvedAt, Instant createdAt,
                                      Instant updatedAt, long version) {}

    public record AdminMemberActionRequest(@PositiveOrZero long version) {}

    public record AdminMemberRoleRequest(@NotNull MemberRole role, @PositiveOrZero long version) {}
}
