package de.javamark.matchoracle.matchday.entity;

import java.time.Instant;
import java.util.List;

/** Spec 01, "Anstoß-Block": every match of both Bundesligas that kicks off at the same time. */
public record KickoffSlot(Instant kickoff, List<Match> matches) {
}
