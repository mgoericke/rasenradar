package de.javamark.matchoracle.matchday.entity;

/**
 * Spec 06: how a competition is played. The format decides whether a table is kept
 * and how a section of the competition is named — instead of a special case per league.
 */
public enum CompetitionFormat {

    /** One table over the whole competition: the two Bundesligas, the Champions League phase. */
    TABLE,
    /** Several groups side by side, each with its own table and its own matchday counter. */
    GROUPS,
    /** Rounds until the final, no table at all. */
    KNOCKOUT;

    public boolean hasTable() {
        return this != KNOCKOUT;
    }
}
