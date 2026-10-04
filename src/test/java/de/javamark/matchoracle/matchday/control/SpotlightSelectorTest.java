package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.control.SpotlightSelector.Selection;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.Score;
import de.javamark.matchoracle.matchday.entity.SpotlightPhase;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Spec 01, "Blickfang der Startseite": running matches, otherwise the next block — never a finished match. */
class SpotlightSelectorTest {

    private static final Duration LIVE_WINDOW = Duration.ofMinutes(150);

    // A Saturday (UTC+2, written in UTC): 1. Bundesliga at 15:30 and 18:30.
    private static final Instant AT_1530 = Instant.parse("2026-09-19T13:30:00Z");
    private static final Instant AT_1830 = Instant.parse("2026-09-19T16:30:00Z");

    @Test
    void showsAllRunningMatchesOfBothLeaguesWhileAnyIsLive() {
        // A Sunday: 2. Bundesliga at 13:30 still running when the 1. Bundesliga kicks off at 15:30.
        Match bl2Live = match(AT_1530.minus(Duration.ofHours(2)), false);
        Match bl1Live = match(AT_1530, false);
        Match bl1Done = match(AT_1530, true);
        Instant now = AT_1530.plus(Duration.ofMinutes(10));

        Selection s = SpotlightSelector.select(List.of(bl1Done, bl1Live, bl2Live, match(AT_1830, false)), now, LIVE_WINDOW).orElseThrow();

        assertEquals(SpotlightPhase.LIVE, s.phase());
        assertEquals(List.of(bl2Live, bl1Live), s.matches());
    }

    @Test
    void previewsTheWholeNextBlockWhenNothingIsRunning() {
        Match a = match(AT_1830, false), b = match(AT_1830, false);
        Instant now = AT_1530.plus(Duration.ofMinutes(110)); // 17:20, the 15:30 block is over

        Selection s = SpotlightSelector.select(List.of(match(AT_1530, true), b, a), now, LIVE_WINDOW).orElseThrow();

        assertEquals(SpotlightPhase.PREVIEW, s.phase());
        assertEquals(AT_1830, s.kickoff());
        assertEquals(2, s.matches().size());
    }

    @Test
    void neverShowsAFinishedMatchEvenIfTheNextBlockIsWeeksAway() {
        Match next = match(AT_1830.plus(Duration.ofDays(19)), false);

        Selection s = SpotlightSelector.select(List.of(match(AT_1830, true), next), AT_1830.plus(Duration.ofDays(1)), LIVE_WINDOW).orElseThrow();

        assertEquals(List.of(next), s.matches());
    }

    @Test
    void disappearsWhenNoBlockIsComingUp() {
        Instant now = AT_1830.plus(Duration.ofHours(4));

        assertTrue(SpotlightSelector.select(List.of(match(AT_1830, true)), now, LIVE_WINDOW).isEmpty());
    }

    private static Match match(Instant kickoff, boolean played) {
        Match m = new Match();
        m.kickoff = kickoff;
        m.fullTimeScore = played ? new Score(1, 0) : null;
        return m;
    }
}
