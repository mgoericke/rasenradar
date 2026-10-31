package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.control.ScoreFrequencyCalculator.ScoreFrequency;
import de.javamark.matchoracle.matchday.entity.Team;
import org.junit.jupiter.api.Test;

import java.util.List;

import static de.javamark.matchoracle.matchday.control.MatchFixtures.played;
import static de.javamark.matchoracle.matchday.control.MatchFixtures.provisional;
import static de.javamark.matchoracle.matchday.control.MatchFixtures.team;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Story r076 "Häufigste Ergebnisse": the 0–4+ grid, ties in the lead, final results only. */
class ScoreFrequencyCalculatorTest {

    private final Team bremen = team("Bremen");
    private final Team koeln = team("Köln");

    @Test
    void bigScoresLandInTheFourPlusRowAndColumn() {
        ScoreFrequency f = ScoreFrequencyCalculator.of(List.of(
                played(1, bremen, 4, koeln, 0), played(2, koeln, 5, bremen, 1), played(3, bremen, 4, koeln, 4),
                played(4, koeln, 6, bremen, 5)));

        assertEquals(1, f.count(4, 0));
        assertEquals(1, f.count(4, 1));
        assertEquals(2, f.count(4, 4));
        assertEquals(4, f.matchesWithFourGoals());
        // the list behind a cell keeps the real score
        assertEquals(6, f.cells().get(4).get(4).get(1).fullTimeScore.home);
    }

    @Test
    void tiedMostFrequentScoresAreAllNamed() {
        ScoreFrequency f = ScoreFrequencyCalculator.of(List.of(
                played(1, bremen, 2, koeln, 1), played(2, koeln, 0, bremen, 0), played(3, bremen, 2, koeln, 1),
                played(4, koeln, 0, bremen, 0), played(5, bremen, 1, koeln, 3)));

        assertEquals(List.of("0:0", "2:1"), f.mostFrequent());
        assertEquals(2, f.mostFrequentCount());
    }

    @Test
    void exactScoresDecideTheLeadNotTheCappedCell() {
        ScoreFrequency f = ScoreFrequencyCalculator.of(List.of(
                played(1, bremen, 4, koeln, 0), played(2, koeln, 5, bremen, 0), played(3, bremen, 1, koeln, 0)));

        // 4:0 and 5:0 share the cell 4+:0 but are two results, each once
        assertEquals(List.of("1:0", "4:0", "5:0"), f.mostFrequent());
    }

    @Test
    void provisionalResultsAreNotCounted() {
        ScoreFrequency f = ScoreFrequencyCalculator.of(List.of(provisional(1, bremen, 1, koeln, 1)));

        assertEquals(0, f.matches());
        assertTrue(f.mostFrequent().isEmpty());
    }
}
