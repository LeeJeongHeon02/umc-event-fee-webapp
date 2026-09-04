package com.dclub.api.auth.presentation;

import com.dclub.api.auth.application.LocalAuthApplicationService;
import com.dclub.api.auth.infrastructure.MemberSessionService;
import com.dclub.api.global.application.ApiMapper;
import com.dclub.api.global.presentation.ApiDtos.LocalLoginRequest;
import com.dclub.api.global.presentation.ApiDtos.LocalLoginResponse;
import com.dclub.api.global.presentation.ApiDtos.LocalRegisterRequest;
import com.dclub.api.global.presentation.ApiDtos.LocalRegisterResponse;
import com.dclub.api.member.domain.Member;
import com.dclub.api.member.domain.MemberStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/local")
public class LocalAuthController {
    private final LocalAuthApplicationService authService;
    private final MemberSessionService sessionService;
    private final ApiMapper mapper;

    public LocalAuthController(LocalAuthApplicationService authService,
                               MemberSessionService sessionService,
                               ApiMapper mapper) {
        this.authService = authService;
        this.sessionService = sessionService;
        this.mapper = mapper;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    LocalRegisterResponse register(@Valid @RequestBody LocalRegisterRequest request) {
        Member member = authService.register(request);
        return new LocalRegisterResponse(member.getId(), member.getLoginId());
    }

    @PostMapping("/login")
    LocalLoginResponse login(@Valid @RequestBody LocalLoginRequest request,
                             HttpServletRequest servletRequest,
                             HttpServletResponse servletResponse) {
        Member member = authService.authenticate(request);
        sessionService.signIn(member, servletRequest, servletResponse);
        return new LocalLoginResponse(mapper.member(member), redirectPath(member));
    }

    private String redirectPath(Member member) {
        if (!member.isOnboardingCompleted()) return "/onboarding";
        return member.getStatus() == MemberStatus.ACTIVE ? "/home" : "/pending";
    }
}
