package de.javamark.matchoracle.matchday.boundary;

import java.time.Instant;
import java.util.List;

/**
 * Everything another feature needs to know about a match before it is played —
 * spec 02, step 2: form, standings, home/away balance, previous meetings.
 * Plain values only, so other features never touch matchday entities.
 */
public record MatchSituation(
        long matchId,
        /** League shortcut, bl1 or bl2. */
        String league,
        int season,
        int matchday,
        Instant kickoff,
        boolean played,
        /** Null unless the match is played; whether it is FINAL is {@code resultFinal}. */
        Score result,
        boolean resultFinal,
        TeamSituation homeTeam,
        TeamSituation awayTeam,
        List<Meeting> previousMeetings,
        boolean includesProvisional) {

    public record Score(int home, int away) {
    }

    public record TeamSituation(
            long teamId,
            String name,
            String shortName,
            Integer position,
            int points,
            List<Result> lastMatches,
            Balance homeBalance,
            Balance awayBalance,
            boolean promoted,
            List<Instant> recentKickoffs) {
    }

    /** A played match from this team's point of view. */
    public record Result(Instant kickoff, String opponent, boolean home, int goalsFor, int goalsAgainst, String result) {
    }

    public record Balance(int played, int wins, int draws, int losses, int goalsFor, int goalsAgainst, int points) {
    }

    /** A previous meeting from the home team's point of view. */
    public record Meeting(Instant kickoff, int season, boolean home, int goalsFor, int goalsAgainst, String result) {
    }
}
