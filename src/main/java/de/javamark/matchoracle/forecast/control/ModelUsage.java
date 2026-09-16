package de.javamark.matchoracle.forecast.control;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Which models answered during the current forecast run. Runs are sequential
 * (see {@link ForecastQueue}), so one shared set per run is enough.
 */
@ApplicationScoped
public class ModelUsage {

    private final Set<String> used = new LinkedHashSet<>();

    public synchronized void reset() {
        used.clear();
    }

    public synchronized void record(String modelName) {
        used.add(modelName);
    }

    /** e.g. "claude-sonnet-5" or "claude-sonnet-5, gemma4:12b (Fallback)". */
    public synchronized String summary() {
        return used.isEmpty() ? "unbekannt" : String.join(", ", used);
    }
}
