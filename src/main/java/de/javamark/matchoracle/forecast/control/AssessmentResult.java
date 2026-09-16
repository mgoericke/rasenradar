package de.javamark.matchoracle.forecast.control;

import de.javamark.matchoracle.forecast.entity.Outcome;
import dev.langchain4j.model.output.structured.Description;

/** Structured answer of one assessor agent. */
public record AssessmentResult(
        @Description("Tendenz, zu der die Fakten aus dieser Perspektive neigen: HOME_WIN, DRAW oder AWAY_WIN") Outcome lean,
        @Description("Wie sicher diese Einschätzung ist, von 0 (reines Raten) bis 1 (sehr sicher)") double confidence,
        @Description("Begründung in höchstens zwei kurzen Sätzen auf Deutsch, wie im Sportteil, ohne Floskeln") String summary) {

    public static AssessmentResult failed(String reason) {
        return new AssessmentResult(null, 0, "Diese Bewertung ist ausgefallen: " + reason);
    }

    public boolean isFailed() {
        return lean == null && confidence == 0;
    }
}
