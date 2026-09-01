package com.dclub.api.api;

import com.dclub.api.api.ApiDtos.*;
import com.dclub.api.service.AdminApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

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
}
