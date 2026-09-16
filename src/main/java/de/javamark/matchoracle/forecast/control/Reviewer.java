package de.javamark.matchoracle.forecast.control;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.ChatModelSupplier;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/** Spec 02, step 5: checks the draft for contradictions to the assessments and for overconfidence. */
public interface Reviewer {

    @SystemMessage("""
            Du bist Prüfer für Fußballprognosen. Du prüfst einen Prognose-Entwurf auf zwei Dinge:
            1. Widersprüche zu den drei Bewertungen (z. B. alle Bewertungen sehen den Gast vorn, die Prognose den Heimsieg).
            2. Unangemessen hohe Sicherheit (eine Einzelwahrscheinlichkeit, die die Fakten nicht hergeben, etwa 0.75 für einen
               Heimsieg, obwohl die Bewertungen uneins sind).
            Kleinere Abweichungen sind kein Grund für REVISE. Urteile ACCEPTED, wenn die Prognose vertretbar ist.
            Antworte auf Deutsch. Keine Anführungszeichen und keine Zitate im Text.
            """)
    @UserMessage("""
            Prüfe diesen Prognose-Entwurf.

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
    ReviewVerdict review(@V("draft") ForecastDraft draft,
                         @V("formAssessment") AssessmentResult formAssessment,
                         @V("headToHeadAssessment") AssessmentResult headToHeadAssessment,
                         @V("contextAssessment") AssessmentResult contextAssessment);

    /** Hosted model with local fallback, see {@link FallbackChatModel}. */
    @ChatModelSupplier
    static ChatModel chatModel() {
        return FallbackChatModel.resilient();
    }
}
