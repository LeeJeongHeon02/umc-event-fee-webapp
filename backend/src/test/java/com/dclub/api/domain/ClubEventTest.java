package com.dclub.api.domain;

import com.dclub.api.common.ApiException;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;

class ClubEventTest {
    private final Instant now = Instant.parse("2026-09-01T00:00:00Z");

    @Test
    void 참가_기한이_지나면_신청할_수_없다() {
        var event = event(now.minusSeconds(1), 10);

        assertThatThrownBy(() -> event.validateJoin(now, 0))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("기한");
    }

    @Test
    void 정원이_가득_차면_신청할_수_없다() {
        var event = event(now.plusSeconds(3600), 10);

        assertThatThrownBy(() -> event.validateJoin(now, 10))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("정원");
    }

    @Test
    void 참가비는_음수일_수_없다() {
        assertThatIllegalArgumentException().isThrownBy(() -> new ClubEvent(
                "행사", "요약", "설명", "장소", now.plusSeconds(7200), null,
                now.plusSeconds(3600), 10, -1, EventStatus.PUBLISHED, false, now));
    }

    @Test
    void 참가_기한이_지났고_늦은_취소를_허용하지_않으면_취소할_수_없다() {
        var event = event(now.minusSeconds(1), 10);

        assertThatThrownBy(() -> event.validateCancellation(now))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("취소 기한");
    }

    @Test
    void 늦은_취소를_허용한_행사는_참가_기한_후에도_취소할_수_있다() {
        var event = new ClubEvent("행사", "요약", "설명", "장소", now.plusSeconds(7200), null,
                now.minusSeconds(1), 10, 10_000, EventStatus.PUBLISHED, true, now.minusSeconds(3600));

        assertThatCode(() -> event.validateCancellation(now)).doesNotThrowAnyException();
    }

    private ClubEvent event(Instant deadline, int capacity) {
        return new ClubEvent("행사", "요약", "설명", "장소", now.plusSeconds(7200), null,
                deadline, capacity, 10_000, EventStatus.PUBLISHED, false, now.minusSeconds(3600));
    }
}
