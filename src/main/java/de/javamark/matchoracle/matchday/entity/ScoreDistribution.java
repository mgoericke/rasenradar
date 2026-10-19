package de.javamark.matchoracle.matchday.entity;

/**
 * A Poisson estimate: the raw expected goals it was computed from, the most likely
 * scoreline within the most likely tendency and its exact probability, plus the tendency
 * probabilities summed from the same grid — always mutually consistent because they come
 * from one distribution, never estimated independently (spec 02, "Richtwert").
 */
public record ScoreDistribution(double expectedHomeGoals, double expectedAwayGoals,
                                int homeGoals, int awayGoals, double scoreProbability,
                                double homeWin, double draw, double awayWin) {
}
