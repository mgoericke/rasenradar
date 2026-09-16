package de.javamark.matchoracle.matchday.entity;

/** Outcome of a match from one team's point of view. */
public enum TeamResult {
    WIN, DRAW, LOSS;

    public static TeamResult of(int goalsFor, int goalsAgainst) {
        if (goalsFor > goalsAgainst) return WIN;
        if (goalsFor < goalsAgainst) return LOSS;
        return DRAW;
    }
}
