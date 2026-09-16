package de.javamark.matchoracle.review.control;

import de.javamark.matchoracle.matchday.boundary.MatchSituation;
import de.javamark.matchoracle.matchday.boundary.MatchSituation.Balance;
import de.javamark.matchoracle.review.entity.Baseline;

/**
 * A Poisson estimate from the goal averages before kickoff: the home side's home
 * record against the away side's away record. With few matches played the averages
 * are pulled towards the league's typical values, so the first matchdays give
 * roughly the league base rates (≈ 45/26/29) instead of wild swings.
 */
final class BaselineForecast {

    /** Typical Bundesliga goals per match: home side 1.6, away side 1.3. */
    static final double LEAGUE_HOME_GOALS = 1.6;
    static final double LEAGUE_AWAY_GOALS = 1.3;
    /** How many matches of evidence weigh as much as the league prior. */
    static final double PRIOR_WEIGHT = 5.0;
    private static final int MAX_GOALS = 10;

    private BaselineForecast() {
    }

    static Baseline from(MatchSituation s) {
        return from(s.homeTeam().homeBalance(), s.awayTeam().awayBalance());
    }

    /** {@code home}: the home side's balance at home; {@code away}: the away side's balance away. */
    static Baseline from(Balance home, Balance away) {
        double homeAttack = shrunk(home.goalsFor(), home.played(), LEAGUE_HOME_GOALS);
        double awayDefence = shrunk(away.goalsAgainst(), away.played(), LEAGUE_HOME_GOALS);
        double awayAttack = shrunk(away.goalsFor(), away.played(), LEAGUE_AWAY_GOALS);
        double homeDefence = shrunk(home.goalsAgainst(), home.played(), LEAGUE_AWAY_GOALS);
        double expectedHome = (homeAttack + awayDefence) / 2;
        double expectedAway = (awayAttack + homeDefence) / 2;

        double homeWin = 0, draw = 0, awayWin = 0;
        for (int h = 0; h <= MAX_GOALS; h++) {
            for (int a = 0; a <= MAX_GOALS; a++) {
                double p = poisson(expectedHome, h) * poisson(expectedAway, a);
                if (h > a) homeWin += p;
                else if (h == a) draw += p;
                else awayWin += p;
            }
        }
        double total = homeWin + draw + awayWin;
        return new Baseline(homeWin / total, draw / total, awayWin / total);
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
