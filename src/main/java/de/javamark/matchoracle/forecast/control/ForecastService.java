package de.javamark.matchoracle.forecast.control;

import de.javamark.matchoracle.forecast.entity.Assessment;
import de.javamark.matchoracle.forecast.entity.AssessmentKind;
import de.javamark.matchoracle.forecast.entity.Forecast;
import de.javamark.matchoracle.forecast.entity.ForecastParameters;
import de.javamark.matchoracle.forecast.entity.Verdict;
import de.javamark.matchoracle.matchday.boundary.MatchSituation;
import de.javamark.matchoracle.matchday.boundary.MatchdayFacade;
import de.javamark.matchoracle.review.boundary.ReviewFacade;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;
import dev.langchain4j.service.output.OutputParsingException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Runs the forecast workflow for one match and commits the result (spec 02).
 * Forecasts run one at a time; a run takes a while with a local model.
 */
@ApplicationScoped
public class ForecastService {

    private static final Logger LOG = Logger.getLogger(ForecastService.class);

    /** Spec 02, rules: a failed assessment lowers the confidence — by this factor per failure. */
    static final double CONFIDENCE_FACTOR_PER_FAILED_ASSESSMENT = 0.75;

    @Inject
    MatchdayFacade matchday;

    @Inject
    ForecastWorkflow workflow;

    @Inject
    ForecastProgress progress;

    @Inject
    ReviewFacade review;

    @Inject
    ModelUsage modelUsage;

    /** Creates a new, immutable live forecast for an unplayed match. Blocks until the agents are done. */
    public long forecast(long matchId) {
        return forecast(matchId, false);
    }

    /**
     * Live forecast (spec 02: only before kickoff) or backtest (a played match of the
     * current season, facts as of before kickoff — the models cannot know these results).
     */
    @ActivateRequestContext // runs on the queue thread, which has no request context of its own
    public long forecast(long matchId, boolean backtest) {
        progress.start(matchId);
        modelUsage.reset();
        try {
            ForecastParameters parameters = currentParameters();
            MatchSituation situation = matchday.situationOf(matchId, parameters.formMatches)
                    .orElseThrow(() -> new IllegalArgumentException("unknown match " + matchId));
            if (!backtest && situation.played()) {
                throw new IllegalStateException("Prognosen entstehen nur für Begegnungen, die noch nicht angepfiffen sind");
            }
            if (backtest && !situation.played()) {
                throw new IllegalStateException("Ein Rücktest braucht ein bereits gespieltes Spiel");
            }
            if (backtest && !matchday.currentSeason(situation.league()).map(season -> season == situation.season()).orElse(false)) {
                throw new IllegalStateException("Rücktests nur für die laufende Saison – ältere Ergebnisse könnten die Modelle kennen");
            }
            String facts = FactSheet.render(situation);
            // spec 03, rules: nothing instead of a thin retrospective
            String retrospective = review.retrospectiveFor(matchId).orElse(null);
            String retrospectiveText = retrospective == null
                    ? "Keine belastbare Rückschau verfügbar (zu wenige vergleichbare Fälle)." : retrospective;
            ResultWithAgenticScope<ForecastDraft> result;
            try {
                result = workflow.run(facts, FactSheet.parameters(parameters), retrospectiveText, "", ForecastWorkflow.NOT_REVIEWED);
            } catch (RuntimeException e) {
                if (!isUnparsableAnswer(e)) throw e;
                // one broken answer of the forecaster or reviewer should not cost the whole run
                LOG.warnf("Forecast for match %d got an unparsable answer, running once more: %s", matchId, causeChain(e));
                result = workflow.run(facts, FactSheet.parameters(parameters), retrospectiveText, "", ForecastWorkflow.NOT_REVIEWED);
            }
            long id = commit(situation, parameters, result.result(), result.agenticScope(), retrospective, backtest);
            handOver(id);
            progress.done(matchId, id);
            return id;
        } catch (RuntimeException e) {
            LOG.errorf(e, "Forecast for match %d failed", matchId);
            progress.failed(matchId, causeChain(e));
            throw e;
        }
    }

    private static final Pattern BASE64_COPY = Pattern.compile(" \\(base64: \"[A-Za-z0-9+/=]*\"\\)");

