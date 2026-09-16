package de.javamark.matchoracle.review.entity;

/** The baseline forecast: three probabilities from plain statistics, the yardstick every forecast is measured against. */
public record Baseline(double homeWin, double draw, double awayWin) {

    public Outcome predictedOutcome() {
        if (homeWin >= draw && homeWin >= awayWin) return Outcome.HOME_WIN;
        if (awayWin >= draw) return Outcome.AWAY_WIN;
        return Outcome.DRAW;
    }

    public double probabilityOf(Outcome outcome) {
        return switch (outcome) {
            case HOME_WIN -> homeWin;
            case DRAW -> draw;
            case AWAY_WIN -> awayWin;
        };
    }
}
