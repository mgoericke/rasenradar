package de.javamark.matchoracle.matchday.control;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Spec 01, "Prüftakt": every minute while a match runs, otherwise every 15 minutes. */
class SyncCadenceTest {

    private static final Instant NOW = Instant.parse("2026-10-10T14:00:00Z");
    private static final Duration IDLE = Duration.ofMinutes(15);

    @Test
    void whileAMatchRunsEveryTickGoesToTheSource() {
        assertTrue(SyncCadence.isDue(true, NOW.minus(Duration.ofMinutes(1)), NOW, IDLE));
    }

    @Test
    void withoutARunningMatchTheIdleIntervalApplies() {
        assertFalse(SyncCadence.isDue(false, NOW.minus(Duration.ofMinutes(5)), NOW, IDLE));
        assertTrue(SyncCadence.isDue(false, NOW.minus(Duration.ofMinutes(15)), NOW, IDLE));
    }

    @Test
    void theFirstRunAfterStartupAlwaysSyncs() {
        assertTrue(SyncCadence.isDue(false, null, NOW, IDLE));
    }
}
