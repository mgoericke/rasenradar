package de.javamark.matchoracle.review.boundary;

import de.javamark.matchoracle.matchday.boundary.ResultFinalized;
import de.javamark.matchoracle.review.control.ReviewService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/** Spec 03, step 1: as soon as a result is final, record the situation and evaluate the forecasts. */
@ApplicationScoped
public class ResultFinalizedObserver {

    @Inject
    ReviewService review;

    void onResultFinalized(@Observes ResultFinalized event) {
        review.recordSituation(event.matchId());
        review.evaluate(event.matchId(), event.homeGoals(), event.awayGoals());
    }
}
