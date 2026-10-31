package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.control.SpecialGoalsCalculator.Card;
import de.javamark.matchoracle.matchday.control.SpecialGoalsCalculator.Kind;
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

/** Story r084 "Tore der besonderen Art": boundaries, own-goal credit, ties on third place, final only. */
class SpecialGoalsCalculatorTest {

    private final Team bremen = team("Bremen");
    private final Team koeln = team("Köln");
    private final Team mainz = team("Mainz");
    private final Team kiel = team("Kiel");

    private static Card card(List<Card> cards, Kind kind) {
        return cards.stream().filter(c -> c.kind() == kind).findFirst().orElseThrow();
    }

    @Test
    void minuteNinetyAndStoppageTimeAreLateEightyNineIsNot() {
        Match m = played(1, bremen, 3, koeln, 0);
        goal(m, "A", 1, 0).minute = 89;
        goal(m, "B", 2, 0).minute = 90;
        goal(m, "C", 3, 0).minute = 94;

        assertEquals(2, card(SpecialGoalsCalculator.of(List.of(m)), Kind.LATE).total());
    }

    @Test
    void minuteFiveIsEarlySixIsNot() {
        Match m = played(1, bremen, 1, koeln, 1);
        goal(m, "A", 1, 0).minute = 5;
        goal(m, "B", 1, 1).minute = 6;

        Card early = card(SpecialGoalsCalculator.of(List.of(m)), Kind.EARLY);
        assertEquals(1, early.total());
        assertEquals(bremen, early.clubs().getFirst().team());
    }

    @Test
    void anOwnGoalCountsForTheTeamThatBenefits() {
        Match m = played(1, bremen, 0, koeln, 1);
        goal(m, "Friedl", 0, 1).ownGoal = true; // a Bremen player, the goal goes to Köln

        Card own = card(SpecialGoalsCalculator.of(List.of(m)), Kind.OWN_GOAL);
        assertEquals(koeln, own.clubs().getFirst().team());
        assertEquals("Friedl", own.latest().goal().scorerName);
    }

    @Test
    void everyClubTiedOnThirdPlaceIsListed() {
        Match a = played(1, bremen, 2, koeln, 1);
        goal(a, "A", 1, 0).penalty = true;
        goal(a, "B", 2, 0).penalty = true;
        goal(a, "C", 2, 1).penalty = true;
        Match b = played(2, mainz, 1, kiel, 1);
        goal(b, "D", 1, 0).penalty = true;
        goal(b, "E", 1, 1).penalty = true;

        Card pens = card(SpecialGoalsCalculator.of(List.of(a, b)), Kind.PENALTY);
        assertEquals(5, pens.total());
        // Bremen 2, then Kiel, Köln and Mainz with 1 each, all on place 2
        assertEquals(4, pens.top().size());
        assertEquals("E", pens.latest().goal().scorerName);
    }

    @Test
    void provisionalMatchesAreLeftOut() {
        Match m = provisional(1, bremen, 1, koeln, 0);
        goal(m, "A", 1, 0).penalty = true;

        Card pens = card(SpecialGoalsCalculator.of(List.of(m)), Kind.PENALTY);
        assertEquals(0, pens.total());
        assertNull(pens.latest());
    }
}
