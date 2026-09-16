package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.entity.Balance;
import de.javamark.matchoracle.matchday.entity.ScoreDistribution;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * A Poisson estimate from the goal averages before kickoff: the home side's home
 * record against the away side's away record. With few matches played the averages
 * are pulled towards the league's typical values, so the first matchdays give
 * roughly the league base rates (≈ 45/24/31) instead of wild swings.
 */
@ApplicationScoped
public class PoissonScoreModel {

    /** Typical Bundesliga goals per match: home side 1.6, away side 1.3. */
    static final double LEAGUE_HOME_GOALS = 1.6;
    static final double LEAGUE_AWAY_GOALS = 1.3;
    /** How many matches of evidence weigh as much as the league prior. */
    static final double PRIOR_WEIGHT = 5.0;
    private static final int MAX_GOALS = 10;

    /** {@code home}: the home side's balance at home; {@code away}: the away side's balance away. */
    public ScoreDistribution forecast(Balance home, Balance away) {
        double homeAttack = shrunk(home.goalsFor(), home.played(), LEAGUE_HOME_GOALS);
        double awayDefence = shrunk(away.goalsAgainst(), away.played(), LEAGUE_HOME_GOALS);
        double awayAttack = shrunk(away.goalsFor(), away.played(), LEAGUE_AWAY_GOALS);
        double homeDefence = shrunk(home.goalsAgainst(), home.played(), LEAGUE_AWAY_GOALS);
        double expectedHome = (homeAttack + awayDefence) / 2;
        double expectedAway = (awayAttack + homeDefence) / 2;
        return fromExpectedGoals(expectedHome, expectedAway);
    }

    /** Same grid, from expected goals already adjusted by the caller (e.g. the forecast agent's read). */
    public ScoreDistribution fromExpectedGoals(double expectedHome, double expectedAway) {
        double homeWin = 0, draw = 0, awayWin = 0;
        int bestHome = 0, bestAway = 0;
        double bestP = -1;
        for (int h = 0; h <= MAX_GOALS; h++) {
            for (int a = 0; a <= MAX_GOALS; a++) {
                double p = poisson(expectedHome, h) * poisson(expectedAway, a);
                if (h > a) homeWin += p;
                else if (h == a) draw += p;
                else awayWin += p;
                if (p > bestP) {
                    bestP = p;
                    bestHome = h;
                    bestAway = a;
                }
            }
        }
        double total = homeWin + draw + awayWin;
        return new ScoreDistribution(bestHome, bestAway, bestP / total, homeWin / total, draw / total, awayWin / total);
    }

    /** Goals per match, blended with the league value; one match counts, but it does not decide alone. */
    private static double shrunk(int goals, int played, double prior) {
        return (goals + PRIOR_WEIGHT * prior) / (played + PRIOR_WEIGHT);
    }

    private static double poisson(double lambda, int k) {
        double p = Math.exp(-lambda);
        for (int i = 1; i <= k; i++) {
            p *= lambda / i;
        }
        return p;
    }
}
