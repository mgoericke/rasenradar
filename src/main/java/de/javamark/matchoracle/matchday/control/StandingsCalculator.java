package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.entity.Balance;
import de.javamark.matchoracle.matchday.entity.League;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.Matchday;
import de.javamark.matchoracle.matchday.entity.ResultStatus;
import de.javamark.matchoracle.matchday.entity.StandingPosition;
import de.javamark.matchoracle.matchday.entity.Standings;
import de.javamark.matchoracle.matchday.entity.Team;
import de.javamark.matchoracle.matchday.entity.TeamMatchView;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Spec 01, step 5: league table computed from the played matches of a season
 * before a matchday. Never stored — always derived, so corrections need no recalculation step.
 */
@ApplicationScoped
public class StandingsCalculator {

    /** Ranking: points, goal difference, goals scored. */
    private static final Comparator<Map.Entry<Team, Balance>> RANKING = Comparator
            .comparingInt((Map.Entry<Team, Balance> e) -> e.getValue().points())
            .thenComparingInt(e -> e.getValue().goalDifference())
            .thenComparingInt(e -> e.getValue().goalsFor())
            .reversed();

    /** Spec 06: a matchday of a group competition brings its group along — its table is the group's. */
    public Standings standingsBefore(Matchday matchday) {
        return standings(matchday.league, matchday.season, matchday.number,
                Match.findPlayedBefore(matchday.league, matchday.season, matchday.groupName, matchday.number));
    }

    public Standings standingsBefore(League league, int season, int matchday) {
        return standings(league, season, matchday, Match.findPlayedBefore(league, season, matchday));
    }

    /**
     * Story r059, "Wenn es so bleibt": the table with every running match counted at its current
     * score — 0:0 while the source has none. A match with a final score is already in
     * {@code before} and never running, so nothing counts twice. Never stored, never used for the
     * review: the result is marked as including provisional results.
     */
    public Standings liveStandings(Standings before, List<Match> running) {
        Map<Team, Balance> balances = new LinkedHashMap<>();
        before.positions().forEach(p -> balances.put(p.team(), p.balance()));
        for (Match match : running) {
            int home = match.liveScore == null ? 0 : match.liveScore.home;
            int away = match.liveScore == null ? 0 : match.liveScore.away;
            balances.merge(match.homeTeam, Balance.EMPTY.plus(new TeamMatchView(match, true, home, away)),
                    (a, b) -> a.plus(new TeamMatchView(match, true, home, away)));
            balances.merge(match.awayTeam, Balance.EMPTY.plus(new TeamMatchView(match, false, away, home)),
                    (a, b) -> a.plus(new TeamMatchView(match, false, away, home)));
        }
        return new Standings(before.league(), before.season(), before.beforeMatchday(), ranked(balances), true);
    }

    /** Pure calculation over the played matches of one season; public for pages that reuse the same matches. */
    public Standings standings(League league, int season, int beforeMatchday, List<Match> played) {
        Map<Team, Balance> balances = new LinkedHashMap<>();
        for (Match match : played) {
            balances.merge(match.homeTeam, Balance.EMPTY.plus(match.viewedBy(match.homeTeam)), (a, b) -> a.plus(match.viewedBy(match.homeTeam)));
            balances.merge(match.awayTeam, Balance.EMPTY.plus(match.viewedBy(match.awayTeam)), (a, b) -> a.plus(match.viewedBy(match.awayTeam)));
        }
        boolean provisional = played.stream().anyMatch(m -> m.resultStatus == ResultStatus.PROVISIONAL);
        return new Standings(league, season, beforeMatchday, ranked(balances), provisional);
    }

    /** Story r041: which matches a split table counts for each team. */
    public enum Split { HOME, AWAY, LAST_FIVE }

    /** Story r041: how many of its most recent matches "Letzte 5" counts per team. */
    static final int LAST = 5;

    /**
     * Story r041: the table counted from home matches only, away matches only, or each team's last
     * five — final results only, ranked like the full table (points, goal difference, goals).
     * {@code played} must be in kickoff order.
     */
    public Standings splitStandings(League league, int season, int beforeMatchday, List<Match> played, Split split) {
        Map<Team, List<TeamMatchView>> views = new LinkedHashMap<>();
        for (Match match : played) {
            if (match.resultStatus != ResultStatus.FINAL) continue;
            if (split != Split.AWAY) views.computeIfAbsent(match.homeTeam, t -> new ArrayList<>()).add(match.viewedBy(match.homeTeam));
            if (split != Split.HOME) views.computeIfAbsent(match.awayTeam, t -> new ArrayList<>()).add(match.viewedBy(match.awayTeam));
        }
        Map<Team, Balance> balances = new LinkedHashMap<>();
        views.forEach((team, list) -> {
            List<TeamMatchView> counted = split == Split.LAST_FIVE ? list.subList(Math.max(0, list.size() - LAST), list.size()) : list;
            Balance b = Balance.EMPTY;
            for (TeamMatchView v : counted) b = b.plus(v);
            balances.put(team, b);
        });
        return new Standings(league, season, beforeMatchday, ranked(balances), false);
    }

    private static List<StandingPosition> ranked(Map<Team, Balance> balances) {
        List<StandingPosition> positions = new ArrayList<>();
        int position = 1;
        for (Map.Entry<Team, Balance> entry : balances.entrySet().stream().sorted(RANKING).toList()) {
            positions.add(new StandingPosition(position++, entry.getKey(), entry.getValue()));
        }
        return List.copyOf(positions);
    }
}
