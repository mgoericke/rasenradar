package de.javamark.matchoracle.forecast.boundary;

import de.javamark.matchoracle.forecast.control.ForecastParametersService;
import de.javamark.matchoracle.forecast.control.ForecastProgress;
import de.javamark.matchoracle.forecast.control.ForecastQueue;
import de.javamark.matchoracle.forecast.entity.Assessment;
import de.javamark.matchoracle.forecast.entity.AssessmentKind;
import de.javamark.matchoracle.forecast.entity.Forecast;
import de.javamark.matchoracle.forecast.entity.ForecastParameters;
import de.javamark.matchoracle.forecast.entity.Outcome;
import de.javamark.matchoracle.forecast.entity.Verdict;
import de.javamark.matchoracle.matchday.boundary.MatchSituation;
import de.javamark.matchoracle.matchday.boundary.MatchdayFacade;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.Nav;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** HTML pages of the forecast feature (spec 02, actor "viewer"). */
@Path("/")
@Produces(MediaType.TEXT_HTML)
@Transactional(Transactional.TxType.SUPPORTS)
public class ForecastPages {

    private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("d. MMMM yyyy, HH:mm", Locale.GERMAN);
    private static final DateTimeFormatter DAY_TIME = DateTimeFormatter.ofPattern("EEE d.M., HH:mm", Locale.GERMAN);

    @CheckedTemplate
    static class Templates {
        static native TemplateInstance forecast(ForecastPage page);
        static native TemplateInstance progress(ProgressFragment fragment, String matchLink);
        static native TemplateInstance matchdayForecasts(MatchdayForecastsPage page);
        static native TemplateInstance parameters(ParametersPage page);
        static native TemplateInstance summary(SummaryFragment fragment);
        static native TemplateInstance markers(List<MarkerView> markers);
    }

    @Inject
    MatchdayFacade matchday;

    @Inject
    ForecastQueue queue;

    @Inject
    ForecastProgress progress;

    @Inject
    ForecastParametersService parameters;

    // --- view models -------------------------------------------------------------

    record AssessmentView(String title, String lean, String leanClass, Integer confidencePercent, String summary, boolean failed) {
        static AssessmentView of(Assessment a) {
            return new AssessmentView(ForecastPages.title(a.kind), ForecastPages.lean(a.lean), a.lean == null ? "none" : a.lean.name().toLowerCase(),
                    a.confidence == null ? null : (int) Math.round(a.confidence * 100), a.summary, a.failed);
        }
    }

    record ForecastView(long id, String createdAt, String tendency, String tendencyClass, int homeWin, int draw, int awayWin,
                        String expectedScore, int confidencePercent, String reasoning, String verdict, String verdictClass,
                        String verdictReason, boolean revised, boolean objectionRemains, List<AssessmentView> assessments,
                        String parameters, String modelName, List<String> retrospectiveLines, boolean backtest) {
        static ForecastView of(Forecast f) {
            int home = (int) Math.round(f.homeWin * 100), draw = (int) Math.round(f.draw * 100);
            return new ForecastView(f.id, DATE_TIME.format(f.createdAt.atZone(ZONE)), ForecastPages.lean(f.tendency()), f.tendency().name().toLowerCase(),
                    home, draw, 100 - home - draw, f.expectedHomeGoals + ":" + f.expectedAwayGoals, (int) Math.round(f.confidence * 100),
                    f.reasoning, f.verdict == Verdict.ACCEPTED ? "angenommen" : "Einwand bleibt", f.verdict == Verdict.ACCEPTED ? "accepted" : "objection",
                    f.verdictReason, f.revised, f.objectionRemains,
                    f.assessments.stream().sorted(java.util.Comparator.comparing(a -> a.kind.ordinal())).map(AssessmentView::of).toList(),
                    "Heimvorteil " + Math.round(f.homeAdvantage * 100) + " Prozentpunkte, Form über " + f.formMatches + " Spiele, Aufsteiger-Malus "
                            + Math.round(f.promotedTeamMalus * 100) + " Prozentpunkte",
                    f.modelName,
                    f.retrospective == null ? List.of() : List.of(f.retrospective.strip().split("\n")),
                    f.backtest);
        }
    }

