package de.javamark.matchoracle.season.control;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Story r039 "Was braucht {Verein}?": per placement goal the typical points total needed (the
 * median points at the goal's last place over the simulated seasons), what is still missing, and
 * the rate it takes. "Bereits sicher" and "rechnerisch nicht mehr erreichbar" are counted on
 * points alone and against the club — every rival is assumed to win out — so they never claim
 * too much. Nothing here is recomputed per page view beyond this arithmetic.
 */
public final class SeasonTargets {

    private SeasonTargets() {
    }

    public enum Status {
        /** No result can push the club below the goal's last place. */
        SECURED,
        /** Not even winning every remaining match lifts the club to the goal's last place. */
        OUT_OF_REACH,
        /** The club's points per match so far would reach the typical total. */
        ON_PACE,
        /** It takes more points per match than so far. */
        OPEN,
        /** The typical total is more than the remaining matches can give, but rivals may still stumble. */
        BEYOND_TYPICAL
    }

    /**
     * A goal as the club page names it: reached by finishing on {@code lastPlace} or better. Its share
     * of the simulated seasons is the sum of the outlook goals in {@code parts}, or one minus that sum
     * when {@code complement} (Klassenerhalt = neither relegation place).
     */
    public record Goal(String label, int lastPlace, List<String> parts, boolean complement) {
    }

    /** One club's state when the outlook run started. */
    public record Club(long teamId, int points, int remaining) {
    }

    public record Target(String label, int typicalPoints, int missing, int remaining, double requiredPerMatch,
                         double currentPerMatch, int percent, Status status) {
    }

    /** The goals of a league's club page; empty for a competition without them. */
    public static List<Goal> goals(String league) {
        return switch (league) {
            case "bl1" -> List.of(
                    new Goal("Meisterschaft", 1, List.of("Meisterschaft"), false),
                    new Goal("Europapokal", 4, List.of("Europapokal"), false),
                    new Goal("Klassenerhalt", 15, List.of("Abstiegsrelegation", "Abstieg"), true));
            case "bl2" -> List.of(
                    new Goal("Aufstieg", 2, List.of("Aufstieg"), false),
                    new Goal("Mindestens Relegationsplatz", 3, List.of("Aufstieg", "Aufstiegsrelegation"), false),
                    new Goal("Klassenerhalt", 15, List.of("Abstiegsrelegation", "Abstieg"), true));
            default -> List.of();
        };
    }

    /**
     * @param clubs   every club of the league, as stored with the outlook run
     * @param medians median final points per table position, from the same run
     * @return empty when there is too little to say anything — no run yet, or no match played
     */
    public static Optional<List<Target>> of(List<Goal> goals, long teamId, List<Club> clubs,
                                            Map<Integer, Integer> medians, Map<String, Double> probabilities) {
        Optional<Club> own = clubs.stream().filter(c -> c.teamId() == teamId).findFirst();
        if (goals.isEmpty() || own.isEmpty() || medians.isEmpty()) {
            return Optional.empty();
        }
        Club me = own.get();
        // a double round robin: every club plays each other one twice
        int played = 2 * (clubs.size() - 1) - me.remaining();
        if (played <= 0) {
            return Optional.empty();
        }
        int worst = 1 + (int) clubs.stream()
                .filter(c -> c.teamId() != teamId && c.points() + 3 * c.remaining() >= me.points()).count();
        int best = 1 + (int) clubs.stream()
                .filter(c -> c.teamId() != teamId && c.points() > me.points() + 3 * me.remaining()).count();
        double current = me.points() / (double) played;

        List<Target> targets = new ArrayList<>();
        for (Goal goal : goals) {
            Integer typical = medians.get(goal.lastPlace());
            if (typical == null) {
                continue;
            }
            int missing = Math.max(0, typical - me.points());
            double required = me.remaining() == 0 ? 0 : missing / (double) me.remaining();
            Status status;
            if (worst <= goal.lastPlace()) {
                status = Status.SECURED;
            } else if (best > goal.lastPlace()) {
                status = Status.OUT_OF_REACH;
            } else if (missing > 3 * me.remaining()) {
                status = Status.BEYOND_TYPICAL;
            } else if (missing == 0 || current >= required) {
                status = Status.ON_PACE;
            } else {
                status = Status.OPEN;
            }
            targets.add(new Target(goal.label(), typical, missing, me.remaining(), required, current,
                    percent(goal, probabilities), status));
        }
        return Optional.of(List.copyOf(targets));
    }

    private static int percent(Goal goal, Map<String, Double> probabilities) {
        double sum = goal.parts().stream().mapToDouble(p -> probabilities.getOrDefault(p, 0.0)).sum();
        return (int) Math.round((goal.complement() ? 1 - sum : sum) * 100);
    }
}
