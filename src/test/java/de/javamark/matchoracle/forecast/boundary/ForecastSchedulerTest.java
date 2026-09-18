package de.javamark.matchoracle.forecast.boundary;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Spec 02, "Auslöser": the Terminplanung re-forecasts an upcoming match once its last one is stale. */
class ForecastSchedulerTest {

    private static final Instant NOW = Instant.parse("2026-09-20T12:00:00Z");
    private static final Duration INTERVAL = Duration.ofHours(24);

    @Test
    void aMatchWithoutAnyForecastYetIsDue() {
        assertTrue(ForecastScheduler.isDue(Optional.empty(), NOW, INTERVAL));
    }

    @Test
    void aMatchForecastLongerAgoThanTheIntervalIsDue() {
        Instant staleSince = NOW.minus(INTERVAL).minusSeconds(1);

        assertTrue(ForecastScheduler.isDue(Optional.of(staleSince), NOW, INTERVAL));
    }

    @Test
    void aMatchForecastWithinTheIntervalIsNotDue() {
        Instant recentlyForecast = NOW.minus(INTERVAL).plusSeconds(1);

        assertFalse(ForecastScheduler.isDue(Optional.of(recentlyForecast), NOW, INTERVAL));
    }
}
