package de.javamark.matchoracle.review.boundary;

import de.javamark.matchoracle.review.control.ReviewService;
import de.javamark.matchoracle.review.entity.Retrospective;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.Optional;

/** Java entry point for the forecast feature (spec 03): retrospective in, committed forecasts out. */
@ApplicationScoped
public class ReviewFacade {

    @Inject
    ReviewService review;

    /** A committed forecast as the review needs it — plain values, no forecast entities. */
    public record CommittedForecast(long forecastId, long matchId, String league, int season, int matchday, Instant createdAt,
                                    double homeWin, double draw, double awayWin, double confidence,
                                    int expectedHomeGoals, int expectedAwayGoals, boolean backtest, Boolean contrarian) {
    }

    /** Spec 03, step 4: the retrospective for an upcoming match as German text; empty when the data is too thin. */
    public Optional<String> retrospectiveFor(long matchId) {
        return review.retrospectiveFor(matchId).map(Retrospective::summary);
    }

    /** Spec 05, "Gegen den Strom": whether a forecast's tendency matched the result; empty until the match is evaluated. */
    public Optional<Boolean> tendencyHit(long forecastId) {
        return review.tendencyHit(forecastId);
    }

    /** Spec 02 -> 03: the forecast feature hands over every committed forecast. Idempotent. */
    public void forecastCommitted(CommittedForecast f) {
        review.recordForecast(f.forecastId(), f.matchId(), f.league(), f.season(), f.matchday(), f.createdAt(),
                f.homeWin(), f.draw(), f.awayWin(), f.confidence(), f.expectedHomeGoals(), f.expectedAwayGoals(), f.backtest(), f.contrarian());
    }
}
