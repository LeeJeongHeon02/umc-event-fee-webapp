package com.dclub.api.member.presentation;

import com.dclub.api.global.presentation.ApiDtos.*;
import com.dclub.api.member.application.MemberApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
public class MemberController {
    private final MemberApplicationService service;

    public MemberController(MemberApplicationService service) {
        this.service = service;
    }

    @GetMapping("/me")
    MeResponse getMe() {
        return service.getMe();
    }

    @PatchMapping({"/me/onboarding", "/me/profile"})
    MeResponse updateProfile(@Valid @RequestBody OnboardingRequest request) {
        return service.completeOnboarding(request);
    }
}
