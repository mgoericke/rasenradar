package de.javamark.matchoracle.review.entity;

/**
 * Tendency of a match. Deliberately the review feature's own enum: features only
 * share boundary types, and the review must not depend on the forecast feature.
 */
public enum Outcome {
    HOME_WIN, DRAW, AWAY_WIN;

    public static Outcome of(int homeGoals, int awayGoals) {
        if (homeGoals > awayGoals) return HOME_WIN;
        if (homeGoals < awayGoals) return AWAY_WIN;
        return DRAW;
    }
}
