package de.javamark.matchoracle.matchday.control;

import java.time.Duration;
import java.time.Instant;

/**
 * Spec 01, "Prüftakt": every minute while a match of the league is running, otherwise at the
 * idle interval. The scheduler ticks at the short interval; this decides per league whether the
 * tick actually goes to the source — OpenLigaDB is a free community service and should not be
 * asked every minute at three in the morning.
 */
final class SyncCadence {

    private SyncCadence() {
    }

    static boolean isDue(boolean matchRunning, Instant lastSync, Instant now, Duration idleInterval) {
        return matchRunning || lastSync == null || !lastSync.plus(idleInterval).isAfter(now);
    }
}
