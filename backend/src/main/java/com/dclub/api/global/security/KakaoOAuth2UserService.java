package com.dclub.api.global.security;

import com.dclub.api.member.domain.Member;
import com.dclub.api.member.infrastructure.MemberRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class KakaoOAuth2UserService extends DefaultOAuth2UserService {
    private final MemberRepository memberRepository;
    private final Clock clock;
    private final String bootstrapAdminKakaoId;

    public KakaoOAuth2UserService(MemberRepository memberRepository, Clock clock,
                                  @Value("${app.auth.bootstrap-admin-kakao-id:}") String bootstrapAdminKakaoId) {
        this.memberRepository = memberRepository;
        this.clock = clock;
        this.bootstrapAdminKakaoId = bootstrapAdminKakaoId;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User kakaoUser = super.loadUser(userRequest);
        Map<String, Object> attributes = new HashMap<>(kakaoUser.getAttributes());
        String kakaoId = String.valueOf(attributes.get("id"));
        String profileName = extractNickname(attributes);
        Member member = findOrCreateMember(kakaoId, profileName);
        attributes.put("memberId", member.getId());
        return new DefaultOAuth2User(kakaoUser.getAuthorities(), attributes, "id");
    }

    @Transactional
    Member findOrCreateMember(String kakaoId, String profileName) {
        if (kakaoId == null || kakaoId.isBlank() || "null".equals(kakaoId)) {
            throw new OAuth2AuthenticationException("Kakao user id is missing");
        }
        return memberRepository.findByKakaoId(kakaoId)
                .map(member -> {
                    if (bootstrapAdminKakaoId.equals(kakaoId)) member.promoteBootstrapAdmin(Instant.now(clock));
                    return member;
                })
                .orElseGet(() -> memberRepository.save(bootstrapAdminKakaoId.equals(kakaoId)
                        ? Member.bootstrapAdmin(kakaoId, normalizeProfileName(profileName), Instant.now(clock))
                        : Member.pendingKakaoMember(kakaoId, normalizeProfileName(profileName), Instant.now(clock))));
    }

    private String extractNickname(Map<String, Object> attributes) {
        Object properties = attributes.get("properties");
        if (properties instanceof Map<?, ?> propertyMap) {
            Object nickname = propertyMap.get("nickname");
            if (nickname != null) return nickname.toString();
        }
        Object kakaoAccount = attributes.get("kakao_account");
        if (kakaoAccount instanceof Map<?, ?> accountMap) {
            Object profile = accountMap.get("profile");
            if (profile instanceof Map<?, ?> profileMap && profileMap.get("nickname") != null) {
                return profileMap.get("nickname").toString();
            }
        }
        return "카카오 사용자";
    }

    private String normalizeProfileName(String profileName) {
        if (profileName == null || profileName.isBlank()) return "카카오 사용자";
        return profileName.trim();
    }
}
