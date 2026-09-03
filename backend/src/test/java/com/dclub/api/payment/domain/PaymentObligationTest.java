package com.dclub.api.payment.domain;

import com.dclub.api.global.common.ApiException;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;

class PaymentObligationTest {
    private final Instant now = Instant.parse("2026-09-01T00:00:00Z");

    @Test
    void 송금_신고와_납부_확정은_서로_다른_상태다() {
        var payment = payment(30_000);

        payment.report(now.plusSeconds(60));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REPORTED);
        payment.confirm(now.plusSeconds(120));
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
    }

    @Test
    void 확인_대기가_아닌_납부는_확정할_수_없다() {
        var payment = payment(30_000);

        assertThatThrownBy(() -> payment.confirm(now))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("확인 대기");
    }

    @Test
    void 납부_금액은_음수일_수_없다() {
        assertThatIllegalArgumentException().isThrownBy(() -> payment(-1));
    }

    @Test
    void 미납_참가비는_참가_취소와_함께_무효화된다() {
        var payment = payment(30_000);

        payment.cancelForEvent(now.plusSeconds(60));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.VOID);
    }

    @Test
    void 납부_완료된_참가비는_참가_취소시_환불_대기가_된다() {
        var payment = payment(30_000);
        payment.report(now.plusSeconds(60));
        payment.confirm(now.plusSeconds(120));

        payment.cancelForEvent(now.plusSeconds(180));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUND_PENDING);
    }

    @Test
    void 환불_대기_납부를_환불_완료한다() {
        var payment = payment(30_000);
        payment.report(now.plusSeconds(60));
        payment.confirm(now.plusSeconds(120));
        payment.cancelForEvent(now.plusSeconds(180));

        payment.completeRefund(now.plusSeconds(240));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    private PaymentObligation payment(long amount) {
        return new PaymentObligation(1L, PaymentType.MEMBERSHIP_DUE, amount,
                PaymentSourceType.DUES_ROUND, 7L, "2학기 회비", now.plusSeconds(3600), now);
    }
}
