package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.control.ComebackCalculator.ClubComebacks;
import de.javamark.matchoracle.matchday.control.ComebackCalculator.Comebacks;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.Team;
import org.junit.jupiter.api.Test;

import java.util.List;

import static de.javamark.matchoracle.matchday.control.MatchFixtures.goal;
import static de.javamark.matchoracle.matchday.control.MatchFixtures.played;
import static de.javamark.matchoracle.matchday.control.MatchFixtures.provisional;
import static de.javamark.matchoracle.matchday.control.MatchFixtures.team;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Story r091 "Aufholjagden": points won after trailing, points dropped after leading. */
class ComebackCalculatorTest {

    private final Team hoffenheim = team("Hoffenheim");
    private final Team dortmund = team("Dortmund");

    private static ClubComebacks of(Comebacks c, Team t) {
        return c.clubs().stream().filter(x -> x.team() == t).findFirst().orElseThrow();
    }

    @Test
    void theLabExampleTwoNilDownAndStillWon() {
        Match m = played(1, hoffenheim, 2, dortmund, 3);
        goal(m, "A", 1, 0);
        goal(m, "B", 2, 0);
        goal(m, "C", 2, 1);
        goal(m, "D", 2, 2);
        goal(m, "E", 2, 3);

        Comebacks c = ComebackCalculator.of(List.of(m));

        assertEquals(3, of(c, dortmund).won());
        assertEquals(3, of(c, hoffenheim).dropped());
        assertEquals(dortmund, c.biggest().team());
        assertEquals(2, c.biggest().deficit());
        assertTrue(c.biggest().won());
        assertEquals(dortmund, c.clubs().getFirst().team());
    }

    @Test
    void aDrawAfterTrailingIsOnePointWonAndTwoDroppedForTheOtherSide() {
        Match m = played(1, hoffenheim, 1, dortmund, 1);
        goal(m, "A", 0, 1);
        goal(m, "B", 1, 1);

        Comebacks c = ComebackCalculator.of(List.of(m));

        assertEquals(1, of(c, hoffenheim).won());
        assertEquals(2, of(c, dortmund).dropped());
    }

    @Test
    void aWinAfterLeadingThroughoutCountsNowhereNorDoesNilNil() {
        Match lead = played(1, hoffenheim, 2, dortmund, 0);
        goal(lead, "A", 1, 0);
        goal(lead, "B", 2, 0);
        Match nil = played(2, dortmund, 0, hoffenheim, 0);

        Comebacks c = ComebackCalculator.of(List.of(lead, nil));

        assertEquals(0, of(c, hoffenheim).won() + of(c, hoffenheim).dropped());
        assertEquals(0, of(c, dortmund).won() + of(c, dortmund).dropped());
        assertNull(c.biggest());
        assertEquals(2, c.matches());
    }

    @Test
    void incompleteGoalListsAndProvisionalResultsAreLeftOut() {
        Match missing = played(1, hoffenheim, 1, dortmund, 2);
        goal(missing, "A", 1, 0); // two goals missing
        Match running = provisional(2, dortmund, 1, hoffenheim, 2);
        goal(running, "B", 1, 0);
        goal(running, "C", 1, 1);
        goal(running, "D", 1, 2);

        Comebacks c = ComebackCalculator.of(List.of(missing, running));

        assertEquals(0, c.matches());
        assertEquals(1, c.excluded());
        assertNull(c.biggest());
    }
}
