package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.entity.League;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.StandingPosition;
import de.javamark.matchoracle.matchday.entity.Standings;
import de.javamark.matchoracle.matchday.entity.Team;

import java.util.ArrayList;
import java.util.List;

/**
 * Story r058 "Wer kann noch?": for every club its points today, the most it can still reach
 * (three for every match not yet final) and where the same pace would take it over the whole
 * season. Plain arithmetic on final results — a calculation, not a forecast.
 */
public final class ReachCalculator {

    private ReachCalculator() {
    }

    /** {@code pace} is null before the club's first final result. */
    public record ClubReach(Team team, int position, int points, int played, int remaining, int maximum, Integer pace,
                            boolean titleOutOfReach) {
    }

    /** A reference line: a table place, what it stands for and the points on it today. */
    public record Threshold(int position, String label, int points) {
    }

    public record Reach(List<ClubReach> clubs, List<Threshold> thresholds, int remainingMatchdays) {
    }

    /**
     * @param finalTable the table from final results only
     * @param seasonMatches every match of the league season, played or not — each club's schedule
     */
    public static Reach of(Standings finalTable, List<Match> seasonMatches) {
        List<StandingPosition> positions = finalTable.positions();
        int leaderPoints = positions.isEmpty() ? 0 : positions.getFirst().balance().points();
        List<ClubReach> clubs = new ArrayList<>();
        int mostRemaining = 0;
        for (StandingPosition p : positions) {
            Team t = p.team();
            int scheduled = (int) seasonMatches.stream().filter(m -> m.involves(t) && Match.countsTowardsTable(m)).count();
            int played = p.balance().played();
            int remaining = Math.max(0, scheduled - played);
            int points = p.balance().points();
            int maximum = points + 3 * remaining;
            Integer pace = played == 0 ? null : (int) Math.round((double) points / played * scheduled);
            clubs.add(new ClubReach(t, p.position(), points, played, remaining, maximum, pace, p.position() > 1 && maximum < leaderPoints));
            mostRemaining = Math.max(mostRemaining, remaining);
        }
        List<Threshold> thresholds = new ArrayList<>();
        if (!positions.isEmpty()) {
            thresholds.add(new Threshold(1, "Platz 1", leaderPoints));
            for (int[] place : places(finalTable.league())) {
                if (place[0] <= positions.size()) {
                    thresholds.add(new Threshold(place[0], label(finalTable.league(), place[0]), positions.get(place[0] - 1).balance().points()));
                }
            }
        }
        return new Reach(List.copyOf(clubs), List.copyOf(thresholds), mostRemaining);
    }

    /** The places that end a zone of the table (the same zones the table marks, see PlacementGoal). */
    private static List<int[]> places(League league) {
        if (league == League.CHAMPIONS_LEAGUE) {
            return List.of(new int[]{8}, new int[]{24});
        }
        return league == League.BUNDESLIGA_2
                ? List.of(new int[]{2}, new int[]{3}, new int[]{15})
                : List.of(new int[]{4}, new int[]{15});
    }

    private static String label(League league, int place) {
        if (league == League.CHAMPIONS_LEAGUE) {
            return place == 8 ? "Platz 8 · Achtelfinale" : "Platz 24 · Play-offs";
        }
        if (league == League.BUNDESLIGA_2) {
            return switch (place) { case 2 -> "Platz 2 · Aufstieg"; case 3 -> "Platz 3 · Relegation"; default -> "Platz 15 · Klassenerhalt"; };
        }
        return place == 4 ? "Platz 4 · Europapokal" : "Platz 15 · Klassenerhalt";
    }
}
