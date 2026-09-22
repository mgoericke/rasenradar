package de.javamark.matchoracle.matchday.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Spec 01: the next matchday only takes over the day after the previous one's last kickoff (Europe/Berlin). */
class MatchdayTest {

    private static final Instant LAST_KICKOFF = Instant.parse("2026-09-20T18:30:00Z"); // 20:30 Europe/Berlin

    @Test
    void stillShowingLaterTheSameEvening() {
        assertTrue(Matchday.stillShowing(LAST_KICKOFF, LAST_KICKOFF.plusSeconds(3 * 3600)));
    }

    @Test
    void noLongerShowingTheNextCalendarDay() {
        assertFalse(Matchday.stillShowing(LAST_KICKOFF, Instant.parse("2026-09-21T06:00:00Z")));
    }

    @Test
    void stillShowingRightUntilMidnightBerlinTime() {
        assertTrue(Matchday.stillShowing(LAST_KICKOFF, Instant.parse("2026-09-20T21:59:59Z")));
    }

    @Test
    void aKnockoutMatchdayIsNamedAfterItsRound() {
        Matchday round = new Matchday();
        round.league = League.BUNDESLIGA_1;
        round.season = 2026;
        round.number = 3;
        round.label = "Achtelfinale";

        assertEquals("Achtelfinale", round.displayName());
    }

    @Test
    void aMatchdayWithoutALabelIsCountedAsUsual() {
        Matchday plain = new Matchday();
        plain.league = League.BUNDESLIGA_1;
        plain.season = 2026;
        plain.number = 3;

        assertEquals("3. Spieltag", plain.displayName());
    }
}
