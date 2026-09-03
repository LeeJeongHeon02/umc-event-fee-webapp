package com.dclub.api.global.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true")
public class DemoDataInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbc;

    public DemoDataInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        Integer count = jdbc.queryForObject("select count(*) from members", Integer.class);
        if (count != null && count > 0) return;

        jdbc.update("""
            insert into members (id, kakao_id, kakao_profile_name, name, part, role, status, onboarding_completed, approved_at, created_at, updated_at, version)
            values
            (1, 'dev-staff', '총무', '김총무', 'PE_WEB', 'ADMIN', 'ACTIVE', true, current_timestamp, current_timestamp, current_timestamp, 0),
            (2, 'dev-member-2', '민지', '김민지', 'DESIGN', 'MEMBER', 'ACTIVE', true, current_timestamp, current_timestamp, current_timestamp, 0),
            (3, 'dev-member-3', '수현', '이수현', 'PLAN', 'MEMBER', 'ACTIVE', true, current_timestamp, current_timestamp, current_timestamp, 0),
            (4, 'dev-member-4', '서준', '박서준', 'PE_MOBILE', 'MEMBER', 'ACTIVE', true, current_timestamp, current_timestamp, current_timestamp, 0),
            (5, 'dev-member-5', '유진', '최유진', 'PE_WEB', 'MEMBER', 'ACTIVE', true, current_timestamp, current_timestamp, current_timestamp, 0)
            """);

        jdbc.update("""
            insert into payment_settings (bank_name, account_number, account_holder, kakao_pay_receive_url, active, created_by, created_at)
            values ('카카오뱅크', '3333-12-3456789', '김총무', 'https://qr.kakaopay.com/example', true, 1, current_timestamp)
            """);

        jdbc.update("""
            insert into club_events (id, title, summary, description, location, starts_at, ends_at, registration_deadline, capacity, fee_amount, status, allow_late_cancellation, created_at, updated_at, version)
            values
            (42, '2026 가을 해커톤', '팀을 꾸려 하루 동안 교내 문제를 해결해요.', '기획, 디자인, 웹, 모바일 파트가 섞인 팀으로 하루 동안 교내의 작은 문제를 해결합니다.', '공학관 101호', '2026-09-15T10:00:00Z', '2026-09-15T12:00:00Z', '2026-09-12T14:59:59Z', 50, 15000, 'PUBLISHED', false, current_timestamp, current_timestamp, 0),
            (43, '신입 부원 네트워킹 데이', '파트를 넘어 관심사와 프로젝트를 나눠요.', '신입 부원과 기존 부원이 편하게 대화하는 행사입니다.', '학생회관 라운지', '2026-09-23T09:30:00Z', '2026-09-23T11:00:00Z', '2026-09-20T14:59:59Z', 40, 0, 'PUBLISHED', false, current_timestamp, current_timestamp, 0)
            """);

        jdbc.update("""
            insert into participations (id, event_id, member_id, status, joined_at, version)
            values
            (84, 42, 1, 'JOINED', '2026-09-02T10:20:00Z', 0),
            (85, 42, 2, 'JOINED', '2026-09-02T11:10:00Z', 0),
            (86, 42, 3, 'JOINED', '2026-09-03T03:15:00Z', 0),
            (87, 42, 4, 'JOINED', '2026-09-03T05:40:00Z', 0)
            """);

        jdbc.update("""
            insert into dues_rounds (id, title, amount, due_at, status, created_at, updated_at, version)
            values (7, '2026년 2학기 회비', 30000, '2026-09-30T14:59:59Z', 'PUBLISHED', current_timestamp, current_timestamp, 0)
            """);

        jdbc.update("""
            insert into payment_obligations (id, member_id, type, amount, status, source_type, source_id, source_title, due_at, bank_name, account_number, account_holder, kakao_pay_receive_url, created_at, updated_at, version)
            values
            (311, 1, 'EVENT_FEE', 15000, 'UNPAID', 'EVENT', 42, '2026 가을 해커톤', '2026-09-12T14:59:59Z', '카카오뱅크', '3333-12-3456789', '김총무', 'https://qr.kakaopay.com/example', current_timestamp, current_timestamp, 0),
            (313, 2, 'EVENT_FEE', 15000, 'REPORTED', 'EVENT', 42, '2026 가을 해커톤', '2026-09-12T14:59:59Z', '카카오뱅크', '3333-12-3456789', '김총무', 'https://qr.kakaopay.com/example', current_timestamp, current_timestamp, 1),
            (314, 3, 'EVENT_FEE', 15000, 'CONFIRMED', 'EVENT', 42, '2026 가을 해커톤', '2026-09-12T14:59:59Z', '카카오뱅크', '3333-12-3456789', '김총무', 'https://qr.kakaopay.com/example', current_timestamp, current_timestamp, 2),
            (315, 4, 'EVENT_FEE', 15000, 'REPORTED', 'EVENT', 42, '2026 가을 해커톤', '2026-09-12T14:59:59Z', '카카오뱅크', '3333-12-3456789', '김총무', 'https://qr.kakaopay.com/example', current_timestamp, current_timestamp, 1),
            (701, 1, 'MEMBERSHIP_DUE', 30000, 'REPORTED', 'DUES_ROUND', 7, '2026년 2학기 회비', '2026-09-30T14:59:59Z', '카카오뱅크', '3333-12-3456789', '김총무', 'https://qr.kakaopay.com/example', current_timestamp, current_timestamp, 1),
            (702, 2, 'MEMBERSHIP_DUE', 30000, 'CONFIRMED', 'DUES_ROUND', 7, '2026년 2학기 회비', '2026-09-30T14:59:59Z', '카카오뱅크', '3333-12-3456789', '김총무', 'https://qr.kakaopay.com/example', current_timestamp, current_timestamp, 2),
            (703, 3, 'MEMBERSHIP_DUE', 30000, 'UNPAID', 'DUES_ROUND', 7, '2026년 2학기 회비', '2026-09-30T14:59:59Z', '카카오뱅크', '3333-12-3456789', '김총무', 'https://qr.kakaopay.com/example', current_timestamp, current_timestamp, 0),
            (704, 4, 'MEMBERSHIP_DUE', 30000, 'REPORTED', 'DUES_ROUND', 7, '2026년 2학기 회비', '2026-09-30T14:59:59Z', '카카오뱅크', '3333-12-3456789', '김총무', 'https://qr.kakaopay.com/example', current_timestamp, current_timestamp, 1),
            (705, 5, 'MEMBERSHIP_DUE', 30000, 'REJECTED', 'DUES_ROUND', 7, '2026년 2학기 회비', '2026-09-30T14:59:59Z', '카카오뱅크', '3333-12-3456789', '김총무', 'https://qr.kakaopay.com/example', current_timestamp, current_timestamp, 2)
            """);

        jdbc.update("""
            insert into payment_reports (id, payment_obligation_id, method, sender_name, transferred_at, note, reported_at)
            values
            (913, 313, 'KAKAO_PAY_CODE', '김민지', '2026-09-01T09:30:00Z', null, '2026-09-01T09:32:00Z'),
            (914, 314, 'BANK_TRANSFER', '이수현', null, null, '2026-09-01T03:00:00Z'),
            (915, 315, 'BANK_TRANSFER', '박서준', null, null, '2026-09-01T05:41:00Z'),
            (1701, 701, 'BANK_TRANSFER', '김총무', null, null, '2026-09-01T08:10:00Z'),
            (1702, 702, 'KAKAO_PAY_CODE', '김민지', null, null, '2026-08-31T04:20:00Z'),
            (1704, 704, 'BANK_TRANSFER', '박서준', null, null, '2026-09-01T05:41:00Z'),
            (1705, 705, 'BANK_TRANSFER', '최유진', null, null, '2026-08-30T12:10:00Z')
            """);

        jdbc.update("""
            insert into payment_status_history (payment_obligation_id, from_status, to_status, reason, changed_at)
            select id, null, status, 'demo seed', current_timestamp from payment_obligations
            """);
    }
}
