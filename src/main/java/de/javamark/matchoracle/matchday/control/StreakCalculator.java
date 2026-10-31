package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.ResultStatus;
import de.javamark.matchoracle.matchday.entity.Team;
import de.javamark.matchoracle.matchday.entity.TeamMatchView;
import de.javamark.matchoracle.matchday.entity.TeamResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Story r068 "Serien": the streaks a club is on right now, counted back from its latest final
 * result within one season (a streak never crosses the season start). Provisional results are left
 * out — the streak is computed as if they had not been played yet.
 */
public final class StreakCalculator {

    private StreakCalculator() {
    }

    /** The kinds of streak with their threshold; the first four are "Läuft", the rest "Hängt". */
    public enum Kind {
        // thresholds decided 31 Oct 2026 (3 wins, 4 unbeaten, 3 defeats), the others from the story
        WINS(3, true, v -> v.result() == TeamResult.WIN),
        UNBEATEN(4, true, v -> v.result() != TeamResult.LOSS),
        CLEAN_SHEETS(3, true, v -> v.goalsAgainst() == 0),
        SCORING(6, true, v -> v.goalsFor() > 0),
        LOSSES(3, false, v -> v.result() == TeamResult.LOSS),
        WINLESS(4, false, v -> v.result() != TeamResult.WIN),
        GOALLESS(3, false, v -> v.goalsFor() == 0);

        public final int threshold;
        /** true for "Läuft", false for "Hängt". */
        public final boolean good;
        private final Predicate<TeamMatchView> holds;

        Kind(int threshold, boolean good, Predicate<TeamMatchView> holds) {
            this.threshold = threshold;
            this.good = good;
            this.holds = holds;
        }
    }

    /**
     * A running streak: its kind, its length, whether it covers every match of the season so far,
     * and the matches it is made of, oldest first.
     */
    public record Streak(Team team, Kind kind, int length, boolean wholeSeason, List<TeamMatchView> matches) {
        /** How far above its threshold — makes a 10-match unbeaten run outrank a 3-win run. */
        double strength() {
            return (double) length / kind.threshold;
        }
    }

    /** "Läuft" and "Hängt": at most one streak per club in each, the strongest first. */
    public record Streaks(List<Streak> going, List<Streak> stuck) {
    }

    public static Streaks of(List<Match> seasonMatches) {
        Map<Team, List<TeamMatchView>> byTeam = new LinkedHashMap<>();
        seasonMatches.stream()
                .filter(m -> m.fullTimeScore != null && m.resultStatus == ResultStatus.FINAL)
                .sorted(Comparator.comparing((Match m) -> m.kickoff))
                .forEach(m -> {
                    byTeam.computeIfAbsent(m.homeTeam, t -> new ArrayList<>()).add(m.viewedBy(m.homeTeam));
                    byTeam.computeIfAbsent(m.awayTeam, t -> new ArrayList<>()).add(m.viewedBy(m.awayTeam));
                });
        List<Streak> going = new ArrayList<>();
        List<Streak> stuck = new ArrayList<>();
        byTeam.forEach((team, views) -> {
            strongest(team, views, true).ifPresent(going::add);
            strongest(team, views, false).ifPresent(stuck::add);
        });
        Comparator<Streak> order = Comparator.comparingDouble(Streak::strength).reversed()
                .thenComparing(s -> s.kind().ordinal())
                .thenComparing(s -> s.team().name);
        going.sort(order);
        stuck.sort(order);
        return new Streaks(List.copyOf(going), List.copyOf(stuck));
    }

    /** The club's strongest running streak of one group; ties go to the kind listed first (wins before unbeaten). */
    static Optional<Streak> strongest(Team team, List<TeamMatchView> oldestFirst, boolean good) {
        Streak best = null;
        for (Kind kind : Kind.values()) {
            if (kind.good != good) continue;
            int length = 0;
            for (int i = oldestFirst.size() - 1; i >= 0 && kind.holds.test(oldestFirst.get(i)); i--) {
                length++;
            }
            if (length < kind.threshold) continue;
            Streak streak = new Streak(team, kind, length, length == oldestFirst.size(),
                    List.copyOf(oldestFirst.subList(oldestFirst.size() - length, oldestFirst.size())));
            if (best == null || streak.strength() > best.strength()) {
                best = streak;
            }
        }
        return Optional.ofNullable(best);
    }
}
