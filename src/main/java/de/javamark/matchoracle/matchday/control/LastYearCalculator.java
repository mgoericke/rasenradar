package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.entity.League;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.ResultStatus;
import de.javamark.matchoracle.matchday.entity.Team;
import de.javamark.matchoracle.matchday.entity.TeamMatchView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Story r081 "Besser als vor einem Jahr?": a club's points now against its points after the same
 * number of games last season — in whichever Bundesliga it played then. Final results only, both seasons.
 */
public final class LastYearCalculator {

    private LastYearCalculator() {
    }

    /** {@code otherLeague}: the Bundesliga of last season when it is not this one, else null. */
    public record Comparison(Team team, int games, int points, int pointsThen, League otherLeague) {
        public int difference() {
            return points - pointsThen;
        }
    }

    /** {@code newcomers}: clubs that played in neither Bundesliga last season. */
    public record LastYear(List<Comparison> clubs, List<Team> newcomers) {
    }

    /**
     * @param league        the league shown
     * @param current       this season's matches of the league
     * @param lastSeasonOf  last season's matches per Bundesliga (both, so promoted and relegated clubs compare too)
     */
    public static LastYear of(League league, List<Match> current, Map<League, List<Match>> lastSeasonOf) {
        Map<Team, List<TeamMatchView>> now = views(current);
        List<Comparison> clubs = new ArrayList<>();
        List<Team> newcomers = new ArrayList<>();
        now.forEach((team, views) -> {
            int games = views.size();
            int points = points(views, games);
            for (var e : lastSeasonOf.entrySet()) {
                List<TeamMatchView> then = views(e.getValue()).get(team);
                if (then != null && !then.isEmpty()) {
                    clubs.add(new Comparison(team, games, points, points(then, games), e.getKey() == league ? null : e.getKey()));
                    return;
                }
            }
            newcomers.add(team);
        });
        clubs.sort(Comparator.comparingInt(Comparison::difference).reversed().thenComparing(c -> c.team().name));
        newcomers.sort(Comparator.comparing(t -> t.name));
        return new LastYear(List.copyOf(clubs), List.copyOf(newcomers));
    }

    private static Map<Team, List<TeamMatchView>> views(List<Match> matches) {
        Map<Team, List<TeamMatchView>> views = new LinkedHashMap<>();
        matches.stream()
                .filter(m -> m.fullTimeScore != null && m.resultStatus == ResultStatus.FINAL && Match.countsTowardsTable(m))
                .sorted(Comparator.comparing((Match m) -> m.kickoff))
                .forEach(m -> {
                    views.computeIfAbsent(m.homeTeam, t -> new ArrayList<>()).add(m.viewedBy(m.homeTeam));
                    views.computeIfAbsent(m.awayTeam, t -> new ArrayList<>()).add(m.viewedBy(m.awayTeam));
                });
        return views;
    }

    /** Points from the first {@code games} matches — "after the same number of games", not the same matchday. */
    private static int points(List<TeamMatchView> views, int games) {
        int p = 0;
        for (TeamMatchView v : views.subList(0, Math.min(games, views.size()))) {
            p += switch (v.result()) { case WIN -> 3; case DRAW -> 1; case LOSS -> 0; };
        }
        return p;
    }
}
