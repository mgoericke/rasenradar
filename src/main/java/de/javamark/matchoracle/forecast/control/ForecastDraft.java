package de.javamark.matchoracle.forecast.control;

import dev.langchain4j.model.output.structured.Description;

/**
 * Structured answer of the forecaster agent: an adjustment of the statistical starting
 * point (expected goals), not a freely estimated probability — the actual tendency
 * probabilities, the most likely scoreline and the confidence are computed from this
 * afterwards by the same Poisson model that produced the starting point.
 */
public record ForecastDraft(
        @Description("Angepasste erwartete Tore der Heimmannschaft, als Dezimalzahl, ausgehend vom statistischen Ausgangswert") double expectedHomeGoals,
        @Description("Angepasste erwartete Tore der Gastmannschaft, als Dezimalzahl, ausgehend vom statistischen Ausgangswert") double expectedAwayGoals,
        @Description("Kurzbegründung in höchstens drei kurzen Sätzen auf Deutsch, wie im Sportteil, ohne Floskeln") String reasoning) {
}
