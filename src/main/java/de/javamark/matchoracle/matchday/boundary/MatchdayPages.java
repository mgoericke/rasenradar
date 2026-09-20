package de.javamark.matchoracle.matchday.boundary;

import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.BalanceRow;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.DataInfo;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.DayGroup;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.GoalRow;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.LandingLeagueSection;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.LandingPage;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.Spotlight;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.LeagueScorerRow;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.LeagueScorersPage;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.MatchPage;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.MatchRow;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.MatchdayPage;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.Nav;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.ResultRow;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.ScheduleRow;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.ScorerRow;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.SeasonChip;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.StandingRow;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.TeamPage;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.TeamSituation;
import de.javamark.matchoracle.matchday.control.FormCalculator;
import de.javamark.matchoracle.matchday.control.GoalTimingCalculator;
import de.javamark.matchoracle.matchday.control.HeadToHeadCalculator;
import de.javamark.matchoracle.matchday.control.ScorerCalculator;
import de.javamark.matchoracle.matchday.control.StandingsCalculator;
import de.javamark.matchoracle.matchday.entity.Form;
import de.javamark.matchoracle.matchday.entity.Goal;
import de.javamark.matchoracle.matchday.entity.HeadToHead;
import de.javamark.matchoracle.matchday.entity.Balance;
import de.javamark.matchoracle.matchday.entity.League;
import de.javamark.matchoracle.matchday.entity.LeagueSeason;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.Matchday;
import de.javamark.matchoracle.matchday.entity.Standings;
import de.javamark.matchoracle.matchday.entity.Team;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** HTML pages for the viewer (spec 01). The JSON API lives in {@link MatchdayResource}. */
@Path("/")
@Produces(MediaType.TEXT_HTML)
@Transactional(Transactional.TxType.SUPPORTS)
public class MatchdayPages {

    @CheckedTemplate
    static class Templates {
        static native TemplateInstance home(LandingPage page);
        static native TemplateInstance matchday(MatchdayPage page);
        static native TemplateInstance match(MatchPage page);
        static native TemplateInstance team(TeamPage page);
        static native TemplateInstance leagueScorers(LeagueScorersPage page);
    }

    @Inject
    FormCalculator formCalculator;

    @Inject
    StandingsCalculator standingsCalculator;

    @Inject
    HeadToHeadCalculator headToHeadCalculator;

    @Inject
    ScorerCalculator scorerCalculator;

    @Inject
    GoalTimingCalculator goalTimingCalculator;

    /** How long after kickoff a match still counts as "läuft" in the UI — never a live score, just the label (Spec 1). */
    @ConfigProperty(name = "matchoracle.matchday.live-window", defaultValue = "PT2H30M")
    Duration liveWindow;

    /** One match together with the league shortcut it needs for its links — the landing page's spotlight candidates. */
    record CandidateMatch(Match match, String shortcut) {
    }

    /** Spec 1: a currently live match takes priority over a merely upcoming one; earliest kickoff breaks ties either way. */
    static Optional<CandidateMatch> pickSpotlight(List<CandidateMatch> candidates, Instant now, Duration liveWindow) {
        Comparator<CandidateMatch> byKickoff = Comparator.comparing(c -> c.match().kickoff);
        return candidates.stream().filter(c -> MatchdayPageModels.isLive(c.match().kickoff, c.match().isPlayed(), now, liveWindow))
                .min(byKickoff)
                .or(() -> candidates.stream().filter(c -> !c.match().isPlayed() && c.match().kickoff.isAfter(now)).min(byKickoff));
    }

