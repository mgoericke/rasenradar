package de.javamark.matchoracle.matchday.boundary;

import de.javamark.matchoracle.matchday.control.CrestFetcher;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Fetches club crests that are missing or due again. Starts a little after the first sync,
 * which is what brings new teams (and their crest URLs) in.
 */
@ApplicationScoped
public class CrestScheduler {

    @Inject
    CrestFetcher fetcher;

    @Scheduled(every = "${matchoracle.matchday.crest-tick}", delayed = "30s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void fetchCrests() {
        fetcher.fetchDue();
    }
}
