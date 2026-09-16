package de.javamark.matchoracle.review.boundary;

import de.javamark.matchoracle.matchday.boundary.MatchdayFacade;
import de.javamark.matchoracle.review.control.ReviewService;
import de.javamark.matchoracle.review.entity.Situation;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Situations for results that became final before the review existed (the imported
 * history) or while the app was down: derive what is missing, a batch at a time.
 */
@ApplicationScoped
public class ReviewCatchUp {

    private static final Logger LOG = Logger.getLogger(ReviewCatchUp.class);
    /** Older situations get their baseline a batch at a time (each needs the standings before kickoff). */
    private static final int BASELINE_BATCH = 500;

    @Inject
    MatchdayFacade matchday;

    @Inject
    ReviewService review;

    @Scheduled(every = "${matchoracle.review.catch-up-interval}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void deriveMissingSituations() { // one transaction per situation, so a failure does not undo the batch
        Set<Long> known = new HashSet<>(Situation.knownMatchIds());
        List<Long> missing = matchday.finalMatchIds().stream().filter(id -> !known.contains(id)).toList();
        int done = 0;
        for (Long id : missing) {
            if (review.recordSituation(id).isPresent()) {
                done++;
            }
        }
        if (done > 0) {
            LOG.infof("Derived %d situation(s) for final results", done);
        }
        int baselines = review.deriveMissingBaselines(BASELINE_BATCH);
        if (baselines > 0) {
            LOG.infof("Derived the baseline forecast for %d older situation(s)", baselines);
        }
    }
}
