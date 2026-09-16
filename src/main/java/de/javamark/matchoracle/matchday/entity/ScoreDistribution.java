package de.javamark.matchoracle.matchday.entity;

/**
 * A Poisson estimate: the most likely scoreline and its exact probability, plus the
 * tendency probabilities summed from the same grid — always mutually consistent
 * because they come from one distribution, never estimated independently.
 */
public record ScoreDistribution(int homeGoals, int awayGoals, double scoreProbability,
                                double homeWin, double draw, double awayWin) {
}
