package de.javamark.matchoracle.review.entity;

import java.util.List;

/**
 * Spec 03, step 6: the hit rate of a league. {@code hitRate} is null while fewer
 * than the required number of evaluations exist — nothing instead of a thin number.
 */
public record AccuracyReport(String league, boolean backtest, int evaluated, int hits, Double hitRate, Double averageConfidence,
                             Double averageBrier, int required, List<MatchdayPoint> perMatchday,
                             Comparison baseline, Comparison alwaysHome, Double skillScore, List<RecentResult> recent,
                             List<CalibrationGroup> calibration, ContrarianReport contrarian) {

    public record MatchdayPoint(int season, int matchday, int evaluated, int hits, int baselineHits, String recap) {
    }

    /**
     * A yardstick on the same matches: the baseline forecast, or the trivial rule "always
     * a home win". Rates are null below the required number, like the hit rate itself.
     */
    public record Comparison(int evaluated, int hits, Double hitRate, Double averageBrier) {
    }

    /** Spec 03: is a stated confidence band honest? Same reliability rule as the overall hit rate, per band. */
    public record CalibrationGroup(String label, Comparison comparison) {
    }

    /**
     * One entry of the plain hit/miss timeline — a second, simpler view than the hit rate:
     * a strip of the most recent evaluations, oldest to newest, no statistics required to read it.
     */
    public record RecentResult(String label, HitLevel level) {
    }

    /** EXACT: the guide score matched too (a Volltreffer). TENDENCY: right winner/draw, different score. MISS: wrong tendency. */
    public enum HitLevel {
        EXACT, TENDENCY, MISS
    }

    public boolean reliable() {
        return hitRate != null;
    }
}
