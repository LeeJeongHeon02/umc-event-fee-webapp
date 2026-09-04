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
                .andExpect(jsonPath("$.loginId").value("secure.member"));
        mvc.perform(patch("/me/onboarding").session(session)
                        .cookie(token).header("X-XSRF-TOKEN", token.getValue())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"새회원\",\"part\":\"PLAN\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.displayNickname").value("Plan 새회원"));
        mvc.perform(get("/events").session(session)).andExpect(status().isForbidden());
    }
}
