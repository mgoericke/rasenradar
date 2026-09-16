package de.javamark.matchoracle.matchday.boundary;

import java.time.Instant;

/**
 * CDI event fired when a result becomes FINAL (spec 01, rule 4) — the trigger for
 * the review (spec 03, step 1). Plain values only; observers must not need entities.
 */
public record ResultFinalized(long matchId, int homeGoals, int awayGoals, Instant kickoff) {
}
