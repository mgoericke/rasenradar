package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.ResultStatus;
import de.javamark.matchoracle.matchday.entity.Team;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Story r098 "Heimvorteil": how often the home side wins, draws or loses in a league season, and
 * which clubs collect more points at home or away. Final results only — a provisional score never
 * counts (Spec 01).
 */
public final class HomeAdvantageCalculator {

    /** Decided 31 Oct 2026: each of the two club lists shows five clubs. */
    public static final int LIST_LENGTH = 5;

    private HomeAdvantageCalculator() {
    }

    /** Points per game at home and away; {@code difference} is home minus away. */
    public record ClubSplit(Team team, int homeGames, int homePoints, int awayGames, int awayPoints) {
        public double homePerGame() {
            return (double) homePoints / homeGames;
        }

        public double awayPerGame() {
            return (double) awayPoints / awayGames;
        }

        public double difference() {
            return homePerGame() - awayPerGame();
        }
    }

    /**
     * The league's shares as whole percent that add up to 100 (largest remainder), the counts behind
     * them, and the two club lists, each with at most {@link #LIST_LENGTH} clubs.
     */
    public record HomeAdvantage(int matches, int homeWins, int draws, int awayWins,
                                int homeWinPercent, int drawPercent, int awayWinPercent,
                                List<ClubSplit> strongestAtHome, List<ClubSplit> preferAway) {

        /** Home wins per away win, or null when there is no away win (no division by zero). */
        public Double homeToAwayRatio() {
            return awayWins == 0 ? null : (double) homeWins / awayWins;
        }
    }

    public static HomeAdvantage of(List<Match> matches) {
        List<Match> settled = matches.stream()
                .filter(m -> m.fullTimeScore != null && m.resultStatus == ResultStatus.FINAL)
                .toList();
        int home = 0, draw = 0, away = 0;
        Map<Team, int[]> clubs = new LinkedHashMap<>(); // homeGames, homePoints, awayGames, awayPoints
        for (Match m : settled) {
            int h = m.fullTimeScore.home, a = m.fullTimeScore.away;
            int homePoints = h > a ? 3 : h == a ? 1 : 0;
            int awayPoints = a > h ? 3 : h == a ? 1 : 0;
            if (h > a) home++;
            else if (h == a) draw++;
            else away++;
            int[] hc = clubs.computeIfAbsent(m.homeTeam, t -> new int[4]);
            hc[0]++;
            hc[1] += homePoints;
            int[] ac = clubs.computeIfAbsent(m.awayTeam, t -> new int[4]);
            ac[2]++;
            ac[3] += awayPoints;
        }
        int[] percent = percentages(home, draw, away);

        // a club without a home or without an away game is left out, not shown with zero
        List<ClubSplit> splits = clubs.entrySet().stream()
                .filter(e -> e.getValue()[0] > 0 && e.getValue()[2] > 0)
                .map(e -> new ClubSplit(e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2], e.getValue()[3]))
                .toList();
        Comparator<ClubSplit> byName = Comparator.comparing(c -> c.team().name);
        List<ClubSplit> atHome = splits.stream().filter(c -> c.difference() > 0)
                .sorted(Comparator.comparingDouble(ClubSplit::difference).reversed().thenComparing(byName))
                .limit(LIST_LENGTH).toList();
        List<ClubSplit> awayList = splits.stream().filter(c -> c.difference() < 0)
                .sorted(Comparator.comparingDouble(ClubSplit::difference).thenComparing(byName))
                .limit(LIST_LENGTH).toList();
        return new HomeAdvantage(settled.size(), home, draw, away, percent[0], percent[1], percent[2], atHome, awayList);
    }

    /** Whole percentages that always add up to 100 — the largest remainders get the leftover points. */
    static int[] percentages(int... counts) {
        int total = 0;
        for (int c : counts) total += c;
        int[] result = new int[counts.length];
        if (total == 0) {
            return result;
        }
        double[] remainder = new double[counts.length];
        int given = 0;
        for (int i = 0; i < counts.length; i++) {
            double exact = 100.0 * counts[i] / total;
            result[i] = (int) Math.floor(exact);
            remainder[i] = exact - result[i];
            given += result[i];
        }
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < counts.length; i++) order.add(i);
        order.sort((x, y) -> Double.compare(remainder[y], remainder[x]));
        for (int i = 0; given < 100; i++, given++) {
            result[order.get(i % counts.length)]++;
        }
        return result;
    }
}
