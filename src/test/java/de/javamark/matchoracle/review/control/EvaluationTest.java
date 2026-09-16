package de.javamark.matchoracle.review.control;

import de.javamark.matchoracle.review.entity.ConfidenceVerdict;
import de.javamark.matchoracle.review.entity.Outcome;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Spec 03, step 2: how well the probabilities fit the outcome, and whether the confidence was appropriate. */
class EvaluationTest {

    @Test
    void brierIsZeroForACertainCorrectForecastAndTwoForACertainWrongOne() {
        assertEquals(0.0, Evaluation.brier(1, 0, 0, Outcome.HOME_WIN), 1e-9);
        assertEquals(2.0, Evaluation.brier(1, 0, 0, Outcome.AWAY_WIN), 1e-9);
    }

    @Test
    void brierRewardsProbabilityOnTheActualOutcome() {
        double cautious = Evaluation.brier(0.5, 0.3, 0.2, Outcome.HOME_WIN);
        double bold = Evaluation.brier(0.8, 0.1, 0.1, Outcome.HOME_WIN);

        assertEquals(0.38, cautious, 1e-9);
        assertEquals(0.06, bold, 1e-9);
    }

    @Test
    void aConfidentMissIsOverconfidentAndATimidHitIsUnderconfident() {
        assertEquals(ConfidenceVerdict.OVERCONFIDENT, Evaluation.confidenceVerdict(false, 0.7));
        assertEquals(ConfidenceVerdict.UNDERCONFIDENT, Evaluation.confidenceVerdict(true, 0.3));
        assertEquals(ConfidenceVerdict.APPROPRIATE, Evaluation.confidenceVerdict(true, 0.7));
        assertEquals(ConfidenceVerdict.APPROPRIATE, Evaluation.confidenceVerdict(false, 0.4));
    }
}
