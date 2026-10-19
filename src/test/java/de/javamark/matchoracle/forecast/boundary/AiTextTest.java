package de.javamark.matchoracle.forecast.boundary;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Stored model output, as seen on the public forecast pages (spec 02: "in verständlicher Sprache"). */
class AiTextTest {

    @Test
    void breakMarkupDoesNotReachTheReader() {
        assertEquals("Dortmund ist makellos in Form.", AiText.forDisplay("Dortmund ist makellos in Form.</br>"));
        assertEquals("Erster Satz. Zweiter Satz.", AiText.forDisplay("Erster Satz.<br/>Zweiter Satz."));
        assertEquals("Die Tendenz geht zum Heimspiel.", AiText.forDisplay("Die Tendenz geht zum Heimspiel. </summary>"));
    }

    @Test
    void outcomeConstantsReadAsGerman() {
        assertEquals("Die Bewertungen stützen klar Heimsieg mit überwiegend hoher Sicherheit.",
                AiText.forDisplay("Die Bewertungen stützen klar HOME_WIN mit überwiegend hoher Konfidenz."));
        assertEquals("Auswärtssieg oder Unentschieden", AiText.forDisplay("AWAY_WIN oder DRAW"));
    }

    @Test
    void aRetrospectiveLineLosesTheModelsOwnBullet() {
        assertEquals("1. FC Union Berlin gewann zuletzt zweimal.", AiText.listItem("- 1. FC Union Berlin gewann zuletzt zweimal."));
    }

    @Test
    void aStoredRetrospectiveNoLongerAddressesTheModel() {
        assertEquals("Sie gelten als Basisrate für diese Konstellation.",
                AiText.forDisplay("Lies sie als Basisrate für diese Konstellation."));
    }

    @Test
    void noTextStaysNoText() {
        assertNull(AiText.forDisplay(null));
    }
}
