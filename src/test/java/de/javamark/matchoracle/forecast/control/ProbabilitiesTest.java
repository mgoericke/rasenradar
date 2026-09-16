package de.javamark.matchoracle.forecast.control;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Spec 02, rules: the three probabilities add up to the full certainty. */
class ProbabilitiesTest {

    @Test
    void scalesToOneWhenTheModelIsSloppy() {
        Probabilities p = Probabilities.normalized(0.5, 0.3, 0.3);

        assertEquals(1.0, p.sum(), 1e-9);
        assertEquals(0.5 / 1.1, p.homeWin(), 1e-9);
    }

    @Test
    void negativeValuesCountAsZero() {
        Probabilities p = Probabilities.normalized(-0.2, 0.5, 0.5);

        assertEquals(0.0, p.homeWin());
        assertEquals(1.0, p.sum(), 1e-9);
    }

    @Test
    void allZeroBecomesAnEvenSplit() {
        Probabilities p = Probabilities.normalized(0, 0, 0);

        assertEquals(1.0 / 3, p.draw(), 1e-9);
        assertEquals(1.0, p.sum(), 1e-9);
    }
}
