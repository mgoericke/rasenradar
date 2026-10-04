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

/** Spec 01, "Blickfang der Startseite": three phases, switched per kickoff block. */
class SpotlightSelectorTest {

    private static final Duration LIVE_WINDOW = Duration.ofMinutes(150);
    private static final Duration HOLD = Duration.ofHours(1);

    // A Saturday: 2. Bundesliga at 13:00, 1. Bundesliga at 15:30 and 18:30 (all UTC+2 → written in UTC).
    private static final Instant AT_13 = Instant.parse("2026-09-19T11:00:00Z");
    private static final Instant AT_1530 = Instant.parse("2026-09-19T13:30:00Z");
    private static final Instant AT_1830 = Instant.parse("2026-09-19T16:30:00Z");

    @Test
    void showsAllRunningMatchesOfBothLeaguesWhileAnyIsLive() {
        // A Sunday: 2. Bundesliga at 13:30 still running when the 1. Bundesliga kicks off at 15:30.
        Match bl2Live = match(AT_1530.minus(Duration.ofHours(2)), false);
        Match bl1Live = match(AT_1530, false);
        Match bl1Done = match(AT_1530, true);
        Instant now = AT_1530.plus(Duration.ofMinutes(10));

        Selection s = SpotlightSelector.select(List.of(bl1Done, bl1Live, bl2Live, match(AT_1830, false)), now, LIVE_WINDOW, HOLD).orElseThrow();

        assertEquals(SpotlightPhase.LIVE, s.phase());
        assertEquals(List.of(bl2Live, bl1Live), s.matches());
    }

    @Test
    void showsTheResultsOfTheLastBlockOnceItIsOver() {
        Match a = match(AT_1530, true), b = match(AT_1530, true);
        Instant now = AT_1530.plus(Duration.ofMinutes(110)); // 17:20

        Selection s = SpotlightSelector.select(List.of(a, b, match(AT_1830, false)), now, LIVE_WINDOW, HOLD).orElseThrow();

        assertEquals(SpotlightPhase.RESULTS, s.phase());
        assertEquals(AT_1530, s.kickoff());
        assertEquals(List.of(a, b), s.matches());
    }

    @Test
    void switchesToThePreviewOnceTheNextBlockIsCloserThanTheThreshold() {
        Match next = match(AT_1830, false);
        Instant now = AT_1830.minus(Duration.ofMinutes(45));

        Selection s = SpotlightSelector.select(List.of(match(AT_1530, true), next), now, LIVE_WINDOW, HOLD).orElseThrow();

        assertEquals(SpotlightPhase.PREVIEW, s.phase());
        assertEquals(List.of(next), s.matches());
    }

    @Test
    void theThresholdDecidesHowLongResultsStay() {
        Instant now = AT_1830.minus(Duration.ofMinutes(45));
        List<Match> matches = List.of(match(AT_1530, true), match(AT_1830, false));

        Selection s = SpotlightSelector.select(matches, now, LIVE_WINDOW, Duration.ofMinutes(30)).orElseThrow();

        assertEquals(SpotlightPhase.RESULTS, s.phase());
    }

    @Test
    void previewsTheWholeNextBlockBeforeTheMatchdayStarts() {
        Match a = match(AT_1530, false), b = match(AT_1530, false);

        Selection s = SpotlightSelector.select(List.of(b, a, match(AT_1830, false)), AT_13, LIVE_WINDOW, HOLD).orElseThrow();

        assertEquals(SpotlightPhase.PREVIEW, s.phase());
        assertEquals(AT_1530, s.kickoff());
        assertEquals(2, s.matches().size());
    }

    @Test
    void disappearsWhenNoBlockIsComingUp() {
        Instant now = AT_1830.plus(Duration.ofHours(4));

        assertTrue(SpotlightSelector.select(List.of(match(AT_1830, true)), now, LIVE_WINDOW, HOLD).isEmpty());
    }

    @Test
    void aMatchPastItsLiveWindowWithoutResultStillBelongsToTheResultsBlock() {
        Match noResultYet = match(AT_13, false);
        Instant now = AT_13.plus(Duration.ofHours(3)); // 16:00, 15:30 block not in candidates, next block far

        Selection s = SpotlightSelector.select(List.of(noResultYet, match(AT_1830, false)), now, LIVE_WINDOW, HOLD).orElseThrow();

        assertEquals(SpotlightPhase.RESULTS, s.phase());
        assertEquals(List.of(noResultYet), s.matches());
    }

    private static Match match(Instant kickoff, boolean played) {
        Match m = new Match();
        m.kickoff = kickoff;
        m.fullTimeScore = played ? new Score(1, 0) : null;
        return m;
    }
}
