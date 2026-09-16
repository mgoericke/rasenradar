package de.javamark.matchoracle.matchday.entity;

import java.util.List;

/** Previous meetings of two teams, newest first, with the balance from {@code team}'s point of view. */
public record HeadToHead(Team team, Team opponent, List<TeamMatchView> matches, Balance balance, boolean includesProvisional) {
}
