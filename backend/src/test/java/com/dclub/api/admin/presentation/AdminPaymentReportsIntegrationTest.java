package com.dclub.api.admin.presentation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminPaymentReportsIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired jakarta.persistence.EntityManager entityManager;

    @Test
    void 고정_회비번호_없이_행사비와_회비_신고를_통합하고_처리후_제외한다() throws Exception {
        jdbc.update("update dues_rounds set id=91 where id=7");
        jdbc.update("update payment_obligations set source_id=91 where source_type='DUES_ROUND' and source_id=7");
        mvc.perform(get("/admin/dues-rounds/7/payments")).andExpect(status().isNotFound());
        mvc.perform(get("/admin/payment-reports")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[*].status", everyItem(is("REPORTED"))))
                .andExpect(jsonPath("$[*].source.type", hasItems("EVENT", "DUES_ROUND")))
                .andExpect(jsonPath("$[*].source.id", hasItems(42, 91)))
                .andExpect(jsonPath("$[0].paymentId").value(313));
        mvc.perform(post("/admin/payment-obligations/313/confirm").contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":1}")).andExpect(status().isOk());
        mvc.perform(post("/admin/payment-obligations/701/reject").contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":1}")).andExpect(status().isOk());
        mvc.perform(get("/admin/payment-reports")).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].paymentId", containsInAnyOrder(315, 704)));
        mvc.perform(get("/admin/dashboard")).andExpect(jsonPath("$.reportedCount").value(2));
    }

    @Test
    void 회비가_없어도_행사_신고를_조회하고_신고가_없으면_빈_배열이다() throws Exception {
        jdbc.update("delete from payment_reports where payment_obligation_id in (select id from payment_obligations where source_type='DUES_ROUND')");
        jdbc.update("delete from payment_status_history where payment_obligation_id in (select id from payment_obligations where source_type='DUES_ROUND')");
        jdbc.update("delete from payment_obligations where source_type='DUES_ROUND'");
        jdbc.update("delete from dues_rounds");
        mvc.perform(get("/admin/payment-reports")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2))).andExpect(jsonPath("$[*].source.type", everyItem(is("EVENT"))));
        jdbc.update("update payment_obligations set status='CONFIRMED' where status='REPORTED'");
        mvc.perform(get("/admin/payment-reports")).andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void 일반_회원은_다른_회원의_신고_목록을_볼_수_없다() throws Exception {
        mvc.perform(get("/admin/payment-reports").header("X-Dev-Member-Id", "2"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
        jdbc.update("update members set role='STAFF' where id=2");
        // The JDBC fixture update bypasses JPA; reload the member before asserting staff access.
        entityManager.clear();
        mvc.perform(get("/admin/payment-reports").header("X-Dev-Member-Id", "2")).andExpect(status().isOk());
    }

    @Test
    void 대시보드_미리보기와_달리_10건을_초과한_신고도_빠짐없이_조회한다() throws Exception {
        for (int i = 0; i < 12; i++) {
            int id = 1000 + i;
            jdbc.update("""
                    insert into club_events (id,title,description,starts_at,registration_deadline,fee_amount,status,created_at,updated_at)
                    values (?, '추가 행사', '설명', current_timestamp, current_timestamp, 1000, 'PUBLISHED', current_timestamp, current_timestamp)
                    """, id);
            jdbc.update("""
                    insert into payment_obligations (id,member_id,type,amount,status,source_type,source_id,source_title,created_at,updated_at)
                    values (?,1,'EVENT_FEE',1000,'REPORTED','EVENT',?,'추가 행사',current_timestamp,current_timestamp)
                    """, id, id);
            jdbc.update("""
                    insert into payment_reports (payment_obligation_id,method,sender_name,reported_at)
                    values (?,'BANK_TRANSFER','김총무',current_timestamp)
                    """, id);
        }
        mvc.perform(get("/admin/dashboard")).andExpect(jsonPath("$.recentReports", hasSize(10)));
        mvc.perform(get("/admin/payment-reports")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(16)));
    }
}
