package com.dclub.api.payment.presentation;

import com.dclub.api.global.presentation.ApiDtos.PaymentSettingRequest;
import com.dclub.api.global.presentation.ApiDtos.PaymentSettingResponse;
import com.dclub.api.payment.application.PaymentSettingApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/admin/payment-settings")
public class AdminPaymentSettingController {
    private final PaymentSettingApplicationService service;
    public AdminPaymentSettingController(PaymentSettingApplicationService service) { this.service = service; }

    @GetMapping("/active")
    PaymentSettingResponse active() { return service.active(); }

    @GetMapping
    List<PaymentSettingResponse> list() { return service.list(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PaymentSettingResponse create(@Valid @RequestBody PaymentSettingRequest request) { return service.create(request); }
}
