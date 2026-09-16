package de.javamark.matchoracle.review.control;

import de.javamark.matchoracle.matchday.boundary.MatchSituation.Balance;
import de.javamark.matchoracle.review.entity.Baseline;
import de.javamark.matchoracle.review.entity.Outcome;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Spec 03: the baseline forecast is the yardstick — a plain statistical estimate from the goal averages before kickoff. */
class BaselineForecastTest {

    private static final Balance NONE = new Balance(0, 0, 0, 0, 0, 0, 0);

    @Test
    void withoutAnyMatchPlayedItFallsBackToTheLeagueBaseRates() {
        Baseline b = BaselineForecast.from(NONE, NONE);

        assertEquals(1.0, b.homeWin() + b.draw() + b.awayWin(), 1e-9);
        assertTrue(b.homeWin() > 0.40 && b.homeWin() < 0.50, "home " + b.homeWin());
        assertTrue(b.draw() > 0.22 && b.draw() < 0.30, "draw " + b.draw());
        assertEquals(Outcome.HOME_WIN, b.predictedOutcome());
    }

    @Test
    void aProlificHomeSideAgainstALeakyAwaySideRaisesTheHomeWinProbability() {
        Balance strongAtHome = new Balance(8, 7, 1, 0, 24, 4, 22);   // 3.0 scored, 0.5 conceded per home game
        Balance weakAway = new Balance(8, 0, 1, 7, 4, 20, 1);        // 0.5 scored, 2.5 conceded per away game

        Baseline b = BaselineForecast.from(strongAtHome, weakAway);

        assertTrue(b.homeWin() > 0.65, "home " + b.homeWin());
        assertTrue(b.awayWin() < 0.12, "away " + b.awayWin());
    }

    @Test
    void aStrongAwaySideCanBeTheFavourite() {
        Balance weakAtHome = new Balance(8, 0, 1, 7, 4, 20, 1);
        Balance strongAway = new Balance(8, 7, 1, 0, 24, 4, 22);

        assertEquals(Outcome.AWAY_WIN, BaselineForecast.from(weakAtHome, strongAway).predictedOutcome());
    }

    @Test
    void fewMatchesMoveTheEstimateOnlyPartOfTheWay() {
        Balance oneBigWin = new Balance(1, 1, 0, 0, 5, 0, 3);
        Balance oneBigLoss = new Balance(1, 0, 0, 1, 0, 5, 0);

        Baseline afterOne = BaselineForecast.from(oneBigWin, oneBigLoss);
        Baseline afterEight = BaselineForecast.from(new Balance(8, 8, 0, 0, 40, 0, 24), new Balance(8, 0, 0, 8, 0, 40, 0));

        assertTrue(afterOne.homeWin() < afterEight.homeWin(), afterOne.homeWin() + " vs " + afterEight.homeWin());
        assertTrue(afterOne.homeWin() > 0.50, "one match should still count: " + afterOne.homeWin());
    }
}
