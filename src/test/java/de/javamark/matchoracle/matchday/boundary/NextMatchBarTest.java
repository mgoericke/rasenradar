package de.javamark.matchoracle.matchday.boundary;

import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.NextMatchBar;
import de.javamark.matchoracle.matchday.entity.League;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.Matchday;
import de.javamark.matchoracle.matchday.entity.ResultStatus;
import de.javamark.matchoracle.matchday.entity.Score;
import de.javamark.matchoracle.matchday.entity.Team;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Story r092: the next match of "Mein Verein". Times in UTC; Berlin is UTC+1 in November. */
class NextMatchBarTest {

    private static final Duration WINDOW = Duration.ofMinutes(150);
    private static final Instant NOW = Instant.parse("2026-11-05T09:04:00Z"); // Thursday 10:04 in Berlin

    private final Team bayern = team("FC Bayern München", "Bayern");
    private final Team mainz = team("1. FSV Mainz 05", "");

    @Test
    void sameDayTomorrowAndLaterAreWordedByCalendarDay() {
        assertEquals("heute 15:30", NextMatchBar.when(Instant.parse("2026-11-05T14:30:00Z"), NOW));
        assertEquals("morgen 20:30", NextMatchBar.when(Instant.parse("2026-11-06T19:30:00Z"), NOW));
        assertEquals("Sa., 7.11. 15:30", NextMatchBar.when(Instant.parse("2026-11-07T14:30:00Z"), NOW));
    }

    @Test
    void countdownInMinutesHoursOrDays() {
        assertEquals("in 56 Min", NextMatchBar.countdown(Instant.parse("2026-11-05T10:00:00Z"), NOW));
        assertEquals("in 5 Std 26 Min", NextMatchBar.countdown(Instant.parse("2026-11-05T14:30:00Z"), NOW));
        assertEquals("in 6 Std", NextMatchBar.countdown(Instant.parse("2026-11-05T15:04:00Z"), NOW));
        assertEquals("in einem Tag", NextMatchBar.countdown(Instant.parse("2026-11-06T19:30:00Z"), NOW));
        assertEquals("in 3 Tagen", NextMatchBar.countdown(Instant.parse("2026-11-08T12:00:00Z"), NOW));
        assertNull(NextMatchBar.countdown(NOW, NOW));
    }

    @Test
    void aFinishedMatchGivesWayToTheNextOpenOne() {
        Match done = match(NOW.minus(Duration.ofMinutes(100)), bayern, mainz, new Score(2, 1), ResultStatus.FINAL);
        Match next = match(NOW.plus(Duration.ofDays(3)), mainz, bayern, null, ResultStatus.PROVISIONAL);

        assertSame(next, NextMatchBar.pick(List.of(done, next), NOW, WINDOW));
    }

    @Test
    void aRunningMatchAndAJustEndedProvisionalOneStay() {
        Match running = match(NOW.minus(Duration.ofMinutes(30)), bayern, mainz, null, ResultStatus.PROVISIONAL);
        Match next = match(NOW.plus(Duration.ofDays(3)), mainz, bayern, null, ResultStatus.PROVISIONAL);
        assertSame(running, NextMatchBar.pick(List.of(running, next), NOW, WINDOW));

        Match ended = match(NOW.minus(Duration.ofMinutes(110)), bayern, mainz, new Score(1, 1), ResultStatus.PROVISIONAL);
        assertSame(ended, NextMatchBar.pick(List.of(ended, next), NOW, WINDOW));

        Match endedLongAgo = match(NOW.minus(Duration.ofMinutes(200)), bayern, mainz, new Score(1, 1), ResultStatus.PROVISIONAL);
        assertSame(next, NextMatchBar.pick(List.of(endedLongAgo, next), NOW, WINDOW));
    }

    @Test
    void noMatchLeftMeansNoBar() {
        assertNull(NextMatchBar.pick(List.of(), NOW, WINDOW));
    }

    @Test
    void awayMatchReadsBeiAndFallsBackToTheFullName() {
        NextMatchBar bar = NextMatchBar.of(match(Instant.parse("2026-11-07T14:30:00Z"), mainz, bayern, null, ResultStatus.PROVISIONAL),
                bayern, NOW, WINDOW);

        assertEquals("Bayern bei 1. FSV Mainz 05", bar.title());
        assertEquals("in 2 Tagen", bar.countdown());
        assertFalse(bar.live());
        assertNull(bar.score());
    }

    @Test
    void runningMatchShowsLiveWithTheInterimScoreAndNoCountdown() {
        Match m = match(NOW.minus(Duration.ofMinutes(30)), bayern, mainz, null, ResultStatus.PROVISIONAL);
        m.liveScore = new Score(1, 0);
        NextMatchBar bar = NextMatchBar.of(m, bayern, NOW, WINDOW);

        assertEquals("Bayern gegen 1. FSV Mainz 05", bar.title());
        assertTrue(bar.live());
        assertEquals("1:0", bar.score());
        assertNull(bar.countdown());
    }

    @Test
    void provisionalResultIsMarked() {
        NextMatchBar bar = NextMatchBar.of(match(NOW.minus(Duration.ofMinutes(110)), bayern, mainz, new Score(2, 2), ResultStatus.PROVISIONAL),
                bayern, NOW, WINDOW);

        assertEquals("2:2", bar.score());
        assertTrue(bar.provisional());
        assertFalse(bar.live());
    }

    private static Match match(Instant kickoff, Team home, Team away, Score full, ResultStatus status) {
        Matchday matchday = new Matchday();
        matchday.league = League.BUNDESLIGA_1;
        matchday.season = 2026;
        Match m = new Match();
        m.id = kickoff.getEpochSecond();
        m.matchday = matchday;
        m.homeTeam = home;
        m.awayTeam = away;
        m.kickoff = kickoff;
        m.fullTimeScore = full;
        m.resultStatus = status;
        return m;
    }

    private static Team team(String name, String shortName) {
        Team t = new Team();
        t.name = name;
        t.shortName = shortName;
        return t;
    }
}
