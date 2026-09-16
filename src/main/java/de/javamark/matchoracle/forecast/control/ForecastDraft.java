package de.javamark.matchoracle.forecast.control;

import dev.langchain4j.model.output.structured.Description;

/** Structured answer of the forecaster agent. */
public record ForecastDraft(
        @Description("Wahrscheinlichkeit für einen Heimsieg, 0 bis 1") double homeWin,
        @Description("Wahrscheinlichkeit für ein Unentschieden, 0 bis 1") double draw,
        @Description("Wahrscheinlichkeit für einen Auswärtssieg, 0 bis 1") double awayWin,
        @Description("Erwartete Tore der Heimmannschaft als ganze Zahl") int expectedHomeGoals,
        @Description("Erwartete Tore der Gastmannschaft als ganze Zahl") int expectedAwayGoals,
        @Description("Kurzbegründung in höchstens drei kurzen Sätzen auf Deutsch, wie im Sportteil, ohne Floskeln") String reasoning) {

    /** The probability of the most likely outcome — the basis of the confidence (computed, not asked from the model). */
    public double tendencyProbability() {
        return Math.max(homeWin, Math.max(draw, awayWin));
    }
}
