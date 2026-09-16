package de.javamark.matchoracle.forecast.control;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.ChatModelSupplier;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/** Spec 02, step 3: "Direktes Duell" — what does the shared history say? */
public interface HeadToHeadAssessor {

    @SystemMessage("""
            Du bist Duellbewerter für Fußballspiele der 1. und 2. Bundesliga.
            Du beurteilst ausschließlich die bisherigen direkten Begegnungen der beiden Mannschaften.
            Wenn es keine oder nur sehr wenige Duelle gibt, sag das deutlich und setze die Sicherheit niedrig an.
            Form und Umstände außerhalb der Duelle sind nicht dein Thema.
            Antworte auf Deutsch. Keine Anführungszeichen und keine Zitate im Text.
            Schreib wie im Sportteil einer Zeitung: höchstens zwei kurze Sätze, konkret, ohne Floskeln wie zeigt sich,
            präsentiert sich, unterstreicht oder spricht für eine klare Tendenz, und ohne die Zahlen
            aus dem Faktenblatt bloß nachzuerzählen. Keine Quoten, keine Wettempfehlungen.
            """)
    @UserMessage("""
            Bewerte die direkten Duelle vor dieser Begegnung.

            {{facts}}
            """)
    @Agent(name = "headToHeadAssessor", description = "Bewertet die bisherigen direkten Duelle", outputKey = "headToHeadAssessment")
    AssessmentResult assess(@V("facts") String facts);

    /** Hosted model with local fallback, see {@link FallbackChatModel}. */
    @ChatModelSupplier
    static ChatModel chatModel() {
        return FallbackChatModel.resilient();
    }
}
