package de.javamark.matchoracle.review.control;

import de.javamark.matchoracle.review.entity.ConfidenceVerdict;
import de.javamark.matchoracle.review.entity.Outcome;

/** Spec 03, step 2: the pure rules of comparing a forecast with the actual outcome. */
final class Evaluation {

    /** A miss with at least this confidence counts as overconfident. */
    static final double OVERCONFIDENT_FROM = 0.6;
    /** A hit with less than this confidence counts as underconfident. */
    static final double UNDERCONFIDENT_BELOW = 0.4;

    private Evaluation() {
    }

    /** Brier score over the three outcomes: sum of squared differences between forecast and reality, 0 (perfect) to 2. */
    static double brier(double homeWin, double draw, double awayWin, Outcome actual) {
        return sq(homeWin - (actual == Outcome.HOME_WIN ? 1 : 0))
                + sq(draw - (actual == Outcome.DRAW ? 1 : 0))
                + sq(awayWin - (actual == Outcome.AWAY_WIN ? 1 : 0));
    }

    static ConfidenceVerdict confidenceVerdict(boolean hit, double confidence) {
        if (!hit && confidence >= OVERCONFIDENT_FROM) return ConfidenceVerdict.OVERCONFIDENT;
        if (hit && confidence < UNDERCONFIDENT_BELOW) return ConfidenceVerdict.UNDERCONFIDENT;
        return ConfidenceVerdict.APPROPRIATE;
    }

    private static double sq(double v) {
        return v * v;
    }
}
