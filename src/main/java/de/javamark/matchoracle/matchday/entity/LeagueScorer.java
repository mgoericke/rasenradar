package de.javamark.matchoracle.matchday.entity;

/** One line of the league-wide scorer list: which club, goals, of which penalties. */
public record LeagueScorer(String name, String team, int goals, int penalties) {
}
