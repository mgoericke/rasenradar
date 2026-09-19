package de.javamark.matchoracle.matchday.boundary;

import de.javamark.matchoracle.matchday.boundary.MatchdayPages.CandidateMatch;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.Score;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The landing page's spotlight: a live match takes priority, otherwise the next kickoff. */
class MatchdayPagesTest {

    private static final Instant NOW = Instant.parse("2026-09-20T16:00:00Z");
    private static final Duration WINDOW = Duration.ofMinutes(150);

    @Test
    void prefersALiveMatchOverAFutureOne() {
        CandidateMatch live = candidate(NOW.minus(Duration.ofMinutes(30)), false, "bl1");
        CandidateMatch future = candidate(NOW.plus(Duration.ofHours(2)), false, "bl2");

        Optional<CandidateMatch> spotlight = MatchdayPages.pickSpotlight(List.of(future, live), NOW, WINDOW);

        assertEquals(live, spotlight.orElseThrow());
    }

    @Test
    void fallsBackToTheEarliestFutureKickoffWhenNothingIsLive() {
        CandidateMatch soon = candidate(NOW.plus(Duration.ofHours(1)), false, "bl1");
        CandidateMatch later = candidate(NOW.plus(Duration.ofHours(3)), false, "bl2");

        Optional<CandidateMatch> spotlight = MatchdayPages.pickSpotlight(List.of(later, soon), NOW, WINDOW);

        assertEquals(soon, spotlight.orElseThrow());
    }

    @Test
    void ignoresAlreadyPlayedAndLongFinishedMatches() {
        CandidateMatch played = candidate(NOW.minus(Duration.ofHours(1)), true, "bl1");
        CandidateMatch longOver = candidate(NOW.minus(Duration.ofHours(5)), false, "bl2");

        assertTrue(MatchdayPages.pickSpotlight(List.of(played, longOver), NOW, WINDOW).isEmpty());
    }

    @Test
    void pickTheEarliestAmongSeveralLiveMatches() {
        CandidateMatch startedFirst = candidate(NOW.minus(Duration.ofMinutes(60)), false, "bl1");
        CandidateMatch startedLater = candidate(NOW.minus(Duration.ofMinutes(10)), false, "bl2");

        Optional<CandidateMatch> spotlight = MatchdayPages.pickSpotlight(List.of(startedLater, startedFirst), NOW, WINDOW);

        assertEquals(startedFirst, spotlight.orElseThrow());
    }

    private static CandidateMatch candidate(Instant kickoff, boolean played, String shortcut) {
        Match m = new Match();
        m.kickoff = kickoff;
        m.fullTimeScore = played ? new Score(1, 0) : null;
        return new CandidateMatch(m, shortcut);
    }
}
