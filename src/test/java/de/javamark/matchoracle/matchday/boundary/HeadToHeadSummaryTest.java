package de.javamark.matchoracle.matchday.boundary;

import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.BiggestWin;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.HeadToHeadSummary;
import de.javamark.matchoracle.matchday.entity.League;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.Matchday;
import de.javamark.matchoracle.matchday.entity.ResultStatus;
import de.javamark.matchoracle.matchday.entity.Score;
import de.javamark.matchoracle.matchday.entity.Team;
import de.javamark.matchoracle.matchday.entity.TeamMatchView;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Story r035: the all-time head-to-head on the match page. */
class HeadToHeadSummaryTest {

    private final Team bayern = team("Bayern");
    private final Team dortmund = team("Dortmund");

    @Test
    void provisionalMeetingsAreListedButNotCounted() {
        HeadToHeadSummary h = summary(
                meeting("2026-03-07", dortmund, 2, bayern, 2, ResultStatus.PROVISIONAL),
                meeting("2025-11-08", bayern, 3, dortmund, 1, ResultStatus.FINAL),
                meeting("2025-04-12", dortmund, 1, bayern, 0, ResultStatus.FINAL));

        assertEquals(2, h.total());
        assertEquals(1, h.wins());
        assertEquals(0, h.draws());
        assertEquals(1, h.losses());
        assertEquals(3, h.recent().size());
        assertTrue(h.recentProvisional());
        assertEquals("Dortmund", h.recent().get(0).homeTeam());
        assertEquals("2:2", h.recent().get(0).score());
        assertEquals("2021/22", h.since());
    }

    @Test
    void onlyTheFiveNewestAreListed() {
        List<TeamMatchView> meetings = IntStream.range(0, 7)
                .mapToObj(i -> meeting("202" + (5 - i / 4) + "-0" + (9 - i % 4) + "-01", bayern, i, dortmund, 0, ResultStatus.FINAL))
                .toList();
        HeadToHeadSummary h = HeadToHeadSummary.of(bayern, dortmund, meetings, 2021, "bl1");

        assertEquals(7, h.total());
        assertEquals(5, h.recent().size());
        assertEquals("Sept. 2025", h.recent().get(0).month());
        assertFalse(h.recentProvisional());
    }

    @Test
    void biggestWinIsTheWidestMarginFromTheWinnersSide() {
        HeadToHeadSummary h = summary(
                meeting("2025-11-08", bayern, 2, dortmund, 0, ResultStatus.FINAL),
                meeting("2024-03-30", bayern, 0, dortmund, 4, ResultStatus.FINAL),
                meeting("2023-04-01", bayern, 4, dortmund, 2, ResultStatus.FINAL));

        BiggestWin b = h.biggestWin();
        assertEquals("Dortmund", b.winner());
        assertEquals("Bayern", b.loser());
        assertEquals("4:0", b.score());
        assertEquals(2024, b.year());
    }

    @Test
    void onEqualMarginMoreGoalsThenTheNewerOneWins() {
        HeadToHeadSummary h = summary(
                meeting("2025-11-08", bayern, 2, dortmund, 0, ResultStatus.FINAL),
                meeting("2024-03-30", dortmund, 1, bayern, 3, ResultStatus.FINAL),
                meeting("2023-04-01", dortmund, 3, bayern, 1, ResultStatus.FINAL));

        assertEquals("3:1", h.biggestWin().score());
        assertEquals(2024, h.biggestWin().year());
    }

    @Test
    void onlyDrawsMeansNoBiggestWinAndNoMeetingsMeansEmpty() {
        assertNull(summary(meeting("2025-11-08", bayern, 1, dortmund, 1, ResultStatus.FINAL)).biggestWin());

        HeadToHeadSummary none = summary();
        assertEquals(0, none.total());
        assertTrue(none.recent().isEmpty());
        assertNull(none.biggestWin());
        assertEquals(0, none.share(0));
    }

    @Test
    void sharesAddUpForTheBar() {
        HeadToHeadSummary h = summary(
                meeting("2025-11-08", bayern, 2, dortmund, 0, ResultStatus.FINAL),
                meeting("2025-04-12", bayern, 1, dortmund, 1, ResultStatus.FINAL),
                meeting("2024-11-30", dortmund, 1, bayern, 0, ResultStatus.FINAL));

        assertEquals(33.3, h.share(h.wins()));
        assertEquals(33.3, h.share(h.draws()));
    }

    private HeadToHeadSummary summary(TeamMatchView... newestFirst) {
        return HeadToHeadSummary.of(bayern, dortmund, List.of(newestFirst), 2021, "bl1");
    }

    private TeamMatchView meeting(String day, Team home, int homeGoals, Team away, int awayGoals, ResultStatus status) {
        Matchday matchday = new Matchday();
        matchday.league = League.BUNDESLIGA_1;
        matchday.season = 2025;
        Match m = new Match();
        m.matchday = matchday;
        m.homeTeam = home;
        m.awayTeam = away;
        m.kickoff = Instant.parse(day + "T14:30:00Z");
        m.fullTimeScore = new Score(homeGoals, awayGoals);
        m.resultStatus = status;
        return m.viewedBy(bayern);
    }

    private static Team team(String name) {
        Team t = new Team();
        t.name = name;
        t.shortName = name;
        return t;
    }
}
