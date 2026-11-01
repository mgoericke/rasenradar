package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.control.TensionCalculator.Side;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Story r073: close in points, both near the top or bottom and both on form make a match tense. */
class TensionCalculatorTest {

    @Test
    void closeTopSidesOnFormGetAllFiveStepsFarApartOnesFewer() {
        int top = TensionCalculator.of(new Side(2, 20, 10), new Side(4, 18, 9), 18);
        int apart = TensionCalculator.of(new Side(2, 20, 10), new Side(12, 9, 4), 18);

        assertEquals(5, top);
        assertTrue(apart < top);
        assertEquals(1, apart);
    }

    @Test
    void theBoundariesCountAsTheStoryNamesThem() {
        // nine points from five count as on form, eight do not
        assertEquals(5, TensionCalculator.of(new Side(1, 10, 9), new Side(6, 10, 9), 18));
        assertEquals(4, TensionCalculator.of(new Side(1, 10, 9), new Side(6, 10, 8), 18));
        // place 6 is in the top six, 7 is not; places 15–18 are the bottom four
        assertEquals(3, TensionCalculator.of(new Side(1, 10, 0), new Side(7, 10, 0), 18));
        assertEquals(4, TensionCalculator.of(new Side(15, 10, 0), new Side(18, 10, 0), 18));
        assertEquals(3, TensionCalculator.of(new Side(14, 10, 0), new Side(18, 10, 0), 18));
        // four to six points apart is one step instead of two
        assertEquals(2, TensionCalculator.of(new Side(8, 16, 0), new Side(10, 10, 0), 18));
    }
}
