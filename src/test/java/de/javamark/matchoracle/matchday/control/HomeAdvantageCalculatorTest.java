package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.control.HomeAdvantageCalculator.HomeAdvantage;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.Team;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static de.javamark.matchoracle.matchday.control.MatchFixtures.played;
import static de.javamark.matchoracle.matchday.control.MatchFixtures.provisional;
import static de.javamark.matchoracle.matchday.control.MatchFixtures.team;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Story r098 "Heimvorteil": shares of the league, ratio, the two club lists — final results only. */
class HomeAdvantageCalculatorTest {

    private final Team bremen = team("Bremen");
    private final Team koeln = team("Köln");
    private final Team mainz = team("Mainz");

    @Test
    void sharesAddUpToHundredEvenWhenEachThirdRoundsDown() {
        HomeAdvantage h = HomeAdvantageCalculator.of(List.of(
                played(1, bremen, 1, koeln, 0), played(2, koeln, 1, bremen, 1), played(3, mainz, 0, bremen, 2)));

        assertEquals(3, h.matches());
        assertEquals(100, h.homeWinPercent() + h.drawPercent() + h.awayWinPercent());
        assertEquals(1.0, h.homeToAwayRatio());
    }

    @Test
    void largestRemaindersGetTheLeftoverPoints() {
        assertArrayEquals(new int[]{43, 29, 28}, HomeAdvantageCalculator.percentages(6, 4, 4));
        assertArrayEquals(new int[]{0, 0, 0}, HomeAdvantageCalculator.percentages(0, 0, 0));
    }

    @Test
    void noRatioWithoutAnAwayWin() {
        HomeAdvantage h = HomeAdvantageCalculator.of(List.of(played(1, bremen, 2, koeln, 0)));

        assertNull(h.homeToAwayRatio());
    }

    @Test
    void provisionalResultsDoNotCount() {
        HomeAdvantage h = HomeAdvantageCalculator.of(List.of(
                played(1, bremen, 2, koeln, 0), provisional(2, koeln, 0, bremen, 3)));

        assertEquals(1, h.matches());
        assertEquals(100, h.homeWinPercent());
        // Köln has no away game and Bremen no counted away game — both are left out, not shown with zero
        assertTrue(h.strongestAtHome().isEmpty());
        assertTrue(h.preferAway().isEmpty());
    }

    @Test
    void clubsAreSortedByTheGapBetweenHomeAndAwayPointsPerGame() {
        HomeAdvantage h = HomeAdvantageCalculator.of(List.of(
                played(1, bremen, 2, koeln, 0),   // Bremen 3 at home, Köln 0 away
                played(2, koeln, 0, bremen, 0),   // Köln 1 at home, Bremen 1 away
                played(3, mainz, 0, koeln, 1),    // Mainz 0 at home, Köln 3 away
                played(4, bremen, 1, mainz, 2))); // Bremen 0 at home, Mainz 3 away

        // Bremen home 1.5 / away 1.0; Köln home 1.0 / away 1.5; Mainz home 0 / away 3
        assertEquals(List.of(bremen), h.strongestAtHome().stream().map(HomeAdvantageCalculator.ClubSplit::team).toList());
        assertEquals(List.of(mainz, koeln), h.preferAway().stream().map(HomeAdvantageCalculator.ClubSplit::team).toList());
    }

    @Test
    void eachListStopsAfterFiveClubs() {
        List<Match> matches = new ArrayList<>();
        Team weak = team("Gast");
        for (int i = 0; i < 7; i++) {
            Team club = team("Verein " + i);
            matches.add(played(1, club, 1, weak, 0));
            matches.add(played(2, weak, 1, club, 0));
        }

        assertEquals(HomeAdvantageCalculator.LIST_LENGTH, HomeAdvantageCalculator.of(matches).strongestAtHome().size());
    }
}
