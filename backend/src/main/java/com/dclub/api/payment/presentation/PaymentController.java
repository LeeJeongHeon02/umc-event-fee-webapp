package com.dclub.api.payment.presentation;

import com.dclub.api.global.presentation.ApiDtos.*;
import com.dclub.api.payment.application.PaymentApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class PaymentController {
    private final PaymentApplicationService service;

    public PaymentController(PaymentApplicationService service) {
        this.service = service;
    }

    @GetMapping("/me/payment-obligations")
    PageResponse<PaymentSummary> getMyPayments() {
        return service.getMyPayments();
    }

    @GetMapping("/payment-obligations/{paymentId}")
    PaymentDetail getPayment(@PathVariable long paymentId) {
        return service.getMyPayment(paymentId);
    }

    @PostMapping("/payment-obligations/{paymentId}/reports")
    @ResponseStatus(HttpStatus.CREATED)
    PaymentReportResponse reportPayment(@PathVariable long paymentId,
                                        @Valid @RequestBody PaymentReportRequest request) {
        return service.report(paymentId, request);
    }
}
