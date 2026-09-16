package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.entity.Balance;
import de.javamark.matchoracle.matchday.entity.Form;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

import static de.javamark.matchoracle.matchday.control.MatchFixtures.played;
import static de.javamark.matchoracle.matchday.control.MatchFixtures.provisional;
import static de.javamark.matchoracle.matchday.control.MatchFixtures.team;
import static de.javamark.matchoracle.matchday.entity.TeamResult.DRAW;
import static de.javamark.matchoracle.matchday.entity.TeamResult.LOSS;
import static de.javamark.matchoracle.matchday.entity.TeamResult.WIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormCalculatorTest {

    private final Team mainz = team("Mainz");
    private final Team leipzig = team("Leipzig");
    private final Team koeln = team("Köln");

    private FormCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new FormCalculator();
        calculator.formMatches = 5;
    }

    @Test
    void lastFiveMatchesNewestFirstFromTheTeamsPointOfView() {
        List<Match> season = newestFirst(
                played(1, mainz, 2, leipzig, 0),   // win (home)
                played(2, koeln, 1, mainz, 1),     // draw (away)
                played(3, mainz, 0, leipzig, 3),   // loss
                played(4, koeln, 0, mainz, 2),     // win (away)
                played(5, mainz, 1, koeln, 1),     // draw
                played(6, leipzig, 2, mainz, 1));  // loss

        Form form = calculator.form(mainz, season);

        assertEquals(List.of(LOSS, DRAW, WIN, LOSS, DRAW), form.results());
        assertFalse(form.includesProvisional());
    }

    @Test
    void seasonStartYieldsShorterFormInsteadOfPadding() {
        List<Match> season = newestFirst(played(1, mainz, 2, leipzig, 0), played(2, koeln, 1, mainz, 1));

        Form form = calculator.form(mainz, season);

        assertEquals(2, form.lastMatches().size());
        assertEquals(List.of(DRAW, WIN), form.results());
    }

    @Test
    void homeAndAwayBalanceCoverTheWholeSeasonNotJustTheLastFive() {
        List<Match> season = newestFirst(
                played(1, mainz, 2, leipzig, 0),
                played(2, koeln, 1, mainz, 1),
                played(3, mainz, 0, leipzig, 3),
                played(4, koeln, 0, mainz, 2),
                played(5, mainz, 1, koeln, 1),
                played(6, leipzig, 2, mainz, 1),
                played(7, mainz, 3, koeln, 0));

        Form form = calculator.form(mainz, season);

        assertEquals(new Balance(2, 1, 1, 6, 4), form.home());
        assertEquals(new Balance(1, 1, 1, 4, 3), form.away());
    }

    @Test
    void provisionalResultsAreFlagged() {
        List<Match> season = newestFirst(played(1, mainz, 2, leipzig, 0), provisional(2, koeln, 1, mainz, 1));

        assertTrue(calculator.form(mainz, season).includesProvisional());
    }

    private static List<Match> newestFirst(Match... matches) {
        return List.of(matches).stream().sorted(Comparator.comparing((Match m) -> m.kickoff).reversed()).toList();
    }
}
