package com.dclub.api.auth.application;

import com.dclub.api.global.common.ApiException;
import com.dclub.api.global.presentation.ApiDtos.LocalLoginRequest;
import com.dclub.api.global.presentation.ApiDtos.LocalRegisterRequest;
import com.dclub.api.member.domain.Member;
import com.dclub.api.member.infrastructure.MemberRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class LocalAuthApplicationService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public LocalAuthApplicationService(MemberRepository memberRepository, PasswordEncoder passwordEncoder, Clock clock) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    public Member register(LocalRegisterRequest request) {
        String loginId = request.loginId().trim();
        String phoneNumber = normalizePhoneNumber(request.phoneNumber());
        if (memberRepository.existsByLoginId(loginId)) {
            throw ApiException.conflict("LOGIN_ID_ALREADY_EXISTS", "이미 사용 중인 아이디입니다.");
        }
        if (memberRepository.existsByPhoneNumber(phoneNumber)) {
            throw ApiException.conflict("PHONE_NUMBER_ALREADY_EXISTS", "이미 가입에 사용된 전화번호입니다.");
        }
        Member member = Member.pendingLocalMember(
                loginId, passwordEncoder.encode(request.password()), phoneNumber, Instant.now(clock));
        return memberRepository.saveAndFlush(member);
    }

    @Transactional(readOnly = true)
    public Member authenticate(LocalLoginRequest request) {
        Member member = memberRepository.findByLoginId(request.loginId().trim()).orElse(null);
        if (member == null || member.getPasswordHash() == null
                || !passwordEncoder.matches(request.password(), member.getPasswordHash())) {
            throw ApiException.unauthorized("INVALID_CREDENTIALS", "아이디 또는 비밀번호가 올바르지 않습니다.");
        }
        return member;
    }

    private String normalizePhoneNumber(String phoneNumber) {
        return phoneNumber.replace("-", "");
    }
}
