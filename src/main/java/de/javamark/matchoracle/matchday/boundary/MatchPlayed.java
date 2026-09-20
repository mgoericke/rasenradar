package de.javamark.matchoracle.matchday.boundary;

/**
 * CDI event fired when a match gets a score for the first time, FINAL or not (spec 01, step 2/3)
 * — the season outlook's trigger (spec 04), which does not wait for {@link ResultFinalized}.
 */
public record MatchPlayed(long matchId) {
}