    /** Landing page: current matchday of both Bundesligas in short form, so a first visit shows both leagues. */
    @GET
    public TemplateInstance home() {
        Instant now = Instant.now();
        List<LandingLeagueSection> sections = new ArrayList<>();
        List<CandidateMatch> candidates = new ArrayList<>();
        for (League l : List.of(League.BUNDESLIGA_1, League.BUNDESLIGA_2)) {
            Matchday.findDisplayed(l, now).ifPresent(matchday -> {
                String shortcut = matchday.league.sourceShortcut();
                List<Match> matches = Match.findByMatchday(matchday);
                sections.add(landingSection(matchday, matches, shortcut, now));
                matches.forEach(m -> candidates.add(new CandidateMatch(m, shortcut)));
            });
        }
        Spotlight spotlight = pickSpotlight(candidates, now, liveWindow).map(c -> spotlight(c, now)).orElse(null);
        return Templates.home(new LandingPage(Nav.page("home"), spotlight, sections));
    }

    private Spotlight spotlight(CandidateMatch c, Instant now) {
        Match m = c.match();
        String base = "/" + c.shortcut() + "/matches/" + m.id;
        return new Spotlight(base, base + "/forecast", m.homeTeam.name, m.awayTeam.name, m.homeTeam.iconUrl, m.awayTeam.iconUrl,
                MatchdayPageModels.time(m.kickoff), MatchdayPageModels.dayLabel(m.kickoff),
                MatchdayPageModels.isLive(m.kickoff, m.isPlayed(), now, liveWindow));
    }

    private static final int LANDING_TABLE_ROWS = 6;

    private LandingLeagueSection landingSection(Matchday matchday, List<Match> matches, String shortcut, Instant now) {
        matches = new ArrayList<>(matches);
        matches.sort((a, b) -> a.kickoff.compareTo(b.kickoff));

        Map<String, List<MatchRow>> byDay = new LinkedHashMap<>();
        for (Match m : matches) {
            byDay.computeIfAbsent(MatchdayPageModels.dayLabel(m.kickoff), k -> new ArrayList<>()).add(MatchRow.of(m, shortcut, now, liveWindow));
        }
        List<DayGroup> days = byDay.entrySet().stream().map(e -> new DayGroup(e.getKey(), e.getValue())).toList();

        Standings standings = standingsCalculator.standingsBefore(matchday.league, matchday.season, matchday.number + 1);
        List<StandingRow> tableTop = standings.positions().stream()
                .limit(LANDING_TABLE_ROWS).map(p -> StandingRow.of(p, matchday.league)).toList();

        String base = "/" + shortcut + "/" + matchday.season + "/" + matchday.number;
        return new LandingLeagueSection(MatchdayPageModels.badge(matchday.league), MatchdayPageModels.leagueName(matchday.league),
                "/" + shortcut, base + "/forecasts", base + "/forecasts/markers", matchday.number, days,
                tableTop, standings.includesProvisional());
    }

    @GET
    @Path("/{league}")
    public TemplateInstance currentMatchday(@PathParam("league") String league) {
        Matchday matchday = Matchday.findDisplayed(league(league), Instant.now())
                .orElseThrow(() -> new NotFoundException("no matchday known yet"));
        return Templates.matchday(matchdayPage(matchday));
    }

    @GET
    @Path("/{league}/{season}/{number}")
    public TemplateInstance matchday(@PathParam("league") String league, @PathParam("season") int season, @PathParam("number") int number) {
        Matchday matchday = Matchday.find(league(league), season, number)
                .orElseThrow(() -> new NotFoundException("matchday not found"));
        return Templates.matchday(matchdayPage(matchday));
    }

    @GET
    @Path("/{league}/matches/{id}")
    public TemplateInstance match(@PathParam("league") String league, @PathParam("id") long id) {
        Match match = Match.<Match>findByIdOptional(id)
                .filter(m -> m.matchday.league == league(league))
                .orElseThrow(() -> new NotFoundException("match not found"));
        return Templates.match(matchPage(match));
    }

    @GET
    @Path("/{league}/teams/{id}")
    public TemplateInstance team(@PathParam("league") String league, @PathParam("id") long id) {
        League l = league(league);
        int season = Matchday.latestSeason(l).orElseThrow(() -> new NotFoundException("no season known yet"));
        return team(league, season, id);
    }

