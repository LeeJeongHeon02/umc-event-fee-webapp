package com.dclub.api.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApiContractIntegrationTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    void 현재_운영진과_행사_상세를_조회한다() throws Exception {
        mockMvc.perform(get("/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STAFF"))
                .andExpect(jsonPath("$.displayNickname").value("PE(Web) 김총무"));

        mockMvc.perform(get("/events/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("2026 가을 해커톤"))
                .andExpect(jsonPath("$.myPayment.status").value("UNPAID"));
    }

    @Test
    void 회원이_송금을_신고하면_확인_대기가_된다() throws Exception {
        mockMvc.perform(post("/payment-obligations/311/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"method":"BANK_TRANSFER","senderName":"김총무","transferConfirmed":true,"version":0}
                            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentStatus").value("REPORTED"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void 운영진은_행사_참가자를_조회하고_신고를_승인한다() throws Exception {
        mockMvc.perform(get("/admin/events/42/participants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[1].nickname").value("Design 김민지"))
                .andExpect(jsonPath("$.items[1].paymentStatus").value("REPORTED"));

        mockMvc.perform(post("/admin/payment-obligations/313/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.version").value(2));
    }

    @Test
    void 운영진은_회비_납부_목록을_조회한다() throws Exception {
        mockMvc.perform(get("/admin/dues-rounds/7/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duesRound.title").value("2026년 2학기 회비"))
                .andExpect(jsonPath("$.items.length()").value(5));
    }

    @Test
    void 미납_참가비가_있는_참가를_취소하면_납부가_무효화된다() throws Exception {
        mockMvc.perform(post("/events/42/participation/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"version":0,"reason":"개인 일정"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participationStatus").value("CANCELED"))
                .andExpect(jsonPath("$.paymentStatus").value("VOID"))
                .andExpect(jsonPath("$.refundRequired").value(false));
    }

    @Test
    void 납부_완료된_참가를_취소하면_환불_대기가_된다() throws Exception {
        mockMvc.perform(post("/events/42/participation/cancel")
                        .header("X-Dev-Member-Id", "3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"version":0,"reason":"개인 일정"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participationStatus").value("CANCELED"))
                .andExpect(jsonPath("$.paymentStatus").value("REFUND_PENDING"))
                .andExpect(jsonPath("$.refundRequired").value(true));
    }

    @Test
    void 일반_회원은_운영진_API에_접근할_수_없다() throws Exception {
        mockMvc.perform(get("/admin/dashboard")
                        .header("X-Dev-Member-Id", "2"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}
