package br.com.nevvesdev.dcache.adapter.out.cache;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CacheCircuitBreakerTest {

    private static final Duration OPEN_DURATION = Duration.ofSeconds(30);

    private final MutableClock clock = new MutableClock();
    private final CacheCircuitBreaker breaker = new CacheCircuitBreaker(3, OPEN_DURATION, clock);

    private void failTimes(int times) {
        for (int i = 0; i < times; i++) {
            breaker.recordFailure();
        }
    }

    @Test
    void shouldAllowRequestsWhileClosed() {
        assertThat(breaker.allowRequest()).isTrue();
        assertThat(breaker.allowRequest()).isTrue();
    }

    @Test
    void shouldStayClosedBelowFailureThreshold() {
        failTimes(2);

        assertThat(breaker.allowRequest()).isTrue();
    }

    @Test
    void shouldOpenWhenFailureThresholdIsReached() {
        failTimes(3);

        assertThat(breaker.allowRequest()).isFalse();
    }

    @Test
    void shouldResetFailureCountOnSuccess() {
        failTimes(2);
        breaker.recordSuccess();
        failTimes(2);

        assertThat(breaker.allowRequest()).isTrue();
    }

    @Test
    void shouldAllowSingleTrialAfterOpenDuration() {
        failTimes(3);
        clock.advance(OPEN_DURATION);

        assertThat(breaker.allowRequest()).isTrue();
        assertThat(breaker.allowRequest()).isFalse();
    }

    @Test
    void shouldCloseAgainWhenTrialSucceeds() {
        failTimes(3);
        clock.advance(OPEN_DURATION);
        breaker.allowRequest();

        breaker.recordSuccess();

        assertThat(breaker.allowRequest()).isTrue();
        assertThat(breaker.allowRequest()).isTrue();
    }

    @Test
    void shouldReopenWhenTrialFails() {
        failTimes(3);
        clock.advance(OPEN_DURATION);
        breaker.allowRequest();

        breaker.recordFailure();

        assertThat(breaker.allowRequest()).isFalse();
        clock.advance(OPEN_DURATION);
        assertThat(breaker.allowRequest()).isTrue();
    }

    @Test
    void shouldRejectInvalidConfiguration() {
        assertThatThrownBy(() -> new CacheCircuitBreaker(0, OPEN_DURATION, clock))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CacheCircuitBreaker(3, Duration.ZERO, clock))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CacheCircuitBreaker(3, null, clock))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldReportOpenStateWhileOpenOrAwaitingTrial() {
        assertThat(breaker.isOpen()).isFalse();

        failTimes(3);
        assertThat(breaker.isOpen()).isTrue();

        clock.advance(OPEN_DURATION);
        assertThat(breaker.isOpen()).isTrue();

        breaker.allowRequest();
        breaker.recordSuccess();
        assertThat(breaker.isOpen()).isFalse();
    }
}
