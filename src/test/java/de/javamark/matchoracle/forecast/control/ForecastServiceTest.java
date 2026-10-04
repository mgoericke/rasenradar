package de.javamark.matchoracle.forecast.control;

import de.javamark.matchoracle.forecast.entity.Outcome;
import de.javamark.matchoracle.matchday.boundary.ScorelineForecast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Spec 02, rules: confidence is the probability of the tendency, lowered per failed assessment, within 0..1. */
class ForecastServiceTest {

    /** Dortmund - Bremen, 5th matchday 2026/27: a 2:0 as the likeliest scoreline, a home win as the tendency. */
    private static ScorelineForecast estimate(double homeWin, double draw, double awayWin, double scoreProbability) {
        return new ScorelineForecast(1.9, 0.6, 2, 0, scoreProbability, homeWin, draw, awayWin);
    }

    /**
     * The confidence is the tendency's probability, not the likeliest scoreline's — those sit next to
     * each other in the estimate, and the scoreline's is a fraction of the tendency's.
     */
    @Test
    void confidenceIsTheProbabilityOfTheTendencyNotOfTheScoreline() {
        assertEquals(0.67, ForecastService.confidence(estimate(0.67, 0.19, 0.14, 0.11), 0), 1e-9);
        assertEquals(0.52, ForecastService.confidence(estimate(0.24, 0.24, 0.52, 0.09), 0), 1e-9);
        assertEquals(0.40, ForecastService.confidence(estimate(0.30, 0.40, 0.30, 0.12), 0), 1e-9);
    }

    @Test
    void eachFailedAssessmentLowersTheConfidence() {
        assertEquals(0.6, ForecastService.confidence(estimate(0.6, 0.25, 0.15, 0.1), 0), 1e-9);
        assertEquals(0.6 * 0.75, ForecastService.confidence(estimate(0.6, 0.25, 0.15, 0.1), 1), 1e-9);
        assertEquals(0.6 * 0.75 * 0.75, ForecastService.confidence(estimate(0.6, 0.25, 0.15, 0.1), 2), 1e-9);
    }

    @Test
    void forecastsAreSupportedOnlyForTheTwoBundesligasForNow() {
        assertTrue(ForecastService.supportsForecast("bl1"));
        assertTrue(ForecastService.supportsForecast("bl2"));
        assertFalse(ForecastService.supportsForecast("ucl"));
    }

    @Test
    void confidenceIsClampedToTheUnitInterval() {
        assertEquals(1.0, ForecastService.confidence(estimate(1.4, 0.0, 0.0, 0.2), 0), 1e-9);
        assertEquals(0.0, ForecastService.confidence(estimate(-0.1, -0.2, -0.3, 0.0), 0), 1e-9);
    }

    /** Spec 05, "Gegen den Strom": the forecast's tendency must diverge from the baseline's to count as contrarian. */
    @Test
    void contrarianOnlyWhenTendenciesDiffer() {
        assertFalse(ForecastService.contrarian(Outcome.HOME_WIN, Outcome.HOME_WIN));
        assertTrue(ForecastService.contrarian(Outcome.HOME_WIN, Outcome.AWAY_WIN));
        assertTrue(ForecastService.contrarian(Outcome.DRAW, Outcome.HOME_WIN));
    }
}
