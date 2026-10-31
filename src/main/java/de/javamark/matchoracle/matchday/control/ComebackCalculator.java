package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.entity.Goal;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.ResultStatus;
import de.javamark.matchoracle.matchday.entity.Team;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Story r091 "Aufholjagden": points a club won after being behind at any moment of a match, and
 * points it dropped after being ahead at any moment. The goal order comes from the source's own
 * sequence (the running score after each goal), never from the minute alone. Matches whose goal
 * list does not add up to the final score, and provisional results, are left out.
 */
public final class ComebackCalculator {

    private ComebackCalculator() {
    }

    /** {@code won}: points after trailing (3 for a win, 1 for a draw); {@code dropped}: 3 minus the points kept after leading. */
    public record ClubComebacks(Team team, int won, int dropped) {
        public int balance() {
            return won - dropped;
        }
    }

    /** The comeback from the largest deficit: who, how far behind, and whether it ended in a win. */
    public record BiggestComeback(Team team, int deficit, boolean won, Match match) {
    }

    public record Comebacks(int matches, int excluded, List<ClubComebacks> clubs, BiggestComeback biggest) {
    }

    public static Comebacks of(List<Match> seasonMatches) {
        List<Match> finals = seasonMatches.stream()
                .filter(m -> m.fullTimeScore != null && m.resultStatus == ResultStatus.FINAL)
                .sorted(Comparator.comparing((Match m) -> m.kickoff))
                .toList();
        Map<Team, int[]> clubs = new LinkedHashMap<>();
        BiggestComeback biggest = null;
        int counted = 0;
        for (Match m : finals) {
            clubs.computeIfAbsent(m.homeTeam, t -> new int[2]);
            clubs.computeIfAbsent(m.awayTeam, t -> new int[2]);
            if (!complete(m)) continue;
            counted++;
            // the largest deficit each side faced at any moment (home minus away, from both sides)
            int worstForHome = 0, worstForAway = 0;
            for (Goal g : m.goals) {
                int diff = g.scoreAfter.home - g.scoreAfter.away;
                worstForHome = Math.max(worstForHome, -diff);
                worstForAway = Math.max(worstForAway, diff);
            }
            int h = m.fullTimeScore.home, a = m.fullTimeScore.away;
            biggest = side(clubs, m, m.homeTeam, worstForHome, worstForAway, h, a, biggest);
            biggest = side(clubs, m, m.awayTeam, worstForAway, worstForHome, a, h, biggest);
        }
        List<ClubComebacks> rows = new ArrayList<>();
        clubs.forEach((t, v) -> rows.add(new ClubComebacks(t, v[0], v[1])));
        rows.sort(Comparator.comparingInt(ClubComebacks::balance).reversed()
                .thenComparing(Comparator.comparingInt(ClubComebacks::won).reversed())
                .thenComparing(c -> c.team().name));
        return new Comebacks(counted, finals.size() - counted, List.copyOf(rows), biggest);
    }

    private static BiggestComeback side(Map<Team, int[]> clubs, Match m, Team team, int deficit, int lead,
                                        int goalsFor, int goalsAgainst, BiggestComeback biggest) {
        int points = goalsFor > goalsAgainst ? 3 : goalsFor == goalsAgainst ? 1 : 0;
        int[] c = clubs.get(team);
        if (deficit > 0 && points > 0) {
            c[0] += points;
            boolean win = points == 3;
            // larger deficit first, a win before a draw, then the later match
            if (biggest == null || deficit > biggest.deficit() || (deficit == biggest.deficit() && (win || !biggest.won()))) {
                biggest = new BiggestComeback(team, deficit, win, m);
            }
        }
        if (lead > 0 && points < 3) {
            c[1] += 3 - points;
        }
        return biggest;
    }

    /** The goal list adds up to the final score, so the course of the match is known. */
    static boolean complete(Match m) {
        if (m.goals.size() != m.fullTimeScore.home + m.fullTimeScore.away) return false;
        if (m.goals.isEmpty()) return true;
        Goal last = m.goals.getLast();
        return last.scoreAfter.home == m.fullTimeScore.home && last.scoreAfter.away == m.fullTimeScore.away;
    }
}
