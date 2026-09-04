package com.dclub.api.auth.presentation;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 실제 운영 SecurityFilterChain의 익명 CSRF 발급 및 로컬 세션 왕복을 검증합니다. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:local-auth-security;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "app.frontend-base-url=http://localhost:5173",
        "spring.security.oauth2.client.registration.kakao.client-id=test-client",
        "spring.security.oauth2.client.registration.kakao.client-secret=test-secret"
})
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@Transactional
class LocalAuthSecurityIntegrationTest {
    @Autowired MockMvc mvc;

    @Test
    void 익명_가입도_CSRF가_필요하고_로그인_세션으로_온보딩을_완료한다() throws Exception {
        String registration = """
                {"loginId":"secure.member","password":"clubpass123!","phoneNumber":"01012345678"}
                """;
        mvc.perform(post("/auth/local/register").contentType(MediaType.APPLICATION_JSON).content(registration))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"));

        var csrfResponse = mvc.perform(get("/auth/csrf"))
                .andExpect(status().isOk()).andReturn().getResponse();
        Cookie token = csrfResponse.getCookie("XSRF-TOKEN");
        assertThat(token).isNotNull();
        mvc.perform(post("/auth/local/register").cookie(token).header("X-XSRF-TOKEN", token.getValue())
                        .contentType(MediaType.APPLICATION_JSON).content(registration))
                .andExpect(status().isCreated());
        mvc.perform(get("/me")).andExpect(status().isUnauthorized());

        MockHttpSession beforeLogin = new MockHttpSession();
        String oldSessionId = beforeLogin.getId();
        var login = mvc.perform(post("/auth/local/login").session(beforeLogin)
                        .cookie(token).header("X-XSRF-TOKEN", token.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginId":"secure.member","password":"clubpass123!"}
                                """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.redirectPath").value("/onboarding"))
                .andReturn();
        var session = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(session.getId()).isNotEqualTo(oldSessionId);
        mvc.perform(get("/me").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.loginId").value("secure.member"))
                .andExpect(jsonPath("$.phoneNumber").value("01012345678"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        mvc.perform(patch("/me/onboarding").session(session)
                        .cookie(token).header("X-XSRF-TOKEN", token.getValue())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"새회원\",\"part\":\"PLAN\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.displayNickname").value("Plan 새회원"));
        mvc.perform(get("/events").session(session)).andExpect(status().isForbidden());

        // Missing CSRF cannot terminate a signed-in session.
        mvc.perform(post("/auth/logout").session(session)).andExpect(status().isForbidden());
        assertThat(session.isInvalid()).isFalse();
        mvc.perform(post("/api/v1/auth/logout").contextPath("/api/v1").session(session)
                        .cookie(token).header("X-XSRF-TOKEN", token.getValue()))
                .andExpect(status().isNoContent()).andExpect(content().string(""))
                .andExpect(cookie().maxAge("JSESSIONID", 0)).andExpect(cookie().path("JSESSIONID", "/api/v1"))
                .andExpect(cookie().maxAge("XSRF-TOKEN", 0)).andExpect(cookie().path("XSRF-TOKEN", "/"));
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get("/me").cookie(new Cookie("JSESSIONID", session.getId())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 카카오_세션도_로그아웃하고_GET_요청은_세션을_종료하지_않는다() throws Exception {
        var user = new org.springframework.security.oauth2.core.user.DefaultOAuth2User(
                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_MEMBER")),
                java.util.Map.of("id", "kakao-test", "memberId", 123L), "id");
        var authentication = new org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken(
                user, user.getAuthorities(), "kakao");
        var session = new MockHttpSession();
        session.setAttribute(org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new org.springframework.security.core.context.SecurityContextImpl(authentication));
        mvc.perform(get("/auth/logout").session(session)).andExpect(status().isMethodNotAllowed());
        assertThat(session.isInvalid()).isFalse();
        var token = mvc.perform(get("/auth/csrf")).andReturn().getResponse().getCookie("XSRF-TOKEN");
        mvc.perform(post("/auth/logout").session(session).cookie(token).header("X-XSRF-TOKEN", token.getValue()))
                .andExpect(status().isNoContent());
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get("/me")).andExpect(status().isUnauthorized());
    }
}
