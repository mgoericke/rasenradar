package de.javamark.matchoracle.forecast.control;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.ChatModelSupplier;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/** Spec 02, step 3: "Umfeld" — for now only what the match data reveals (workload, promoted teams, kickoff, season phase). */
public interface ContextAssessor {

    @SystemMessage("""
            Du bist Umfeldbewerter für Fußballspiele der 1. und 2. Bundesliga.
            Du beurteilst nur die Umstände rund um die Begegnung, die aus den Spieldaten hervorgehen:
            Belastung durch dichte Spielfolge, Aufsteiger-Status, Anstoßzeit und Wochentag, Saisonphase.
            Du hast keinen Zugang zu Nachrichten; spekuliere nicht über Verletzungen oder Trainerwechsel.
            Form und direkte Duelle sind nicht dein Thema.
            Antworte auf Deutsch. Keine Anführungszeichen und keine Zitate im Text.
            Schreib wie im Sportteil einer Zeitung: höchstens zwei kurze Sätze, konkret, ohne Floskeln wie zeigt sich,
            präsentiert sich, unterstreicht oder spricht für eine klare Tendenz, und ohne die Zahlen
            aus dem Faktenblatt bloß nachzuerzählen. Keine Quoten, keine Wettempfehlungen.
            """)
    @UserMessage("""
            Bewerte das Umfeld dieser Begegnung.

            {{facts}}

            Bewertungsmaßstäbe:
            {{parameters}}
            """)
    @Agent(name = "contextAssessor", description = "Bewertet das Umfeld der Begegnung", outputKey = "contextAssessment")
    AssessmentResult assess(@V("facts") String facts, @V("parameters") String parameters);

    /** Hosted model with local fallback, see {@link FallbackChatModel}. */
    @ChatModelSupplier
    static ChatModel chatModel() {
        return FallbackChatModel.resilient();
    }
}