    @GET
    @Path("/{league}/{season}/teams/{id}")
    public TemplateInstance team(@PathParam("league") String league, @PathParam("season") int season, @PathParam("id") long id) {
        Team team = Team.<Team>findByIdOptional(id).orElseThrow(() -> new NotFoundException("team not found"));
        return Templates.team(teamPage(team, league(league), season));
    }

    /** Liga-Torschützenliste: current season, every club. */
    @GET
    @Path("/{league}/torschuetzen")
    public TemplateInstance leagueScorers(@PathParam("league") String league) {
        League l = league(league);
        int season = Matchday.latestSeason(l).orElseThrow(() -> new NotFoundException("no season known yet"));
        List<LeagueScorerRow> rows = new ArrayList<>();
        int rank = 0;
        for (var s : scorerCalculator.leagueScorersFor(l, season)) {
            rows.add(new LeagueScorerRow(++rank, s.name(), s.team(), s.goals(), s.penalties()));
        }
        return Templates.leagueScorers(new LeagueScorersPage(Nav.of(l), MatchdayPageModels.seasonLabel(season), rows));
    }

    private MatchdayPage matchdayPage(Matchday matchday) {
        String shortcut = matchday.league.sourceShortcut();
        List<Match> matches = Match.findByMatchday(matchday);
        matches.sort((a, b) -> a.kickoff.compareTo(b.kickoff));
        Instant now = Instant.now();

        Map<String, List<MatchRow>> byDay = new LinkedHashMap<>();
        for (Match m : matches) {
            byDay.computeIfAbsent(MatchdayPageModels.dayLabel(m.kickoff), k -> new ArrayList<>()).add(MatchRow.of(m, shortcut, now, liveWindow));
        }
        List<DayGroup> days = byDay.entrySet().stream().map(e -> new DayGroup(e.getKey(), e.getValue())).toList();

        // table after this matchday (everything played up to and including it)
        Standings standings = standingsCalculator.standingsBefore(matchday.league, matchday.season, matchday.number + 1);
        List<StandingRow> rows = standings.positions().stream().map(p -> StandingRow.of(p, matchday.league)).toList();

        String base = "/" + shortcut + "/" + matchday.season + "/";
        String prev = matchday.number > 1 ? base + (matchday.number - 1) : null;
        String next = Matchday.find(matchday.league, matchday.season, matchday.number + 1).map(n -> base + n.number).orElse(null);

        return new MatchdayPage(Nav.of(matchday.league), MatchdayPageModels.seasonLabel(matchday.season), matchday.season, matchday.number,
                prev, next, days, rows, standings.includesProvisional(), DataInfo.of(matchday, Instant.now()));
    }

    private MatchPage matchPage(Match match) {
        Matchday matchday = match.matchday;
        String shortcut = matchday.league.sourceShortcut();
        Standings standings = standingsCalculator.standingsBefore(matchday);
        HeadToHead h2h = headToHeadCalculator.headToHeadBefore(match);

        List<GoalRow> goals = new ArrayList<>();
        int prevHome = 0;
        for (Goal g : match.goals) {
            goals.add(GoalRow.of(g, g.scoreAfter.home > prevHome));
            prevHome = g.scoreAfter.home;
        }

        return new MatchPage(Nav.of(matchday.league), MatchdayPageModels.seasonLabel(matchday.season), matchday.number,
                "/" + shortcut + "/" + matchday.season + "/" + matchday.number,
                "/" + shortcut + "/matches/" + match.id + "/forecast",
                MatchdayPageModels.dayLabel(match.kickoff), MatchRow.of(match, shortcut, Instant.now(), liveWindow), goals,
                situation(match.homeTeam, matchday, standings, shortcut),
                situation(match.awayTeam, matchday, standings, shortcut),
                h2h.matches().stream().map(v -> ResultRow.of(v, shortcut)).toList(),
                BalanceRow.of("Bilanz", h2h.balance()), h2h.includesProvisional(),
                positionChart(matchday, match.homeTeam, match.awayTeam),
                DataInfo.of(matchday, Instant.now()));
    }

