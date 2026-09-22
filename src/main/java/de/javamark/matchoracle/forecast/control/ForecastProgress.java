package de.javamark.matchoracle.forecast.control;

import dev.langchain4j.observability.api.event.AiServiceCompletedEvent;
import dev.langchain4j.observability.api.event.AiServiceErrorEvent;
import dev.langchain4j.observability.api.event.AiServiceResponseReceivedEvent;
import dev.langchain4j.observability.api.event.AiServiceStartedEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spec 02, rules: the viewer sees which step is working. Forecasts run one at a
 * time, so the AI service events are attributed to the run currently in progress.
 */
@ApplicationScoped
public class ForecastProgress {

    public enum State { WAITING, RUNNING, DONE, FAILED }

    public record Step(String agent, String label, State state) {
    }

    /** Token usage of a run — the basis for estimating what a hosted model would cost. */
    public record Tokens(long input, long output, int calls) {
        Tokens plus(Integer in, Integer out) {
            return new Tokens(input + (in == null ? 0 : in), output + (out == null ? 0 : out), calls + 1);
        }
    }

    /** {@code finishedAt} is null while the run is in progress. */
    public record Run(long matchId, Map<String, State> steps, State state, String error, Long forecastId, Tokens tokens, Instant finishedAt) {
        public List<Step> stepList() {
            return steps.entrySet().stream().map(e -> new Step(e.getKey(), LABELS.get(e.getKey()), e.getValue())).toList();
        }
    }

    private static final Map<String, String> LABELS = new LinkedHashMap<>();
    static {
        LABELS.put("FormAssessor", "Formbewerter");
        LABELS.put("HeadToHeadAssessor", "Duellbewerter");
        LABELS.put("ContextAssessor", "Umfeldbewerter");
        LABELS.put("Forecaster", "Prognostiker");
        LABELS.put("Reviewer", "Prüfer");
    }

    private final Map<Long, Run> runs = new ConcurrentHashMap<>();
    private volatile Long current;

    public void start(long matchId) {
        Map<String, State> steps = Collections.synchronizedMap(new LinkedHashMap<>());
        LABELS.keySet().forEach(k -> steps.put(k, State.WAITING));
        runs.put(matchId, new Run(matchId, steps, State.RUNNING, null, null, new Tokens(0, 0, 0), null));
        current = matchId;
    }

    public void done(long matchId, long forecastId) {
        runs.computeIfPresent(matchId, (k, r) -> new Run(k, r.steps(), State.DONE, null, forecastId, r.tokens(), Instant.now()));
        current = null;
    }

    public void failed(long matchId, String error) {
        runs.computeIfPresent(matchId, (k, r) -> new Run(k, r.steps(), State.FAILED, error, null, r.tokens(), Instant.now()));
        current = null;
    }

    public Optional<Run> of(long matchId) {
        return Optional.ofNullable(runs.get(matchId));
    }

    /** When the last run for this match failed — since the last restart; a failure before that is simply not known. */
    public Optional<Instant> lastFailureAt(long matchId) {
        return of(matchId).filter(r -> r.state() == State.FAILED).map(Run::finishedAt);
    }

    public boolean isRunning(long matchId) {
        return of(matchId).map(r -> r.state() == State.RUNNING).orElse(false);
    }

    void started(@Observes AiServiceStartedEvent event) {
        mark(event.invocationContext().interfaceName(), State.RUNNING);
    }

    void completed(@Observes AiServiceCompletedEvent event) {
        mark(event.invocationContext().interfaceName(), State.DONE);
    }

    void responseReceived(@Observes AiServiceResponseReceivedEvent event) {
        Long matchId = current;
        if (matchId == null || event.response() == null || event.response().tokenUsage() == null) {
            return;
        }
        var usage = event.response().tokenUsage();
        runs.computeIfPresent(matchId, (k, r) -> new Run(k, r.steps(), r.state(), r.error(), r.forecastId(),
                r.tokens().plus(usage.inputTokenCount(), usage.outputTokenCount()), r.finishedAt()));
    }

    void error(@Observes AiServiceErrorEvent event) {
        mark(event.invocationContext().interfaceName(), State.FAILED);
    }

    private void mark(String interfaceName, State state) {
        Long matchId = current;
        if (matchId == null || interfaceName == null) {
            return;
        }
        String agent = interfaceName.substring(interfaceName.lastIndexOf('.') + 1);
        Run run = runs.get(matchId);
        if (run != null && run.steps().containsKey(agent)) {
            run.steps().put(agent, state);
        }
    }
}