    record StepView(String label, String state) {
    }

    record ProgressFragment(long matchId, String state, List<StepView> steps, String error, boolean finished) {
    }

    record ForecastPage(Nav nav, long matchId, String matchLink, String matchdayLink, int matchdayNumber, String homeTeam, String awayTeam,
                        String kickoff, boolean played, boolean backtestAllowed, String result, ForecastView latest, List<ForecastView> older,
                        ProgressFragment progress) {
    }

    record MatchRow(long matchId, String link, String kickoff, String homeTeam, String awayTeam, ForecastView forecast, boolean running) {
    }

    record MatchdayForecastsPage(Nav nav, int season, int number, String matchdayLink, String startAllLink, List<MatchRow> matches, int openCount,
                                 int backtestCount) {
    }

    record ParametersPage(Nav nav, int homeAdvantagePercent, int formMatches, int promotedTeamMalusPercent, String message) {
    }

    /** Embedded on the match page: the latest forecast in one line, or the way to create one. */
    record SummaryFragment(String forecastLink, boolean played, boolean running, ForecastView forecast) {
    }

    /** One out-of-band marker per match of a matchday that already has a forecast, swapped into the fixture list. */
    record MarkerView(long matchId, String label, String tendency, String tendencyClass, int homeWin, int draw, int awayWin) {
        static MarkerView of(Forecast f) {
            ForecastView v = ForecastView.of(f);
            return new MarkerView(f.matchId, "KI " + ForecastPages.abbreviation(f.tendency()), v.tendency(), v.tendencyClass(), v.homeWin(), v.draw(), v.awayWin());
        }
    }

    // --- pages -------------------------------------------------------------------

    @GET
    @Path("/{league}/matches/{id}/forecast")
    public TemplateInstance forecast(@PathParam("league") String league, @PathParam("id") long matchId) {
        MatchSituation situation = matchday.situationOf(matchId, parameters.current().formMatches)
                .filter(s -> s.league().equals(league(league)))
                .orElseThrow(() -> new NotFoundException("match not found"));
        List<ForecastView> all = Forecast.findByMatch(matchId).stream().map(ForecastView::of).toList();
        String shortcut = league(league);
        return Templates.forecast(new ForecastPage(Nav.of(situation.league()), matchId,
                "/" + shortcut + "/matches/" + matchId, "/" + shortcut + "/" + situation.season() + "/" + situation.matchday(), situation.matchday(),
                situation.homeTeam().name(), situation.awayTeam().name(), DATE_TIME.format(situation.kickoff().atZone(ZONE)), situation.played(),
                backtestAllowed(situation), situation.result() == null ? null : situation.result().home() + ":" + situation.result().away(),
                all.isEmpty() ? null : all.get(0), all.size() > 1 ? all.subList(1, all.size()) : List.of(),
                pageProgress(matchId)));
    }

    /** Backtests only for played matches of the current season — the models cannot know these results. */
    private boolean backtestAllowed(MatchSituation s) {
        return s.played() && matchday.currentSeason(s.league()).map(season -> season == s.season()).orElse(false);
    }

    /** Starts a forecast (or, for a played match, a backtest) from the page's button; the page then polls the progress. */
    @POST
    @Path("/{league}/matches/{id}/forecast")
    public Response start(@PathParam("league") String league, @PathParam("id") long matchId) {
        boolean played = matchday.situationOf(matchId, 1).map(MatchSituation::played).orElse(false);
        queue.enqueue(matchId, played);
        return Response.seeOther(URI.create("/" + league + "/matches/" + matchId + "/forecast")).build();
    }

