package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.control.ScenarioScores.Rescored;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.Score;
import de.javamark.matchoracle.matchday.entity.Team;
import org.junit.jupiter.api.Test;

import java.util.List;

import static de.javamark.matchoracle.matchday.control.MatchFixtures.goal;
import static de.javamark.matchoracle.matchday.control.MatchFixtures.played;
import static de.javamark.matchoracle.matchday.control.MatchFixtures.provisional;
import static de.javamark.matchoracle.matchday.control.MatchFixtures.team;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Story r071: the table at half time and "Schluss nach 80" — the minute-80 boundary and what is left out. */
class ScenarioScoresTest {

    private final Team bremen = team("Bremen");
    private final Team koeln = team("Köln");

    @Test
    void aGoalInMinuteEightyCountsEightyOneDoesNot() {
        Match m = played(1, bremen, 2, koeln, 1);
        goal(m, "A", 1, 0).minute = 12;
        goal(m, "B", 1, 1).minute = 80;
        goal(m, "C", 2, 1).minute = 81;

        Rescored r = ScenarioScores.afterEighty(List.of(m));

        assertEquals(1, r.matches().getFirst().fullTimeScore.home);
        assertEquals(1, r.matches().getFirst().fullTimeScore.away);
        // the stored match itself stays untouched
        assertEquals(2, m.fullTimeScore.home);
    }

    @Test
    void matchesWithoutMinutesOrWithoutHalfTimeAreLeftOutAndCounted() {
        Match noMinute = played(1, bremen, 1, koeln, 0);
        goal(noMinute, "A", 1, 0); // minute unknown
        Match noHalfTime = played(2, koeln, 0, bremen, 0);
        Match running = provisional(3, bremen, 1, koeln, 0);
        running.halfTimeScore = new Score(1, 0);

        assertEquals(1, ScenarioScores.afterEighty(List.of(noMinute, noHalfTime, running)).excluded());
        Rescored half = ScenarioScores.atHalfTime(List.of(noMinute, noHalfTime, running));
        assertEquals(2, half.excluded());
        assertEquals(0, half.matches().size());
    }

    @Test
    void halfTimeUsesTheHalfTimeScore() {
        Match m = played(1, bremen, 3, koeln, 2);
        m.halfTimeScore = new Score(0, 2);

        Match rescored = ScenarioScores.atHalfTime(List.of(m)).matches().getFirst();
        assertEquals(0, rescored.fullTimeScore.home);
        assertEquals(2, rescored.fullTimeScore.away);
    }
}
