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

    /** In a cup this same field carries the name of the round ("Achtelfinale"). */
    public record Group(@JsonProperty("groupOrderID") int number, @JsonProperty("groupName") String name) {
    }

    public record Team(int teamId, String teamName, String shortName, String teamIconUrl) {
    }

    /**
     * resultTypeID 1 = half time, 2 = a summary whose meaning varies by edition,
     * 3 = after 90 minutes, 4 = after extra time, 5 = after a penalty shootout.
     */
    public record Result(@JsonProperty("resultTypeID") int type, int pointsTeam1, int pointsTeam2) {
        public static final int HALF_TIME = 1;
        /** Careful: carries the 90-minute score, the extra-time score or the shootout — see {@link #ninetyMinuteResult()}. */
        public static final int SUMMARY = 2;
        public static final int NINETY_MINUTES = 3;
        public static final int EXTRA_TIME = 4;
        public static final int PENALTIES = 5;
    }

    public record Goal(int scoreTeam1, int scoreTeam2, Integer matchMinute, String goalGetterName,
                       boolean isPenalty, boolean isOwnGoal) {
    }

    public Optional<Result> result(int type) {
        return matchResults == null ? Optional.empty()
                : matchResults.stream().filter(r -> r.type() == type).findFirst();
    }

    /**
     * The score after 90 minutes, but only once the match has actually finished: the source
     * publishes a summary entry with the live, still-changing score well before {@code finished}
     * turns true, and reading it unconditionally makes a match in progress look already played.
     * <p>
     * The source only gives the 90 minutes their own entry when the match went beyond them.
     * Reading the summary instead would show a shootout aggregate as the score — seen in the
     * 2023/24 cup, where Sandhausen against Hannover reads "7:5" for a match that ended 3:3.
     */
    public Optional<Result> ninetyMinuteResult() {
        return finished ? result(Result.NINETY_MINUTES).or(() -> result(Result.SUMMARY)) : Optional.empty();
    }

    public Optional<Result> extraTimeResult() {
        return finished ? result(Result.EXTRA_TIME) : Optional.empty();
    }

    public Optional<Result> penaltyResult() {
        return finished ? result(Result.PENALTIES) : Optional.empty();
    }

    /** Goals of the match itself — a shootout taker carries no match minute and is not a goal. */
    public List<Goal> matchGoals() {
        return goalsOrEmpty().stream().filter(g -> g.matchMinute() != null).toList();
    }

    public List<Goal> goalsOrEmpty() {
        return goals == null ? List.of() : goals;
    }
}