    /** The club in one season: standing, position curve, schedule, home/away balance, scorers and goal timing. */
    private TeamPage teamPage(Team team, League league, int season) {
        String shortcut = league.sourceShortcut();
        List<Match> schedule = Match.findByTeam(team, league, season);
        if (schedule.isEmpty()) {
            throw new NotFoundException(team.name + " has no matches in " + shortcut + " " + season);
        }
        List<Match> played = schedule.stream().filter(Match::isPlayed).toList();
        int lastPlayedMatchday = played.stream().mapToInt(m -> m.matchday.number).max().orElse(0);
        Standings standings = standingsCalculator.standingsBefore(league, season, lastPlayedMatchday + 1);
        var standing = standings.of(team);
        boolean currentSeason = Matchday.latestSeason(league).map(s -> s == season).orElse(false);

        List<SeasonChip> otherSeasons = new ArrayList<>();
        boolean hasPreviousSeasonInLeague = false;
        for (LeagueSeason ls : Match.findLeagueSeasonsOf(team)) {
            if (ls.league() == league && ls.season() == season) continue;
            if (ls.league() == league && ls.season() == season - 1) hasPreviousSeasonInLeague = true;
            int last = Match.findPlayedByTeamBefore(team, ls.league(), ls.season(), Integer.MAX_VALUE).stream()
                    .mapToInt(m -> m.matchday.number).max().orElse(0);
            Integer position = standingsCalculator.standingsBefore(ls.league(), ls.season(), last + 1).of(team).map(p -> p.position()).orElse(null);
            otherSeasons.add(new SeasonChip(MatchdayPageModels.seasonLabel(ls.season()), MatchdayPageModels.leagueName(ls.league()), position,
                    "/" + ls.league().sourceShortcut() + "/" + ls.season() + "/teams/" + team.id));
        }
        String positionChartJson = hasPreviousSeasonInLeague
                ? seasonComparisonChart(league, team, season, lastPlayedMatchday)
                : positionChart(league, season, lastPlayedMatchday, List.of(team));

        Matchday reference = (played.isEmpty() ? schedule.get(0) : played.get(played.size() - 1)).matchday;
        return new TeamPage(Nav.of(league), MatchdayPageModels.seasonLabel(season), season, currentSeason, team.name, team.iconUrl,
                standing.map(p -> p.position()).orElse(null),
                standing.map(p -> MatchdayPageModels.zone(league, p.position())).orElse(""),
                BalanceRow.of("Gesamt", standing.map(p -> p.balance()).orElse(Balance.EMPTY)), standings.includesProvisional(),
                otherSeasons,
                schedule.stream().map(m -> ScheduleRow.of(m, team, shortcut)).toList(),
                BalanceRow.of("Heim", Balance.of(team, played.stream().filter(m -> m.homeTeam.equals(team)).toList())),
                BalanceRow.of("Auswärts", Balance.of(team, played.stream().filter(m -> m.awayTeam.equals(team)).toList())),
                scorerCalculator.scorersFor(team, league, season).stream().map(ScorerRow::of).toList(),
                MatchdayPageModels.goalTimingJson(goalTimingCalculator.timingFor(team, league, season)),
                positionChartJson,
                MatchdayPageModels.outlookLink(shortcut, season, team.id),
                DataInfo.of(reference, Instant.now()));
    }

    private TeamSituation situation(Team team, Matchday matchday, Standings standings, String shortcut) {
        Form form = formCalculator.formBefore(team, matchday);
        var standing = standings.of(team);
        return new TeamSituation(team.name, team.shortName, MatchdayPageModels.teamLink(team, shortcut),
                standing.map(s -> s.position()).orElse(null),
                standing.map(s -> s.balance().points()).orElse(0),
                form.results(), chronological(form.lastMatches()).stream().map(v -> ResultRow.of(v, shortcut)).toList(),
                BalanceRow.of("Heim", form.home()), BalanceRow.of("Auswärts", form.away()), form.includesProvisional());
    }

