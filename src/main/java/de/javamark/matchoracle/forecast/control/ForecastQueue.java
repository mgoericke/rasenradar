package de.javamark.matchoracle.forecast.control;

import io.quarkus.runtime.ShutdownEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Runs forecasts one after another on a single background thread — a local
 * model cannot serve several workflows at once anyway, and sequential runs keep
 * the progress display unambiguous.
 */
@ApplicationScoped
public class ForecastQueue {

    private static final Logger LOG = Logger.getLogger(ForecastQueue.class);

    @Inject
    ForecastService service;

    @Inject
    ForecastProgress progress;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "forecast-queue");
        t.setDaemon(true);
        return t;
    });
    private final Set<Long> queued = ConcurrentHashMap.newKeySet();

    /** Queues a live forecast for the match unless one is already queued or running. Returns false if it was. */
    public boolean enqueue(long matchId) {
        return enqueue(matchId, false);
    }

    /** Queues a live forecast or a backtest. */
    public boolean enqueue(long matchId, boolean backtest) {
        if (!queued.add(matchId)) {
            return false;
        }
        executor.submit(() -> {
            try {
                service.forecast(matchId, backtest);
            } catch (RuntimeException e) {
                LOG.warnf("Forecast for match %d failed: %s", matchId, e.getMessage());
            } finally {
                queued.remove(matchId);
            }
        });
        return true;
    }

    public boolean isQueuedOrRunning(long matchId) {
        return queued.contains(matchId) || progress.isRunning(matchId);
    }

    void shutdown(@Observes ShutdownEvent event) {
        executor.shutdownNow();
    }
}
