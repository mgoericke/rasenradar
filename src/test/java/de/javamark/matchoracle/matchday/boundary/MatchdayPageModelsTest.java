package de.javamark.matchoracle.matchday.boundary;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Spec 1: a match counts as "läuft" purely from its kickoff time — no live score. */
class MatchdayPageModelsTest {

    private static final Instant NOW = Instant.parse("2026-09-20T16:00:00Z");
    private static final Duration WINDOW = Duration.ofMinutes(150);

    @Test
    void notYetKickedOffIsNotLive() {
        assertFalse(MatchdayPageModels.isLive(NOW.plusSeconds(60), false, NOW, WINDOW));
    }

    @Test
    void withinTheWindowAfterKickoffAndUnplayedIsLive() {
        assertTrue(MatchdayPageModels.isLive(NOW.minus(Duration.ofMinutes(80)), false, NOW, WINDOW));
    }

    @Test
    void beyondTheWindowIsNoLongerLive() {
        assertFalse(MatchdayPageModels.isLive(NOW.minus(Duration.ofMinutes(200)), false, NOW, WINDOW));
    }

    @Test
    void aPlayedMatchIsNeverLiveEvenWithinTheWindow() {
        assertFalse(MatchdayPageModels.isLive(NOW.minus(Duration.ofMinutes(80)), true, NOW, WINDOW));
    }

    @Test
    void kickoffThisInstantIsLive() {
        assertTrue(MatchdayPageModels.isLive(NOW, false, NOW, WINDOW));
    }
}
