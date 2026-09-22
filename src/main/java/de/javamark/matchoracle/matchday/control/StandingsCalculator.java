package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.entity.Balance;
import de.javamark.matchoracle.matchday.entity.League;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.Matchday;
import de.javamark.matchoracle.matchday.entity.ResultStatus;
import de.javamark.matchoracle.matchday.entity.StandingPosition;
import de.javamark.matchoracle.matchday.entity.Standings;
import de.javamark.matchoracle.matchday.entity.Team;
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

    /** Pure calculation over the played matches of one season. */
    Standings standings(League league, int season, int beforeMatchday, List<Match> played) {
        Map<Team, Balance> balances = new LinkedHashMap<>();
        for (Match match : played) {
            balances.merge(match.homeTeam, Balance.EMPTY.plus(match.viewedBy(match.homeTeam)), (a, b) -> a.plus(match.viewedBy(match.homeTeam)));
            balances.merge(match.awayTeam, Balance.EMPTY.plus(match.viewedBy(match.awayTeam)), (a, b) -> a.plus(match.viewedBy(match.awayTeam)));
        }
        List<StandingPosition> positions = new ArrayList<>();
        int position = 1;
        for (Map.Entry<Team, Balance> entry : balances.entrySet().stream().sorted(RANKING).toList()) {
            positions.add(new StandingPosition(position++, entry.getKey(), entry.getValue()));
        }
        boolean provisional = played.stream().anyMatch(m -> m.resultStatus == ResultStatus.PROVISIONAL);
        return new Standings(league, season, beforeMatchday, List.copyOf(positions), provisional);
    }
}
