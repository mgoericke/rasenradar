package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.ResultStatus;
import de.javamark.matchoracle.matchday.entity.StandingPosition;
import de.javamark.matchoracle.matchday.entity.Standings;
import de.javamark.matchoracle.matchday.entity.Team;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Story r045 "Restprogramm": who has the easier run-in. Difficulty is nothing but the opponent's
 * current table position (final results only) — a lower number is a stronger opponent. No forecast.
 */
public final class RunInCalculator {

    /** How many coming matchdays the grid shows. */
    public static final int COLUMNS = 6;

    private RunInCalculator() {
    }

    /** One cell: the opponent and where it stands, or null for a matchday without an open match ({@code state}). */
    public record Fixture(Match match, Team opponent, boolean home, int opponentPosition) {
    }

    public enum State { OPEN, PLAYED, FREE }

    public record Cell(int matchday, State state, Fixture fixture) {
    }

    /**
     * {@code averagePosition} over all remaining matches (null without any), {@code rank} 1 = hardest
     * programme; equal averages share a rank.
     */
    public record ClubRunIn(Team team, int position, List<Cell> cells, Double averagePosition, Integer rank) {
    }

    public record RunIn(List<Integer> matchdays, List<ClubRunIn> clubs) {
    }

    /**
     * @param finalTable the table from final results only
     * @param seasonMatches every league match of the season, played or not
     */
    public static RunIn of(Standings finalTable, List<Match> seasonMatches) {
        Map<Team, Integer> positions = new LinkedHashMap<>();
        for (StandingPosition p : finalTable.positions()) positions.put(p.team(), p.position());
        List<Match> league = seasonMatches.stream().filter(Match::countsTowardsTable).toList();
        int first = league.stream().filter(m -> !isFinal(m)).mapToInt(m -> m.matchday.number).min().orElse(-1);
        if (first < 0) {
            return new RunIn(List.of(), List.of());
        }
        int last = league.stream().mapToInt(m -> m.matchday.number).max().orElse(first);
        List<Integer> columns = new ArrayList<>();
        for (int n = first; n <= last && columns.size() < COLUMNS; n++) columns.add(n);

        List<ClubRunIn> clubs = new ArrayList<>();
        for (Map.Entry<Team, Integer> e : positions.entrySet()) {
            Team team = e.getKey();
            List<Match> own = league.stream().filter(m -> m.involves(team)).toList();
            List<Cell> cells = new ArrayList<>();
            for (int n : columns) {
                Match m = own.stream().filter(x -> x.matchday.number == n).findFirst().orElse(null);
                if (m == null) cells.add(new Cell(n, State.FREE, null));
                else if (isFinal(m)) cells.add(new Cell(n, State.PLAYED, null));
                else cells.add(new Cell(n, State.OPEN, fixture(m, team, positions)));
            }
            double avg = own.stream().filter(m -> !isFinal(m))
                    .mapToInt(m -> positions.getOrDefault(m.homeTeam.equals(team) ? m.awayTeam : m.homeTeam, positions.size()))
                    .average().orElse(Double.NaN);
            clubs.add(new ClubRunIn(team, e.getValue(), List.copyOf(cells), Double.isNaN(avg) ? null : avg, null));
        }
        // rank: 1 = the lowest average opponent position = the hardest run-in
        List<ClubRunIn> byDifficulty = clubs.stream().filter(c -> c.averagePosition() != null)
                .sorted(Comparator.comparingDouble(ClubRunIn::averagePosition)).toList();
        Map<Team, Integer> ranks = new LinkedHashMap<>();
        for (int i = 0; i < byDifficulty.size(); i++) {
            boolean tied = i > 0 && Math.abs(byDifficulty.get(i).averagePosition() - byDifficulty.get(i - 1).averagePosition()) < 1e-9;
            ranks.put(byDifficulty.get(i).team(), tied ? ranks.get(byDifficulty.get(i - 1).team()) : i + 1);
        }
        List<ClubRunIn> ranked = clubs.stream()
                .map(c -> new ClubRunIn(c.team(), c.position(), c.cells(), c.averagePosition(), ranks.get(c.team()))).toList();
        return new RunIn(List.copyOf(columns), ranked);
    }

    private static Fixture fixture(Match m, Team team, Map<Team, Integer> positions) {
        boolean home = m.homeTeam.equals(team);
        Team opponent = home ? m.awayTeam : m.homeTeam;
        return new Fixture(m, opponent, home, positions.getOrDefault(opponent, positions.size()));
    }

    /** Only a final result counts as played; a provisional one is still open for the run-in. */
    private static boolean isFinal(Match m) {
        return m.fullTimeScore != null && m.resultStatus == ResultStatus.FINAL;
    }
}
