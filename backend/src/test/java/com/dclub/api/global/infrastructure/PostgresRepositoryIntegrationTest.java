package com.dclub.api.global.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.sql.Timestamp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class PostgresRepositoryIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("dclub_test")
            .withUsername("dclub")
            .withPassword("dclub");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("app.seed-demo-data", () -> "false");
        registry.add("app.auth.development-mode-enabled", () -> "true");
    }

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void flywaySchemaRunsOnPostgresAndEnforcesParticipationUniqueness() {
        String database = jdbc.queryForObject("select current_database()", String.class);
        assertThat(database).isEqualTo("dclub_test");

        Instant now = Instant.parse("2026-09-02T00:00:00Z");
        Timestamp nowTimestamp = Timestamp.from(now);
        jdbc.update("""
                insert into members (kakao_id, kakao_profile_name, name, part, role, status,
                                     onboarding_completed, approved_at, created_at, updated_at, version)
                values (?, ?, ?, ?, ?, ?, true, ?, ?, ?, 0)
                """, "postgres-member", "테스트", "테스트", "PE_WEB", "MEMBER", "ACTIVE",
                nowTimestamp, nowTimestamp, nowTimestamp);
        jdbc.update("""
                insert into club_events (title, description, starts_at, registration_deadline,
                                         fee_amount, status, allow_late_cancellation, created_at, updated_at, version)
                values (?, ?, ?, ?, ?, ?, false, ?, ?, 0)
                """, "PostgreSQL 행사", "마이그레이션 검증", Timestamp.from(now.plusSeconds(7200)),
                Timestamp.from(now.plusSeconds(3600)), 10000L, "PUBLISHED", nowTimestamp, nowTimestamp);

        Long memberId = jdbc.queryForObject("select id from members where kakao_id = ?", Long.class, "postgres-member");
        Long eventId = jdbc.queryForObject("select id from club_events where title = ?", Long.class, "PostgreSQL 행사");
        jdbc.update("insert into participations (event_id, member_id, status, joined_at, version) values (?, ?, ?, ?, 0)",
                eventId, memberId, "JOINED", nowTimestamp);

        assertThatThrownBy(() -> jdbc.update(
                "insert into participations (event_id, member_id, status, joined_at, version) values (?, ?, ?, ?, 0)",
                eventId, memberId, "JOINED", Timestamp.from(now.plusSeconds(1))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
