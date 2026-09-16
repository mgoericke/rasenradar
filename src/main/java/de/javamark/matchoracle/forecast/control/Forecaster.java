package de.javamark.matchoracle.forecast.control;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.ChatModelSupplier;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/** Spec 02, step 4: condenses the three assessments (and the retrospective) into a forecast. */
public interface Forecaster {

    @SystemMessage("""
            Du bist Prognostiker für Fußballspiele der 1. und 2. Bundesliga.
            Du verdichtest drei Bewertungen (Form, direkte Duelle, Umfeld) zu einer Prognose.
            Die drei Wahrscheinlichkeiten für Heimsieg, Unentschieden und Auswärtssieg müssen zusammen genau 1 ergeben.
            Sie drücken deine Sicherheit aus: ein klarer Favorit bekommt eine hohe Wahrscheinlichkeit, ein offenes Spiel
            verteilt sie breit. Berücksichtige den Heimvorteil und den Aufsteiger-Malus aus den Bewertungsmaßstäben.
            Ist eine Bewertung ausgefallen, verteile die Wahrscheinlichkeiten breiter und erwähne den Ausfall.
            Vermerkt das Faktenblatt eine dünne Formdatenbasis (Achtung-Hinweis), stütze dich stärker auf Tabellenlage
            und Heim-/Auswärtsbilanz statt auf die Form - ein oder zwei Saisonspiele sind kein verlässlicher Trend.
            Der erwartete Torverlauf ist ein Richtwert: leite ihn aus den Torschnitten im Faktenblatt ab
            (Tore des Gastgebers zu Hause, Gegentore des Gastes auswärts und umgekehrt), runde auf ganze Tore
            und wähle nicht pauschal 1:1.
            Antworte auf Deutsch. Die Begründung wie im Sportteil einer Zeitung: höchstens drei kurze Sätze, konkret,
            ohne Floskeln wie zeigt sich, präsentiert sich oder unterstreicht, und ohne die Bewertungen bloß zusammenzufassen.
            Keine Anführungszeichen und keine Zitate im Text. Keine Quoten, keine Wettempfehlungen.
            """)
    @UserMessage("""
            Erstelle die Prognose für diese Begegnung.

            {{facts}}

            Bewertungsmaßstäbe:
            {{parameters}}

            Bewertung Form:
            {{formAssessment}}

            Bewertung direkte Duelle:
            {{headToHeadAssessment}}

            Bewertung Umfeld:
            {{contextAssessment}}

            Rückschau auf frühere Begegnungen mit ähnlicher Ausgangslage (andere Vereine, gleiche Konstellation –
            als Basisrate zu lesen, nicht als Aussage über die beteiligten Vereine):
            {{retrospective}}

            Anmerkung des Prüfers zu einem früheren Entwurf (leer, wenn es der erste Entwurf ist):
            {{reviewNote}}
            """)
    @Agent(name = "forecaster", description = "Verdichtet die Bewertungen zu einer Prognose", outputKey = "draft")
    ForecastDraft forecast(@V("facts") String facts, @V("parameters") String parameters,
                           @V("formAssessment") AssessmentResult formAssessment,
                           @V("headToHeadAssessment") AssessmentResult headToHeadAssessment,
                           @V("contextAssessment") AssessmentResult contextAssessment,
                           @V("retrospective") String retrospective,
                           @V("reviewNote") String reviewNote);

    /** Hosted model with local fallback, see {@link FallbackChatModel}. */
    @ChatModelSupplier
    static ChatModel chatModel() {
        return FallbackChatModel.resilient();
    }
}
