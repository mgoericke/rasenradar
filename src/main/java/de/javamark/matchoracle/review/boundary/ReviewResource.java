package de.javamark.matchoracle.review.boundary;

import de.javamark.matchoracle.review.control.ReviewService;
import de.javamark.matchoracle.review.entity.AccuracyReport;
import de.javamark.matchoracle.review.entity.ForecastEvaluation;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/** Spec 03, JSON API under /review. */
@Path("/review")
@Produces(MediaType.APPLICATION_JSON)
public class ReviewResource {

    @Inject
    ReviewService review;

    /** Live forecasts by default; {@code ?backtest=true} for the backtest series. */
    @GET
    @Path("/accuracy/{league}")
    public AccuracyReport accuracy(@PathParam("league") String league, @QueryParam("backtest") @DefaultValue("false") boolean backtest) {
        return review.accuracy(league, backtest);
    }

    @GET
    @Path("/evaluations/{forecastId}")
    @Transactional(Transactional.TxType.SUPPORTS)
    public ForecastEvaluation evaluation(@PathParam("forecastId") long forecastId) {
        return ForecastEvaluation.findByForecast(forecastId).orElseThrow(() -> new NotFoundException("not evaluated yet"));
    }
}
