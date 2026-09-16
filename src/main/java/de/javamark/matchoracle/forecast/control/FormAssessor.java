package de.javamark.matchoracle.forecast.control;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.ChatModelSupplier;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/** Spec 02, step 3: "Form" — in what shape are both teams? */
public interface FormAssessor {

    @SystemMessage("""
            Du bist Formbewerter für Fußballspiele der 1. und 2. Bundesliga.
            Du beurteilst ausschließlich die aktuelle Verfassung beider Mannschaften:
            die letzten Spiele dieser Saison, die Heim- und Auswärtsbilanz und die Tabellenlage.
            Direkte Duelle und Umstände außerhalb der Zahlen sind nicht dein Thema.
            Die Spiele stehen neuestes zuerst: das letzte Spiel zählt am meisten, ältere Ergebnisse weniger.
            Mehrere Spiele mit demselben Muster (z. B. drei Siege in Folge) sind ein echter Lauf und zählen entsprechend
            schwer. Ein einzelnes Ergebnis ist dagegen kein Lauf - werte es entsprechend zurückhaltender, besonders wenn
            das Faktenblatt eine dünne Datenbasis vermerkt; dann geben Tabellenlage und Bilanz mehr her als die Form.
            Antworte auf Deutsch. Keine Anführungszeichen und keine Zitate im Text.
            Schreib wie im Sportteil einer Zeitung: höchstens zwei kurze Sätze, konkret, ohne Floskeln wie zeigt sich,
            präsentiert sich, unterstreicht oder spricht für eine klare Tendenz, und ohne die Zahlen
            aus dem Faktenblatt bloß nachzuerzählen. Keine Quoten, keine Wettempfehlungen.
            """)
    @UserMessage("""
            Bewerte die Form vor dieser Begegnung.

            {{facts}}

            Bewertungsmaßstäbe:
            {{parameters}}
            """)
    @Agent(name = "formAssessor", description = "Bewertet die Form beider Mannschaften", outputKey = "formAssessment")
    AssessmentResult assess(@V("facts") String facts, @V("parameters") String parameters);

    /** Hosted model with local fallback, see {@link FallbackChatModel}. */
    @ChatModelSupplier
    static ChatModel chatModel() {
        return FallbackChatModel.resilient();
    }
}