    /** htmx polls this while a run is in progress. */
    @GET
    @Path("/{league}/matches/{id}/forecast/progress")
    public TemplateInstance progressFragment(@PathParam("league") String league, @PathParam("id") long matchId) {
        return Templates.progress(progressFragment(matchId), "/" + league + "/matches/" + matchId);
    }

    /**
     * Fragment for the match page (loaded by htmx): the latest forecast, a running note, or the link to create one.
     * The match page belongs to the matchday feature and composes this on the HTML level, so it stays unaware of forecasts.
     */
    @GET
    @Path("/{league}/matches/{id}/forecast/summary")
    public TemplateInstance summaryFragment(@PathParam("league") String league, @PathParam("id") long matchId,
                                            @QueryParam("played") @DefaultValue("false") boolean played) {
        String link = "/" + league(league) + "/matches/" + matchId + "/forecast";
        ForecastView latest = Forecast.findLatestByMatch(matchId).map(ForecastView::of).orElse(null);
        return Templates.summary(new SummaryFragment(link, played, latest == null && queue.isQueuedOrRunning(matchId), latest));
    }

    /** Fragment for the matchday page (loaded by htmx): out-of-band markers for every match that already has a forecast. */
    @GET
    @Path("/{league}/{season}/{number}/forecasts/markers")
    public TemplateInstance markersFragment(@PathParam("league") String league, @PathParam("season") int season, @PathParam("number") int number) {
        Map<Long, Forecast> latest = Forecast.findByMatchday(league(league), season, number).stream()
                .collect(Collectors.toMap(f -> f.matchId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        return Templates.markers(latest.values().stream().map(MarkerView::of).toList());
    }

    @GET
    @Path("/{league}/{season}/{number}/forecasts")
    public TemplateInstance matchdayForecasts(@PathParam("league") String league, @PathParam("season") int season, @PathParam("number") int number) {
        String l = league(league);
        int formMatches = parameters.current().formMatches;
        List<Long> ids = matchday.matchIds(l, season, number);
        if (ids.isEmpty()) {
            throw new NotFoundException("matchday not found");
        }
        Map<Long, Forecast> latest = Forecast.findByMatchday(l, season, number).stream()
                .collect(Collectors.toMap(f -> f.matchId, Function.identity(), (a, b) -> a));
        List<MatchRow> rows = ids.stream()
                .map(id -> matchday.situationOf(id, formMatches).orElseThrow())
                .map(s -> new MatchRow(s.matchId(), "/" + l + "/matches/" + s.matchId() + "/forecast",
                        DAY_TIME.format(s.kickoff().atZone(ZONE)), s.homeTeam().name(), s.awayTeam().name(),
                        Optional.ofNullable(latest.get(s.matchId())).map(ForecastView::of).orElse(null),
                        queue.isQueuedOrRunning(s.matchId())))
                .toList();
        List<MatchSituation> situations = ids.stream().map(id -> matchday.situationOf(id, formMatches).orElseThrow()).toList();
        long open = situations.stream().filter(s -> !s.played()).count();
        long backtestable = situations.stream().filter(this::backtestAllowed).filter(s -> !latest.containsKey(s.matchId())).count();
        String base = "/" + l + "/" + season + "/" + number;
        return Templates.matchdayForecasts(new MatchdayForecastsPage(Nav.of(l), season, number, base, base + "/forecasts", rows, (int) open,
                (int) backtestable));
    }

    /** Starts forecasts for every unplayed match of the matchday. */
    @POST
    @Path("/{league}/{season}/{number}/forecasts")
    public Response startAll(@PathParam("league") String league, @PathParam("season") int season, @PathParam("number") int number) {
        matchday.unplayedMatchIds(league(league), season, number).forEach(queue::enqueue);
        return Response.seeOther(URI.create("/" + league + "/" + season + "/" + number + "/forecasts")).build();
    }

    /** Backtests for every played match of the matchday that has no forecast yet (current season only). */
    @POST
    @Path("/{league}/{season}/{number}/backtests")
    public Response backtestAll(@PathParam("league") String league, @PathParam("season") int season, @PathParam("number") int number) {
        String l = league(league);
        Map<Long, Forecast> latest = Forecast.findByMatchday(l, season, number).stream()
                .collect(Collectors.toMap(f -> f.matchId, Function.identity(), (a, b) -> a));
        matchday.playedMatchIds(l, season, number).stream()
                .filter(id -> !latest.containsKey(id))
                .filter(id -> matchday.situationOf(id, 1).map(this::backtestAllowed).orElse(false))
                .forEach(id -> queue.enqueue(id, true));
        return Response.seeOther(URI.create("/" + l + "/" + season + "/" + number + "/forecasts")).build();
    }

    @GET
    @Path("/forecast-parameters")
    public TemplateInstance parameters() {
        ForecastParameters p = parameters.current();
        return Templates.parameters(new ParametersPage(Nav.page("parameters"), (int) Math.round(p.homeAdvantage * 100), p.formMatches, (int) Math.round(p.promotedTeamMalus * 100), null));
    }

    @POST
    @Path("/forecast-parameters")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public TemplateInstance updateParameters(@FormParam("homeAdvantage") double homeAdvantage, @FormParam("formMatches") int formMatches,
                                             @FormParam("promotedTeamMalus") double promotedTeamMalus) {
        String message;
        try {
            parameters.update(homeAdvantage / 100, formMatches, promotedTeamMalus / 100);
            message = "Gespeichert. Gilt ab der nächsten Prognose.";
        } catch (IllegalArgumentException e) {
            message = e.getMessage();
        }
        ForecastParameters p = parameters.current();
        return Templates.parameters(new ParametersPage(Nav.page("parameters"), (int) Math.round(p.homeAdvantage * 100), p.formMatches, (int) Math.round(p.promotedTeamMalus * 100), message));
    }

    // --- helpers -----------------------------------------------------------------

    /** On the full page a finished run is simply the forecast shown below; only the polled fragment triggers the reload. */
    private ProgressFragment pageProgress(long matchId) {
        ProgressFragment f = progressFragment(matchId);
        return "DONE".equals(f.state()) ? new ProgressFragment(matchId, "NONE", List.of(), null, false) : f;
    }

    private ProgressFragment progressFragment(long matchId) {
        return progress.of(matchId)
                .map(r -> new ProgressFragment(matchId, r.state().name(),
                        r.stepList().stream().map(s -> new StepView(s.label(), s.state().name())).toList(),
                        r.error(), r.state() == ForecastProgress.State.DONE || r.state() == ForecastProgress.State.FAILED))
                .orElseGet(() -> new ProgressFragment(matchId, queue.isQueuedOrRunning(matchId) ? "WAITING" : "NONE", List.of(), null, false));
    }

    static String title(AssessmentKind kind) {
        return switch (kind) {
            case FORM -> "Form";
            case HEAD_TO_HEAD -> "Direkte Duelle";
            case CONTEXT -> "Umfeld";
        };
    }

    static String lean(Outcome outcome) {
        if (outcome == null) return "keine Tendenz";
        return switch (outcome) {
            case HOME_WIN -> "Heimsieg";
            case DRAW -> "Unentschieden";
            case AWAY_WIN -> "Auswärtssieg";
        };
    }

    /** Toto-style abbreviation of the tendency, for the small markers in the fixture list. */
    static String abbreviation(Outcome outcome) {
        return switch (outcome) {
            case HOME_WIN -> "1";
            case DRAW -> "X";
            case AWAY_WIN -> "2";
        };
    }

    private String league(String shortcut) {
        return matchday.leagueShortcut(shortcut).orElseThrow(() -> new NotFoundException("unknown league " + shortcut));
    }
}
