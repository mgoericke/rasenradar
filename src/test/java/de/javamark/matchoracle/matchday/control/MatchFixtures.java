package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.entity.Goal;
import de.javamark.matchoracle.matchday.entity.League;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.Matchday;
import de.javamark.matchoracle.matchday.entity.ResultStatus;
import de.javamark.matchoracle.matchday.entity.Score;
import de.javamark.matchoracle.matchday.entity.Team;

import java.time.Duration;
import java.time.Instant;

/** Builds in-memory entities for unit tests — no database involved. */
final class MatchFixtures {

    static final Instant SEASON_START = Instant.parse("2025-08-22T18:30:00Z");

    private MatchFixtures() {
    }

    static Team team(String name) {
        Team team = new Team();
        team.name = name;
        team.shortName = name;
        return team;
    }

    static Matchday matchday(int number) {
        Matchday matchday = new Matchday();
        matchday.league = League.BUNDESLIGA_1;
        matchday.season = 2025;
        matchday.number = number;
        return matchday;
    }

    /** A played match on the given matchday; kickoff is one week per matchday after season start. */
    static Match played(int matchdayNumber, Team home, int homeGoals, Team away, int awayGoals) {
        Match match = new Match();
        match.matchday = matchday(matchdayNumber);
        match.homeTeam = home;
        match.awayTeam = away;
        match.kickoff = SEASON_START.plus(Duration.ofDays(7L * (matchdayNumber - 1)));
        match.fullTimeScore = new Score(homeGoals, awayGoals);
        match.resultStatus = ResultStatus.FINAL;
        return match;
    }

    /** Appends a goal to the match; the running score after it tells which side scored. */
    static Goal goal(Match match, String scorer, int homeAfter, int awayAfter) {
        Goal goal = new Goal();
        goal.match = match;
        goal.position = match.goals.size() + 1;
        goal.scorerName = scorer;
        goal.scoreAfter = new Score(homeAfter, awayAfter);
        match.goals.add(goal);
        return goal;
    }

    static Match provisional(int matchdayNumber, Team home, int homeGoals, Team away, int awayGoals) {
        Match match = played(matchdayNumber, home, homeGoals, away, awayGoals);
        match.resultStatus = ResultStatus.PROVISIONAL;
        return match;
    }
}
