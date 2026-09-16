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
import de.javamark.matchoracle.matchday.boundary.MatchdayFacade;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.time.Instant;
import java.util.List;

/** Spec 02, JSON API under /forecasts: start forecasts, read them, view progress, view and change the scales. */
@Path("/forecasts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ForecastResource {

    @Inject
    ForecastQueue queue;

    @Inject
    ForecastProgress progress;

    @Inject
    ForecastParametersService parameters;

    @Inject
    MatchdayFacade matchday;

    record AssessmentRep(AssessmentKind kind, Outcome lean, Double confidence, String summary, boolean failed) {
        static AssessmentRep of(Assessment a) {
            return new AssessmentRep(a.kind, a.lean, a.confidence, a.summary, a.failed);
        }
    }

    record ForecastRep(long id, long matchId, Instant createdAt, String homeTeam, String awayTeam, Instant kickoff,
                       Outcome tendency, double homeWin, double draw, double awayWin,
                       int expectedHomeGoals, int expectedAwayGoals, double confidence, String reasoning,
                       Verdict verdict, String verdictReason, boolean revised, boolean objectionRemains,
                       List<AssessmentRep> assessments, ParametersRep parameters, String modelName, boolean backtest, String retrospective) {
        static ForecastRep of(Forecast f) {
            return new ForecastRep(f.id, f.matchId, f.createdAt, f.homeTeam, f.awayTeam, f.kickoff, f.tendency(),
                    f.homeWin, f.draw, f.awayWin, f.expectedHomeGoals, f.expectedAwayGoals, f.confidence, f.reasoning,
                    f.verdict, f.verdictReason, f.revised, f.objectionRemains,
                    f.assessments.stream().map(AssessmentRep::of).toList(),
                    new ParametersRep(f.homeAdvantage, f.formMatches, f.promotedTeamMalus), f.modelName, f.backtest, f.retrospective);
        }
    }

    record ParametersRep(double homeAdvantage, int formMatches, double promotedTeamMalus) {
        static ParametersRep of(ForecastParameters p) {
            return new ParametersRep(p.homeAdvantage, p.formMatches, p.promotedTeamMalus);
        }
    }

    record ProgressRep(long matchId, ForecastProgress.State state, List<ForecastProgress.Step> steps, String error, Long forecastId,
                       ForecastProgress.Tokens tokens) {
    }

    /** Starts a new forecast for the match; 202 with the progress location. */
    /** Live forecast; {@code ?backtest=true} for a played match of the current season. */
    @POST
    @Path("/matches/{id}")
    public Response start(@PathParam("id") long matchId, @QueryParam("backtest") @DefaultValue("false") boolean backtest) {
        queue.enqueue(matchId, backtest);
        return Response.accepted().location(URI.create("/forecasts/matches/" + matchId + "/progress")).build();
    }

    /** Starts forecasts for every unplayed match of the matchday. */
    @POST
    @Path("/leagues/{league}/seasons/{season}/matchdays/{number}")
    public Response startMatchday(@PathParam("league") String league, @PathParam("season") int season, @PathParam("number") int number) {
        List<Long> ids = matchday.unplayedMatchIds(league(league), season, number);
        ids.forEach(queue::enqueue);
        return Response.accepted().entity(ids).build();
    }

    @GET
    @Path("/matches/{id}/progress")
    public ProgressRep progress(@PathParam("id") long matchId) {
        return progress.of(matchId)
                .map(r -> new ProgressRep(matchId, r.state(), r.stepList(), r.error(), r.forecastId(), r.tokens()))
                .orElseGet(() -> new ProgressRep(matchId, queue.isQueuedOrRunning(matchId) ? ForecastProgress.State.WAITING : null, List.of(), null, null, null));
    }

    @GET
    @Path("/matches/{id}")
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<ForecastRep> forecasts(@PathParam("id") long matchId) {
        return Forecast.findByMatch(matchId).stream().map(ForecastRep::of).toList();
    }

    @GET
    @Path("/{id}")
    @Transactional(Transactional.TxType.SUPPORTS)
    public ForecastRep forecast(@PathParam("id") long id) {
        return Forecast.<Forecast>findByIdOptional(id).map(ForecastRep::of).orElseThrow(() -> new NotFoundException("forecast not found"));
    }

    @GET
    @Path("/parameters")
    public ParametersRep parameters() {
        return ParametersRep.of(parameters.current());
    }

    @PUT
    @Path("/parameters")
    public ParametersRep updateParameters(ParametersRep rep) {
        return ParametersRep.of(parameters.update(rep.homeAdvantage(), rep.formMatches(), rep.promotedTeamMalus()));
    }

    private String league(String shortcut) {
        return matchday.leagueShortcut(shortcut).orElseThrow(() -> new NotFoundException("unknown league " + shortcut));
    }
}
