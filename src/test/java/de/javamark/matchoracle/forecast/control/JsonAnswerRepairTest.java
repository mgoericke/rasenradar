package de.javamark.matchoracle.forecast.control;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Seen in production and reproduced locally: the hosted model closes the German summary
 * with a typographic quote, so under schema-constrained decoding the JSON string never
 * ends — the model then pads with invisible characters until the token limit. The answer
 * is salvageable, and salvaging it is far cheaper than a retry that fails the same way.
 */
class JsonAnswerRepairTest {

    private static final String VALID = "{\"lean\":\"HOME_WIN\",\"confidence\":0.55,\"summary\":\"Schalke gewann vier der letzten sechs Duelle.\"}";

    @Test
    void aValidAnswerIsReturnedUntouched() {
        assertSame(VALID, JsonAnswerRepair.repair(VALID));
    }

    @Test
    void aTypographicClosingQuoteWithTrailingPaddingIsRepaired() {
        String broken = "{\"lean\":\"HOME_WIN\",\"confidence\":0.55,\"summary\":\"Schalke gewann vier der letzten sechs Duelle.“}"
                + "         (Hinweis: Bitte ignoriere diesen letzten Satz, er gehört nicht zur Antwort.)  "
                + "​ ​ ​ ​ ​ ​ ​ ​";

        assertEquals(VALID, JsonAnswerRepair.repair(broken));
    }

    @Test
    void aTypographicQuoteFollowedByNewTextInsteadOfTheClosingBraceIsRepairedToo() {
        String broken = "{\"lean\":\"HOME_WIN\",\"confidence\":0.35,\"summary\":\"Beide Teams sind gleich erholt.\u201c E"
                + "\u200b \u200b \u200b \u200b";

        assertEquals("{\"lean\":\"HOME_WIN\",\"confidence\":0.35,\"summary\":\"Beide Teams sind gleich erholt.\"}",
                JsonAnswerRepair.repair(broken));
    }

    @Test
    void aTypographicQuoteInsideTheTextStaysAsItIs() {
        String withQuoteInText = "{\"lean\":\"DRAW\",\"confidence\":0.4,\"summary\":\"Ein „Lauf“ ist das nicht.\"}";

        assertSame(withQuoteInText, JsonAnswerRepair.repair(withQuoteInText));
    }

    @Test
    void anAnswerThatCannotBeRepairedIsReturnedUntouched() {
        String hopeless = "{\"lean\":\"HOME_WIN\",\"confidence\":";

        assertSame(hopeless, JsonAnswerRepair.repair(hopeless));
    }
}
