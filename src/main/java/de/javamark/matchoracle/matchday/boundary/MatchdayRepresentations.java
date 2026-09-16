package de.javamark.matchoracle.matchday.boundary;

import de.javamark.matchoracle.matchday.entity.Balance;
import de.javamark.matchoracle.matchday.entity.Form;
import de.javamark.matchoracle.matchday.entity.Goal;
import de.javamark.matchoracle.matchday.entity.HeadToHead;
import de.javamark.matchoracle.matchday.entity.League;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.Matchday;
import de.javamark.matchoracle.matchday.entity.ResultStatus;
import de.javamark.matchoracle.matchday.entity.Score;
import de.javamark.matchoracle.matchday.entity.StandingPosition;
import de.javamark.matchoracle.matchday.entity.Standings;
import de.javamark.matchoracle.matchday.entity.Team;
import de.javamark.matchoracle.matchday.entity.TeamMatchView;
import de.javamark.matchoracle.matchday.entity.TeamResult;

import java.time.Instant;
import java.util.List;

/** JSON shapes for the viewer. Entities are never serialized directly (bidirectional links, lazy state). */
final class MatchdayRepresentations {

    /** Spec 01, rules: the origin of the data is shown to the viewer. */
    static final String SOURCE = "OpenLigaDB";

    private MatchdayRepresentations() {
    }

    record TeamRep(long id, String name, String shortName) {
        static TeamRep of(Team t) {
            return new TeamRep(t.id, t.name, t.shortName);
        }
    }

    record ScoreRep(int home, int away) {
        static ScoreRep of(Score s) {
            return s == null ? null : new ScoreRep(s.home, s.away);
        }
    }

    record GoalRep(Integer minute, String scorer, ScoreRep scoreAfter, boolean penalty, boolean ownGoal) {
        static GoalRep of(Goal g) {
            return new GoalRep(g.minute, g.scorerName, ScoreRep.of(g.scoreAfter), g.penalty, g.ownGoal);
        }
    }

    record MatchRep(long id, Instant kickoff, TeamRep homeTeam, TeamRep awayTeam,
                    ScoreRep halfTime, ScoreRep fullTime, ResultStatus resultStatus, List<GoalRep> goals) {
        static MatchRep of(Match m) {
            return new MatchRep(m.id, m.kickoff, TeamRep.of(m.homeTeam), TeamRep.of(m.awayTeam),
                    ScoreRep.of(m.halfTimeScore), ScoreRep.of(m.fullTimeScore), m.resultStatus,
                    m.goals.stream().map(GoalRep::of).toList());
        }
    }

    /** Provenance and age of the data (spec 01, rules). */
    record DataRep(String source, Instant sourceLastChangedAt, Instant lastCheckedAt) {
        static DataRep of(Matchday md) {
            return new DataRep(SOURCE, md.sourceLastChangedAt, md.lastCheckedAt);
        }
    }

    record MatchdayRep(League league, int season, int number, DataRep data, List<MatchRep> matches) {
        static MatchdayRep of(Matchday md, List<Match> matches) {
            return new MatchdayRep(md.league, md.season, md.number, DataRep.of(md), matches.stream().map(MatchRep::of).toList());
        }
    }

    record BalanceRep(int played, int wins, int draws, int losses, int goalsFor, int goalsAgainst, int points) {
        static BalanceRep of(Balance b) {
            return new BalanceRep(b.played(), b.wins(), b.draws(), b.losses(), b.goalsFor(), b.goalsAgainst(), b.points());
        }
    }

    record TeamMatchRep(long matchId, Instant kickoff, boolean home, TeamRep opponent, int goalsFor, int goalsAgainst,
                        TeamResult result, ResultStatus resultStatus) {
        static TeamMatchRep of(TeamMatchView v) {
            return new TeamMatchRep(v.match().id, v.match().kickoff, v.home(), TeamRep.of(v.opponent()),
                    v.goalsFor(), v.goalsAgainst(), v.result(), v.match().resultStatus);
        }
    }

    record FormRep(List<TeamResult> results, List<TeamMatchRep> lastMatches, BalanceRep home, BalanceRep away, boolean includesProvisional) {
        static FormRep of(Form f) {
            return new FormRep(f.results(), f.lastMatches().stream().map(TeamMatchRep::of).toList(),
                    BalanceRep.of(f.home()), BalanceRep.of(f.away()), f.includesProvisional());
        }
    }

    record StandingRep(int position, TeamRep team, BalanceRep balance) {
        static StandingRep of(StandingPosition p) {
            return new StandingRep(p.position(), TeamRep.of(p.team()), BalanceRep.of(p.balance()));
        }
    }

    record StandingsRep(League league, int season, int beforeMatchday, List<StandingRep> positions, boolean includesProvisional) {
        static StandingsRep of(Standings s) {
            return new StandingsRep(s.league(), s.season(), s.beforeMatchday(),
                    s.positions().stream().map(StandingRep::of).toList(), s.includesProvisional());
        }
    }

    record HeadToHeadRep(BalanceRep balance, List<TeamMatchRep> matches, boolean includesProvisional) {
        static HeadToHeadRep of(HeadToHead h) {
            return new HeadToHeadRep(BalanceRep.of(h.balance()), h.matches().stream().map(TeamMatchRep::of).toList(), h.includesProvisional());
        }
    }

    record TeamSituationRep(TeamRep team, StandingRep standing, FormRep form) {
    }

    /** A match with everything the viewer (and later the forecaster) needs to judge it. */
    record MatchDetailRep(MatchRep match, League league, int season, int matchday, DataRep data,
                          TeamSituationRep homeTeam, TeamSituationRep awayTeam, HeadToHeadRep headToHead) {
    }
}
