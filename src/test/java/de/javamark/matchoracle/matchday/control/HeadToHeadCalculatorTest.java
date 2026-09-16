package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.entity.Balance;
import de.javamark.matchoracle.matchday.entity.HeadToHead;
import de.javamark.matchoracle.matchday.entity.Team;
import org.junit.jupiter.api.Test;

import java.util.List;

import static de.javamark.matchoracle.matchday.control.MatchFixtures.played;
import static de.javamark.matchoracle.matchday.control.MatchFixtures.team;
import static de.javamark.matchoracle.matchday.entity.TeamResult.LOSS;
import static de.javamark.matchoracle.matchday.entity.TeamResult.WIN;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HeadToHeadCalculatorTest {

    private final Team mainz = team("Mainz");
    private final Team leipzig = team("Leipzig");

    private final HeadToHeadCalculator calculator = new HeadToHeadCalculator();

    @Test
    void balanceAndResultsAreFromTheGivenTeamsPointOfView() {
        HeadToHead h2h = calculator.headToHead(mainz, leipzig, List.of(
                played(20, leipzig, 2, mainz, 1),   // mainz lost away
                played(3, mainz, 3, leipzig, 0)));  // mainz won at home

        assertEquals(new Balance(1, 0, 1, 4, 2), h2h.balance());
        assertEquals(List.of(LOSS, WIN), h2h.matches().stream().map(v -> v.result()).toList());
        assertEquals(leipzig, h2h.matches().get(0).opponent());
    }

    @Test
    void noMeetingsYieldsEmptyBalanceNotAnError() {
        HeadToHead h2h = calculator.headToHead(mainz, leipzig, List.of());

        assertEquals(Balance.EMPTY, h2h.balance());
        assertEquals(0, h2h.matches().size());
    }
}
