package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.Team;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * "Was noch geht" in a group: every outcome of the group's open matches played through, and for
 * each team the best and the worst place it can still finish on. Points only — on equal points the
 * best place counts the tie in the team's favour, the worst against it, so "sicher" holds whatever
 * the tie-breakers say. A calculation, not a forecast.
 */
public final class GroupReachCalculator {

    /** More open matches than this and every place is still open anyway; the count stays small. */
    static final int MAX_OPEN = 12;

    private GroupReachCalculator() {
    }

    /** Places counted from 1. */
    public record PlaceRange(int best, int worst) {
        public boolean settled() {
            return best == worst;
        }
    }

    /**
     * @param points the group's points today, every team of the group
     * @param open   the group's matches not yet counted in {@code points}
     * @return each team's range, in the order of {@code points}; empty when there are too many open matches
     */
    public static Map<Team, PlaceRange> of(Map<Team, Integer> points, List<Match> open) {
        if (open.size() > MAX_OPEN) {
            return Map.of();
        }
        // plain arrays: a group's first matchday leaves 3^10 outcomes per group to play through
        List<Team> teams = List.copyOf(points.keySet());
        int n = teams.size();
        Map<Team, Integer> index = new HashMap<>();
        for (int i = 0; i < n; i++) index.put(teams.get(i), i);
        int[] today = new int[n];
        for (int i = 0; i < n; i++) today[i] = points.get(teams.get(i));
        int[] home = open.stream().mapToInt(m -> index.getOrDefault(m.homeTeam, -1)).toArray();
        int[] away = open.stream().mapToInt(m -> index.getOrDefault(m.awayTeam, -1)).toArray();
        int[] best = new int[n];
        int[] worst = new int[n];
        java.util.Arrays.fill(best, Integer.MAX_VALUE);
        int[] end = new int[n];
        int outcomes = (int) Math.pow(3, open.size());
        for (int code = 0; code < outcomes; code++) {
            System.arraycopy(today, 0, end, 0, n);
            int rest = code;
            for (int k = 0; k < home.length; k++) {
                switch (rest % 3) {
                    case 0 -> add(end, home[k], 3);
                    case 1 -> {
                        add(end, home[k], 1);
                        add(end, away[k], 1);
                    }
                    default -> add(end, away[k], 3);
                }
                rest /= 3;
            }
            for (int t = 0; t < n; t++) {
                int above = 0;
                int level = 0;
                for (int o = 0; o < n; o++) {
                    if (o == t) continue;
                    if (end[o] > end[t]) above++;
                    else if (end[o] == end[t]) level++;
                }
                best[t] = Math.min(best[t], above + 1);
                worst[t] = Math.max(worst[t], above + level + 1);
            }
        }
        Map<Team, PlaceRange> ranges = new LinkedHashMap<>();
        for (int t = 0; t < n; t++) ranges.put(teams.get(t), new PlaceRange(best[t], worst[t]));
        return ranges;
    }

    /** A team outside the group (never expected) simply gets no points. */
    private static void add(int[] end, int team, int points) {
        if (team >= 0) end[team] += points;
    }
}
