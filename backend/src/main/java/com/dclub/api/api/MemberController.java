package com.dclub.api.api;

import com.dclub.api.api.ApiDtos.*;
import com.dclub.api.service.MemberApplicationService;
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
