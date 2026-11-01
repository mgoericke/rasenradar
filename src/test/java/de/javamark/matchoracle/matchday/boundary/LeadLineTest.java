package de.javamark.matchoracle.matchday.boundary;

import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.LeadLine;
import de.javamark.matchoracle.matchday.entity.Goal;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.ResultStatus;
import de.javamark.matchoracle.matchday.entity.Score;
import de.javamark.matchoracle.matchday.entity.Team;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Story r053: the lead line — steps by the running score, own goals for the side that benefits, clamped, only with a complete goal list. */
class LeadLineTest {

    private static Match match(int home, int away) {
        Match m = new Match();
        m.homeTeam = new Team();
        m.awayTeam = new Team();
        m.fullTimeScore = new Score(home, away);
        m.resultStatus = ResultStatus.FINAL;
        return m;
    }

    private static Goal goal(Match m, int minute, int home, int away) {
        Goal g = new Goal();
        g.match = m;
        g.position = m.goals.size() + 1;
        g.minute = minute;
        g.scoreAfter = new Score(home, away);
        m.goals.add(g);
        return g;
    }

    @Test
    void stepsUpForHomeAndDownForAwayGoals() {
        Match m = match(1, 1);
        goal(m, 19, 1, 0);
        goal(m, 76, 1, 1).ownGoal = true; // a home player's own goal: the line goes down, it counts for the guests

        LeadLine line = LeadLine.of(m);

        assertEquals("M0 12 H20.0 V7.0 H80.0 V12.0 H100", line.path());
        assertEquals("Eigentor", line.goals().get(1).note());
    }

    @Test
    void aBigLeadIsClampedButTheGoalListStaysExact() {
        Match m = match(4, 0);
        goal(m, 10, 1, 0);
        goal(m, 20, 2, 0);
        goal(m, 30, 3, 0);
        goal(m, 95, 4, 0);

        LeadLine line = LeadLine.of(m);

        assertEquals("M0 12 H10.5 V7.0 H21.1 V2.0 H31.6 V2.0 H100.0 V2.0 H100", line.path());
        assertEquals("4:0", line.goals().getLast().score());
    }

    @Test
    void noGoalsIsAFlatLineAnIncompleteListIsNone() {
        assertEquals("M0 12 H100", LeadLine.of(match(0, 0)).path());
        Match incomplete = match(2, 0);
        goal(incomplete, 10, 1, 0);
        assertNull(LeadLine.of(incomplete));
        Match unplayed = match(0, 0);
        unplayed.fullTimeScore = null;
        assertNull(LeadLine.of(unplayed));
    }
}
