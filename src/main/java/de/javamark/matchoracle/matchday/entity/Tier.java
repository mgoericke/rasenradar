package de.javamark.matchoracle.matchday.entity;

/**
 * Spec 06: the division a club played in during a season. LOWER covers everything below
 * the third division — in the cup those are the amateur sides, and they are the reason
 * the competition is worth watching.
 */
public enum Tier {

    FIRST(1, "1. Liga"),
    SECOND(2, "2. Liga"),
    THIRD(3, "3. Liga"),
    LOWER(4, "Amateur");

    private final int level;
    private final String label;

    Tier(int level, String label) {
        this.level = level;
        this.label = label;
    }

    /** 1 is the top division; a higher number means further down. */
    public int level() {
        return level;
    }

    public String label() {
        return label;
    }
}
