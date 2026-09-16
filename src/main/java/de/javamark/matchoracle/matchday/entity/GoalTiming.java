package de.javamark.matchoracle.matchday.entity;

/** Goals scored and conceded by a club in one 15-minute window of a match (Nachspielzeit counts to the last window). */
public record GoalTiming(String label, int scored, int conceded) {
}
