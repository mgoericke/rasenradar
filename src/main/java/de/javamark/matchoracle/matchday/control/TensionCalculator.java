package de.javamark.matchoracle.matchday.control;

/**
 * Story r073 "Spannendste zuerst": how much an open match promises, in five steps, from the table
 * before the matchday and the form alone — never from a forecast. One step for every match, two more
 * when the sides are at most three points apart (one at most six), one when both are in the top six
 * or both in the bottom four, one when both took at least nine points from their last five matches.
 */
public final class TensionCalculator {

    public static final int STEPS = 5;
    static final int CLOSE = 3;
    static final int NEAR = 6;
    static final int TOP = 6;
    static final int BOTTOM = 4;
    static final int ON_FORM = 9;

    private TensionCalculator() {
    }

    /** One side of an open match: place and points before the matchday, points from the last five. */
    public record Side(int position, int points, int formPoints) {
    }

    /** @param teams how many teams the table holds — the bottom four are counted from its end */
    public static int of(Side home, Side away, int teams) {
        int gap = Math.abs(home.points() - away.points());
        int steps = 1 + (gap <= CLOSE ? 2 : gap <= NEAR ? 1 : 0);
        boolean top = home.position() <= TOP && away.position() <= TOP;
        boolean bottom = home.position() > teams - BOTTOM && away.position() > teams - BOTTOM;
        if (top || bottom) steps++;
        if (home.formPoints() >= ON_FORM && away.formPoints() >= ON_FORM) steps++;
        return Math.min(STEPS, steps);
    }
}
