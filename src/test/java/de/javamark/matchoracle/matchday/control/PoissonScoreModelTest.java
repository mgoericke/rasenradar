package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.entity.Balance;
import de.javamark.matchoracle.matchday.entity.ScoreDistribution;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spec 01/03 shared statistics: a plain Poisson estimate from the goal averages before
 * kickoff, used both as the Rückschau yardstick and as the KI-Vorschau's starting point.
 */
class PoissonScoreModelTest {

    private static final Balance NONE = Balance.EMPTY;

    private final PoissonScoreModel model = new PoissonScoreModel();

    @Test
    void withoutAnyMatchPlayedItFallsBackToTheLeagueBaseRatesAndAOneOneScoreline() {
        ScoreDistribution f = model.forecast(NONE, NONE);

        assertEquals(1.0, f.homeWin() + f.draw() + f.awayWin(), 1e-9);
        assertTrue(f.homeWin() > 0.40 && f.homeWin() < 0.50, "home " + f.homeWin());
        assertTrue(f.draw() > 0.22 && f.draw() < 0.30, "draw " + f.draw());
        assertEquals(1, f.homeGoals());
        assertEquals(1, f.awayGoals());
        assertTrue(f.scoreProbability() > 0.10 && f.scoreProbability() < 0.13, "score probability " + f.scoreProbability());
    }

    @Test
    void aProlificHomeSideAgainstALeakyAwaySideRaisesTheHomeWinProbabilityAndTheExpectedScore() {
        Balance strongAtHome = new Balance(7, 1, 0, 24, 4);
        Balance weakAway = new Balance(0, 1, 7, 4, 20);

        ScoreDistribution f = model.forecast(strongAtHome, weakAway);

        assertTrue(f.homeWin() > 0.65, "home " + f.homeWin());
        assertTrue(f.awayWin() < 0.12, "away " + f.awayWin());
        assertEquals(2, f.homeGoals());
        assertEquals(0, f.awayGoals());
    }

    @Test
    void aStrongAwaySideCanBeTheFavourite() {
        Balance weakAtHome = new Balance(0, 1, 7, 4, 20);
        Balance strongAway = new Balance(7, 1, 0, 24, 4);

        ScoreDistribution f = model.forecast(weakAtHome, strongAway);

        assertTrue(f.awayWin() > f.homeWin(), "away " + f.awayWin() + " vs home " + f.homeWin());
    }

    @Test
    void fewMatchesMoveTheEstimateOnlyPartOfTheWay() {
        Balance oneBigWin = new Balance(1, 0, 0, 5, 0);
        Balance oneBigLoss = new Balance(0, 0, 1, 0, 5);

        ScoreDistribution afterOne = model.forecast(oneBigWin, oneBigLoss);
        ScoreDistribution afterEight = model.forecast(new Balance(8, 0, 0, 40, 0), new Balance(0, 0, 8, 0, 40));

        assertTrue(afterOne.homeWin() < afterEight.homeWin(), afterOne.homeWin() + " vs " + afterEight.homeWin());
        assertTrue(afterOne.homeWin() > 0.50, "one match should still count: " + afterOne.homeWin());
    }

    @Test
    void adjustedExpectedGoalsProduceAConsistentGridWithoutTeamData() {
        // the forecast agent's adjusted read, fed straight in — same math, no Balance needed
        ScoreDistribution f = model.fromExpectedGoals(2.3, 0.8);

        assertEquals(2, f.homeGoals());
        assertEquals(0, f.awayGoals());
        assertEquals(1.0, f.homeWin() + f.draw() + f.awayWin(), 1e-9);
    }
}
