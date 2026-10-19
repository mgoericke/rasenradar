package de.javamark.matchoracle.forecast.entity;

/** The tendency of a match: home win, draw or away win. */
public enum Outcome {
    HOME_WIN, DRAW, AWAY_WIN;

    /** The tendency a final (or provisional) score actually had. */
    public static Outcome of(int homeGoals, int awayGoals) {
        if (homeGoals > awayGoals) return HOME_WIN;
        if (homeGoals < awayGoals) return AWAY_WIN;
        return DRAW;
    }
}
