package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.entity.Goal;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.ResultStatus;
import de.javamark.matchoracle.matchday.entity.Score;
import de.javamark.matchoracle.matchday.entity.Team;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Story r084 "Tore der besonderen Art": late goals, very early goals, converted penalties and own
 * goals of a league season, with the clubs behind them. A goal counts for the side whose score it
 * raised — an own goal for the team that benefits. Final results only.
 */
public final class SpecialGoalsCalculator {

    /** How many places each card lists; every club tied on the last place comes along. */
    public static final int PLACES = 3;

    private SpecialGoalsCalculator() {
    }

    public enum Kind {
        /** From the 90th minute on, stoppage time included (the source counts it on, 90+3 is 93). */
        LATE(g -> g.minute != null && g.minute >= 90),
        /** Minutes 1 to 5. */
        EARLY(g -> g.minute != null && g.minute <= 5),
        PENALTY(g -> g.penalty),
        OWN_GOAL(g -> g.ownGoal);

        private final Predicate<Goal> matches;

        Kind(Predicate<Goal> matches) {
            this.matches = matches;
        }
    }

    /** One goal of a kind, with the side it counted for. */
    public record SpecialGoal(Goal goal, Match match, Team creditedTo) {
    }

    /** A club's count within one kind and its rank (equal counts share a rank). */
    public record ClubCount(Team team, int count, int rank) {
    }

    /** One card: the total, every club's count by rank, the latest goal (null if there is none). */
    public record Card(Kind kind, int total, List<ClubCount> clubs, SpecialGoal latest) {
        /** The top {@link #PLACES} places, ties on the last place included. */
        public List<ClubCount> top() {
            return clubs.stream().filter(c -> c.rank() <= PLACES).toList();
        }
    }

    public static List<Card> of(List<Match> matches) {
        List<Match> settled = matches.stream()
                .filter(m -> m.fullTimeScore != null && m.resultStatus == ResultStatus.FINAL)
                .sorted(Comparator.comparing((Match m) -> m.kickoff))
                .toList();
        List<SpecialGoal> all = new ArrayList<>();
        for (Match m : settled) {
            Score before = new Score(0, 0);
            for (Goal g : m.matchGoals()) {
                boolean home = g.scoreAfter.home > before.home;
                before = g.scoreAfter;
                all.add(new SpecialGoal(g, m, home ? m.homeTeam : m.awayTeam));
            }
        }
        List<Card> cards = new ArrayList<>();
        for (Kind kind : Kind.values()) {
            List<SpecialGoal> goals = all.stream().filter(sg -> kind.matches.test(sg.goal())).toList();
            Map<Team, Integer> counts = new LinkedHashMap<>();
            goals.forEach(sg -> counts.merge(sg.creditedTo(), 1, Integer::sum));
            List<Map.Entry<Team, Integer>> sorted = counts.entrySet().stream()
                    .sorted(Map.Entry.<Team, Integer>comparingByValue().reversed()
                            .thenComparing(e -> e.getKey().name))
                    .toList();
            List<ClubCount> clubs = new ArrayList<>();
            int rank = 0;
            for (int i = 0; i < sorted.size(); i++) {
                if (i == 0 || !sorted.get(i).getValue().equals(sorted.get(i - 1).getValue())) rank = i + 1;
                clubs.add(new ClubCount(sorted.get(i).getKey(), sorted.get(i).getValue(), rank));
            }
            // the goals are in kickoff order, and within a match in their own order: the last one is the latest
            cards.add(new Card(kind, goals.size(), List.copyOf(clubs), goals.isEmpty() ? null : goals.getLast()));
        }
        return List.copyOf(cards);
    }
}
