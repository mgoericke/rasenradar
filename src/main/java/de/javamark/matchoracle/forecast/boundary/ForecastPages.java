package de.javamark.matchoracle.forecast.boundary;

import de.javamark.matchoracle.forecast.control.ForecastParametersService;
import de.javamark.matchoracle.forecast.control.ForecastProgress;
import de.javamark.matchoracle.forecast.control.ForecastQueue;
import de.javamark.matchoracle.forecast.entity.Assessment;
import de.javamark.matchoracle.forecast.entity.AssessmentKind;
import de.javamark.matchoracle.forecast.entity.Forecast;
import de.javamark.matchoracle.forecast.entity.Outcome;
import de.javamark.matchoracle.forecast.entity.Verdict;
import de.javamark.matchoracle.matchday.boundary.MatchSituation;
import de.javamark.matchoracle.matchday.boundary.MatchdayFacade;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.Nav;
import de.javamark.matchoracle.review.boundary.ReviewFacade;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
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
        static native TemplateInstance howItWorks(HowItWorksPage page);
        static native TemplateInstance summary(SummaryFragment fragment);
        static native TemplateInstance markers(List<MarkerView> markers);
        static native TemplateInstance streamForecasts(List<StreamCell> cells, List<SlotSummary> slots);
    }

    @Inject
    MatchdayFacade matchday;

    @Inject
    ForecastQueue queue;

    @Inject
    ForecastProgress progress;

    @Inject
    ForecastParametersService parameters;

    @Inject
    ReviewFacade review;

    // --- view models -------------------------------------------------------------

    record AssessmentView(String title, String lean, String leanClass, Integer confidencePercent, String summary, boolean failed) {
        static AssessmentView of(Assessment a) {
            return new AssessmentView(ForecastPages.title(a.kind), ForecastPages.lean(a.lean), a.lean == null ? "none" : a.lean.name().toLowerCase(),
                    a.confidence == null ? null : (int) Math.round(a.confidence * 100), AiText.forDisplay(a.summary), a.failed);
        }
    }

    record ForecastView(long id, String createdAt, String tendency, String tendencyClass, int homeWin, int draw, int awayWin,
                        String expectedScore, int confidencePercent, String reasoning, String verdict, String verdictClass,
                        String verdictReason, boolean revised, boolean objectionRemains, List<AssessmentView> assessments,
                        String parameters, String modelName, List<String> retrospectiveLines, boolean backtest,
                        Boolean contrarian, Boolean tendencyHit) {
        static ForecastView of(Forecast f) {
            int home = (int) Math.round(f.homeWin * 100), draw = (int) Math.round(f.draw * 100);
            return new ForecastView(f.id, DATE_TIME.format(f.createdAt.atZone(ZONE)), ForecastPages.lean(f.tendency()), f.tendency().name().toLowerCase(),
                    home, draw, 100 - home - draw, f.expectedHomeGoals + ":" + f.expectedAwayGoals, (int) Math.round(f.confidence * 100),
                    AiText.forDisplay(f.reasoning), f.verdict == Verdict.ACCEPTED ? "angenommen" : "Einwand bleibt", f.verdict == Verdict.ACCEPTED ? "accepted" : "objection",
                    AiText.forDisplay(f.verdictReason), f.revised, f.objectionRemains,
                    f.assessments.stream().sorted(java.util.Comparator.comparing(a -> a.kind.ordinal())).map(AssessmentView::of).toList(),
                    "Heimvorteil " + Math.round(f.homeAdvantage * 100) + " Prozentpunkte, Form über " + f.formMatches + " Spiele, Aufsteiger-Malus "
                            + Math.round(f.promotedTeamMalus * 100) + " Prozentpunkte",
                    f.modelName,
                    f.retrospective == null ? List.of() : java.util.Arrays.stream(f.retrospective.strip().split("\n")).map(AiText::listItem).toList(),
                    f.backtest, f.contrarian, null);
        }

        /** Spec 05, "Gegen den Strom": whether the deviation paid off, once the match is evaluated. */
        ForecastView withTendencyHit(Boolean hit) {
            return new ForecastView(id, createdAt, tendency, tendencyClass, homeWin, draw, awayWin, expectedScore, confidencePercent,
                    reasoning, verdict, verdictClass, verdictReason, revised, objectionRemains, assessments, parameters, modelName,
                    retrospectiveLines, backtest, contrarian, hit);
        }

        /** For the template: three states, since a plain boolean can't tell "not yet evaluated" from "missed". */
        public String contrarianState() {
            if (tendencyHit == null) return "pending";
            return tendencyHit ? "paid-off" : "did-not-pay-off";
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

    record MatchdayForecastsPage(Nav nav, int season, int number, String matchdayLink, List<MatchRow> matches, int openCount,
                                 int backtestCount) {
    }

    /** Spec 05, step 9: purely static content — no forecast data needed. */
    record HowItWorksPage(Nav nav) {
    }

    /**
     * Embedded in the match page's scoreboard: the latest forecast with its verdict once the match is
     * played, or the way to create one. The assessors' reasoning travels along out of band into the
     * page body, so the match page only knows this fragment's URL.
     */
    record SummaryFragment(String forecastLink, boolean played, boolean running, ForecastView forecast, StreamCell verdict,
                           Settlement settlement) {
    }

    /**
     * One out-of-band marker per match of a matchday that already has a forecast, swapped into the
     * fixture list: the expected score, with the tendency and its probability as a tooltip.
     */
    record MarkerView(long matchId, String expectedScore, String tendency, int probabilityPercent, Boolean hit) {
        static MarkerView of(Forecast f) {
            return of(f, null);
        }

        /**
         * Story r012: with a FINAL score the marker settles the forecast — {@code hit} says whether the
         * tendency was right. A provisional score settles nothing yet; it stays null like an open match.
         */
        static MarkerView of(Forecast f, MatchdayFacade.MatchScore score) {
            ForecastView v = ForecastView.of(f);
            int probability = switch (f.tendency()) {
                case HOME_WIN -> v.homeWin();
                case DRAW -> v.draw();
                case AWAY_WIN -> v.awayWin();
            };
            Boolean hit = score == null || score.provisional() ? null : f.tendency() == Outcome.of(score.homeGoals(), score.awayGoals());
            return new MarkerView(f.matchId, v.expectedScore(), v.tendency(), probability, hit);
        }

        public boolean settled() {
            return hit != null;
        }
    }

    /**
     * Story r012, "Abgerechnet" on the match page: a forecast against the FINAL score — the verdict in
     * the terms of Spec 03 and which of the three assessors leaned the right way.
     */
    record Settlement(String finalScore, String expectedScore, boolean hit, boolean exact, List<String> rightAssessors) {
        static Settlement of(Forecast f, MatchdayFacade.MatchScore score) {
            if (score == null || score.provisional()) {
                return null;
            }
            Outcome actual = Outcome.of(score.homeGoals(), score.awayGoals());
            List<String> right = f.assessments.stream()
                    .filter(a -> !a.failed && a.lean == actual)
                    .sorted(java.util.Comparator.comparing(a -> a.kind.ordinal()))
                    .map(a -> ForecastPages.title(a.kind)).toList();
            return new Settlement(score.homeGoals() + ":" + score.awayGoals(), f.expectedHomeGoals + ":" + f.expectedAwayGoals,
                    f.tendency() == actual, f.expectedHomeGoals == score.homeGoals() && f.expectedAwayGoals == score.awayGoals(), right);
        }

        /** "Recht hatte: Umfeld.", "Recht hatten: Form und Umfeld.", "Keiner der drei Bewerter lag richtig." */
        public String assessorLine() {
            if (rightAssessors.isEmpty()) return "Keiner der drei Bewerter lag richtig.";
            if (rightAssessors.size() == 1) return "Recht hatte: " + rightAssessors.getFirst() + ".";
            return "Recht hatten: " + String.join(", ", rightAssessors.subList(0, rightAssessors.size() - 1))
                    + " und " + rightAssessors.getLast() + ".";
        }
    }

    // --- pages -------------------------------------------------------------------

    @GET
    @Path("/{league}/matches/{id}/forecast")
    public TemplateInstance forecast(@PathParam("league") String league, @PathParam("id") long matchId) {
        MatchSituation situation = matchday.situationOf(matchId, parameters.current().formMatches)
                .filter(s -> s.league().equals(league(league)))
                .orElseThrow(() -> new NotFoundException("match not found"));
        List<ForecastView> all = Forecast.findByMatch(matchId).stream()
                .map(f -> ForecastView.of(f).withTendencyHit(review.tendencyHit(f.id).orElse(null)))
                .toList();
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

    /**
     * Starts a backtest from the page's button; the page then polls the progress. Live forecasts
     * are no longer viewer-triggered (spec 02, "Terminplanung") — this rejects them.
     */
    @POST
    @Path("/{league}/matches/{id}/forecast")
    public Response start(@PathParam("league") String league, @PathParam("id") long matchId) {
        boolean played = matchday.situationOf(matchId, 1).map(MatchSituation::played).orElse(false);
        if (!played) {
            throw new jakarta.ws.rs.BadRequestException("Die KI-Vorschau für bevorstehende Begegnungen entsteht automatisch");
        }
        queue.enqueue(matchId, true);
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
    /**
     * Spec 01, "Spieltags-Strom": the KI cell of one stream row — the guide value of the last forecast
     * before kickoff, its tendency before the final whistle, afterwards whether that tendency was right.
     * Display only; the Rückschau has its own evaluation (spec 03).
     */
    record StreamCell(long matchId, String expectedScore, String tendency, int probabilityPercent,
                      Boolean hit, boolean exact, boolean provisional) {
        static StreamCell of(long matchId, Optional<Forecast> forecast, MatchdayFacade.MatchScore score) {
            if (forecast.isEmpty()) {
                return new StreamCell(matchId, null, null, 0, null, false, false);
            }
            Forecast f = forecast.get();
            MarkerView m = MarkerView.of(f);
            Boolean hit = score == null ? null : f.tendency() == Outcome.of(score.homeGoals(), score.awayGoals());
            boolean exact = score != null && f.expectedHomeGoals == score.homeGoals() && f.expectedAwayGoals == score.awayGoals();
            return new StreamCell(matchId, m.expectedScore(), m.tendency(), m.probabilityPercent(), hit, exact,
                    score != null && score.provisional());
        }

        public boolean missing() {
            return expectedScore == null;
        }
    }

    /** Spec 01: "KI 2 von 3 getroffen" for one kickoff block — only played matches that had a forecast count. */
    record SlotSummary(String key, int hits, int judged, boolean provisional) {
        static List<SlotSummary> of(List<Forecast> forecasts, List<StreamCell> cells) {
            Map<Long, StreamCell> byMatch = cells.stream().collect(Collectors.toMap(StreamCell::matchId, Function.identity()));
            return forecasts.stream().collect(Collectors.groupingBy(f -> f.kickoff, java.util.TreeMap::new, Collectors.toList()))
                    .entrySet().stream().map(e -> {
                        List<StreamCell> judged = e.getValue().stream().map(f -> byMatch.get(f.matchId))
                                .filter(c -> c != null && c.hit() != null).toList();
                        return new SlotSummary(String.valueOf(e.getKey().getEpochSecond()),
                                (int) judged.stream().filter(StreamCell::hit).count(), judged.size(),
                                judged.stream().anyMatch(StreamCell::provisional));
                    }).toList();
        }
    }

    @GET
    @Path("/{league}/matches/{id}/forecast/summary")
    public TemplateInstance summaryFragment(@PathParam("league") String league, @PathParam("id") long matchId,
                                            @QueryParam("played") @DefaultValue("false") boolean played) {
        String link = "/" + league(league) + "/matches/" + matchId + "/forecast";
        Optional<Forecast> forecast = Forecast.findLatestByMatch(matchId);
        ForecastView latest = forecast.map(ForecastView::of).orElse(null);
        MatchdayFacade.MatchScore score = matchday.scoresOf(List.of(matchId)).get(matchId);
        StreamCell verdict = StreamCell.of(matchId, forecast, score);
        Settlement settlement = forecast.map(f -> Settlement.of(f, score)).orElse(null);
        return Templates.summary(new SummaryFragment(link, played, latest == null && queue.isQueuedOrRunning(matchId), latest, verdict,
                settlement));
    }

    /**
     * Fragment for the landing page's stream (loaded by htmx): one out-of-band KI cell per match and one
     * summary per kickoff block. Opened directly, the bare cells would arrive without layout — anything
     * but htmx goes to the landing page.
     */
    @GET
    @Path("/strom/ki-vorschau")
    public Response streamFragment(@QueryParam("match") List<Long> matchIds, @HeaderParam("HX-Request") String htmxRequest) {
        if (htmxRequest == null) {
            return Response.seeOther(URI.create("/")).build();
        }
        List<Long> ids = matchIds.stream().distinct().limit(MAX_STREAM_ROWS).toList();
        Map<Long, MatchdayFacade.MatchScore> scores = matchday.scoresOf(ids);
        List<Forecast> forecasts = ids.stream().map(id -> Forecast.latestBeforeKickoff(Forecast.findByMatch(id)))
                .flatMap(Optional::stream).toList();
        Map<Long, Forecast> byMatch = forecasts.stream().collect(Collectors.toMap(f -> f.matchId, Function.identity()));
        List<StreamCell> cells = ids.stream()
                .map(id -> StreamCell.of(id, Optional.ofNullable(byMatch.get(id)), scores.get(id))).toList();
        return Response.ok(Templates.streamForecasts(cells, SlotSummary.of(forecasts, cells))).build();
    }

    /** Two matchdays of both Bundesligas; caps what a crafted URL can ask for. */
    private static final int MAX_STREAM_ROWS = 40;

    /** Fragment for the matchday page (loaded by htmx): out-of-band markers for every match that already has a forecast. */
    @GET
    @Path("/{league}/{season}/{number}/forecasts/markers")
    public TemplateInstance markersFragment(@PathParam("league") String league, @PathParam("season") int season, @PathParam("number") int number) {
        Map<Long, Forecast> latest = Forecast.findByMatchday(league(league), season, number).stream()
                .collect(Collectors.toMap(f -> f.matchId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        Map<Long, MatchdayFacade.MatchScore> scores = matchday.scoresOf(List.copyOf(latest.keySet()));
        return Templates.markers(latest.values().stream().map(f -> MarkerView.of(f, scores.get(f.matchId))).toList());
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
        return Templates.matchdayForecasts(new MatchdayForecastsPage(Nav.of(l), season, number, base, rows, (int) open, (int) backtestable));
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

    /** Spec 05, step 9: describes the procedure, not the current scales — those are changed directly in the database now. */
    @GET
    @Path("/so-entsteht-eine-prognose")
    public TemplateInstance howItWorks() {
        return Templates.howItWorks(new HowItWorksPage(Nav.page("howItWorks")));
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

    private String league(String shortcut) {
        return matchday.leagueShortcut(shortcut).orElseThrow(() -> new NotFoundException("unknown league " + shortcut));
    }
}
