package de.javamark.matchoracle.season.control;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.IntFunction;

/**
 * Spec 04: Monte Carlo simulation of the remaining season. Each run draws one scoreline per
 * remaining fixture — independent home/away goal counts, same Poisson assumption the KI-Vorschau
 * uses — applies it to a running table, and records which placement goal each team's final
 * position satisfies. Averaged over many runs, that gives each goal's probability.
 */
@ApplicationScoped
public class SeasonSimulator {

    public static final int RUNS = 10_000;

    public record TeamState(long teamId, int points, int goalDifference, int goalsFor) {
    }

    public record Fixture(long homeTeamId, long awayTeamId, double expectedHomeGoals, double expectedAwayGoals) {
    }

    public record Outcome(long teamId, Map<String, Double> probabilities) {
    }

    /**
     * Story r039: besides each team's probabilities, the median final points total at every table
     * position (key counted from 1) — "Platz 4 hatte am Ende typischerweise 58 Punkte".
     */
    public record Result(List<Outcome> outcomes, Map<Integer, Integer> medianPointsAtPosition) {
    }

    public List<Outcome> simulate(List<TeamState> teams, List<Fixture> fixtures, List<String> allGoals,
                                   IntFunction<List<String>> goalsAtPosition, Random random) {
        return simulateSeason(teams, fixtures, allGoals, goalsAtPosition, random).outcomes();
    }

    public Result simulateSeason(List<TeamState> teams, List<Fixture> fixtures, List<String> allGoals,
                                 IntFunction<List<String>> goalsAtPosition, Random random) {
        Map<Long, Map<String, Integer>> hits = new HashMap<>();
        for (TeamState team : teams) {
            Map<String, Integer> zeros = new HashMap<>();
            for (String goal : allGoals) {
                zeros.put(goal, 0);
            }
            hits.put(team.teamId(), zeros);
        }

        // With no remaining fixtures every run produces the same table — one run is exact, not an approximation.
        int runs = fixtures.isEmpty() ? 1 : RUNS;
        // a histogram per position instead of every run's points: final totals are small whole numbers
        int maxPoints = teams.stream().mapToInt(TeamState::points).max().orElse(0) + 3 * fixtures.size();
        int[][] pointsAtPosition = new int[teams.size()][maxPoints + 1];
        for (int run = 0; run < runs; run++) {
            Map<Long, long[]> table = new HashMap<>();
            for (TeamState team : teams) {
                table.put(team.teamId(), new long[]{team.teamId(), team.points(), team.goalDifference(), team.goalsFor()});
            }
            for (Fixture fixture : fixtures) {
                int homeGoals = poisson(fixture.expectedHomeGoals(), random);
                int awayGoals = poisson(fixture.expectedAwayGoals(), random);
                credit(table.get(fixture.homeTeamId()), homeGoals, awayGoals);
                credit(table.get(fixture.awayTeamId()), awayGoals, homeGoals);
            }
            List<long[]> ranked = new ArrayList<>(table.values());
            ranked.sort(Comparator.<long[]>comparingLong(row -> row[1])
                    .thenComparingLong(row -> row[2])
                    .thenComparingLong(row -> row[3])
                    .reversed());
            for (int i = 0; i < ranked.size(); i++) {
                long teamId = ranked.get(i)[0];
                pointsAtPosition[i][(int) ranked.get(i)[1]]++;
                for (String goal : goalsAtPosition.apply(i + 1)) {
                    hits.get(teamId).merge(goal, 1, Integer::sum);
                }
            }
        }

        List<Outcome> outcomes = new ArrayList<>();
        for (TeamState team : teams) {
            Map<String, Double> probabilities = new HashMap<>();
            for (String goal : allGoals) {
                probabilities.put(goal, hits.get(team.teamId()).get(goal) / (double) runs);
            }
            outcomes.add(new Outcome(team.teamId(), probabilities));
        }
        Map<Integer, Integer> medians = new HashMap<>();
        for (int i = 0; i < pointsAtPosition.length; i++) {
            medians.put(i + 1, median(pointsAtPosition[i], runs));
        }
        return new Result(outcomes, medians);
    }

    /** The lower median of a histogram holding {@code runs} values. */
    static int median(int[] histogram, int runs) {
        int seen = 0;
        for (int points = 0; points < histogram.length; points++) {
            seen += histogram[points];
            if (seen * 2 >= runs) {
                return points;
            }
        }
        return histogram.length - 1;
    }

    private static void credit(long[] row, int goalsFor, int goalsAgainst) {
        row[1] += goalsFor > goalsAgainst ? 3 : goalsFor == goalsAgainst ? 1 : 0;
        row[2] += goalsFor - goalsAgainst;
        row[3] += goalsFor;
    }

    /** Knuth's algorithm: draws a Poisson(lambda)-distributed goal count. */
    static int poisson(double lambda, Random random) {
        double threshold = Math.exp(-lambda);
        int k = 0;
        double product = 1.0;
        do {
            k++;
            product *= random.nextDouble();
        } while (product > threshold);
        return k - 1;
    }
}
