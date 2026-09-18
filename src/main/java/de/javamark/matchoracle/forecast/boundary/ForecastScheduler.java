package de.javamark.matchoracle.forecast.boundary;

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
 * Bundesligas without the viewer triggering it. A match without any forecast yet is
 * forecast right away (also covers gaps after a pause); one that already has a forecast
 * is forecast again once that forecast is older than the refresh interval, so the
 * tendency can reflect a changed situation closer to kickoff. Only the last forecast
 * before kickoff counts for display and review (see ReviewService.forecastsToEvaluate).
 * A backtest stays the one forecast kind the viewer triggers directly.
 */
@ApplicationScoped
public class ForecastScheduler {

    private static final List<String> LEAGUES = List.of("bl1", "bl2");

    @Inject
    MatchdayFacade matchday;

    @Inject
    ForecastQueue queue;

    @ConfigProperty(name = "matchoracle.forecast.auto.interval")
    Duration interval;

    @ConfigProperty(name = "matchoracle.forecast.auto.max-per-run")
    int maxPerRun;

    @Scheduled(every = "${matchoracle.forecast.auto.interval}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    @Transactional
    void forecastDueMatches() {
        Instant now = Instant.now();
        int started = 0;
        for (String league : LEAGUES) {
            for (long matchId : matchday.currentUnplayedMatchIds(league)) {
                if (started >= maxPerRun) {
                    return;
                }
                Optional<Instant> lastForecast = Forecast.findLatestByMatch(matchId).map(f -> f.createdAt);
                if (isDue(lastForecast, now, interval) && queue.enqueue(matchId)) {
                    started++;
                }
            }
        }
    }

    static boolean isDue(Optional<Instant> lastForecastCreatedAt, Instant now, Duration interval) {
        return lastForecastCreatedAt.map(createdAt -> createdAt.isBefore(now.minus(interval))).orElse(true);
    }
}