    static boolean isUnparsableAnswer(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (t instanceof OutputParsingException) return true;
        }
        return false;
    }

    /** Agent frameworks wrap exceptions several times; the viewer wants the root cause — without the base64 copy of the answer. */
    static String causeChain(Throwable e) {
        StringBuilder sb = new StringBuilder();
        for (Throwable t = e; t != null && sb.length() < 2000; t = t.getCause() == t ? null : t.getCause()) {
            if (sb.length() > 0) sb.append(" <- ");
            String message = t.getMessage() == null ? null : BASE64_COPY.matcher(t.getMessage()).replaceAll("");
            sb.append(t.getClass().getSimpleName()).append(": ").append(message);
        }
        return sb.toString();
    }

    @Transactional
    ForecastParameters currentParameters() {
        return ForecastParameters.current();
    }

    /** Spec 02 -> 03: the review keeps its own record of every committed forecast. */
    @Transactional
    void handOver(long forecastId) {
        Forecast f = Forecast.findById(forecastId);
        review.forecastCommitted(new ReviewFacade.CommittedForecast(f.id, f.matchId, f.league, f.season, f.matchday, f.createdAt,
                f.homeWin, f.draw, f.awayWin, f.confidence, f.expectedHomeGoals, f.expectedAwayGoals, f.backtest));
    }

    /** Spec 02, step 7: written once, never changed. */
    @Transactional
    long commit(MatchSituation situation, ForecastParameters parameters, ForecastDraft draft, AgenticScope scope, String retrospective,
                boolean backtest) {
        Map<AssessmentKind, AssessmentResult> assessments = Map.of(
                AssessmentKind.FORM, assessmentFrom(scope, "formAssessment"),
                AssessmentKind.HEAD_TO_HEAD, assessmentFrom(scope, "headToHeadAssessment"),
                AssessmentKind.CONTEXT, assessmentFrom(scope, "contextAssessment"));
        ReviewVerdict verdict = scope.readState("verdict", ForecastWorkflow.NOT_REVIEWED);
        if (verdict == ForecastWorkflow.NOT_REVIEWED) {
            verdict = new ReviewVerdict(Verdict.ACCEPTED, "Keine Prüfung erfolgt.");
        }
        String reviewNote = scope.readState("reviewNote", "");
        long failed = assessments.values().stream().filter(AssessmentResult::isFailed).count();

        Forecast forecast = new Forecast();
        forecast.matchId = situation.matchId();
        forecast.createdAt = Instant.now();
        forecast.league = situation.league();
        forecast.season = situation.season();
        forecast.matchday = situation.matchday();
        forecast.homeTeam = situation.homeTeam().name();
        forecast.awayTeam = situation.awayTeam().name();
        forecast.kickoff = situation.kickoff();

        Probabilities p = Probabilities.normalized(draft.homeWin(), draft.draw(), draft.awayWin());
        forecast.homeWin = p.homeWin();
        forecast.draw = p.draw();
        forecast.awayWin = p.awayWin();
        forecast.expectedHomeGoals = Math.max(0, draft.expectedHomeGoals());
        forecast.expectedAwayGoals = Math.max(0, draft.expectedAwayGoals());
        forecast.confidence = confidence(Math.max(p.homeWin(), Math.max(p.draw(), p.awayWin())), failed);
        forecast.reasoning = draft.reasoning();

        forecast.verdict = verdict.verdict();
        forecast.verdictReason = verdict.reason();
        forecast.revised = reviewNote != null && !reviewNote.isBlank();
        forecast.objectionRemains = verdict.verdict() == Verdict.REVISE;

        forecast.homeAdvantage = parameters.homeAdvantage;
        forecast.formMatches = parameters.formMatches;
        forecast.promotedTeamMalus = parameters.promotedTeamMalus;
        forecast.modelName = modelUsage.summary();
        forecast.retrospective = retrospective;
        forecast.backtest = backtest;

        assessments.forEach((kind, result) -> {
            Assessment a = new Assessment();
            a.forecast = forecast;
            a.kind = kind;
            a.lean = result.lean();
            a.confidence = result.isFailed() ? null : clamp(result.confidence());
            a.summary = result.summary() == null ? "" : result.summary();
            a.failed = result.isFailed();
            forecast.assessments.add(a);
        });
        forecast.persist();
        LOG.infof("%s %d for %s - %s: %.0f/%.0f/%.0f, confidence %.2f, verdict %s%s", backtest ? "Backtest" : "Forecast", forecast.id,
                forecast.homeTeam, forecast.awayTeam, p.homeWin() * 100, p.draw() * 100, p.awayWin() * 100,
                forecast.confidence, forecast.verdict, forecast.revised ? " (revised)" : "");
        return forecast.id;
    }

    /**
     * Confidence is not asked from the model (small models anchor on a default value) but derived:
     * the probability of the predicted tendency, lowered for every failed assessment.
     */
    static double confidence(double tendencyProbability, long failedAssessments) {
        return clamp(tendencyProbability) * Math.pow(CONFIDENCE_FACTOR_PER_FAILED_ASSESSMENT, failedAssessments);
    }

    private static AssessmentResult assessmentFrom(AgenticScope scope, String key) {
        Object value = scope.readState(key);
        return value instanceof AssessmentResult r ? r : AssessmentResult.failed("keine Antwort erhalten");
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }
}
