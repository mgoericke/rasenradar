package de.javamark.matchoracle.matchday.entity;

import java.util.List;

/**
 * The last matches of a team in the current season, newest first. May contain
 * fewer entries than requested (season start) — this is reported, not padded.
 */
public record Form(Team team, List<TeamMatchView> lastMatches, Balance home, Balance away, boolean includesProvisional) {

    public List<TeamResult> results() {
        return lastMatches.stream().map(TeamMatchView::result).toList();
    }
}
