package com.dclub.api.member.application;

import com.dclub.api.global.presentation.ApiDtos.*;
import com.dclub.api.global.application.ApiMapper;
import com.dclub.api.global.security.CurrentMemberProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Instant;

@Service
public class MemberApplicationService {
    private final CurrentMemberProvider currentMemberProvider;
    private final ApiMapper mapper;
    private final Clock clock;

    public MemberApplicationService(CurrentMemberProvider currentMemberProvider, ApiMapper mapper, Clock clock) {
        this.currentMemberProvider = currentMemberProvider;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public MeResponse getMe() {
        return mapper.member(currentMemberProvider.current());
    }

    @Transactional
    public MeResponse completeOnboarding(OnboardingRequest request) {
        var member = currentMemberProvider.current();
        member.completeOnboarding(request.name().trim(), request.part(), Instant.now(clock));
        return mapper.member(member);
    }
}
