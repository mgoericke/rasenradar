package de.javamark.matchoracle.matchday.boundary;

import de.javamark.matchoracle.matchday.control.MatchdaySynchronizer;
import de.javamark.matchoracle.matchday.control.ResultFinalizer;
import de.javamark.matchoracle.matchday.entity.Match;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import java.time.Instant;

/**
 * Spec 01, step 2 and rule 4: periodic change check, then finalization of settled
 * results. The first run happens right at startup (that is how {@code every} works),
 * which also triggers the initial import.
 */
@ApplicationScoped
public class MatchdaySyncScheduler {

    @Inject
    MatchdaySynchronizer synchronizer;

    @Inject
    ResultFinalizer finalizer;

    @Inject
    Event<ResultFinalized> resultFinalized;

    @Inject
    Event<MatchPlayed> matchPlayed;

    @Scheduled(every = "${matchoracle.matchday.sync-interval}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void syncAndFinalize() {
        // announce each newly played match to other features (spec 04's season outlook reacts to it)
        for (Long matchId : synchronizer.syncCurrentMatchdays()) {
            matchPlayed.fire(new MatchPlayed(matchId));
        }
        // announce each newly final result to other features (spec 03 reacts to it)
        for (Match match : finalizer.finalizeDueResults(Instant.now())) {
            resultFinalized.fire(new ResultFinalized(match.id, match.fullTimeScore.home, match.fullTimeScore.away, match.kickoff));
        }
    }
}
