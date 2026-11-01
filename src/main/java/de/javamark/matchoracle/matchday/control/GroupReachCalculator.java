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
        List<Team> teams = List.copyOf(points.keySet());
        Map<Team, Integer> best = new HashMap<>();
        Map<Team, Integer> worst = new HashMap<>();
        int outcomes = (int) Math.pow(3, open.size());
        for (int code = 0; code < outcomes; code++) {
            Map<Team, Integer> end = new HashMap<>(points);
            int rest = code;
            for (Match m : open) {
                switch (rest % 3) {
                    case 0 -> end.merge(m.homeTeam, 3, Integer::sum);
                    case 1 -> {
                        end.merge(m.homeTeam, 1, Integer::sum);
                        end.merge(m.awayTeam, 1, Integer::sum);
                    }
                    default -> end.merge(m.awayTeam, 3, Integer::sum);
                }
                rest /= 3;
            }
            for (Team t : teams) {
                int own = end.get(t);
                int above = (int) teams.stream().filter(o -> o != t && end.get(o) > own).count();
                int level = (int) teams.stream().filter(o -> o != t && end.get(o) == own).count();
                best.merge(t, above + 1, Math::min);
                worst.merge(t, above + level + 1, Math::max);
            }
        }
        Map<Team, PlaceRange> ranges = new LinkedHashMap<>();
        teams.forEach(t -> ranges.put(t, new PlaceRange(best.get(t), worst.get(t))));
        return ranges;
    }
}
