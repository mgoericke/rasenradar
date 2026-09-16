package de.javamark.matchoracle.forecast.control;

/** Spec 02, rules: the three probabilities add up to exactly 1. */
public record Probabilities(double homeWin, double draw, double awayWin) {

    /** Clamps negatives to 0 and scales so the sum is 1; an all-zero input becomes an even split. */
    public static Probabilities normalized(double homeWin, double draw, double awayWin) {
        double h = Math.max(0, homeWin), d = Math.max(0, draw), a = Math.max(0, awayWin);
        double sum = h + d + a;
        if (sum == 0) {
            return new Probabilities(1.0 / 3, 1.0 / 3, 1.0 / 3);
        }
        return new Probabilities(h / sum, d / sum, a / sum);
    }

    public double sum() {
        return homeWin + draw + awayWin;
    }
}
