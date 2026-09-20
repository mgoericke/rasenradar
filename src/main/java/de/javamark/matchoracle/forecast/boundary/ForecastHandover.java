package de.javamark.matchoracle.forecast.boundary;

import de.javamark.matchoracle.forecast.entity.Forecast;
import de.javamark.matchoracle.review.boundary.ReviewFacade;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Hands every committed forecast to the review (spec 02 -> 03). The forecast
 * service does this on commit; this catch-up covers forecasts made before the
 * review existed or while a hand-over failed. The review ignores duplicates.
 */
@ApplicationScoped
public class ForecastHandover {

    @Inject
    ReviewFacade review;

    @Scheduled(every = "${matchoracle.review.catch-up-interval}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    @Transactional
    void handOverAll() {
        for (Forecast f : Forecast.<Forecast>listAll()) {
            review.forecastCommitted(new ReviewFacade.CommittedForecast(f.id, f.matchId, f.league, f.season, f.matchday, f.createdAt,
                    f.homeWin, f.draw, f.awayWin, f.confidence, f.expectedHomeGoals, f.expectedAwayGoals, f.backtest, f.contrarian));
        }
    }
}
