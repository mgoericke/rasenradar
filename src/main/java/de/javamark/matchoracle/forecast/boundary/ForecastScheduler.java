package de.javamark.matchoracle.forecast.boundary;

import de.javamark.matchoracle.forecast.control.ForecastProgress;
import de.javamark.matchoracle.forecast.control.ForecastQueue;
import de.javamark.matchoracle.forecast.entity.Forecast;
import de.javamark.matchoracle.matchday.boundary.MatchdayFacade;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Spec 02, actor "Terminplanung": creates forecasts for the current matchday of both
 * Bundesligas without the viewer triggering it. A match without any attempt yet is
 * forecast right away (also covers gaps after a pause). One with a usable forecast is
 * forecast again once that forecast is older than the refresh interval, so the tendency
 * can reflect a changed situation closer to kickoff. A failed attempt — the run threw,
 * or it produced a forecast without a single working assessment — is retried after the
 * much shorter retry interval instead. Only the last forecast before kickoff counts for
 * display and review (see ReviewService.forecastsToEvaluate). A backtest stays the one
 * forecast kind the viewer triggers directly.
 */
@ApplicationScoped
public class ForecastScheduler {

    private static final List<String> LEAGUES = List.of("bl1", "bl2");

    @Inject
    MatchdayFacade matchday;

    @Inject
    ForecastQueue queue;

    @Inject
    ForecastProgress progress;

    @ConfigProperty(name = "matchoracle.forecast.auto.retry-after")
    Duration retryAfter;

    @ConfigProperty(name = "matchoracle.forecast.auto.refresh-after")
    Duration refreshAfter;

    @ConfigProperty(name = "matchoracle.forecast.auto.max-per-run")
    int maxPerRun;

    @Scheduled(every = "${matchoracle.forecast.auto.check-every}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    @Transactional
    void forecastDueMatches() {
        Instant now = Instant.now();
        int started = 0;
        for (String league : LEAGUES) {
            for (long matchId : matchday.currentUnplayedMatchIds(league)) {
                if (started >= maxPerRun) {
                    return;
                }
                Optional<LastAttempt> last = LastAttempt.of(Forecast.findLatestByMatch(matchId), progress.lastFailureAt(matchId));
                if (isDue(last, now, retryAfter, refreshAfter) && queue.enqueue(matchId)) {
                    started++;
                }
            }
        }
    }

    static boolean isDue(Optional<LastAttempt> last, Instant now, Duration retryAfter, Duration refreshAfter) {
        return last.map(attempt -> attempt.at().isBefore(now.minus(attempt.usable() ? refreshAfter : retryAfter))).orElse(true);
    }

    /** The most recent attempt to forecast a match: a stored forecast or a run that failed after it. */
    record LastAttempt(Instant at, boolean usable) {

        static Optional<LastAttempt> of(Optional<Forecast> lastForecast, Optional<Instant> lastFailedRunAt) {
            Optional<LastAttempt> forecast = lastForecast.map(f -> new LastAttempt(f.createdAt, !f.withoutAnyAssessment()));
            Optional<LastAttempt> failedRun = lastFailedRunAt.map(at -> new LastAttempt(at, false));
            if (forecast.isEmpty()) return failedRun;
            if (failedRun.isEmpty()) return forecast;
            return failedRun.get().at().isAfter(forecast.get().at()) ? failedRun : forecast;
        }
    }
}
