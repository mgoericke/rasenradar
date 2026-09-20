package de.javamark.matchoracle.matchday.control;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** One match as returned by OpenLigaDB. Only the fields we use are mapped. */
public record OpenLigaDbMatch(
        @JsonProperty("matchID") int id,
        @JsonProperty("leagueSeason") int season,
        @JsonProperty("matchDateTimeUTC") Instant kickoff,
        @JsonProperty("lastUpdateDateTime") LocalDateTime lastUpdate,
        @JsonProperty("matchIsFinished") boolean finished,
        Group group,
        Team team1,
        Team team2,
        List<Result> matchResults,
        List<Goal> goals) {

    public record Group(@JsonProperty("groupOrderID") int number) {
    }

    public record Team(int teamId, String teamName, String shortName, String teamIconUrl) {
    }

    /** resultTypeID 1 = half time, 2 = full time (after 90 minutes). */
    public record Result(@JsonProperty("resultTypeID") int type, int pointsTeam1, int pointsTeam2) {
        public static final int HALF_TIME = 1;
        public static final int FULL_TIME = 2;
    }

    public record Goal(int scoreTeam1, int scoreTeam2, Integer matchMinute, String goalGetterName,
                       boolean isPenalty, boolean isOwnGoal) {
    }

    public Optional<Result> result(int type) {
        return matchResults == null ? Optional.empty()
                : matchResults.stream().filter(r -> r.type() == type).findFirst();
    }

    /**
     * The full-time result, but only once the match has actually finished. The source already
     * publishes a "full time" entry with the live, still-changing score well before {@code finished}
     * turns true — reading it unconditionally makes a match in progress look already played.
     */
    public Optional<Result> finalResult() {
        return finished ? result(Result.FULL_TIME) : Optional.empty();
    }

    public List<Goal> goalsOrEmpty() {
        return goals == null ? List.of() : goals;
    }
}
