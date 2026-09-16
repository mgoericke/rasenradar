package de.javamark.matchoracle.review.control;

import de.javamark.matchoracle.review.entity.Situation;

/**
 * Spec 03, interview: two situations are similar when table gap, form of both
 * teams, home/away strength and promoted status are close. The distance is a
 * weighted Euclidean distance over normalized features, 0 = identical.
 */
final class Similarity {

    /** Below this distance two situations count as similar. */
    static final double SIMILAR_BELOW = 0.22;
    /** A different league is comparable but a little further away (spec 03, rules). */
    static final double OTHER_LEAGUE_PENALTY = 0.05;

    private Similarity() {
    }

    static double distance(Situation a, Situation b) {
        double gap = diff(gap(a), gap(b)) / 17.0;                              // table positions 1..18
        double homeForm = diff(formShare(a.homeFormPoints, a.formMatches), formShare(b.homeFormPoints, b.formMatches));
        double awayForm = diff(formShare(a.awayFormPoints, a.formMatches), formShare(b.awayFormPoints, b.formMatches));
        double homeStrength = diff(a.homeHomePpg, b.homeHomePpg) / 3.0;
        double awayStrength = diff(a.awayAwayPpg, b.awayAwayPpg) / 3.0;
        double promoted = (a.homePromoted != b.homePromoted ? 0.5 : 0) + (a.awayPromoted != b.awayPromoted ? 0.5 : 0);
        double d = Math.sqrt(2.0 * gap * gap + homeForm * homeForm + awayForm * awayForm
                + homeStrength * homeStrength + awayStrength * awayStrength + promoted * promoted) / Math.sqrt(7.0);
        return a.league.equals(b.league) ? d : d + OTHER_LEAGUE_PENALTY;
    }

    static boolean similar(double distance) {
        return distance < SIMILAR_BELOW;
    }

    private static double gap(Situation s) {
        return s.positionGap == null ? 0 : s.positionGap;
    }

    /** Share of the points that were available in the form window, 0..1. */
    private static double formShare(int points, int matches) {
        return matches == 0 ? 0 : points / (3.0 * matches);
    }

    private static double diff(double a, double b) {
        return Math.abs(a - b);
    }
}
