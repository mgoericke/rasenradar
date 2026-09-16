package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.entity.Balance;
import de.javamark.matchoracle.matchday.entity.HeadToHead;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.ResultStatus;
import de.javamark.matchoracle.matchday.entity.Team;
import de.javamark.matchoracle.matchday.entity.TeamMatchView;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * Spec 01, step 5: previous meetings of the two teams of a match, across leagues
 * and seasons (the whole history we keep), before that match's kickoff.
 */
@ApplicationScoped
public class HeadToHeadCalculator {

    /** Meetings before the given match, from the home team's point of view. */
    public HeadToHead headToHeadBefore(Match match) {
        List<Match> meetings = Match.findPlayedBetweenBefore(match.homeTeam, match.awayTeam, match.kickoff);
        return headToHead(match.homeTeam, match.awayTeam, meetings);
    }

    /** Pure calculation; {@code meetingsNewestFirst} are played matches between the two teams. */
    HeadToHead headToHead(Team team, Team opponent, List<Match> meetingsNewestFirst) {
        List<TeamMatchView> views = meetingsNewestFirst.stream().map(m -> m.viewedBy(team)).toList();
        Balance balance = Balance.of(team, meetingsNewestFirst);
        boolean provisional = meetingsNewestFirst.stream().anyMatch(m -> m.resultStatus == ResultStatus.PROVISIONAL);
        return new HeadToHead(team, opponent, views, balance, provisional);
    }
}
