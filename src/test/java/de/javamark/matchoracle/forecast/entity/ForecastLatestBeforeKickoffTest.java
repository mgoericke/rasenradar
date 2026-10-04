package de.javamark.matchoracle.forecast.entity;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Spec 01, "Blickfang der Startseite": the spotlight compares against the last forecast before kickoff only. */
class ForecastLatestBeforeKickoffTest {

    private static final Instant KICKOFF = Instant.parse("2026-09-19T13:30:00Z");

    @Test
    void takesTheNewestForecastMadeBeforeKickoff() {
        Forecast older = forecast(KICKOFF.minus(Duration.ofDays(1)), false);
        Forecast newer = forecast(KICKOFF.minus(Duration.ofHours(2)), false);

        assertEquals(newer, Forecast.latestBeforeKickoff(List.of(older, newer)).orElseThrow());
    }

    @Test
    void ignoresBacktestsEvenIfTheyAreNewer() {
        Forecast live = forecast(KICKOFF.minus(Duration.ofHours(2)), false);
        Forecast backtest = forecast(KICKOFF.plus(Duration.ofDays(1)), true);

        assertEquals(live, Forecast.latestBeforeKickoff(List.of(backtest, live)).orElseThrow());
    }

    @Test
    void isEmptyWhenOnlyABacktestExists() {
        assertTrue(Forecast.latestBeforeKickoff(List.of(forecast(KICKOFF.plus(Duration.ofDays(1)), true))).isEmpty());
    }

    private static Forecast forecast(Instant createdAt, boolean backtest) {
        Forecast f = new Forecast();
        f.kickoff = KICKOFF;
        f.createdAt = createdAt;
        f.backtest = backtest;
        return f;
    }
}
