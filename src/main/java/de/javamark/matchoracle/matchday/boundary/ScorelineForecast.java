package de.javamark.matchoracle.matchday.boundary;

/**
 * A Poisson estimate: the most likely scoreline and its exact probability, plus the
 * tendency probabilities summed from the same distribution — always mutually
 * consistent because they come from one grid, never estimated independently.
 */
public record ScorelineForecast(int homeGoals, int awayGoals, double scoreProbability,
                                double homeWin, double draw, double awayWin) {
}
