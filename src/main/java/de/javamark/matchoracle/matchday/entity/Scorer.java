package de.javamark.matchoracle.matchday.entity;

/** One line of a club's scorer list: goals for the club, of which penalties. */
public record Scorer(String name, int goals, int penalties) {
}
