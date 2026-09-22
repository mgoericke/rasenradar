package de.javamark.matchoracle.forecast.boundary;

import de.javamark.matchoracle.forecast.boundary.ForecastScheduler.LastAttempt;
import de.javamark.matchoracle.forecast.entity.Assessment;
import de.javamark.matchoracle.forecast.entity.Forecast;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spec 02, "Auslöser": the Terminplanung re-forecasts an upcoming match once its last
 * forecast is stale — and much sooner when the last attempt failed or produced a forecast
 * without a single working assessment.
 */
class ForecastSchedulerTest {

    private static final Instant NOW = Instant.parse("2026-09-20T12:00:00Z");
    private static final Duration RETRY_AFTER = Duration.ofHours(1);
    private static final Duration REFRESH_AFTER = Duration.ofHours(24);

    @Test
    void aMatchWithoutAnyAttemptYetIsDue() {
        assertTrue(ForecastScheduler.isDue(Optional.empty(), NOW, RETRY_AFTER, REFRESH_AFTER));
    }

    @Test
    void aUsableForecastIsRefreshedOnlyAfterTheRefreshInterval() {
        assertTrue(ForecastScheduler.isDue(usable(NOW.minus(REFRESH_AFTER).minusSeconds(1)), NOW, RETRY_AFTER, REFRESH_AFTER));
        assertFalse(ForecastScheduler.isDue(usable(NOW.minus(REFRESH_AFTER).plusSeconds(1)), NOW, RETRY_AFTER, REFRESH_AFTER));
    }

    @Test
    void aFailedAttemptIsRetriedAfterTheRetryInterval() {
        assertTrue(ForecastScheduler.isDue(failed(NOW.minus(RETRY_AFTER).minusSeconds(1)), NOW, RETRY_AFTER, REFRESH_AFTER));
        assertFalse(ForecastScheduler.isDue(failed(NOW.minus(RETRY_AFTER).plusSeconds(1)), NOW, RETRY_AFTER, REFRESH_AFTER));
    }

    @Test
    void aForecastWhoseAssessmentsAllFailedCountsAsFailedAttempt() {
        Forecast forecast = forecastAt(NOW.minusSeconds(10), true, true, true);

        LastAttempt attempt = LastAttempt.of(Optional.of(forecast), Optional.empty()).orElseThrow();

        assertEquals(forecast.createdAt, attempt.at());
        assertFalse(attempt.usable());
    }

    @Test
    void aForecastWithAtLeastOneWorkingAssessmentIsUsable() {
        Forecast forecast = forecastAt(NOW.minusSeconds(10), true, false, true);

        assertTrue(LastAttempt.of(Optional.of(forecast), Optional.empty()).orElseThrow().usable());
    }

    @Test
    void theLaterOfForecastAndFailedRunIsTheLastAttempt() {
        Forecast forecast = forecastAt(NOW.minusSeconds(100), false, false, false);
        Instant failedRun = NOW.minusSeconds(10);

        LastAttempt attempt = LastAttempt.of(Optional.of(forecast), Optional.of(failedRun)).orElseThrow();

        assertEquals(failedRun, attempt.at());
        assertFalse(attempt.usable());
    }

    @Test
    void aFailedRunOlderThanTheForecastDoesNotCount() {
        Forecast forecast = forecastAt(NOW.minusSeconds(10), false, false, false);
        Instant failedRun = NOW.minusSeconds(100);

        LastAttempt attempt = LastAttempt.of(Optional.of(forecast), Optional.of(failedRun)).orElseThrow();

        assertEquals(forecast.createdAt, attempt.at());
        assertTrue(attempt.usable());
    }

    @Test
    void withoutForecastOrFailedRunThereIsNoLastAttempt() {
        assertTrue(LastAttempt.of(Optional.empty(), Optional.empty()).isEmpty());
    }

    private static Optional<LastAttempt> usable(Instant at) {
        return Optional.of(new LastAttempt(at, true));
    }

    private static Optional<LastAttempt> failed(Instant at) {
        return Optional.of(new LastAttempt(at, false));
    }

    private static Forecast forecastAt(Instant createdAt, boolean... assessmentFailed) {
        Forecast forecast = new Forecast();
        forecast.createdAt = createdAt;
        for (boolean failed : assessmentFailed) {
            Assessment assessment = new Assessment();
            assessment.failed = failed;
            forecast.assessments.add(assessment);
        }
        return forecast;
    }
}
