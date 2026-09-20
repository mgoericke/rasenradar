package de.javamark.matchoracle.forecast.control;

import de.javamark.matchoracle.forecast.entity.Outcome;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Spec 02, rules: confidence is the probability of the tendency, lowered per failed assessment, within 0..1. */
class ForecastServiceTest {

    @Test
    void eachFailedAssessmentLowersTheConfidence() {
        assertEquals(0.6, ForecastService.confidence(0.6, 0), 1e-9);
        assertEquals(0.6 * 0.75, ForecastService.confidence(0.6, 1), 1e-9);
        assertEquals(0.6 * 0.75 * 0.75, ForecastService.confidence(0.6, 2), 1e-9);
    }

    @Test
    void forecastsAreSupportedOnlyForTheTwoBundesligasForNow() {
        assertTrue(ForecastService.supportsForecast("bl1"));
        assertTrue(ForecastService.supportsForecast("bl2"));
        assertFalse(ForecastService.supportsForecast("ucl"));
    }

    @Test
    void confidenceIsClampedToTheUnitInterval() {
        assertEquals(1.0, ForecastService.confidence(1.4, 0), 1e-9);
        assertEquals(0.0, ForecastService.confidence(-0.1, 0), 1e-9);
    }

    /** Spec 05, "Gegen den Strom": the forecast's tendency must diverge from the baseline's to count as contrarian. */
    @Test
    void contrarianOnlyWhenTendenciesDiffer() {
        assertFalse(ForecastService.contrarian(Outcome.HOME_WIN, Outcome.HOME_WIN));
        assertTrue(ForecastService.contrarian(Outcome.HOME_WIN, Outcome.AWAY_WIN));
        assertTrue(ForecastService.contrarian(Outcome.DRAW, Outcome.HOME_WIN));
    }
}
