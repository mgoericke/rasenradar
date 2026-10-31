package de.javamark.matchoracle.review.boundary;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Story r018: the plain sentences beside the 100-dot grids. */
class HundredDotsTest {

    private static ReviewPages.LeagueAccuracy accuracy(int oracle, int baseline) {
        return new ReviewPages.LeagueAccuracy("bl1", "1. Bundesliga", false, 40, 20, oracle, 50, "0.60", 30, List.of(), "{}",
                List.of(new ReviewPages.YardstickRow("KI-Vorschau", oracle, "0.60", true),
                        new ReviewPages.YardstickRow("Basisprognose (Statistik)", baseline, "0.58", false)),
                null, 40, List.of(), 0, 0, List.of(), List.of(), 0, null);
    }

    @Test
    void sayingBehindAsPlainlyAsAhead() {
        assertEquals("Die KI-Vorschau trifft öfter als die reine Statistik.", accuracy(56, 44).hitVerdict());
        assertEquals("Die reine Statistik trifft öfter als die KI-Vorschau.", accuracy(44, 56).hitVerdict());
        assertEquals("KI-Vorschau und reine Statistik treffen gleich oft.", accuracy(50, 50).hitVerdict());
    }

    @Test
    void thinBandsGetNoFigure() {
        assertEquals("niedrige Sicherheit: zu wenige Fälle für eine Aussage (n = 12).",
                new ReviewPages.CalibrationRow("niedrige Sicherheit", 12, null).sentence(30));
        assertEquals("Wenn das System hohe Sicherheit ausweist, trifft die Tendenz in 61 von 100 Fällen.",
                new ReviewPages.CalibrationRow("hohe Sicherheit", 40, 61).sentence(30));
    }
}
