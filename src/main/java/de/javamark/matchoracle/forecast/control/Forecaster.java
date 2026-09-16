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
            Du gehst von einem statistischen Ausgangswert für die erwarteten Tore beider Mannschaften aus
            (rein aus den Torschnitten der laufenden Saison berechnet) und passt ihn an, wenn die drei
            Bewertungen (Form, direkte Duelle, Umfeld) etwas anderes nahelegen. Berücksichtige dabei den
            Heimvorteil und den Aufsteiger-Malus aus den Bewertungsmaßstäben - der Ausgangswert kennt sie
            noch nicht.
            Bleib in der Nähe des Ausgangswerts: kleine, begründete Anpassungen statt großer Sprünge. Die
            tatsächlichen Wahrscheinlichkeiten für Heimsieg, Unentschieden und Auswärtssieg sowie das
            wahrscheinlichste Ergebnis werden aus deinen angepassten Torerwartungen berechnet, nicht von dir
            geschätzt.
            Ist eine Bewertung ausgefallen, sei bei der Anpassung zurückhaltender und erwähne den Ausfall.
            Vermerkt das Faktenblatt eine dünne Formdatenbasis (Achtung-Hinweis), stütze dich stärker auf
            Tabellenlage und Heim-/Auswärtsbilanz statt auf die Form - ein oder zwei Saisonspiele sind kein
            verlässlicher Trend.
            Antworte auf Deutsch. Die Begründung wie im Sportteil einer Zeitung: höchstens drei kurze Sätze,
            konkret, ohne Floskeln wie zeigt sich, präsentiert sich oder unterstreicht, und ohne die
            Bewertungen bloß zusammenzufassen.
            Keine Anführungszeichen und keine Zitate im Text. Keine Quoten, keine Wettempfehlungen.
            """)
    @UserMessage("""
            Erstelle die Prognose für diese Begegnung.

            {{facts}}

            Statistischer Ausgangswert:
            {{baseline}}

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
    @Agent(name = "forecaster", description = "Passt den statistischen Ausgangswert an die Bewertungen an", outputKey = "draft")
    ForecastDraft forecast(@V("facts") String facts, @V("baseline") String baseline, @V("parameters") String parameters,
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
