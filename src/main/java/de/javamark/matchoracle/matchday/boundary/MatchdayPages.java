package de.javamark.matchoracle.matchday.boundary;

import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.BalanceRow;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.Countdown;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.DataInfo;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.DayGroup;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.GoalRow;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.HeadToHeadSummary;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.LiveScoresFragment;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.MatchNotFoundPage;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.LandingPage;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.LandingLeague;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.MyClubChoice;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.MyClubLeague;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.MyClubPage;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.NextMatchBar;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.StreamDayView;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.StreamRow;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.SinceRow;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.TopStage;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.StreamSlot;
import de.javamark.matchoracle.matchday.control.MatchdayStream;
import de.javamark.matchoracle.matchday.control.MatchdayStream.StreamDay;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.LeagueScorerRow;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.LeagueScorersPage;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.ClubLook;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.MatchPage;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.NavLeague;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.MatchRow;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.MatchdayPage;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.Nav;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.ResultRow;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.ScheduleRow;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.ScorerRow;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.SeasonChip;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.StandingRow;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.TeamPage;
import jakarta.ws.rs.QueryParam;
import de.javamark.matchoracle.matchday.entity.Tier;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.UpsetRow;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.RoundSection;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.FunnelFigures;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.FunnelRow;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.FunnelShare;
import de.javamark.matchoracle.matchday.control.CupFunnelCalculator;
import de.javamark.matchoracle.matchday.entity.CupFunnel;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.KnockoutPage;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.GroupsPage;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.TeamSituation;
import de.javamark.matchoracle.matchday.control.FormCalculator;
import de.javamark.matchoracle.matchday.control.GoalTimingCalculator;
import de.javamark.matchoracle.matchday.control.HeadToHeadCalculator;
import de.javamark.matchoracle.matchday.control.ScorerCalculator;
import de.javamark.matchoracle.matchday.control.StandingsCalculator;
import de.javamark.matchoracle.matchday.control.MatchCalendar;
import de.javamark.matchoracle.matchday.control.Stakes;
import de.javamark.matchoracle.matchday.control.TopMatch;
import de.javamark.matchoracle.matchday.entity.Form;
import de.javamark.matchoracle.matchday.entity.ResultStatus;
import de.javamark.matchoracle.matchday.entity.Goal;
import de.javamark.matchoracle.matchday.entity.HeadToHead;
import de.javamark.matchoracle.matchday.entity.Balance;
import de.javamark.matchoracle.matchday.entity.CompetitionFormat;
import de.javamark.matchoracle.matchday.entity.TeamTier;
import de.javamark.matchoracle.matchday.entity.League;
import de.javamark.matchoracle.matchday.entity.LeagueSeason;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.Matchday;
import de.javamark.matchoracle.matchday.entity.Standings;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.LiveTable;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.SplitTable;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.LiveRow;
import de.javamark.matchoracle.matchday.entity.StandingPosition;
import de.javamark.matchoracle.matchday.entity.TeamResult;
import de.javamark.matchoracle.matchday.entity.Team;
import de.javamark.matchoracle.matchday.entity.TeamMatchView;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
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
        static native TemplateInstance liveScores(LiveScoresFragment fragment);
        static native TemplateInstance matchday(MatchdayPage page);
        static native TemplateInstance match(MatchPage page);
        static native TemplateInstance matchNotFound(MatchNotFoundPage page);
        static native TemplateInstance team(TeamPage page);
        static native TemplateInstance knockout(KnockoutPage page);
        static native TemplateInstance groups(GroupsPage page);
        static native TemplateInstance leagueScorers(LeagueScorersPage page);
        static native TemplateInstance myClub(MyClubPage page);
        static native TemplateInstance nextMatch(NextMatchBar bar);
        static native TemplateInstance liveTable(LiveTable live);
        static native TemplateInstance topStage(TopStage stage);
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

    @Inject
    CupFunnelCalculator cupFunnelCalculator;

    /** How long after kickoff a match still counts as "läuft" in the UI — never a live score, just the label (Spec 1). */
    @ConfigProperty(name = "matchoracle.matchday.live-window", defaultValue = "PT2H30M")
    Duration liveWindow;

    /** Landing page, spec 01 "Spieltags-Strom": both Bundesligas as one stream by day and kickoff block. */
    @GET
    public TemplateInstance home() {
        Instant now = Instant.now();
        List<LandingLeague> leagues = new ArrayList<>();
        List<Match> shown = new ArrayList<>();
        List<Match> next = new ArrayList<>();
        List<String> finalPlaces = new ArrayList<>();
        for (League l : List.of(League.BUNDESLIGA_1, League.BUNDESLIGA_2)) {
            Matchday.findDisplayed(l, now).ifPresent(matchday -> {
                List<Match> matches = Match.findByMatchday(matchday);
                shown.addAll(matches);
                leagues.add(landingLeague(matchday));
                finalPlaces.add(MatchdayPageModels.finalPlaces(finalTable(matchday.league, matchday.season)));
                if (MatchdayStream.showsNext(matches)) {
                    Matchday.find(matchday.league, matchday.season, matchday.number + 1)
                            .ifPresent(md -> next.addAll(Match.findByMatchday(md)));
                }
            });
        }
        List<StreamDay> days = MatchdayStream.of(shown);
        List<StreamDayView> dayViews = streamViews(days, now);
        List<StreamDayView> nextViews = streamViews(MatchdayStream.of(next), now);
        String range = days.isEmpty() ? null : MatchdayPageModels.dateRange(days.getFirst().date(), days.getLast().date());
        int live = (int) shown.stream().filter(m -> m.isLive(now, liveWindow)).count();
        return Templates.home(new LandingPage(Nav.page("home"), range, leagues, dayViews, live, nextViews,
                MatchdayPageModels.forecastLink(dayViews), MatchdayPageModels.forecastLink(nextViews),
                MatchdayPageModels.LEADERBOARD_LINK, sinceRows(now), String.join(";", finalPlaces),
                topStage(shown, next, now)));
    }

    /** The top match of the start page: the 1. Bundesliga first, the 2. Bundesliga when it has nothing. */
    private TopStage topStage(List<Match> shown, List<Match> next, Instant now) {
        for (League l : List.of(League.BUNDESLIGA_1, League.BUNDESLIGA_2)) {
            List<Match> candidates = java.util.stream.Stream.concat(shown.stream(), next.stream())
                    .filter(m -> m.matchday.league == l).toList();
            if (candidates.isEmpty()) continue;
            Matchday md = candidates.getFirst().matchday;
            Map<Team, Integer> places = placesOf(l, md.season, Integer.MAX_VALUE);
            Optional<Match> top = TopMatch.pick(candidates, places, now, liveWindow);
            if (top.isPresent()) {
                return stageOf(top.get(), "Topspiel", places, now);
            }
        }
        return null;
    }

    /** The stage for one match, with both table places when the competition has a table. */
    private TopStage stageOf(Match m, String label, Map<Team, Integer> places, Instant now) {
        String shortcut = m.matchday.league.sourceShortcut();
        MatchRow row = MatchRow.of(m, shortcut, now, liveWindow);
        Integer home = places.get(m.homeTeam);
        Integer away = places.get(m.awayTeam);
        String reason = home == null || away == null ? "" : "Platz " + home + " gegen Platz " + away;
        String when = m.isPlayed() ? "Endstand · " + MatchdayPageModels.dayLabel(m.kickoff)
                : m.isLive(now, liveWindow) ? "läuft"
                : NextMatchBar.when(m.kickoff, now) + " Uhr";
        return new TopStage(label, MatchdayPageModels.leagueName(m.matchday.league), when, reason, row);
    }

    private Map<Team, Integer> placesOf(League league, int season, int beforeMatchday) {
        if (!league.hasTable()) {
            return Map.of();
        }
        Map<Team, Integer> places = new HashMap<>();
        standingsCalculator.standingsBefore(league, season, beforeMatchday).positions()
                .forEach(p -> places.put(p.team(), p.position()));
        return places;
    }

    /**
     * The start page's stage for "Mein Verein": the club's running or next match in any competition,
     * swapped in by nav.js. Empty when the club has none — the top match then stays.
     */
    @GET
    @Path("/mein-verein/{id}/topspiel")
    public Response clubStage(@PathParam("id") long id, @HeaderParam("HX-Request") String htmxRequest) {
        if (htmxRequest == null) {
            return Response.seeOther(URI.create("/")).build();
        }
        Instant now = Instant.now();
        TopStage stage = Team.<Team>findByIdOptional(id)
                .map(team -> nextMatchOf(team, now))
                .map(m -> stageOf(m, "Mein Verein", placesOf(m.matchday.league, m.matchday.season, Integer.MAX_VALUE), now))
                .orElse(null);
        return Response.ok(stage == null ? "" : Templates.topStage(stage).render())
                .header("Cache-Control", "no-store").build();
    }

    /** Story r044: the played matches the card "Seit dem letzten Besuch" can pick from, newest first. */
    private List<SinceRow> sinceRows(Instant now) {
        return Match.findPlayedSince(List.of(League.BUNDESLIGA_1, League.BUNDESLIGA_2), now.minus(MatchdayPageModels.SINCE_REACH))
                .stream().map(m -> new SinceRow(MatchdayPageModels.badge(m.matchday.league),
                        MatchRow.of(m, m.matchday.league.sourceShortcut(), now, liveWindow),
                        m.kickoff.plus(MatchdayPageModels.MATCH_LENGTH).getEpochSecond()))
                .toList();
    }

    /** The table of final results only — provisional scores do not move a club (story r044). */
    private Standings finalTable(League league, int season) {
        List<Match> settled = Match.findPlayedBefore(league, season, Integer.MAX_VALUE).stream()
                .filter(m -> m.resultStatus == ResultStatus.FINAL).toList();
        return standingsCalculator.standings(league, season, Integer.MAX_VALUE, settled);
    }

    private List<StreamDayView> streamViews(List<StreamDay> days, Instant now) {
        return days.stream().map(d -> new StreamDayView(
                MatchdayPageModels.dayLabel(d.slots().getFirst().kickoff()),
                d.slots().stream().map(slot -> new StreamSlot(MatchdayPageModels.time(slot.kickoff()),
                        String.valueOf(slot.kickoff().getEpochSecond()),
                        slot.matches().stream().map(m -> new StreamRow(m.matchday.league.sourceShortcut(),
                                MatchdayPageModels.badge(m.matchday.league),
                                MatchRow.of(m, m.matchday.league.sourceShortcut(), now, liveWindow))).toList())).toList()))
                .toList();
    }

    private LandingLeague landingLeague(Matchday matchday) {
        Standings standings = standingsCalculator.standingsBefore(matchday.league, matchday.season, matchday.number + 1);
        String shortcut = matchday.league.sourceShortcut();
        return new LandingLeague(shortcut, MatchdayPageModels.badge(matchday.league), MatchdayPageModels.leagueName(matchday.league),
                matchday.number, "/" + shortcut,
                standings.positions().stream().map(p -> StandingRow.of(p, matchday.league)).toList(),
                standings.includesProvisional());
    }

    /**
     * Spec 06: a knockout competition has no "current matchday" worth landing on — the whole
     * edition is the page. An older edition is reached with ?saison=2023.
     */
    @GET
    @Path("/{league}")
    public TemplateInstance currentMatchday(@PathParam("league") String league,
                                            @QueryParam("saison") Integer season) {
        League l = league(league);
        if (l.format() == CompetitionFormat.KNOCKOUT) {
            int edition = season != null ? season
                    : Matchday.latestSeason(l).orElseThrow(() -> new NotFoundException("no season known yet"));
            return Templates.knockout(knockoutPage(l, edition));
        }
        if (l.format() == CompetitionFormat.GROUPS) {
            Matchday.SeasonMatchday current = Matchday.currentNumber(l)
                    .orElseThrow(() -> new NotFoundException("no matchday known yet"));
            return Templates.groups(groupsPage(l, current.season(), current.number()));
        }
        Matchday matchday = Matchday.findDisplayed(l, Instant.now())
                .orElseThrow(() -> new NotFoundException("no matchday known yet"));
        return Templates.matchday(matchdayPage(matchday));
    }

    @GET
    @Path("/{league}/{season}/{number}")
    public TemplateInstance matchday(@PathParam("league") String league, @PathParam("season") int season, @PathParam("number") int number) {
        League l = league(league);
        if (l.format() == CompetitionFormat.GROUPS) {
            return Templates.groups(groupsPage(l, season, number));
        }
        Matchday matchday = Matchday.find(l, season, number)
                .orElseThrow(() -> new NotFoundException("matchday not found"));
        return Templates.matchday(matchdayPage(matchday));
    }

    @GET
    @Path("/{league}/matches/{id}")
    public Response match(@PathParam("league") String league, @PathParam("id") long id) {
        Optional<League> l = League.bySourceShortcut(league);
        Optional<Match> match = l.flatMap(x -> Match.<Match>findByIdOptional(id).filter(m -> m.matchday.league == x));
        if (match.isEmpty()) {
            // story r005: a shared link to a match that isn't there ends on a page, not a blank 404
            League shown = l.orElse(League.BUNDESLIGA_1);
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Templates.matchNotFound(new MatchNotFoundPage(Nav.of(shown), "/" + shown.sourceShortcut())))
                    .build();
        }
        return Response.ok(Templates.match(matchPage(match.get()))).build();
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

    /**
     * Story r027: the club's open matches of the season in this competition as a calendar file.
     * Nothing about the visitor is involved; the file is the same for everyone.
     */
    @GET
    @Path("/{league}/{season}/teams/{id}/spielplan.ics")
    public Response clubCalendar(@PathParam("league") String league, @PathParam("season") int season, @PathParam("id") long id) {
        League l = league(league);
        Team team = Team.<Team>findByIdOptional(id).orElseThrow(() -> new NotFoundException("team not found"));
        Instant now = Instant.now();
        List<Match> open = openMatches(Match.findByTeam(team, l, season), now);
        if (open.isEmpty()) {
            throw new NotFoundException(team.name + " has no open matches in " + league + " " + season);
        }
        String name = team.name + " · " + MatchdayPageModels.leagueName(l) + " " + MatchdayPageModels.seasonLabel(season);
        return calendar(MatchCalendar.of(name, open, this::absoluteLink, now), "spielplan-" + team.id + ".ics");
    }

    /** Story r027: one open match as a calendar file. */
    @GET
    @Path("/{league}/matches/{id}/kalender.ics")
    public Response matchCalendar(@PathParam("league") String league, @PathParam("id") long id) {
        League l = league(league);
        Instant now = Instant.now();
        Match match = Match.<Match>findByIdOptional(id).filter(m -> m.matchday.league == l)
                .filter(m -> !openMatches(List.of(m), now).isEmpty())
                .orElseThrow(() -> new NotFoundException("no open match " + id));
        String title = match.homeTeam.name + " – " + match.awayTeam.name;
        return calendar(MatchCalendar.of(title, List.of(match), this::absoluteLink, now), "spiel-" + match.id + ".ics");
    }

    /** Matches still ahead: not played and not kicked off yet. */
    private static List<Match> openMatches(List<Match> matches, Instant now) {
        return matches.stream().filter(m -> !m.isPlayed() && m.kickoff.isAfter(now)).toList();
    }

    private String absoluteLink(Match m) {
        return SiteGlobals.siteBaseUrl() + "/" + m.matchday.league.sourceShortcut() + "/matches/" + m.id;
    }

    private static Response calendar(String ics, String fileName) {
        return Response.ok(ics, "text/calendar; charset=utf-8")
                .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                .header("Cache-Control", "no-store").build();
    }

    /** Story r003: "Mein Verein" is chosen among the clubs of the two Bundesligas. */
    private static final List<League> MY_CLUB_LEAGUES = List.of(League.BUNDESLIGA_1, League.BUNDESLIGA_2);

    /** Story r003: the clubs to choose "Mein Verein" from; the choice itself stays in the browser. */
    @GET
    @Path("/mein-verein")
    public TemplateInstance myClubChoice() {
        List<MyClubLeague> leagues = MY_CLUB_LEAGUES.stream()
                .flatMap(l -> Matchday.latestSeason(l).map(season -> new MyClubLeague(MatchdayPageModels.leagueName(l),
                        Team.findInLeagueSeason(l, season).stream().map(MyClubChoice::of).toList())).stream())
                .toList();
        return Templates.myClub(new MyClubPage(Nav.page("myClub"), leagues));
    }

    /**
     * Story r003: the chosen club's page in whichever Bundesliga it plays this season, so a stored
     * choice survives promotion and relegation. A club in neither leads back to the choice.
     */
    @GET
    @Path("/mein-verein/{id}")
    public Response myClub(@PathParam("id") long id) {
        String target = Team.<Team>findByIdOptional(id).flatMap(team -> Match.findLeagueSeasonsOf(team).stream()
                        .filter(ls -> MY_CLUB_LEAGUES.contains(ls.league()))
                        .filter(ls -> Matchday.latestSeason(ls.league()).orElse(-1) == ls.season())
                        .findFirst()
                        .map(ls -> MatchdayPageModels.teamLink(team, ls.league().sourceShortcut())))
                .orElse("/mein-verein?nicht-gefunden=" + id);
        return Response.seeOther(URI.create(target)).build();
    }

    /**
     * Story r092: the next-match bar for "Mein Verein", fetched by nav.js every minute (htmx only).
     * Empty when the club has no match left; the club id comes from the visitor's browser.
     */
    @GET
    @Path("/mein-verein/{id}/naechstes-spiel")
    public Response nextMatch(@PathParam("id") long id, @HeaderParam("HX-Request") String htmxRequest) {
        if (htmxRequest == null) {
            return Response.seeOther(URI.create("/mein-verein/" + id)).build();
        }
        Instant now = Instant.now();
        NextMatchBar bar = Team.<Team>findByIdOptional(id)
                .map(team -> {
                    Match next = nextMatchOf(team, now);
                    return next == null ? null : NextMatchBar.of(next, team, now, liveWindow);
                })
                .orElse(null);
        return Response.ok(bar == null ? "" : Templates.nextMatch(bar).render())
                .header("Cache-Control", "no-store").build();
    }

    /** Liga-Torschützenliste: current season, every club. */
    @GET
    @Path("/{league}/torschuetzen")
    public TemplateInstance leagueScorers(@PathParam("league") String league) {
        League l = league(league);
        int season = Matchday.latestSeason(l).orElseThrow(() -> new NotFoundException("no season known yet"));
        var scorers = scorerCalculator.leagueScorersFor(l, season);
        Map<String, ClubLook> clubs = new HashMap<>();
        Match.findPlayedBefore(l, season, Integer.MAX_VALUE).forEach(m -> {
            for (Team t : List.of(m.homeTeam, m.awayTeam)) {
                clubs.putIfAbsent(t.name, new ClubLook(t.shortName == null ? t.name : t.shortName, MatchdayPageModels.crestLink(t)));
            }
        });
        List<LeagueScorerRow> rows = MatchdayPageModels.topScorers(scorers, MatchdayPageModels.TOP_SCORER_PLACES, clubs);
        // story r014: switch between the two Bundesligas; the address carries the league
        List<NavLeague> switcher = l == League.BUNDESLIGA_1 || l == League.BUNDESLIGA_2
                ? Nav.of(l).leagues().stream().filter(n -> n.shortcut().equals("bl1") || n.shortcut().equals("bl2")).toList()
                : List.of();
        return Templates.leagueScorers(new LeagueScorersPage(Nav.of(l), MatchdayPageModels.seasonLabel(season), rows, switcher));
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
        List<Match> playedSoFar = Match.findPlayedBefore(matchday.league, matchday.season, matchday.number + 1);
        Standings standings = standingsCalculator.standings(matchday.league, matchday.season, matchday.number + 1, playedSoFar);
        Map<Team, List<TeamMatchView>> form = formCalculator.finalFormOf(playedSoFar);
        List<StandingRow> rows = standings.positions().stream()
                .map(p -> StandingRow.of(p, matchday.league, form.getOrDefault(p.team(), List.of()))).toList();

        String base = "/" + shortcut + "/" + matchday.season + "/";
        String prev = matchday.number > 1 ? base + (matchday.number - 1) : null;
        String next = Matchday.find(matchday.league, matchday.season, matchday.number + 1)
                .filter(n -> n.label == null) // spec 06: the pager stays inside the league phase
                .map(n -> base + n.number).orElse(null);

        return new MatchdayPage(Nav.of(matchday.league), MatchdayPageModels.seasonLabel(matchday.season), matchday.season, matchday.number,
                prev, next, days, rows, standings.includesProvisional(), DataInfo.of(matchday, Instant.now()),
                roundSections(matchday.league, matchday.season, shortcut, now),
                matches.isEmpty() ? null : MatchdayPageModels.dateRange(
                        matches.getFirst().kickoff.atZone(MatchdayPageModels.DISPLAY_ZONE).toLocalDate(),
                        matches.getLast().kickoff.atZone(MatchdayPageModels.DISPLAY_ZONE).toLocalDate()),
                Matchday.findCurrent(matchday.league).map(c -> c.id.equals(matchday.id)).orElse(false),
                stakes(matchday, matches, now), liveTable(matchday, standings, matches, now),
                splitTables(matchday, playedSoFar));
    }

    /** Story r041: Heim, Auswärts and Letzte 5 beside the full table — final results only, no zones. */
    private List<SplitTable> splitTables(Matchday matchday, List<Match> playedSoFar) {
        if (!matchday.league.hasTable() || playedSoFar.isEmpty()) {
            return List.of();
        }
        List<Match> ordered = playedSoFar.stream().sorted(java.util.Comparator.comparing((Match m) -> m.kickoff)).toList();
        List<SplitTable> tables = new ArrayList<>();
        for (var split : StandingsCalculator.Split.values()) {
            Standings s = standingsCalculator.splitStandings(matchday.league, matchday.season, matchday.number + 1, ordered, split);
            String key = switch (split) { case HOME -> "heim"; case AWAY -> "auswaerts"; case LAST_FIVE -> "letzte-5"; };
            String label = switch (split) { case HOME -> "Heim"; case AWAY -> "Auswärts"; case LAST_FIVE -> "Letzte 5"; };
            String note = switch (split) {
                case HOME -> "Gezählt sind nur die Heimspiele jeder Mannschaft.";
                case AWAY -> "Gezählt sind nur die Auswärtsspiele jeder Mannschaft.";
                case LAST_FIVE -> "Gezählt sind die letzten fünf Spiele jeder Mannschaft, bei weniger Spielen entsprechend weniger.";
            } + " Vorläufige Ergebnisse zählen nicht mit.";
            tables.add(new SplitTable(key, label, note,
                    s.positions().stream().map(p -> StandingRow.of(p, matchday.league)).toList()));
        }
        return List.copyOf(tables);
    }

    /**
     * Story r059: the table "wenn es so bleibt" while a match of this matchday runs, else null.
     * {@code before} is the table of everything played so far, the running matches are not in it.
     */
    private LiveTable liveTable(Matchday matchday, Standings before, List<Match> matches, Instant now) {
        List<Match> running = matches.stream().filter(m -> m.isLive(now, liveWindow)).toList();
        if (running.isEmpty() || !matchday.league.hasTable()) {
            return null;
        }
        String shortcut = matchday.league.sourceShortcut();
        String link = "/" + shortcut + "/" + matchday.season + "/" + matchday.number + "/live-tabelle";
        Standings live = standingsCalculator.liveStandings(before, running);
        List<LiveRow> rows = live.positions().stream().map(p -> {
            Team t = p.team();
            int was = before.of(t).map(StandingPosition::position).orElse(p.position());
            String text = "";
            String css = "";
            for (Match m : running) {
                if (!m.homeTeam.equals(t) && !m.awayTeam.equals(t)) continue;
                boolean home = m.homeTeam.equals(t);
                Team opponent = home ? m.awayTeam : m.homeTeam;
                String name = opponent.shortName == null || opponent.shortName.isBlank() ? opponent.name : opponent.shortName;
                if (m.liveScore == null) {
                    text = "läuft, Stand unbekannt · " + (home ? "gegen " : "bei ") + name;
                    css = "unknown";
                } else {
                    int own = home ? m.liveScore.home : m.liveScore.away;
                    int other = home ? m.liveScore.away : m.liveScore.home;
                    text = own + ":" + other + (home ? " gegen " : " bei ") + name;
                    css = TeamResult.of(own, other).toString().toLowerCase();
                }
            }
            int diff = p.balance().goalDifference();
            return new LiveRow(p.position(), was - p.position(), t.id, t.name,
                    t.shortName == null || t.shortName.isBlank() ? t.name : t.shortName,
                    MatchdayPageModels.crestLink(t), MatchdayPageModels.teamLink(t, shortcut),
                    p.balance().played(), (diff > 0 ? "+" : "") + diff, p.balance().points(), text, css);
        }).toList();
        return new LiveTable(link, false, rows);
    }

    /** Story r059: the live table again, polled every minute while it shows; "ended" once nothing runs. */
    @GET
    @Path("/{league}/{season}/{number}/live-tabelle")
    public Response liveTableFragment(@PathParam("league") String league, @PathParam("season") int season,
                                      @PathParam("number") int number, @HeaderParam("HX-Request") String htmxRequest) {
        String page = "/" + league + "/" + season + "/" + number;
        if (htmxRequest == null) {
            return Response.seeOther(URI.create(page)).build();
        }
        Matchday matchday = Matchday.find(league(league), season, number)
                .orElseThrow(() -> new NotFoundException("matchday not found"));
        Standings before = standingsCalculator.standings(matchday.league, matchday.season, matchday.number + 1,
                Match.findPlayedBefore(matchday.league, matchday.season, matchday.number + 1));
        LiveTable live = liveTable(matchday, before, Match.findByMatchday(matchday), Instant.now());
        if (live == null) {
            live = new LiveTable(page + "/live-tabelle", true, List.of());
        }
        return Response.ok(Templates.liveTable(live)).header("Cache-Control", "no-store").build();
    }

    /**
     * Story r070: "Es geht um" for the open matches of the current and coming matchdays, from the
     * table of final results only — a provisional or running score elsewhere does not change a line.
     */
    private Map<Long, String> stakes(Matchday matchday, List<Match> matches, Instant now) {
        boolean ahead = Matchday.findCurrent(matchday.league).map(c -> matchday.number >= c.number).orElse(false);
        if (!ahead || !matchday.league.hasTable()) {
            return Map.of();
        }
        List<Match> settled = Match.findPlayedBefore(matchday.league, matchday.season, Integer.MAX_VALUE).stream()
                .filter(m -> m.resultStatus == ResultStatus.FINAL).toList();
        Standings table = standingsCalculator.standings(matchday.league, matchday.season, matchday.number, settled);
        Map<Long, String> lines = new HashMap<>();
        for (Match m : matches) {
            if (m.isPlayed() || MatchdayPageModels.isLive(m.kickoff, false, now, liveWindow)) continue;
            Stakes.of(m.homeTeam, m.awayTeam, table).ifPresent(line -> lines.put(m.id, line));
        }
        return lines;
    }

    /**
     * Polled every minute by a page that shows a running match (loaded by htmx, swaps out of band):
     * fresh score cells for the requested matches, on the match page also half time and goals.
     * Opened directly, the bare cells would arrive without layout — anything but htmx goes to "/".
     */
    @GET
    @Path("/live/spielstaende")
    public Response liveScores(@QueryParam("match") List<Long> matchIds, @QueryParam("details") boolean details,
                               @HeaderParam("HX-Request") String htmxRequest) {
        if (htmxRequest == null) {
            return Response.seeOther(URI.create("/")).build();
        }
        Instant now = Instant.now();
        List<Match> matches = matchIds.stream().limit(MAX_LIVE_SCORES).distinct()
                .map(id -> Match.<Match>findByIdOptional(id)).flatMap(Optional::stream).toList();
        List<MatchRow> rows = matches.stream()
                .map(m -> MatchRow.of(m, m.matchday.league.sourceShortcut(), now, liveWindow)).toList();
        MatchRow detail = details && rows.size() == 1 ? rows.getFirst() : null;
        List<GoalRow> goals = detail == null ? List.of() : goalRows(matches.getFirst());
        boolean keepPolling = rows.stream().anyMatch(MatchRow::watched);
        Countdown countdown = detail == null ? null
                : Countdown.of(matches.getFirst().kickoff, detail.played(), now);
        return Response.ok(Templates.liveScores(new LiveScoresFragment(rows, detail, goals, keepPolling, countdown))).build();
    }

    /** More than any page shows at once (two Bundesligas, one matchday each); caps what a crafted URL can ask for. */
    private static final int MAX_LIVE_SCORES = 40;

    private static List<GoalRow> goalRows(Match match) {
        List<GoalRow> goals = new ArrayList<>();
        int prevHome = 0;
        for (Goal g : match.goals) {
            goals.add(GoalRow.of(g, g.scoreAfter.home > prevHome));
            prevHome = g.scoreAfter.home;
        }
        return goals;
    }

    private MatchPage matchPage(Match match) {
        Matchday matchday = match.matchday;
        String shortcut = matchday.league.sourceShortcut();
        Standings standings = standingsCalculator.standingsBefore(matchday);
        HeadToHead h2h = headToHeadCalculator.headToHeadBefore(match);

        List<GoalRow> goals = goalRows(match);

        return new MatchPage(Nav.of(matchday.league), MatchdayPageModels.seasonLabel(matchday.season), matchday.number,
                "/" + shortcut + "/" + matchday.season + "/" + matchday.number,
                "/" + shortcut + "/matches/" + match.id + "/forecast",
                MatchdayPageModels.dayLabel(match.kickoff), MatchRow.of(match, shortcut, Instant.now(), liveWindow), goals,
                situation(match.homeTeam, matchday, standings, shortcut),
                situation(match.awayTeam, matchday, standings, shortcut),
                HeadToHeadSummary.of(h2h.team(), h2h.opponent(), h2h.matches(), Matchday.earliestSeason().orElse(null), shortcut),
                positionChart(matchday, match.homeTeam, match.awayTeam),
                standings.positions().stream().map(p -> StandingRow.of(p, matchday.league)).toList(), standings.includesProvisional(),
                DataInfo.of(matchday, Instant.now()),
                Countdown.of(match.kickoff, match.isPlayed(), Instant.now()));
    }

    /** The club in one season: standing, position curve, schedule, home/away balance, scorers and goal timing. */
    /**
     * Spec 06: the knockout rounds of a season as a band — the Champions League after its
     * league phase, the Nations League after its groups. Open is the round being played.
     */
    private List<RoundSection> roundSections(League league, int season, String shortcut, Instant now) {
        List<Matchday> rounds = Matchday.findRounds(league, season);
        if (rounds.isEmpty()) {
            return List.of();
        }
        int open = rounds.stream()
                .filter(r -> Match.findByMatchday(r).stream().anyMatch(m -> !m.isPlayed()))
                .mapToInt(r -> r.number).min()
                .orElse(rounds.get(rounds.size() - 1).number);
        List<RoundSection> sections = new ArrayList<>();
        for (Matchday round : rounds) {
            List<Match> matches = Match.findByMatchday(round);
            matches.sort(Comparator.comparing((Match m) -> m.kickoff));
            sections.add(new RoundSection(round.displayName(), matches.size(), dateRange(matches),
                    matches.stream().map(m -> MatchdayPageModels.MatchRow.of(m, shortcut, now, liveWindow)).toList(),
                    round.number == open));
        }
        return sections;
    }

    /**
     * Spec 06: all groups of a group competition, each with its own table and its matches of
     * the shown matchday. One competition page, not one entry per group.
     */
    private GroupsPage groupsPage(League league, int season, int number) {
        String shortcut = league.sourceShortcut();
        // the groups' matchday of that number, plus the edition's final round — the latter is a
        // phase of its own and does not share the groups' counting, so it always sits at the end
        List<Matchday> groups = Matchday.findAll(league, season, number).stream()
                .filter(m -> m.groupName != null).toList();
        List<Matchday> finalRound = Matchday.findRounds(league, season);
        if (groups.isEmpty() && finalRound.isEmpty()) {
            throw new NotFoundException("matchday not found");
        }
        Instant now = Instant.now();
        boolean provisional = false;
        List<MatchdayPageModels.GroupSection> sections = new ArrayList<>();
        List<RoundSection> rounds = new ArrayList<>();
        for (Matchday group : groups) {
            List<Match> matches = Match.findByMatchday(group);
            matches.sort(Comparator.comparing((Match m) -> m.kickoff));
            // table after this matchday: everything played in this group up to and including it
            Standings standings = standingsCalculator.standingsBefore(withNumber(group, group.number + 1));
            provisional = provisional || standings.includesProvisional();
            sections.add(new MatchdayPageModels.GroupSection(group.groupName,
                    standings.positions().stream().map(p -> StandingRow.of(p, league)).toList(),
                    byDay(matches, m -> MatchdayPageModels.MatchRow.of(m, shortcut, now, liveWindow))));
        }
        // spec 06: sections of the final round are rounds, not groups — no table
        rounds.addAll(roundSections(league, season, shortcut, now));
        String base = "/" + shortcut + "/" + season + "/";
        String prev = number > 1 ? base + (number - 1) : null;
        boolean hasNext = Matchday.findAll(league, season, number + 1).stream().anyMatch(m -> m.groupName != null);
        String next = hasNext ? base + (number + 1) : null;
        Matchday reference = groups.isEmpty() ? finalRound.get(0) : groups.get(0);
        return new GroupsPage(Nav.of(league), MatchdayPageModels.seasonLabel(season), season, number,
                prev, next, sections, rounds, provisional, DataInfo.of(reference, now));
    }

    /** Matches in kickoff order, grouped under their day label ("Donnerstag, 24. September"). */
    private static List<DayGroup> byDay(List<Match> matches, Function<Match, MatchRow> row) {
        Map<String, List<MatchRow>> byDay = new LinkedHashMap<>();
        for (Match m : matches) {
            byDay.computeIfAbsent(MatchdayPageModels.dayLabel(m.kickoff), k -> new ArrayList<>()).add(row.apply(m));
        }
        return byDay.entrySet().stream().map(e -> new DayGroup(e.getKey(), e.getValue())).toList();
    }

    /** A detached stand-in used only to ask for the table *after* a matchday of the same group. */
    private static Matchday withNumber(Matchday group, int number) {
        Matchday reference = new Matchday();
        reference.league = group.league;
        reference.season = group.season;
        reference.groupName = group.groupName;
        reference.number = number;
        return reference;
    }

    /**
     * Spec 06: the round band — every round of one edition, the final on top, each with its
     * pairings; below it the surprises of that edition. No bracket: the competition is drawn
     * anew after every round, so there are no paths to draw.
     */
    private KnockoutPage knockoutPage(League league, int season) {
        String shortcut = league.sourceShortcut();
        List<Matchday> rounds = Matchday.<Matchday>list("league = ?1 and season = ?2", league, season);
        if (rounds.isEmpty()) {
            throw new NotFoundException("no matchdays known for " + shortcut + " " + season);
        }
        rounds.sort(Comparator.comparingInt((Matchday m) -> m.number).reversed());
        Instant now = Instant.now();
        // the round the viewer should land on — same rule the rest of the app follows for the
        // current matchday: the first one still to be played, the last one once the edition is
        // over. A finished edition opens on the final instead of 32 first-round pairings.
        int openRound = rounds.stream()
                .filter(r -> Match.findByMatchday(r).stream().anyMatch(m -> !m.isPlayed()))
                .mapToInt(r -> r.number).min()
                .orElse(rounds.get(0).number);

        List<RoundSection> sections = new ArrayList<>();
        List<UpsetRow> upsets = new ArrayList<>();
        for (Matchday round : rounds) {
            List<Match> matches = Match.findByMatchday(round);
            matches.sort(Comparator.comparing((Match m) -> m.kickoff));
            sections.add(new RoundSection(round.displayName(), matches.size(), dateRange(matches),
                    matches.stream().map(m -> MatchdayPageModels.MatchRow.of(m, shortcut, now, liveWindow,
                            TeamTier.of(m.homeTeam, season).label(), TeamTier.of(m.awayTeam, season).label())).toList(),
                    round.number == openRound));
            matches.forEach(m -> upsetOf(m, round, season, shortcut).ifPresent(upsets::add));
        }
        upsets.sort(Comparator.comparingInt(UpsetRow::gap).reversed());

        List<SeasonChip> otherEditions = Matchday.<Matchday>list("league = ?1", league).stream()
                .map(m -> m.season).distinct().filter(y -> y != season).sorted(Comparator.reverseOrder())
                .map(y -> new SeasonChip(MatchdayPageModels.seasonLabel(y), MatchdayPageModels.leagueName(league), null,
                        "/" + shortcut + "?saison=" + y))
                .toList();

        CupFunnel funnel = cupFunnelCalculator.funnelFor(league, season);
        return new KnockoutPage(Nav.of(league), MatchdayPageModels.seasonLabel(season), season, otherEditions,
                sections, upsets, DataInfo.of(rounds.get(0), now), funnelRows(funnel), figuresOf(funnel));
    }

    /**
     * Spec 06: the funnel as bars — each round against the widest one, and inside a round
     * each division against that round's field. The widths carry the meaning, the divisions
     * are told apart by ink alone (styleguide: green and red are results, lilac is the AI).
     */
    private static List<FunnelRow> funnelRows(CupFunnel funnel) {
        int widest = funnel.widest();
        List<FunnelRow> rows = new ArrayList<>();
        for (CupFunnel.Round round : funnel.rounds()) {
            List<FunnelShare> shares = new ArrayList<>();
            for (Tier tier : Tier.values()) {
                int clubs = round.byTier().getOrDefault(tier, 0);
                if (clubs == 0) {
                    continue;
                }
                shares.add(new FunnelShare(tier.label(), "tier-" + tier.name().toLowerCase(java.util.Locale.ROOT),
                        clubs, round.clubs() == 0 ? 0 : Math.round(100f * clubs / round.clubs())));
            }
            rows.add(new FunnelRow(round.name(), round.clubs(), round.percentOf(widest), List.copyOf(shares)));
        }
        return List.copyOf(rows);
    }

    private static FunnelFigures figuresOf(CupFunnel funnel) {
        CupFunnel.Figures f = funnel.figures();
        return new FunnelFigures(f.matches(), f.goals(), f.shootouts(), f.extraTime(), f.upsets());
    }

    /** Spec 06: the lower-division side winning is the surprise. */
    private static Optional<UpsetRow> upsetOf(Match m, Matchday round, int season, String shortcut) {
        Optional<Team> winner = m.winner();
        if (winner.isEmpty()) {
            return Optional.empty();
        }
        Team won = winner.get();
        Team lost = won.equals(m.homeTeam) ? m.awayTeam : m.homeTeam;
        Tier winnerTier = TeamTier.of(won, season);
        Tier loserTier = TeamTier.of(lost, season);
        if (!TeamTier.isUpset(winnerTier, loserTier)) {
            return Optional.empty();
        }
        return Optional.of(new UpsetRow(round.displayName(), won.name, lost.name,
                winnerTier.label(), loserTier.label(),
                m.fullTimeScore.home + ":" + m.fullTimeScore.away,
                MatchdayPageModels.decisionLabel(m.decision, m.penaltyScore),
                "/" + shortcut + "/matches/" + m.id, TeamTier.gap(winnerTier, loserTier)));
    }

    private static String dateRange(List<Match> matches) {
        if (matches.isEmpty()) {
            return "";
        }
        String first = MatchdayPageModels.dayLabel(matches.get(0).kickoff);
        String last = MatchdayPageModels.dayLabel(matches.get(matches.size() - 1).kickoff);
        return first.equals(last) ? first : first + " – " + last;
    }

    private TeamPage teamPage(Team team, League league, int season) {
        String shortcut = league.sourceShortcut();
        List<Match> schedule = Match.findByTeam(team, league, season);
        if (schedule.isEmpty()) {
            throw new NotFoundException(team.name + " has no matches in " + shortcut + " " + season);
        }
        List<Match> played = schedule.stream().filter(Match::isPlayed).toList();
        int lastPlayedMatchday = played.stream().mapToInt(m -> m.matchday.number).max().orElse(0);
        // spec 06: no table in a knockout competition — no position, no zone, no position curve
        boolean hasTable = league.hasTable();
        Standings standings = hasTable
                ? standingsCalculator.standingsBefore(league, season, lastPlayedMatchday + 1)
                : new Standings(league, season, lastPlayedMatchday + 1, List.of(), false);
        var standing = standings.of(team);
        boolean currentSeason = Matchday.latestSeason(league).map(s -> s == season).orElse(false);

        List<SeasonChip> otherSeasons = new ArrayList<>();
        boolean hasPreviousSeasonInLeague = false;
        for (LeagueSeason ls : Match.findLeagueSeasonsOf(team)) {
            if (ls.league() == league && ls.season() == season) continue;
            if (ls.league() == league && ls.season() == season - 1) hasPreviousSeasonInLeague = true;
            int last = Match.findPlayedByTeamBefore(team, ls.league(), ls.season(), Integer.MAX_VALUE).stream()
                    .mapToInt(m -> m.matchday.number).max().orElse(0);
            Integer position = ls.league().hasTable()
                    ? standingsCalculator.standingsBefore(ls.league(), ls.season(), last + 1).of(team).map(p -> p.position()).orElse(null)
                    : null;
            otherSeasons.add(new SeasonChip(MatchdayPageModels.seasonLabel(ls.season()), MatchdayPageModels.leagueName(ls.league()), position,
                    "/" + ls.league().sourceShortcut() + "/" + ls.season() + "/teams/" + team.id));
        }
        String positionChartJson = !hasTable ? null
                : hasPreviousSeasonInLeague
                ? seasonComparisonChart(league, team, season, lastPlayedMatchday)
                : positionChart(league, season, lastPlayedMatchday, List.of(team));

        Matchday reference = (played.isEmpty() ? schedule.get(0) : played.get(played.size() - 1)).matchday;
        return new TeamPage(Nav.of(league), MatchdayPageModels.seasonLabel(season), season, currentSeason, team.name, MatchdayPageModels.crestLink(team),
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
                hasTable ? MatchdayPageModels.outlookLink(shortcut, season, team.id) : null,
                DataInfo.of(reference, Instant.now()),
                hasTable, cupRunOutcome(team, league, played), tierLabel(team, league, season),
                MY_CLUB_LEAGUES.contains(league) ? MyClubChoice.of(team) : null,
                currentSeason && nextMatchOf(team, Instant.now()) == null,
                clubForm(team, shortcut, played),
                currentSeason && !openMatches(schedule, Instant.now()).isEmpty()
                        ? "/" + shortcut + "/" + season + "/teams/" + team.id + "/spielplan.ics" : null);
    }

    /** Story r024: the club's last final results, oldest first — provisional ones do not count as form. */
    private static List<ResultRow> clubForm(Team team, String shortcut, List<Match> played) {
        List<Match> settled = played.stream().filter(m -> m.resultStatus == ResultStatus.FINAL).toList();
        return settled.subList(Math.max(0, settled.size() - MatchdayPageModels.CLUB_FORM_LENGTH), settled.size()).stream()
                .map(m -> ResultRow.of(m.viewedBy(team), shortcut)).toList();
    }

    /** Story r092: the match the next-match bar shows for a club, in any competition; null when none is left. */
    private Match nextMatchOf(Team team, Instant now) {
        return NextMatchBar.pick(Match.findOfTeamFrom(team, now.minus(liveWindow)), now, liveWindow);
    }

    /** Spec 06: how a club's cup run ended — empty while it is still in, or outside a cup. */
    private static String cupRunOutcome(Team team, League league, List<Match> played) {
        if (league.format() != CompetitionFormat.KNOCKOUT || played.isEmpty()) {
            return "";
        }
        Match last = played.get(played.size() - 1);
        boolean wonIt = last.winner().map(w -> w.equals(team)).orElse(false);
        boolean wasTheFinal = Matchday.find(league, last.matchday.season, last.matchday.number + 1).isEmpty()
                && lastRoundOf(league, last.matchday.season) == last.matchday.number;
        if (wonIt && !wasTheFinal) {
            return ""; // won its last match but the competition goes on — still in it
        }
        return MatchdayPageModels.cupRunOutcome(last.matchday.displayName(), wonIt && wasTheFinal);
    }

    private static int lastRoundOf(League league, int season) {
        return Matchday.<Matchday>list("league = ?1 and season = ?2", league, season).stream()
                .mapToInt(m -> m.number).max().orElse(0);
    }

    /** Spec 06: the division a club played in, shown in a cup only. */
    private static String tierLabel(Team team, League league, int season) {
        return league.format() == CompetitionFormat.KNOCKOUT ? TeamTier.of(team, season).label() : "";
    }

    private TeamSituation situation(Team team, Matchday matchday, Standings standings, String shortcut) {
        Form form = formCalculator.formBefore(team, matchday);
        var standing = standings.of(team);
        return new TeamSituation(team.name, team.shortName, MatchdayPageModels.teamLink(team, shortcut),
                standing.map(s -> s.position()).orElse(null),
                standing.map(s -> s.balance().points()).orElse(0),
                form.results(), form.lastMatches().stream().map(v -> ResultRow.of(v, shortcut)).toList(),
                BalanceRow.of("Heim", form.home()), BalanceRow.of("Auswärts", form.away()), form.includesProvisional());
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
