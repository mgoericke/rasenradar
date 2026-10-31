package de.javamark.matchoracle.review.entity;

/**
 * Story N01: how the live KI-Vorschau fared in one club's matches of a season. {@code exact} counts
 * the exact results, {@code tendency} every right tendency, exact ones included — so exact <= tendency.
 * Rates are null below the required number of evaluations, like the hit rate of a league (Spec 03).
 */
public record ClubRecord(String club, int evaluated, int exact, int tendency, int required) {

    public boolean reliable() {
        return evaluated >= required && evaluated > 0;
    }

    public Integer exactPercent() {
        return reliable() ? (int) Math.round(100.0 * exact / evaluated) : null;
    }

    public Integer tendencyPercent() {
        return reliable() ? (int) Math.round(100.0 * tendency / evaluated) : null;
    }
}
