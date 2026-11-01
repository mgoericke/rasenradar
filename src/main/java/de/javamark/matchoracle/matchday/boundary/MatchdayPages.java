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
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.MatchdayStep;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.BriefLine;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.CupRound;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.StageTeam;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.StageBrowse;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.StageRoute;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.CupField;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.CupTierGroup;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.CupClub;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.CupExit;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.FormChip;
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
import de.javamark.matchoracle.matchday.entity.Decision;
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
import de.javamark.matchoracle.matchday.control.GroupReachCalculator;
import de.javamark.matchoracle.matchday.control.StandingsCalculator;
import de.javamark.matchoracle.matchday.control.MatchCalendar;
import de.javamark.matchoracle.matchday.control.Stakes;
import de.javamark.matchoracle.matchday.control.StreakCalculator;
import de.javamark.matchoracle.matchday.control.TopMatch;
import de.javamark.matchoracle.matchday.control.TravelCalculator;
import de.javamark.matchoracle.matchday.control.Stadiums;
import de.javamark.matchoracle.matchday.control.WeatherService;
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
import jakarta.ws.rs.DefaultValue;
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
        static native TemplateInstance compare(MatchdayPageModels.ComparePage page);
        static native TemplateInstance weatherLine(WeatherService.Weather weather);
        static native TemplateInstance weatherCells(Map<Long, WeatherService.Weather> cells);
        static native TemplateInstance knockout(KnockoutPage page);
        static native TemplateInstance groups(GroupsPage page);
        static native TemplateInstance leagueScorers(LeagueScorersPage page);
        static native TemplateInstance season(MatchdayPageModels.SeasonPage page);
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
    WeatherService weatherService;

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

    /**
     * Landing page, spec 01 "Spieltags-Strom": both Bundesligas as one stream by day and kickoff block.
     * {@code spiel} puts that match of the page on the stage — the stage's arrows without script.
     * {@code versatz} steps both Bundesligas back or ahead by that many matchdays (design r102,
     * "‹ Aktueller Spieltag ›"); away from the current one the page leaves out what only belongs to
     * now: "Seit dem letzten Besuch" and the next matchday.
     */
    @GET
    public TemplateInstance home(@QueryParam("spiel") Long spiel, @QueryParam("versatz") @DefaultValue("0") int versatz) {
        Instant now = Instant.now();
        int offset = Math.clamp(versatz, -MAX_STEPS, MAX_STEPS);
        List<LandingLeague> leagues = new ArrayList<>();
        List<Match> shown = new ArrayList<>();
        List<Match> next = new ArrayList<>();
        List<String> finalPlaces = new ArrayList<>();
        boolean[] steps = new boolean[2];
        for (League l : List.of(League.BUNDESLIGA_1, League.BUNDESLIGA_2)) {
            Matchday.findDisplayed(l, now)
                    .flatMap(current -> offset == 0 ? Optional.of(current) : Matchday.find(l, current.season, current.number + offset))
                    .ifPresent(matchday -> {
                List<Match> matches = Match.findByMatchday(matchday);
                shown.addAll(matches);
                leagues.add(landingLeague(matchday));
                finalPlaces.add(MatchdayPageModels.finalPlaces(finalTable(matchday.league, matchday.season)));
                if (offset == 0 && MatchdayStream.showsNext(matches)) {
                    Matchday.find(matchday.league, matchday.season, matchday.number + 1)
                            .ifPresent(md -> next.addAll(Match.findByMatchday(md)));
                }
                steps[0] |= Matchday.find(l, matchday.season, matchday.number - 1).isPresent();
                steps[1] |= Matchday.find(l, matchday.season, matchday.number + 1).isPresent();
            });
        }
        MatchdayStep step = new MatchdayStep(offset,
                steps[0] && offset > -MAX_STEPS ? offset - 1 : null, steps[1] && offset < MAX_STEPS ? offset + 1 : null);
        List<Match> cupMatches = cupMatchesAround(shown);
        shown.addAll(cupMatches);
        CupRound cup = cupMatches.isEmpty() ? null : new CupRound("dfb", MatchdayPageModels.leagueName(League.DFB_POKAL),
                cupMatches.getFirst().matchday.displayName(), "/dfb");
        List<StreamDay> days = MatchdayStream.of(shown);
        List<StreamDayView> dayViews = streamViews(days, now);
        List<StreamDayView> nextViews = streamViews(MatchdayStream.of(next), now);
        String range = days.isEmpty() ? null : MatchdayPageModels.dateRange(days.getFirst().date(), days.getLast().date());
        int live = (int) shown.stream().filter(m -> m.isLive(now, liveWindow)).count();
        return Templates.home(new LandingPage(Nav.page("home"), range, leagues, dayViews, live, nextViews,
                MatchdayPageModels.forecastLink(dayViews), MatchdayPageModels.forecastLink(nextViews),
                MatchdayPageModels.LEADERBOARD_LINK, offset == 0 ? sinceRows(now) : List.of(), String.join(";", finalPlaces),
                topStage(shown, next, now, spiel), step, offset == 0 ? brief(shown, now) : List.of(), cup));
    }

    /**
     * The top match of the start page: the 1. Bundesliga first, the 2. Bundesliga when it has nothing.
     * A match of the page the visitor browsed to ({@code spiel}) comes first.
     */
    private TopStage topStage(List<Match> shown, List<Match> next, Instant now, Long spiel) {
        List<Match> page = java.util.stream.Stream.concat(shown.stream(), next.stream()).toList();
        if (spiel != null) {
            Optional<Match> chosen = page.stream().filter(m -> m.id.equals(spiel)).findFirst();
            if (chosen.isPresent()) {
                return stageOf(chosen.get(), "", page, now);
            }
        }
        for (League l : List.of(League.BUNDESLIGA_1, League.BUNDESLIGA_2)) {
            List<Match> candidates = page.stream().filter(m -> m.matchday.league == l).toList();
            if (candidates.isEmpty()) continue;
            Matchday md = candidates.getFirst().matchday;
            Map<Team, Integer> places = placesOf(l, md.season, Integer.MAX_VALUE);
            Optional<Match> top = TopMatch.pick(candidates, places, now, liveWindow);
            if (top.isPresent()) {
                return stageOf(top.get(), "Topspiel", page, now);
            }
        }
        return null;
    }

    /**
     * Design r102, "Vorbericht" over the open matches of the page, from final results only: the top
     * match with what is at stake, for every club with an open match the place a win would bring (the
     * browser shows the line of "Mein Verein" only), and the strongest running streak at stake.
     */
    private List<BriefLine> brief(List<Match> shown, Instant now) {
        // the Bundesligas only: a cup tie has no table and its own streaks
        List<Match> open = shown.stream().filter(m -> m.matchday.league.hasTable() && !m.isPlayed() && !m.isLive(now, liveWindow))
                .sorted(Comparator.comparing((Match m) -> m.kickoff).thenComparing(m -> m.id)).toList();
        if (open.isEmpty()) {
            return List.of();
        }
        Map<League, Standings> tables = new HashMap<>();
        Map<League, StreakCalculator.Streaks> streaks = new HashMap<>();
        for (Match m : open) {
            League l = m.matchday.league;
            if (!l.hasTable() || tables.containsKey(l)) continue;
            List<Match> settled = Match.findPlayedBefore(l, m.matchday.season, Integer.MAX_VALUE).stream()
                    .filter(x -> x.resultStatus == ResultStatus.FINAL).toList();
            tables.put(l, standingsCalculator.standings(l, m.matchday.season, Integer.MAX_VALUE, settled));
            streaks.put(l, StreakCalculator.of(settled));
        }
        List<BriefLine> lines = new ArrayList<>();
        Match top = null;
        for (League l : List.of(League.BUNDESLIGA_1, League.BUNDESLIGA_2)) {
            List<Match> candidates = open.stream().filter(m -> m.matchday.league == l).toList();
            if (candidates.isEmpty()) continue;
            Map<Team, Integer> places = placesOf(l, candidates.getFirst().matchday.season, Integer.MAX_VALUE);
            top = TopMatch.pick(candidates, places, now, liveWindow).orElse(null);
            if (top != null) break;
        }
        if (top == null) {
            top = open.getFirst();
        }
        Standings topTable = tables.get(top.matchday.league);
        String stake = topTable == null ? "" : Stakes.of(top.homeTeam, top.awayTeam, topTable).map(s -> " – " + s).orElse("");
        lines.add(new BriefLine("Spiel des Spieltags:", shortName(top.homeTeam) + " – " + shortName(top.awayTeam)
                + " (" + briefWhen(top) + ")" + stake + ".", null));
        for (Match m : open) {
            Standings table = tables.get(m.matchday.league);
            if (m == top || table == null) continue;
            for (boolean home : new boolean[] {true, false}) {
                Team team = home ? m.homeTeam : m.awayTeam;
                Team opponent = home ? m.awayTeam : m.homeTeam;
                var side = table.of(team);
                var other = table.of(opponent);
                if (side.isEmpty() || other.isEmpty()) continue;
                int place = Stakes.placeAfterWin(side.get(), other.get(), table);
                String chance = place < side.get().position() ? "mit einem Sieg ginge es auf Platz " + place
                        : "zurzeit Platz " + side.get().position();
                lines.add(new BriefLine(shortName(team) + ":", (home ? "gegen " : "bei ") + shortName(opponent)
                        + " (" + briefWhen(m) + ") – " + chance + ".", team.id));
            }
        }
        Map<Team, Match> playing = new HashMap<>();
        open.forEach(m -> { playing.put(m.homeTeam, m); playing.put(m.awayTeam, m); });
        streaks.values().stream()
                .flatMap(s -> java.util.stream.Stream.concat(s.going().stream(), s.stuck().stream()))
                .filter(s -> playing.containsKey(s.team()) && s.length() >= 4)
                .max(Comparator.comparingInt(StreakCalculator.Streak::length))
                .ifPresent(s -> {
                    Match m = playing.get(s.team());
                    Team opponent = m.homeTeam.equals(s.team()) ? m.awayTeam : m.homeTeam;
                    lines.add(new BriefLine("Serie auf dem Spiel:", shortName(s.team()) + ", "
                            + MatchdayPageModels.streakSentence(s.kind(), s.length(), s.wholeSeason()) + ". "
                            + (s.kind().good ? "Hält das" : "Endet das") + " gegen " + shortName(opponent) + "?", null));
                });
        return lines;
    }

    private static String briefWhen(Match m) {
        return MatchdayPageModels.weekday(m.kickoff) + " " + MatchdayPageModels.time(m.kickoff);
    }

    private static String shortName(Team t) {
        return t.shortName == null || t.shortName.isBlank() ? t.name : t.shortName;
    }

    /** How far "‹ Aktueller Spieltag ›" reaches either way — a season has at most 38 matchdays. */
    private static final int MAX_STEPS = 40;

    /** How long before a Bundesliga matchday a cup round still belongs to its week — Tuesday and Wednesday before the weekend. */
    private static final Duration CUP_LEAD = Duration.ofDays(4);

    /**
     * The DFB-Pokal on the start page: the cup's matches that kick off in the days before the shown
     * Bundesliga matchdays or during them — a midweek round stands with the weekend that follows it.
     */
    static List<Match> cupMatchesAround(List<Match> leagueMatches) {
        if (leagueMatches.isEmpty()) {
            return List.of();
        }
        Instant first = leagueMatches.stream().map(m -> m.kickoff).min(Comparator.naturalOrder()).orElseThrow();
        Instant last = leagueMatches.stream().map(m -> m.kickoff).max(Comparator.naturalOrder()).orElseThrow();
        return Match.findKickoffBetween(League.DFB_POKAL, first.minus(CUP_LEAD), last);
    }

    /** The matches of the start page: the displayed matchday of both Bundesligas, and the next one where the stream shows it. */
    static List<Match> startPageMatches(Instant now) {
        List<Match> all = new ArrayList<>();
        for (League l : List.of(League.BUNDESLIGA_1, League.BUNDESLIGA_2)) {
            Matchday.findDisplayed(l, now).ifPresent(matchday -> {
                List<Match> matches = Match.findByMatchday(matchday);
                all.addAll(matches);
                if (MatchdayStream.showsNext(matches)) {
                    Matchday.find(matchday.league, matchday.season, matchday.number + 1)
                            .ifPresent(md -> all.addAll(Match.findByMatchday(md)));
                }
            });
        }
        all.addAll(cupMatchesAround(all));
        return all;
    }

    /**
     * The stage for one match (design r102): both clubs' place, points and form before the match when
     * the competition has a table, the KI line where there is a KI-Vorschau, and the way through
     * {@code page}, the start page's matches.
     */
    private TopStage stageOf(Match m, String label, List<Match> page, Instant now) {
        String shortcut = m.matchday.league.sourceShortcut();
        MatchRow row = MatchRow.of(m, shortcut, now, liveWindow);
        StageTeam home = null;
        StageTeam away = null;
        String reason = "";
        if (m.matchday.league.hasTable()) {
            Standings standings = standingsCalculator.standingsBefore(m.matchday);
            home = stageTeam(m.homeTeam, m.matchday, standings);
            away = stageTeam(m.awayTeam, m.matchday, standings);
            if (home.position() != null && away.position() != null) {
                reason = "Platz " + home.position() + " gegen Platz " + away.position();
            }
        }
        boolean live = m.isLive(now, liveWindow);
        String status = m.isPlayed() ? (row.provisional() ? "Vorläufig" : "Endstand") : live ? "Läuft" : "Anstoß";
        String when = m.isPlayed() || live
                ? MatchdayPageModels.dayLabel(m.kickoff) + ", " + MatchdayPageModels.time(m.kickoff) + " Uhr"
                : NextMatchBar.when(m.kickoff, now) + " Uhr";
        String kiLink = "bl1".equals(shortcut) || "bl2".equals(shortcut)
                ? "/" + shortcut + "/matches/" + m.id + "/forecast/buehne?home=" + urlEncode(m.homeTeam.shortName)
                        + "&away=" + urlEncode(m.awayTeam.shortName)
                : null;
        List<Long> order = page.stream()
                .sorted(Comparator.comparing((Match x) -> x.kickoff).thenComparing(x -> x.matchday.league).thenComparing(x -> x.id))
                .map(x -> x.id).toList();
        return new TopStage(label, MatchdayPageModels.leagueName(m.matchday.league), when, reason, status, row,
                home, away, kiLink, StageBrowse.of(order, m.id), StageRoute.HOME);
    }

    private StageTeam stageTeam(Team team, Matchday matchday, Standings standings) {
        var standing = standings.of(team);
        return new StageTeam(standing.map(s -> s.position()).orElse(null),
                standing.map(s -> s.balance().points()).orElse(0),
                formCalculator.formBefore(team, matchday).lastMatches().stream().map(FormChip::of).toList());
    }

    private static String urlEncode(String s) {
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Design r102: the stage's arrows — another match of the start page on the stage, swapped in by htmx.
     * Without htmx the start page itself shows it.
     */
    @GET
    @Path("/topspiel/{id}")
    public Response browseStage(@PathParam("id") long id, @HeaderParam("HX-Request") String htmxRequest) {
        if (htmxRequest == null) {
            return Response.seeOther(URI.create("/?spiel=" + id)).build();
        }
        Instant now = Instant.now();
        List<Match> page = startPageMatches(now);
        TopStage stage = page.stream().filter(m -> m.id == id).findFirst()
                .map(m -> stageOf(m, "", page, now)).orElse(null);
        return Response.ok(stage == null ? "" : Templates.topStage(stage).render())
                .header("Cache-Control", "no-store").build();
    }

    /**
     * The stage's arrows on a competition page: another pairing of the same cup round, or another
     * match of the same group matchday, swapped in by htmx. Without htmx the page itself shows it.
     */
    @GET
    @Path("/{league}/topspiel/{id}")
    public Response browseCupStage(@PathParam("league") String league, @PathParam("id") long id,
                                   @HeaderParam("HX-Request") String htmxRequest) {
        League l = league(league);
        Match m = Match.<Match>findByIdOptional(id)
                .filter(x -> x.matchday.league == l && l.format() != CompetitionFormat.TABLE)
                .orElseThrow(() -> new NotFoundException("no cup or group match " + id));
        if (l.format() == CompetitionFormat.GROUPS) {
            String page = "/" + l.sourceShortcut() + "/" + m.matchday.season + "/" + m.matchday.number;
            if (htmxRequest == null) {
                return Response.seeOther(URI.create(page + "?spiel=" + id)).build();
            }
            List<Match> day = Matchday.findAll(l, m.matchday.season, m.matchday.number).stream()
                    .filter(md -> md.groupName != null).flatMap(md -> Match.findByMatchday(md).stream()).toList();
            TopStage stage = groupStage(day, id, Instant.now(), page);
            return Response.ok(stage == null ? "" : Templates.topStage(stage).render())
                    .header("Cache-Control", "no-store").build();
        }
        if (htmxRequest == null) {
            return Response.seeOther(URI.create("/" + l.sourceShortcut() + "?saison=" + m.matchday.season + "&spiel=" + id)).build();
        }
        List<Match> round = Match.findByMatchday(m.matchday);
        TopStage stage = cupStage(round, m.matchday.season, id, Instant.now());
        return Response.ok(stage == null ? "" : Templates.topStage(stage).render())
                .header("Cache-Control", "no-store").build();
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
                .map(m -> stageOf(m, "Mein Verein", startPageMatches(now), now))
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
                                MatchRow.of(m, m.matchday.league.sourceShortcut(), now, liveWindow),
                                MatchdayPageModels.LeadLine.of(m))).toList())).toList()))
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
                                            @QueryParam("saison") Integer season,
                                            @QueryParam("spiel") Long spiel) {
        League l = league(league);
        if (l.format() == CompetitionFormat.KNOCKOUT) {
            int edition = season != null ? season
                    : Matchday.latestSeason(l).orElseThrow(() -> new NotFoundException("no season known yet"));
            return Templates.knockout(knockoutPage(l, edition, spiel));
        }
        if (l.format() == CompetitionFormat.GROUPS) {
            Matchday.SeasonMatchday current = Matchday.currentNumber(l)
                    .orElseThrow(() -> new NotFoundException("no matchday known yet"));
            return Templates.groups(groupsPage(l, current.season(), current.number(), spiel));
        }
        Matchday matchday = Matchday.findDisplayed(l, Instant.now())
                .orElseThrow(() -> new NotFoundException("no matchday known yet"));
        return Templates.matchday(matchdayPage(matchday));
    }

    @GET
    @Path("/{league}/{season}/{number}")
    public TemplateInstance matchday(@PathParam("league") String league, @PathParam("season") int season, @PathParam("number") int number,
                                     @QueryParam("spiel") Long spiel) {
        League l = league(league);
        if (l.format() == CompetitionFormat.GROUPS) {
            return Templates.groups(groupsPage(l, season, number, spiel));
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
        List<LeagueScorerRow> rows = MatchdayPageModels.topScorers(scorers, MatchdayPageModels.TOP_SCORER_PLACES,
                clubLooks(ScorerCalculator.seasonMatches(l, season)));
        // story r014: switch between the two Bundesligas; the address carries the league
        List<NavLeague> switcher = l == League.BUNDESLIGA_1 || l == League.BUNDESLIGA_2
                ? Nav.of(l).leagues().stream().filter(n -> n.shortcut().equals("bl1") || n.shortcut().equals("bl2")).toList()
                : List.of();
        return Templates.leagueScorers(new LeagueScorersPage(Nav.of(l), MatchdayPageModels.seasonLabel(season), rows, switcher));
    }

    /**
     * The season page of a Bundesliga: figures of the season so far, final results only. The cup has its
     * own figures ("Pokal in Zahlen"), the Champions League those that fit its league phase; the Nations
     * League has none yet.
     */
    @GET
    @Path("/{league}/saison")
    public TemplateInstance season(@PathParam("league") String league) {
        League l = league(league);
        if (l == League.DFB_POKAL) {
            return cupSeason(l);
        }
        if (l == League.CHAMPIONS_LEAGUE) {
            return championsSeason(l);
        }
        if (l != League.BUNDESLIGA_1 && l != League.BUNDESLIGA_2) {
            throw new NotFoundException("no season page for " + league);
        }
        int season = Matchday.latestSeason(l).orElseThrow(() -> new NotFoundException("no season known yet"));
        List<Match> settled = Match.findPlayedBefore(l, season, Integer.MAX_VALUE).stream()
                .filter(m -> m.resultStatus == ResultStatus.FINAL).toList();
        String shortcut = l.sourceShortcut();
        var streaks = de.javamark.matchoracle.matchday.control.StreakCalculator.of(settled);
        var homeAdvantage = de.javamark.matchoracle.matchday.control.HomeAdvantageCalculator.of(settled);
        List<NavLeague> switcher = Nav.of(l).leagues().stream()
                .filter(n -> n.shortcut().equals("bl1") || n.shortcut().equals("bl2")).toList();
        return Templates.season(new MatchdayPageModels.SeasonPage(Nav.of(l), MatchdayPageModels.seasonLabel(season), switcher,
                settled.size(),
                MatchdayPageModels.HomeAdvantageView.of(homeAdvantage, shortcut),
                streaks.going().stream().map(st -> MatchdayPageModels.StreakRow.of(st, shortcut)).toList(),
                streaks.stuck().stream().map(st -> MatchdayPageModels.StreakRow.of(st, shortcut)).toList(),
                MatchdayPageModels.ScoreGridView.of(de.javamark.matchoracle.matchday.control.ScoreFrequencyCalculator.of(settled), shortcut),
                de.javamark.matchoracle.matchday.control.SpecialGoalsCalculator.of(settled).stream()
                        .map(c -> MatchdayPageModels.SpecialGoalCard.of(c, shortcut)).toList(),
                MatchdayPageModels.ComebackView.of(de.javamark.matchoracle.matchday.control.ComebackCalculator.of(settled), shortcut),
                crossTable(l, season, shortcut, homeAdvantage),
                reach(l, season, settled, shortcut),
                runIn(l, season, settled, shortcut),
                MatchdayPageModels.GoalTimingView.ofLeague(GoalTimingCalculator.league(settled)),
                MatchdayPageModels.LastYearView.of(de.javamark.matchoracle.matchday.control.LastYearCalculator.of(l, settled, Map.of(
                        League.BUNDESLIGA_1, Match.findPlayedBefore(League.BUNDESLIGA_1, season - 1, Integer.MAX_VALUE),
                        League.BUNDESLIGA_2, Match.findPlayedBefore(League.BUNDESLIGA_2, season - 1, Integer.MAX_VALUE))), shortcut),
                null,
                seasonRace(l, season, shortcut, true),
                travel(l, season, shortcut)));
    }

    /** Story r060: kilometres to the away grounds of the season and the pairings of close grounds. */
    private static MatchdayPageModels.TravelView travel(League league, int season, String shortcut) {
        List<Match> all = new ArrayList<>(Match.findPlayedBefore(league, season, Integer.MAX_VALUE));
        all.addAll(Match.findUnplayed(league, season));
        var ground = TravelCalculator.byExternalId();
        return MatchdayPageModels.TravelView.of(TravelCalculator.trips(all, ground), TravelCalculator.derbies(all, ground), shortcut);
    }

    /**
     * Story r019: the table after every matchday so far, from one query. With {@code finalOnly} (the
     * season page, where provisional results do not count) only final results; otherwise provisional
     * ones too (the club comparison), so the last step is drawn dashed while a result is still open.
     */
    private MatchdayPageModels.SeasonRace seasonRace(League league, int season, String shortcut, boolean finalOnly) {
        List<Match> played = Match.findPlayedBefore(league, season, Integer.MAX_VALUE).stream()
                .filter(m -> !finalOnly || m.resultStatus == ResultStatus.FINAL).toList();
        return raceOf(league, season, played, shortcut);
    }

    /** The race over the given played matches — a league, or one group of a group competition. */
    private MatchdayPageModels.SeasonRace raceOf(League league, int season, List<Match> played, String shortcut) {
        int last = played.stream().mapToInt(m -> m.matchday.number).max().orElse(0);
        List<Standings> afterEach = new ArrayList<>();
        for (int n = 1; n <= last; n++) {
            int before = n + 1;
            afterEach.add(standingsCalculator.standings(league, season, before,
                    played.stream().filter(m -> m.matchday.number < before).toList()));
        }
        boolean lastProvisional = played.stream()
                .anyMatch(m -> m.matchday.number == last && m.resultStatus == ResultStatus.PROVISIONAL);
        return MatchdayPageModels.SeasonRace.of(afterEach, lastProvisional, shortcut);
    }

    /**
     * "Pokal in Zahlen": the season page's figures that make sense in a knockout competition — most
     * frequent results, special goals, the biggest comeback, when the goals fall (with extra time), and
     * how often a pairing went to extra time or penalties. Final results only.
     */
    private TemplateInstance championsSeason(League l) {
        int season = Matchday.latestSeason(l).orElseThrow(() -> new NotFoundException("no season known yet"));
        // the whole edition counts for the figures, the league phase alone for the table sections
        List<Match> settled = Match.findPlayedInSeason(l, season).stream()
                .filter(m -> m.resultStatus == ResultStatus.FINAL).toList();
        List<Match> leaguePhase = settled.stream().filter(Match::countsTowardsTable).toList();
        List<Match> knockout = settled.stream().filter(m -> !Match.countsTowardsTable(m)).toList();
        String shortcut = l.sourceShortcut();
        var streaks = de.javamark.matchoracle.matchday.control.StreakCalculator.of(settled);
        var homeAdvantage = de.javamark.matchoracle.matchday.control.HomeAdvantageCalculator.of(settled);
        boolean extraTime = knockout.stream().anyMatch(m -> m.decision != Decision.REGULAR);
        return Templates.season(new MatchdayPageModels.SeasonPage(Nav.of(l), MatchdayPageModels.seasonLabel(season), List.of(),
                settled.size(),
                MatchdayPageModels.HomeAdvantageView.of(homeAdvantage, shortcut),
                streaks.going().stream().map(st -> MatchdayPageModels.StreakRow.of(st, shortcut)).toList(),
                streaks.stuck().stream().map(st -> MatchdayPageModels.StreakRow.of(st, shortcut)).toList(),
                MatchdayPageModels.ScoreGridView.of(de.javamark.matchoracle.matchday.control.ScoreFrequencyCalculator.of(settled), shortcut),
                de.javamark.matchoracle.matchday.control.SpecialGoalsCalculator.of(settled).stream()
                        .map(c -> MatchdayPageModels.SpecialGoalCard.of(c, shortcut)).toList(),
                MatchdayPageModels.ComebackView.of(de.javamark.matchoracle.matchday.control.ComebackCalculator.of(settled), shortcut),
                // 36 clubs with eight matches each: a cross table would be mostly empty; the field turns
                // over every year, so there is no "last year" either
                null,
                reach(l, season, leaguePhase, shortcut),
                runIn(l, season, leaguePhase, shortcut),
                MatchdayPageModels.GoalTimingView.ofLeague(extraTime ? GoalTimingCalculator.cup(settled) : GoalTimingCalculator.league(settled)),
                null,
                knockout.isEmpty() ? null : MatchdayPageModels.CupDecisionView.of(knockout, shortcut),
                seasonRace(l, season, shortcut, true), null));
    }

    private TemplateInstance cupSeason(League l) {
        int season = Matchday.latestSeason(l).orElseThrow(() -> new NotFoundException("no season known yet"));
        List<Match> settled = ScorerCalculator.seasonMatches(l, season).stream()
                .filter(m -> m.resultStatus == ResultStatus.FINAL).toList();
        String shortcut = l.sourceShortcut();
        return Templates.season(new MatchdayPageModels.SeasonPage(Nav.of(l), MatchdayPageModels.seasonLabel(season), List.of(),
                settled.size(), null, List.of(), List.of(),
                MatchdayPageModels.ScoreGridView.of(de.javamark.matchoracle.matchday.control.ScoreFrequencyCalculator.of(settled), shortcut),
                de.javamark.matchoracle.matchday.control.SpecialGoalsCalculator.of(settled).stream()
                        .map(c -> MatchdayPageModels.SpecialGoalCard.of(c, shortcut)).toList(),
                MatchdayPageModels.ComebackView.of(de.javamark.matchoracle.matchday.control.ComebackCalculator.of(settled), shortcut),
                null, null, null,
                MatchdayPageModels.GoalTimingView.ofLeague(GoalTimingCalculator.cup(settled)),
                null, MatchdayPageModels.CupDecisionView.of(settled, shortcut), null, null));
    }

    /** Story r058: points, maximum and pace per club, from final results only. */
    private MatchdayPageModels.ReachView reach(League league, int season, List<Match> settled, String shortcut) {
        List<Match> all = new ArrayList<>(Match.findPlayedBefore(league, season, Integer.MAX_VALUE));
        all.addAll(Match.findUnplayed(league, season));
        Standings finalTable = standingsCalculator.standings(league, season, Integer.MAX_VALUE, settled);
        return MatchdayPageModels.ReachView.of(de.javamark.matchoracle.matchday.control.ReachCalculator.of(finalTable, all), league, shortcut);
    }

    /** Story r045: the next matchdays per club, opponents by their current place (final results only). */
    private MatchdayPageModels.RunInView runIn(League league, int season, List<Match> settled, String shortcut) {
        List<Match> all = new ArrayList<>(Match.findPlayedBefore(league, season, Integer.MAX_VALUE));
        all.addAll(Match.findUnplayed(league, season));
        Standings finalTable = standingsCalculator.standings(league, season, Integer.MAX_VALUE, settled);
        return MatchdayPageModels.RunInView.of(de.javamark.matchoracle.matchday.control.RunInCalculator.of(finalTable, all), shortcut);
    }

    /** Story r072: every pairing of the season, in the order of the table as it stands (provisional results included, as there). */
    private MatchdayPageModels.CrossTable crossTable(League league, int season, String shortcut,
                                                     de.javamark.matchoracle.matchday.control.HomeAdvantageCalculator.HomeAdvantage shares) {
        List<Match> played = Match.findPlayedBefore(league, season, Integer.MAX_VALUE);
        List<Match> all = new ArrayList<>(played);
        all.addAll(Match.findUnplayed(league, season).stream().filter(Match::countsTowardsTable).toList());
        List<Team> order = new ArrayList<>(standingsCalculator.standings(league, season, Integer.MAX_VALUE, played)
                .positions().stream().map(StandingPosition::team).toList());
        Team.findInLeagueSeason(league, season).stream().filter(t -> !order.contains(t)).forEach(order::add);
        return MatchdayPageModels.CrossTable.of(order, all, shortcut, shares);
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

        Map<Long, Integer> tension = tension(matchday, matches, now);
        return new MatchdayPage(Nav.of(matchday.league), MatchdayPageModels.seasonLabel(matchday.season), matchday.season, matchday.number,
                prev, next, days, rows, standings.includesProvisional(), DataInfo.of(matchday, Instant.now()),
                roundSections(matchday.league, matchday.season, shortcut, now),
                matches.isEmpty() ? null : MatchdayPageModels.dateRange(
                        matches.getFirst().kickoff.atZone(MatchdayPageModels.DISPLAY_ZONE).toLocalDate(),
                        matches.getLast().kickoff.atZone(MatchdayPageModels.DISPLAY_ZONE).toLocalDate()),
                Matchday.findCurrent(matchday.league).map(c -> c.id.equals(matchday.id)).orElse(false),
                stakes(matchday, matches, now), liveTable(matchday, standings, matches, now),
                splitTables(matchday, playedSoFar), leadLines(matches), tension, tensionOrder(matches, tension),
                matches.stream().filter(m -> WeatherService.shown(m, now)).map(m -> m.id).collect(java.util.stream.Collectors.toSet()));
    }

    /**
     * Story r073: the Spannung of every open match from the table before the matchday and the form —
     * the same table the "Es geht um" lines use; nothing from a forecast enters.
     */
    private Map<Long, Integer> tension(Matchday matchday, List<Match> matches, Instant now) {
        if (!matchday.league.hasTable()) {
            return Map.of();
        }
        List<Match> open = matches.stream().filter(m -> !m.isPlayed() && !m.isLive(now, liveWindow)).toList();
        if (open.isEmpty()) {
            return Map.of();
        }
        Standings before = standingsCalculator.standingsBefore(matchday);
        int teams = Math.max(before.positions().size(), Team.findInLeagueSeason(matchday.league, matchday.season).size());
        Map<Long, Integer> tension = new HashMap<>();
        for (Match m : open) {
            tension.put(m.id, de.javamark.matchoracle.matchday.control.TensionCalculator.of(
                    side(m.homeTeam, matchday, before, teams), side(m.awayTeam, matchday, before, teams), teams));
        }
        return tension;
    }

    private de.javamark.matchoracle.matchday.control.TensionCalculator.Side side(Team team, Matchday matchday, Standings before, int teams) {
        var standing = before.of(team);
        int formPoints = formCalculator.formBefore(team, matchday).lastMatches().stream()
                .mapToInt(v -> switch (v.result()) { case WIN -> 3; case DRAW -> 1; case LOSS -> 0; }).sum();
        return new de.javamark.matchoracle.matchday.control.TensionCalculator.Side(
                standing.map(StandingPosition::position).orElse(teams), standing.map(s -> s.balance().points()).orElse(0), formPoints);
    }

    /** Story r073: open matches by Spannung (ties by kickoff), then the started and finished ones by kickoff. */
    private static Map<Long, Integer> tensionOrder(List<Match> matches, Map<Long, Integer> tension) {
        if (tension.isEmpty()) {
            return Map.of();
        }
        List<Match> ordered = matches.stream()
                .sorted(Comparator.comparingInt((Match m) -> -tension.getOrDefault(m.id, 0)).thenComparing(m -> m.kickoff))
                .toList();
        Map<Long, Integer> order = new HashMap<>();
        for (int i = 0; i < ordered.size(); i++) order.put(ordered.get(i).id, i + 1);
        return order;
    }

    /** Story r053: a lead line for every finished match of the matchday whose goal list is complete. */
    private static Map<Long, MatchdayPageModels.LeadLine> leadLines(List<Match> matches) {
        Map<Long, MatchdayPageModels.LeadLine> lines = new HashMap<>();
        for (Match m : matches) {
            MatchdayPageModels.LeadLine line = MatchdayPageModels.LeadLine.of(m);
            if (line != null) lines.put(m.id, line);
        }
        return lines;
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
        // story r071: the same season as if every match had ended at half time ("Schluss nach 80" was dropped on 1 Nov 2026)
        List<Match> finals = ordered.stream().filter(m -> m.resultStatus == ResultStatus.FINAL).toList();
        Standings real = standingsCalculator.standings(matchday.league, matchday.season, matchday.number + 1, finals);
        var half = de.javamark.matchoracle.matchday.control.ScenarioScores.atHalfTime(finals);
        tables.add(scenarioTable("halbzeit", "Halbzeit", matchday, real, half,
                "Jedes beendete Spiel zählt mit dem Halbzeitstand. ± zeigt die Plätze gegenüber der Tabelle aus endgültigen Ergebnissen."));
        return List.copyOf(tables);
    }

    private SplitTable scenarioTable(String key, String label, Matchday matchday, Standings real,
                                     de.javamark.matchoracle.matchday.control.ScenarioScores.Rescored rescored, String note) {
        Standings s = standingsCalculator.standings(matchday.league, matchday.season, matchday.number + 1, rescored.matches());
        List<String> moves = s.positions().stream()
                .map(p -> MatchdayPageModels.placesMoved(real.of(p.team()).map(StandingPosition::position).orElse(p.position()), p.position()))
                .toList();
        String left = rescored.excluded() == 0 ? ""
                : " " + rescored.excluded() + (rescored.excluded() == 1 ? " Spiel fehlt" : " Spiele fehlen") + ", weil dafür die Angaben in den Daten fehlen.";
        return new SplitTable(key, label, note + " Vorläufige Ergebnisse zählen nicht mit." + left,
                s.positions().stream().map(p -> StandingRow.of(p, matchday.league)).toList(), moves);
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

    /**
     * Story r052: the last complete section of a competition — every match FINAL — as data for the
     * "Konferenz nachspielen" board: a matchday (all groups of it in the Nations League) or a cup
     * round. 404 while nothing of the season is complete yet.
     */
    @GET
    @Path("/{league}/konferenz")
    @Produces(MediaType.APPLICATION_JSON)
    public MatchdayPageModels.ConferenceReplay conference(@PathParam("league") String league) {
        League l = league(league);
        int season = Matchday.latestSeason(l).orElseThrow(() -> new NotFoundException("no season known yet"));
        List<Match> all = new ArrayList<>(Match.findPlayedInSeason(l, season));
        all.addAll(Match.findUnplayed(l, season));
        List<Match> matches = MatchdayPageModels.ConferenceReplay.lastComplete(all)
                .orElseThrow(() -> new NotFoundException("no complete matchday yet"));
        Matchday section = matches.getFirst().matchday;
        boolean round = section.label != null;
        return MatchdayPageModels.ConferenceReplay.of(section.displayName() + " · " + MatchdayPageModels.leagueName(l),
                round ? "in dieser Runde" : "an diesem Spieltag", matches, l.sourceShortcut());
    }

    /** More than any page shows at once (two Bundesligas, one matchday each); caps what a crafted URL can ask for. */
    private static final int MAX_LIVE_SCORES = 40;

    private static List<GoalRow> goalRows(Match match) {
        List<GoalRow> goals = new ArrayList<>();
        int prevHome = 0;
        for (Goal g : match.matchGoals()) {
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
                Countdown.of(match.kickoff, match.isPlayed(), Instant.now()),
                MatchdayPageModels.travelLine(match, TravelCalculator.km(match, TravelCalculator.byExternalId()).orElse(null),
                        Stadiums.of(match.homeTeam.externalId).map(s -> s.city()).orElse(null)),
                WeatherService.shown(match, Instant.now()) ? "/wetter/spiel/" + match.id : null);
    }

    /** Story r034: the weather line of a match page (htmx only) — empty when there is nothing to say. */
    @GET
    @Path("/wetter/spiel/{id}")
    public Response weatherLine(@PathParam("id") long id, @HeaderParam("HX-Request") String htmxRequest) {
        if (htmxRequest == null) {
            return Response.seeOther(URI.create("/")).build();
        }
        Instant now = Instant.now();
        var weather = Match.<Match>findByIdOptional(id).flatMap(m -> weatherService.at(m, now));
        return Response.ok(Templates.weatherLine(weather.orElse(null))).build();
    }

    /** Story r034: the short weather values of a matchday's fixtures, swapped out of band (htmx only). */
    @GET
    @Path("/wetter/spieltag")
    public Response weatherCells(@QueryParam("match") List<Long> matchIds, @HeaderParam("HX-Request") String htmxRequest) {
        if (htmxRequest == null) {
            return Response.seeOther(URI.create("/")).build();
        }
        Instant now = Instant.now();
        Map<Long, WeatherService.Weather> cells = new java.util.LinkedHashMap<>();
        matchIds.stream().limit(MAX_LIVE_SCORES).distinct().forEach(id -> Match.<Match>findByIdOptional(id)
                .flatMap(m -> weatherService.at(m, now)).ifPresent(w -> cells.put(id, w)));
        return Response.ok(Templates.weatherCells(cells)).build();
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
    private GroupsPage groupsPage(League league, int season, int number, Long spiel) {
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
        List<Match> day = new ArrayList<>();
        for (Matchday group : groups) {
            List<Match> matches = Match.findByMatchday(group);
            matches.sort(Comparator.comparing((Match m) -> m.kickoff));
            day.addAll(matches);
            // table after this matchday: everything played in this group up to and including it
            Standings standings = standingsCalculator.standingsBefore(withNumber(group, group.number + 1));
            provisional = provisional || standings.includesProvisional();
            Map<Team, GroupReachCalculator.PlaceRange> reach = groupReach(group, standings);
            Map<Long, String> possible = new HashMap<>();
            reach.forEach((t, r) -> possible.put(t.id, r.settled() ? r.best() + ". fest" : r.best() + ".–" + r.worst() + "."));
            sections.add(new MatchdayPageModels.GroupSection(group.groupName,
                    standings.positions().stream().map(p -> StandingRow.of(p, league)).toList(),
                    byDay(matches, m -> MatchdayPageModels.MatchRow.of(m, shortcut, now, liveWindow)),
                    possible, groupLead(reach),
                    raceOf(league, season, Match.findPlayedBefore(league, season, group.groupName, Integer.MAX_VALUE), shortcut)));
        }
        // spec 06: sections of the final round are rounds, not groups — no table
        rounds.addAll(roundSections(league, season, shortcut, now));
        String base = "/" + shortcut + "/" + season + "/";
        String prev = number > 1 ? base + (number - 1) : null;
        boolean hasNext = Matchday.findAll(league, season, number + 1).stream().anyMatch(m -> m.groupName != null);
        String next = hasNext ? base + (number + 1) : null;
        Matchday reference = groups.isEmpty() ? finalRound.get(0) : groups.get(0);
        List<LeagueScorerRow> scorers = MatchdayPageModels.topScorers(scorerCalculator.leagueScorersFor(league, season),
                RAIL_SCORER_PLACES, clubLooks(ScorerCalculator.seasonMatches(league, season))).stream().limit(RAIL_SCORER_PLACES).toList();
        List<Match> groupMatches = Match.findPlayedBefore(league, season, Integer.MAX_VALUE);
        var figures = new MatchdayPageModels.GroupFigures(groupMatches.size(),
                groupMatches.stream().mapToInt(MatchdayPages::goalsOf).sum());
        return new GroupsPage(Nav.of(league), MatchdayPageModels.seasonLabel(season), season, number,
                prev, next, sections, rounds, provisional, DataInfo.of(reference, now),
                groupStage(day, spiel, now, base + number), scorers, figures, holder(league, season));
    }

    /**
     * "Was noch geht" for one group after the shown matchday: every match of the group not counted in
     * {@code standings} is still open — later matchdays and the shown one's unplayed matches.
     */
    private static Map<Team, GroupReachCalculator.PlaceRange> groupReach(Matchday group, Standings standings) {
        List<Match> all = Matchday.<Matchday>list("league = ?1 and season = ?2 and groupName = ?3 and label is null",
                group.league, group.season, group.groupName).stream().flatMap(md -> Match.findByMatchday(md).stream()).toList();
        Map<Team, Integer> points = new java.util.LinkedHashMap<>();
        standings.positions().forEach(p -> points.put(p.team(), p.balance().points()));
        all.forEach(m -> {
            points.putIfAbsent(m.homeTeam, 0);
            points.putIfAbsent(m.awayTeam, 0);
        });
        List<Match> open = all.stream()
                .filter(m -> m.matchday.number > group.number || m.fullTimeScore == null)
                .toList();
        return GroupReachCalculator.of(points, open);
    }

    /** The decided facts of a group in a line, or that nothing is decided yet. */
    private static String groupLead(Map<Team, GroupReachCalculator.PlaceRange> reach) {
        if (reach.isEmpty()) {
            return null;
        }
        List<String> through = new ArrayList<>();
        List<String> noQuarter = new ArrayList<>();
        List<String> down = new ArrayList<>();
        reach.forEach((t, r) -> {
            if (r.worst() <= 2) {
                through.add(t.name);
            } else if (r.best() == 4) {
                down.add(t.name);
            } else if (r.best() > 2) {
                noQuarter.add(t.name);
            }
        });
        List<String> facts = new ArrayList<>();
        if (!through.isEmpty()) facts.add("Viertelfinale sicher: " + String.join(", ", through) + ".");
        if (!noQuarter.isEmpty()) facts.add("Viertelfinale nicht mehr erreichbar: " + String.join(", ", noQuarter) + ".");
        if (!down.isEmpty()) facts.add("Steigt in Liga B ab: " + String.join(", ", down) + ".");
        return facts.isEmpty() ? "Noch ist in dieser Gruppe nichts entschieden." : String.join(" ", facts);
    }

    /**
     * The stage of a group matchday: the match of the matchday's two best-placed sides before it —
     * open matches first, the closer points on a tie. A match the visitor browsed to takes its place.
     */
    private TopStage groupStage(List<Match> day, Long spiel, Instant now, String page) {
        if (day.isEmpty()) {
            return null;
        }
        StageRoute route = StageRoute.competition(page, day.get(0).matchday.league.sourceShortcut());
        Optional<Match> browsed = spiel == null ? Optional.empty() : day.stream().filter(m -> m.id.equals(spiel)).findFirst();
        if (browsed.isPresent()) {
            TopStage s = stageOf(browsed.get(), "", day, now);
            return s.on(route, browsed.get().matchday.groupName, s.reason());
        }
        Map<Matchday, Standings> before = new HashMap<>();
        Comparator<Match> order = Comparator.comparingInt((Match m) -> m.isPlayed() ? 1 : 0)
                .thenComparingInt(m -> {
                    Standings s = before.computeIfAbsent(m.matchday, standingsCalculator::standingsBefore);
                    return s.of(m.homeTeam).map(p -> p.position()).orElse(9) + s.of(m.awayTeam).map(p -> p.position()).orElse(9);
                })
                .thenComparingInt(m -> {
                    Standings s = before.get(m.matchday);
                    int home = s.of(m.homeTeam).map(p -> p.balance().points()).orElse(0);
                    int away = s.of(m.awayTeam).map(p -> p.balance().points()).orElse(0);
                    return Math.abs(home - away);
                })
                .thenComparing(m -> m.kickoff);
        Match pick = day.stream().min(order).orElseThrow();
        TopStage s = stageOf(pick, "", day, now);
        return s.on(route, "Spiel des Spieltags",
                pick.matchday.groupName + (s.reason().isEmpty() ? "" : " · " + s.reason()));
    }

    /**
     * The winner of the previous edition and its way through the final round. The last round may hold
     * the match for third place as well: the final is the one between the two semi-final winners.
     */
    private static MatchdayPageModels.Holder holder(League league, int season) {
        Optional<Integer> previous = Matchday.<Matchday>list("league = ?1 and season < ?2 and label is not null", league, season)
                .stream().map(m -> m.season).max(Integer::compare);
        if (previous.isEmpty()) {
            return null;
        }
        List<Matchday> rounds = Matchday.findRounds(league, previous.get());
        List<Match> knockout = rounds.stream().flatMap(r -> Match.findByMatchday(r).stream())
                .sorted(Comparator.comparing((Match m) -> m.kickoff)).toList();
        int last = rounds.stream().mapToInt(r -> r.number).max().orElse(0);
        List<Match> lastRound = knockout.stream().filter(m -> m.matchday.number == last).toList();
        List<Match> earlier = knockout.stream().filter(m -> m.matchday.number < last).toList();
        java.util.function.Predicate<Team> wonLastTie = t -> earlier.stream().filter(m -> m.involves(t))
                .reduce((a, b) -> b).flatMap(Match::winner).map(t::equals).orElse(false);
        Optional<Team> champion = lastRound.stream()
                .filter(m -> wonLastTie.test(m.homeTeam) && wonLastTie.test(m.awayTeam))
                .findFirst().flatMap(Match::winner);
        if (champion.isEmpty()) {
            return null;
        }
        Team t = champion.get();
        String shortcut = league.sourceShortcut();
        List<MatchdayPageModels.HolderStep> path = knockout.stream()
                .filter(m -> m.involves(t) && m.isPlayed())
                .filter(m -> m.matchday.number < last || lastRound.stream().anyMatch(x -> x == m && wonLastTie.test(m.homeTeam) && wonLastTie.test(m.awayTeam)))
                .map(m -> new MatchdayPageModels.HolderStep(m.matchday.number == last ? "Endspiel" : m.matchday.displayName(),
                        m.homeTeam.name + " – " + m.awayTeam.name,
                        m.fullTimeScore.home + ":" + m.fullTimeScore.away,
                        MatchdayPageModels.decisionLabel(m.decision, m.penaltyScore),
                        "/" + shortcut + "/matches/" + m.id))
                .toList();
        return new MatchdayPageModels.Holder(MatchdayPageModels.seasonLabel(previous.get()), t.name, MatchdayPageModels.crestLink(t), path);
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
    private KnockoutPage knockoutPage(League league, int season, Long spiel) {
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
        List<Match> edition = new ArrayList<>();
        List<Match> stageRound = List.of();
        for (Matchday round : rounds) {
            List<Match> matches = Match.findByMatchday(round);
            matches.sort(Comparator.comparing((Match m) -> m.kickoff));
            edition.addAll(matches);
            // the stage shows the open round — or the round of the match the visitor browsed to
            boolean browsed = spiel != null && matches.stream().anyMatch(m -> m.id.equals(spiel));
            if (browsed || (round.number == openRound && stageRound.isEmpty())) {
                stageRound = matches;
            }
            sections.add(new RoundSection(round.displayName(), matches.size(), dateRange(matches),
                    matches.stream().map(m -> MatchdayPageModels.MatchRow.of(m, shortcut, now, liveWindow,
                            TeamTier.of(m.homeTeam, season).label(), TeamTier.of(m.awayTeam, season).label())).toList(),
                    round.number == openRound));
            matches.forEach(m -> upsetOf(m, round, season, shortcut).ifPresent(upsets::add));
        }
        upsets.sort(Comparator.comparingInt(UpsetRow::gap).reversed());

        Map<Long, MatchdayPageModels.LeadLine> lines = new HashMap<>();
        rounds.forEach(r -> lines.putAll(leadLines(Match.findByMatchday(r))));

        List<SeasonChip> otherEditions = Matchday.<Matchday>list("league = ?1", league).stream()
                .map(m -> m.season).distinct().filter(y -> y != season).sorted(Comparator.reverseOrder())
                .map(y -> new SeasonChip(MatchdayPageModels.seasonLabel(y), MatchdayPageModels.leagueName(league), null,
                        "/" + shortcut + "?saison=" + y))
                .toList();

        CupFunnel funnel = cupFunnelCalculator.funnelFor(league, season);
        // ties can make the first five places longer — the rail keeps five rows
        List<LeagueScorerRow> scorers = MatchdayPageModels.topScorers(scorerCalculator.leagueScorersFor(league, season),
                RAIL_SCORER_PLACES, clubLooks(edition)).stream().limit(RAIL_SCORER_PLACES).toList();
        return new KnockoutPage(Nav.of(league), MatchdayPageModels.seasonLabel(season), season, otherEditions,
                sections, upsets, DataInfo.of(rounds.get(0), now), funnelRows(funnel), figuresOf(funnel), lines,
                cupStage(stageRound, season, spiel, now), cupField(rounds, edition, tierOf(season), shortcut), scorers,
                edition.stream().filter(m -> WeatherService.shown(m, now)).map(m -> m.id).collect(java.util.stream.Collectors.toSet()));
    }

    /** The cup page's rail lists five rows of the scorers; the full list is one click away. */
    private static final int RAIL_SCORER_PLACES = 5;

    private static Map<String, ClubLook> clubLooks(List<Match> matches) {
        Map<String, ClubLook> clubs = new HashMap<>();
        matches.forEach(m -> {
            for (Team t : List.of(m.homeTeam, m.awayTeam)) {
                clubs.putIfAbsent(t.name, new ClubLook(t.shortName == null ? t.name : t.shortName, MatchdayPageModels.crestLink(t)));
            }
        });
        return clubs;
    }

    /**
     * The cup stage: one pairing of {@code round} on the pitch over the full width. While the round
     * is still to be played it is "Spiel der Runde", the pairing of the two highest divisions; once
     * it is over the biggest surprise, or the match with the most goals when there was none. A match
     * the visitor browsed to ({@code spiel}) takes its place; the arrows browse the round.
     */
    private TopStage cupStage(List<Match> round, int season, Long spiel, Instant now) {
        if (round.isEmpty()) {
            return null;
        }
        StageRoute route = StageRoute.cup(round.get(0).matchday.league.sourceShortcut());
        Optional<Match> browsed = spiel == null ? Optional.empty()
                : round.stream().filter(m -> m.id.equals(spiel)).findFirst();
        if (browsed.isPresent()) {
            Match m = browsed.get();
            return stageOf(m, "", round, now).on(route, m.matchday.displayName(), cupReason(m, season));
        }
        StagePick pick = stagePick(round, tierOf(season));
        return stageOf(pick.match(), "", round, now).on(route, pick.label(), cupReason(pick.match(), season));
    }

    record StagePick(Match match, String label) {
    }

    /**
     * Which pairing of a round the cup stage shows: while matches are still open the one of the two
     * highest divisions (the earlier kickoff on a tie); once the round is over the biggest surprise
     * (more goals on a tie), else the match with the most goals. A round of one match is named by itself.
     */
    static StagePick stagePick(List<Match> round, java.util.function.Function<Team, Tier> tierOf) {
        List<Match> open = round.stream().filter(m -> !m.isPlayed()).toList();
        if (!open.isEmpty()) {
            Match pick = open.stream().min(Comparator.comparingInt((Match m) ->
                    tierOf.apply(m.homeTeam).level() + tierOf.apply(m.awayTeam).level())
                    .thenComparing(m -> m.kickoff)).orElseThrow();
            return new StagePick(pick, round.size() == 1 ? pick.matchday.displayName() : "Spiel der Runde");
        }
        java.util.function.ToIntFunction<Match> gap = m -> m.winner().map(won -> {
            Team lost = won.equals(m.homeTeam) ? m.awayTeam : m.homeTeam;
            return TeamTier.gap(tierOf.apply(won), tierOf.apply(lost));
        }).orElse(0);
        Optional<Match> surprise = round.stream().filter(m -> gap.applyAsInt(m) > 0)
                .max(Comparator.comparingInt(gap).thenComparingInt(MatchdayPages::goalsOf));
        Match pick = surprise.orElseGet(() -> round.stream().max(Comparator.comparingInt(MatchdayPages::goalsOf)).orElseThrow());
        String label = round.size() == 1 ? pick.matchday.displayName()
                : surprise.isPresent() ? "Überraschung der Runde" : "Torreichstes Spiel der Runde";
        return new StagePick(pick, label);
    }

    /** Why the pairing matters: who beat whom across divisions once it is decided, the divisions before. */
    private static String cupReason(Match m, int season) {
        Tier home = TeamTier.of(m.homeTeam, season);
        Tier away = TeamTier.of(m.awayTeam, season);
        Optional<Team> winner = m.winner();
        if (winner.isPresent()) {
            boolean homeWon = winner.get().equals(m.homeTeam);
            Team won = homeWon ? m.homeTeam : m.awayTeam;
            Team lost = homeWon ? m.awayTeam : m.homeTeam;
            return shortName(won) + " (" + (homeWon ? home : away).label() + ") schlägt "
                    + shortName(lost) + " (" + (homeWon ? away : home).label() + ")";
        }
        return home.label() + " gegen " + away.label();
    }

    private static int goalsOf(Match m) {
        return m.fullTimeScore == null ? 0 : m.fullTimeScore.home + m.fullTimeScore.away;
    }

    /**
     * "Noch im Wettbewerb": every club of the edition that has not lost a decided pairing, by
     * division; the clubs that went out under the round they lost in, the latest round first.
     */
    private static java.util.function.Function<Team, Tier> tierOf(int season) {
        Map<Team, Tier> tiers = new HashMap<>();
        return t -> tiers.computeIfAbsent(t, x -> TeamTier.of(x, season));
    }

    static CupField cupField(List<Matchday> rounds, List<Match> edition, java.util.function.Function<Team, Tier> tierOf,
                             String shortcut) {
        Map<Team, String> outIn = new HashMap<>();
        java.util.LinkedHashSet<Team> clubs = new java.util.LinkedHashSet<>();
        for (Match m : edition) {
            clubs.add(m.homeTeam);
            clubs.add(m.awayTeam);
            m.winner().ifPresent(won -> {
                Team lost = won.equals(m.homeTeam) ? m.awayTeam : m.homeTeam;
                outIn.put(lost, m.matchday.displayName());
            });
        }
        List<CupTierGroup> remaining = new ArrayList<>();
        for (Tier tier : Tier.values()) {
            List<CupClub> inTier = clubs.stream()
                    .filter(t -> !outIn.containsKey(t) && tierOf.apply(t) == tier)
                    .sorted(Comparator.comparing(MatchdayPages::shortName))
                    .map(t -> new CupClub(t.name, MatchdayPageModels.crestLink(t), MatchdayPageModels.teamLink(t, shortcut)))
                    .toList();
            if (!inTier.isEmpty()) {
                remaining.add(new CupTierGroup(tier.label(), "tier-" + tier.name().toLowerCase(java.util.Locale.ROOT), inTier));
            }
        }
        List<CupExit> out = rounds.stream()
                .map(Matchday::displayName).distinct()
                .map(name -> new CupExit(name, outIn.entrySet().stream().filter(e -> e.getValue().equals(name))
                        .map(e -> shortName(e.getKey())).sorted().toList()))
                .filter(e -> !e.clubs().isEmpty())
                .toList();
        // the final is played: one club left, the winner of the edition
        boolean over = !rounds.isEmpty() && edition.stream()
                .filter(m -> m.matchday.number == rounds.get(0).number).allMatch(m -> m.winner().isPresent())
                && remaining.stream().mapToInt(g -> g.clubs().size()).sum() == 1;
        CupClub winner = over ? remaining.get(0).clubs().get(0) : null;
        return new CupField(remaining, out, winner == null ? null : winner.name(), winner == null ? null : winner.icon());
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
        List<de.javamark.matchoracle.matchday.entity.GoalTiming> timing = goalTimingCalculator.timingFor(team, league, season);
        return new TeamPage(Nav.of(league), MatchdayPageModels.seasonLabel(season), season, currentSeason, team.name, MatchdayPageModels.crestLink(team),
                standing.map(p -> p.position()).orElse(null),
                standing.map(p -> MatchdayPageModels.zone(league, p.position())).orElse(""),
                BalanceRow.of("Gesamt", standing.map(p -> p.balance()).orElse(Balance.EMPTY)), standings.includesProvisional(),
                otherSeasons,
                schedule.stream().map(m -> ScheduleRow.of(m, team, shortcut)).toList(),
                BalanceRow.of("Heim", Balance.of(team, played.stream().filter(m -> m.homeTeam.equals(team)).toList())),
                BalanceRow.of("Auswärts", Balance.of(team, played.stream().filter(m -> m.awayTeam.equals(team)).toList())),
                scorerCalculator.scorersFor(team, league, season).stream().map(ScorerRow::of).toList(),
                MatchdayPageModels.goalTimingJson(timing),
                positionChartJson,
                hasTable ? MatchdayPageModels.outlookLink(shortcut, season, team.id) : null,
                DataInfo.of(reference, Instant.now()),
                hasTable, cupRunOutcome(team, league, played), tierLabel(team, league, season),
                MY_CLUB_LEAGUES.contains(league) ? MyClubChoice.of(team) : null,
                currentSeason && nextMatchOf(team, Instant.now()) == null,
                clubForm(team, shortcut, played),
                currentSeason && !openMatches(schedule, Instant.now()).isEmpty()
                        ? "/" + shortcut + "/" + season + "/teams/" + team.id + "/spielplan.ics" : null,
                MatchdayPageModels.GoalTimingView.ofTeam(team.shortName == null || team.shortName.isBlank() ? team.name : team.shortName, timing),
                scorerProfiles(team, schedule, shortcut),
                MatchdayPageModels.clubCalendar(schedule, team, shortcut, java.time.LocalDate.now(MatchdayPageModels.DISPLAY_ZONE)),
                // story r026: the other clubs of this league and season, alphabetically
                hasTable && currentSeason && standing.isPresent()
                        ? new MatchdayPageModels.ComparePicker(team.id, standings.positions().stream().map(p -> p.team())
                                .filter(t -> !t.equals(team)).sorted(java.util.Comparator.comparing(t -> t.name))
                                .map(t -> new MatchdayPageModels.CompareOption(t.id, t.name)).toList())
                        : null);
    }

    /** Story r026: the picker on the team page sends both ids here; the comparison has an address of its own. */
    @GET
    @Path("/{league}/vergleich")
    public Response comparePick(@PathParam("league") String league, @QueryParam("a") long a, @QueryParam("b") long b) {
        return Response.seeOther(URI.create("/" + league(league).sourceShortcut() + "/vergleich/" + a + "/" + b)).build();
    }

    /**
     * Story r026 "Vereinsvergleich": two clubs of a league in the current season. Anything in the
     * address that is not two clubs of this league gets an explanation instead of an error page.
     */
    @GET
    @Path("/{league}/vergleich/{a}/{b}")
    public TemplateInstance compare(@PathParam("league") String league, @PathParam("a") long a, @PathParam("b") long b) {
        League l = league(league);
        if (!l.hasTable()) {
            throw new NotFoundException("no comparison in " + league);
        }
        String shortcut = l.sourceShortcut();
        int season = Matchday.latestSeason(l).orElseThrow(() -> new NotFoundException("no season known yet"));
        List<Match> played = Match.findPlayedBefore(l, season, Integer.MAX_VALUE);
        int last = played.stream().mapToInt(m -> m.matchday.number).max().orElse(0);
        Standings table = standingsCalculator.standings(l, season, last + 1, played);
        Optional<Team> teamA = Team.<Team>findByIdOptional(a);
        Optional<Team> teamB = Team.<Team>findByIdOptional(b);
        var posA = teamA.flatMap(table::of);
        var posB = teamB.flatMap(table::of);
        MatchdayPageModels.CompareClub clubA = teamA.map(t -> compareClub(t, shortcut, posA.map(p -> p.position()).orElse(null))).orElse(null);
        MatchdayPageModels.CompareClub clubB = teamB.map(t -> compareClub(t, shortcut, posB.map(p -> p.position()).orElse(null))).orElse(null);
        if (posA.isEmpty() || posB.isEmpty() || a == b) {
            String error = a == b ? "Ein Verein lässt sich nicht mit sich selbst vergleichen."
                    : "Beide Vereine müssen in dieser Saison in der " + MatchdayPageModels.leagueName(l) + " spielen.";
            // a club of another league: its page wherever it plays this season (the "Mein Verein" route finds it)
            java.util.function.UnaryOperator<MatchdayPageModels.CompareClub> anywhere = c -> c == null ? null
                    : new MatchdayPageModels.CompareClub(c.id(), c.name(), c.shortName(), c.icon(), "/mein-verein/" + c.id(), c.position());
            return Templates.compare(new MatchdayPageModels.ComparePage(Nav.of(l), MatchdayPageModels.seasonLabel(season),
                    anywhere.apply(clubA), anywhere.apply(clubB), List.of(), null, List.of(), false, error));
        }
        List<MatchdayPageModels.DuelRow> duels = Match.findByTeam(teamA.get(), l, season).stream()
                .filter(m -> m.homeTeam.equals(teamB.get()) || m.awayTeam.equals(teamB.get()))
                .map(m -> MatchdayPageModels.DuelRow.of(m, shortcut)).toList();
        return Templates.compare(new MatchdayPageModels.ComparePage(Nav.of(l), MatchdayPageModels.seasonLabel(season), clubA, clubB,
                MatchdayPageModels.CompareRow.of(posA.get().balance(), posB.get().balance()),
                seasonRace(l, season, shortcut, false).only(List.of(a, b)), duels, table.includesProvisional(), null));
    }

    private static MatchdayPageModels.CompareClub compareClub(Team t, String shortcut, Integer position) {
        return new MatchdayPageModels.CompareClub(t.id, t.name, t.shortName == null || t.shortName.isBlank() ? t.name : t.shortName,
                MatchdayPageModels.crestLink(t), "/" + shortcut + "/teams/" + t.id, position);
    }

    /** Story r063: one profile per scorer of the club, keyed by the name the scorer list shows. */
    private static Map<String, MatchdayPageModels.ScorerProfileView> scorerProfiles(Team team, List<Match> schedule, String shortcut) {
        String club = team.shortName == null || team.shortName.isBlank() ? team.name : team.shortName;
        Map<String, MatchdayPageModels.ScorerProfileView> views = new LinkedHashMap<>();
        int i = 0;
        for (var e : de.javamark.matchoracle.matchday.control.ScorerProfileCalculator.of(team, schedule).entrySet()) {
            views.put(e.getKey(), MatchdayPageModels.ScorerProfileView.of("scorer-" + (i++), e.getValue(), club, shortcut));
        }
        return views;
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
