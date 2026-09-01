package com.dclub.api.domain;

import com.dclub.api.common.ApiException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

class ParticipationTest {
    private final Instant now = Instant.parse("2026-09-01T00:00:00Z");

    @Test
    void 참가자는_자신이_조회한_버전으로_참가를_취소할_수_있다() {
        var participation = new Participation(42L, 1L, now.minusSeconds(60));

        participation.cancel(0, now);

        assertThat(participation.getStatus()).isEqualTo(ParticipationStatus.CANCELED);
        assertThat(participation.getCanceledAt()).isEqualTo(now);
    }

    @Test
    void 오래된_버전으로는_참가를_취소할_수_없다() {
        var participation = new Participation(42L, 1L, now.minusSeconds(60));

        assertThatThrownBy(() -> participation.cancel(1, now))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("새로고침");
    }
}
