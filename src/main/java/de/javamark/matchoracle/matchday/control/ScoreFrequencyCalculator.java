package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.ResultStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Story r076 "Häufigste Ergebnisse": how often each final score occurred in a league season, as a
 * grid of home goals against away goals from 0 to 4+. Final results only.
 */
public final class ScoreFrequencyCalculator {

    /** The last row and column collect every score with this many goals or more ("4+"). */
    public static final int CAP = 4;

    private ScoreFrequencyCalculator() {
    }

    /** {@code cells[home][away]} holds the matches that ended in that (capped) score, in kickoff order. */
    public record ScoreFrequency(int matches, List<List<List<Match>>> cells, List<String> mostFrequent, int mostFrequentCount,
                                 int matchesWithFourGoals) {
        public int count(int home, int away) {
            return cells.get(home).get(away).size();
        }

        public int max() {
            int max = 0;
            for (var row : cells) for (var cell : row) max = Math.max(max, cell.size());
            return max;
        }
    }

    public static ScoreFrequency of(List<Match> matches) {
        List<Match> settled = matches.stream()
                .filter(m -> m.fullTimeScore != null && m.resultStatus == ResultStatus.FINAL)
                .sorted(Comparator.comparing((Match m) -> m.kickoff))
                .toList();
        List<List<List<Match>>> cells = new ArrayList<>();
        for (int h = 0; h <= CAP; h++) {
            List<List<Match>> row = new ArrayList<>();
            for (int a = 0; a <= CAP; a++) row.add(new ArrayList<>());
            cells.add(row);
        }
        int four = 0;
        // the exact scores, not the capped cells, decide "most frequent": 4:0 and 5:1 are different results
        java.util.Map<String, Integer> exact = new java.util.LinkedHashMap<>();
        for (Match m : settled) {
            int h = m.fullTimeScore.home, a = m.fullTimeScore.away;
            cells.get(Math.min(h, CAP)).get(Math.min(a, CAP)).add(m);
            if (h >= 4 || a >= 4) four++;
            exact.merge(h + ":" + a, 1, Integer::sum);
        }
        int best = exact.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        List<String> mostFrequent = exact.entrySet().stream().filter(e -> e.getValue() == best)
                .map(java.util.Map.Entry::getKey)
                .sorted(Comparator.comparingInt((String s) -> goals(s)).thenComparing(Comparator.naturalOrder()))
                .toList();
        return new ScoreFrequency(settled.size(), cells, best == 0 ? List.of() : mostFrequent, best, four);
    }

    private static int goals(String score) {
        String[] p = score.split(":");
        return Integer.parseInt(p[0]) + Integer.parseInt(p[1]);
    }
}
