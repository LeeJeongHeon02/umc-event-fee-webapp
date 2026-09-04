package com.dclub.api.auth.presentation;

import com.dclub.api.member.infrastructure.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "app.seed-demo-data=false")
@Transactional
class LocalAuthIntegrationTest {
    @Autowired
    MockMvc mockMvc;
    @Autowired
    MemberRepository memberRepository;

    @Test
    void 회원가입_후_로그인하면_세션으로_온보딩_회원에_접근한다() throws Exception {
        mockMvc.perform(post("/auth/local/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginId":"local.member","password":"clubpass123!","phoneNumber":"010-1234-5678"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.loginId").value("local.member"));

        var stored = memberRepository.findByLoginId("local.member").orElseThrow();
        assertThat(stored.getPasswordHash()).isNotEqualTo("clubpass123!");
        assertThat(stored.getPhoneNumber()).isEqualTo("01012345678");

        var loginResult = mockMvc.perform(post("/auth/local/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginId":"local.member","password":"clubpass123!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.member.loginId").value("local.member"))
                .andExpect(jsonPath("$.member.onboardingCompleted").value(false))
                .andExpect(jsonPath("$.redirectPath").value("/onboarding"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();
        mockMvc.perform(get("/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loginId").value("local.member"));
    }

    @Test
    void 잘못된_비밀번호는_공통_401_응답을_반환한다() throws Exception {
        mockMvc.perform(post("/auth/local/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"loginId":"wrong.password","password":"clubpass123!","phoneNumber":"010-2222-3333"}
                        """));

        mockMvc.perform(post("/auth/local/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginId":"wrong.password","password":"not-the-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }
}