    /** Form is computed newest first; on the page it reads left to right like a timeline. */
    private static <T> List<T> chronological(List<T> newestFirst) {
        List<T> copy = new ArrayList<>(newestFirst);
        Collections.reverse(copy);
        return copy;
    }

    /** Table position of both teams after each matchday before this one, as JSON for the chart. */
    private String positionChart(Matchday matchday, Team home, Team away) {
        return positionChart(matchday.league, matchday.season, matchday.number - 1, List.of(home, away));
    }

    /** Table position of the teams after each of the first {@code matchdays} matchdays of the season. */
    private String positionChart(League league, int season, int matchdays, List<Team> teams) {
        List<String> labels = new ArrayList<>();
        List<List<String>> positions = new ArrayList<>();
        teams.forEach(t -> positions.add(new ArrayList<>()));
        int teamCount = 18;
        for (int n = 1; n <= matchdays; n++) {
            Standings s = standingsCalculator.standingsBefore(league, season, n + 1);
            labels.add("\"" + n + "\"");
            for (int i = 0; i < teams.size(); i++) {
                positions.get(i).add(s.of(teams.get(i)).map(p -> String.valueOf(p.position())).orElse("null"));
            }
            teamCount = Math.max(teamCount, s.positions().size());
        }
        List<String> series = new ArrayList<>();
        for (int i = 0; i < teams.size(); i++) {
            series.add(seriesJson(teams.get(i).shortName, positions.get(i), false));
        }
        return "{\"labels\":[" + String.join(",", labels) + "],\"teams\":[" + String.join(",", series) + "],\"teamCount\":" + teamCount + "}";
    }

    /**
     * Table position of one team across the current season and, aligned by matchday number, the
     * immediately preceding season in the same league — "where did we stand at matchday X last
     * year". Caller has already checked that a previous season in the same league exists.
     */
    private String seasonComparisonChart(League league, Team team, int season, int currentMatchdays) {
        int previousSeason = season - 1;
        int previousMatchdays = Match.findPlayedByTeamBefore(team, league, previousSeason, Integer.MAX_VALUE).stream()
                .mapToInt(m -> m.matchday.number).max().orElse(0);
        int maxMatchdays = Math.max(currentMatchdays, previousMatchdays);

        List<String> labels = new ArrayList<>();
        List<String> current = new ArrayList<>();
        List<String> previous = new ArrayList<>();
        int teamCount = 18;
        for (int n = 1; n <= maxMatchdays; n++) {
            labels.add("\"" + n + "\"");
            if (n <= currentMatchdays) {
                Standings s = standingsCalculator.standingsBefore(league, season, n + 1);
                current.add(s.of(team).map(p -> String.valueOf(p.position())).orElse("null"));
                teamCount = Math.max(teamCount, s.positions().size());
            } else {
                current.add("null");
            }
            if (n <= previousMatchdays) {
                Standings s = standingsCalculator.standingsBefore(league, previousSeason, n + 1);
                previous.add(s.of(team).map(p -> String.valueOf(p.position())).orElse("null"));
                teamCount = Math.max(teamCount, s.positions().size());
            } else {
                previous.add("null");
            }
        }
        List<String> series = List.of(
                seriesJson(MatchdayPageModels.seasonLabel(season), current, false),
                seriesJson(MatchdayPageModels.seasonLabel(previousSeason), previous, true));
        return "{\"labels\":[" + String.join(",", labels) + "],\"teams\":[" + String.join(",", series) + "],\"teamCount\":" + teamCount + "}";
    }

    private static String seriesJson(String name, List<String> positions, boolean dashed) {
        return "{\"name\":\"" + name + "\",\"positions\":[" + String.join(",", positions) + "],\"dashed\":" + dashed + "}";
    }

    private static League league(String shortcut) {
        return Arrays.stream(League.values())
                .filter(l -> l.sourceShortcut().equalsIgnoreCase(shortcut))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("unknown league " + shortcut));
    }
}
