package de.javamark.matchoracle.matchday.boundary;

import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.ConferenceReplay;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.ReplayGoal;
import de.javamark.matchoracle.matchday.entity.Goal;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.Score;
import de.javamark.matchoracle.matchday.entity.Team;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Story r052: the data the "Konferenz nachspielen" board plays back. */
class ConferenceReplayTest {

    private static Team team(long id, String name, String shortName) {
        Team t = new Team();
        t.id = id;
        t.name = name;
        t.shortName = shortName;
        return t;
    }

    private static Goal goal(Integer minute, String scorer, int home, int away, boolean penalty, boolean ownGoal) {
        Goal g = new Goal();
        g.minute = minute;
        g.scorerName = scorer;
        g.scoreAfter = new Score(home, away);
        g.penalty = penalty;
        g.ownGoal = ownGoal;
        return g;
    }

    private static Match match(long id, Team home, Team away, int homeGoals, int awayGoals, String kickoff, Goal... goals) {
        Match m = new Match();
        m.id = id;
        m.homeTeam = home;
        m.awayTeam = away;
        m.kickoff = Instant.parse(kickoff);
        m.fullTimeScore = new Score(homeGoals, awayGoals);
        m.goals.addAll(List.of(goals));
        return m;
    }

    @Test
    void everyGoalKeepsItsMinuteSideAndKindAndTheTotalIsTheRealOne() {
        Team leipzig = team(1, "RB Leipzig", "Leipzig");
        Team elversberg = team(2, "SV Elversberg", "Elversberg");
        Team bremen = team(3, "SV Werder Bremen", "Bremen");
        Team mainz = team(4, "1. FSV Mainz 05", "Mainz");
        Match late = match(10, leipzig, elversberg, 1, 1, "2026-10-25T14:30:00Z",
                goal(12, "Romulo", 1, 0, true, false),
                goal(94, "Fofana", 1, 1, false, true));
        Match early = match(11, mainz, bremen, 0, 0, "2026-10-24T18:30:00Z");

        ConferenceReplay replay = ConferenceReplay.of("7. Spieltag", List.of(late, early), "bl1");

        assertEquals(2, replay.goals());
        assertEquals(List.of("Mainz", "Leipzig"), replay.matches().stream().map(m -> m.homeShort()).toList());
        List<ReplayGoal> goals = replay.matches().get(1).goals();
        assertEquals(12, goals.get(0).minute());
        assertTrue(goals.get(0).forHome());
        assertEquals("Elfmeter", goals.get(0).note());
        assertEquals(94, goals.get(1).minute());
        assertFalse(goals.get(1).forHome());                 // an own goal counts for the side it helped
        assertEquals("Eigentor", goals.get(1).note());
        assertEquals("1:1", replay.matches().get(1).result());
        assertEquals("/bl1/matches/10", replay.matches().get(1).link());
    }

    @Test
    void aGoalWithoutAMinuteFallsAtTheNinetieth() {
        Match m = match(12, team(5, "A", "A"), team(6, "B", "B"), 1, 0, "2026-10-25T14:30:00Z",
                goal(null, "X", 1, 0, false, false));

        assertEquals(90, ConferenceReplay.of("t", List.of(m), "bl2").matches().getFirst().goals().getFirst().minute());
    }
}
