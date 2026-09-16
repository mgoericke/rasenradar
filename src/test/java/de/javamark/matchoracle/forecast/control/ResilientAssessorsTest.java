package de.javamark.matchoracle.forecast.control;

import de.javamark.matchoracle.forecast.entity.Outcome;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Spec 02, rules: a failed assessor does not abort the forecast — but a single broken answer earns one more try. */
class ResilientAssessorsTest {

    private final AssessmentResult ok = new AssessmentResult(Outcome.HOME_WIN, 0.6, "Form spricht für die Gastgeber.");

    @Test
    void retriesOnceAfterABrokenAnswer() {
        AtomicInteger calls = new AtomicInteger();
        AssessmentResult result = ResilientAssessors.guarded("Formbewerter", () -> {
            if (calls.incrementAndGet() == 1) throw new IllegalStateException("Failed to parse");
            return ok;
        });
        assertEquals(ok, result);
        assertEquals(2, calls.get());
    }

    @Test
    void givesUpAfterTheSecondFailure() {
        AtomicInteger calls = new AtomicInteger();
        AssessmentResult result = ResilientAssessors.guarded("Formbewerter", () -> {
            calls.incrementAndGet();
            throw new IllegalStateException("Failed to parse");
        });
        assertTrue(result.isFailed());
        assertEquals(2, calls.get());
    }

    @Test
    void causeChainDropsTheBase64CopyOfTheModelAnswer() {
        var e = new IllegalStateException("Failed to parse \"{\"lean\":\"HOME_WIN\"“}“}\" (base64: \"eyJsZWFuIjoiSE9NRV9XSU4i\") into AssessmentResult");
        String chain = ForecastService.causeChain(e);
        assertFalse(chain.contains("base64"), chain);
        assertTrue(chain.contains("into AssessmentResult"), chain);
    }
}
