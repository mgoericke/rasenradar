package de.javamark.matchoracle.forecast.control;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.ChatModelSupplier;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/** Spec 02, step 5: checks the draft for contradictions to the assessments and for overconfident adjustments. */
public interface Reviewer {

    @SystemMessage("""
            Du bist Prüfer für Fußballprognosen. Du prüfst einen Prognose-Entwurf auf zwei Dinge:
            1. Widersprüche zu den drei Bewertungen (z. B. alle Bewertungen sehen den Gast vorn, der Entwurf
               erhöht aber die erwarteten Tore der Heimmannschaft).
            2. Unangemessen große Abweichung vom statistischen Ausgangswert, die die Bewertungen nicht hergeben.
            Kleinere Anpassungen sind kein Grund für REVISE. Urteile ACCEPTED, wenn die Anpassung vertretbar ist.
            Antworte auf Deutsch. Keine Anführungszeichen und keine Zitate im Text.
            """)
    @UserMessage("""
            Prüfe diesen Prognose-Entwurf.

            Statistischer Ausgangswert:
            {{baseline}}

            Entwurf:
            {{draft}}

            Bewertung Form:
            {{formAssessment}}

            Bewertung direkte Duelle:
            {{headToHeadAssessment}}

            Bewertung Umfeld:
            {{contextAssessment}}
            """)
    @Agent(name = "reviewer", description = "Prüft die Prognose auf Widersprüche und Übermut", outputKey = "verdict")
    ReviewVerdict review(@V("baseline") String baseline, @V("draft") ForecastDraft draft,
                         @V("formAssessment") AssessmentResult formAssessment,
                         @V("headToHeadAssessment") AssessmentResult headToHeadAssessment,
                         @V("contextAssessment") AssessmentResult contextAssessment);

    /** Hosted model with local fallback, see {@link FallbackChatModel}. */
    @ChatModelSupplier
    static ChatModel chatModel() {
        return FallbackChatModel.resilient();
    }
}
