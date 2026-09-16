package de.javamark.matchoracle.forecast.control;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;
import jakarta.enterprise.inject.spi.CDI;
import org.jboss.logging.Logger;

import java.util.function.Supplier;

/**
 * Spec 02, rules: if one assessment fails, the forecast is still made and the
 * failure is reported. The declarative {@code @ErrorHandler} is not applied to
 * parallel agents in this version, so each assessor is wrapped in a plain Java
 * agent that turns an exception into a failed {@link AssessmentResult}.
 */
final class ResilientAssessors {

    private static final Logger LOG = Logger.getLogger(ResilientAssessors.class);

    private ResilientAssessors() {
    }

    /** One more try before giving up: most failures are a single unparsable answer, not a dead model. */
    static AssessmentResult guarded(String name, Supplier<AssessmentResult> call) {
        try {
            return call.get();
        } catch (Exception first) { // the agent proxy sneaky-throws checked exceptions (InvocationTargetException)
            LOG.warnf("%s failed, trying once more: %s", name, ForecastService.causeChain(first));
            try {
                return call.get();
            } catch (Exception second) {
                LOG.warnf("%s failed again, forecasting without it: %s", name, ForecastService.causeChain(second));
                return AssessmentResult.failed(rootMessage(second));
            }
        }
    }

    private static String rootMessage(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        String m = t.getMessage();
        return m == null ? t.getClass().getSimpleName() : m.length() > 200 ? m.substring(0, 200) + "…" : m;
    }

    public static class Form {
        @Agent(name = "formAssessment", description = "Bewertet die Form beider Mannschaften (ausfallsicher)", outputKey = "formAssessment")
        public static AssessmentResult assess(@V("facts") String facts, @V("parameters") String parameters) {
            return guarded("Formbewerter", () -> CDI.current().select(FormAssessor.class).get().assess(facts, parameters));
        }
    }

    public static class HeadToHead {
        @Agent(name = "headToHeadAssessment", description = "Bewertet die direkten Duelle (ausfallsicher)", outputKey = "headToHeadAssessment")
        public static AssessmentResult assess(@V("facts") String facts) {
            return guarded("Duellbewerter", () -> CDI.current().select(HeadToHeadAssessor.class).get().assess(facts));
        }
    }

    public static class Context {
        @Agent(name = "contextAssessment", description = "Bewertet das Umfeld (ausfallsicher)", outputKey = "contextAssessment")
        public static AssessmentResult assess(@V("facts") String facts, @V("parameters") String parameters) {
            return guarded("Umfeldbewerter", () -> CDI.current().select(ContextAssessor.class).get().assess(facts, parameters));
        }
    }
}
