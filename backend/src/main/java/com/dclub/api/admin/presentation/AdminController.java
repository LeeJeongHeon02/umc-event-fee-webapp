package com.dclub.api.admin.presentation;

import com.dclub.api.global.presentation.ApiDtos.*;
import com.dclub.api.admin.application.AdminApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {
    private final AdminApplicationService service;

    public AdminController(AdminApplicationService service) {
        this.service = service;
    }

    @GetMapping("/dashboard")
    AdminDashboardResponse dashboard() {
        return service.dashboard();
    }

    @GetMapping("/events/{eventId}/participants")
    AdminEventParticipantPage eventParticipants(@PathVariable long eventId) {
        return service.eventParticipants(eventId);
    }

    @GetMapping("/dues-rounds/{duesRoundId}/payments")
    AdminDuesPaymentPage duesPayments(@PathVariable long duesRoundId) {
        return service.duesPayments(duesRoundId);
    }

    @PostMapping("/payment-obligations/{paymentId}/confirm")
    AdminPaymentReviewResponse confirm(@PathVariable long paymentId,
                                       @Valid @RequestBody AdminPaymentReviewRequest request) {
        return service.review(paymentId, request, true);
    }

    @PostMapping("/payment-obligations/{paymentId}/reject")
    AdminPaymentReviewResponse reject(@PathVariable long paymentId,
                                      @Valid @RequestBody AdminPaymentReviewRequest request) {
        return service.review(paymentId, request, false);
    }

    @GetMapping("/refunds")
    List<AdminPaymentRow> refunds() {
        return service.refunds();
    }

    @GetMapping("/payment-reports")
    List<AdminPaymentRow> paymentReports() {
        return service.paymentReports();
    }

    @PostMapping("/payment-obligations/{paymentId}/refund")
    AdminPaymentReviewResponse completeRefund(@PathVariable long paymentId,
                                              @Valid @RequestBody AdminPaymentReviewRequest request) {
        return service.completeRefund(paymentId, request);
    }

    @GetMapping("/members")
    List<AdminMemberResponse> members() {
        return service.members();
    }

    @PostMapping("/members/{memberId}/approve")
    AdminMemberResponse approveMember(@PathVariable long memberId,
                                      @Valid @RequestBody AdminMemberActionRequest request) {
        return service.approveMember(memberId, request);
    }

    @PostMapping("/members/{memberId}/suspend")
    AdminMemberResponse suspendMember(@PathVariable long memberId,
                                      @Valid @RequestBody AdminMemberActionRequest request) {
        return service.suspendMember(memberId, request);
    }

    @PatchMapping("/members/{memberId}/role")
    AdminMemberResponse changeMemberRole(@PathVariable long memberId,
                                         @Valid @RequestBody AdminMemberRoleRequest request) {
        return service.changeMemberRole(memberId, request);
    }
}
