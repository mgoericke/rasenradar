package de.javamark.matchoracle.forecast.control;

import de.javamark.matchoracle.forecast.entity.Verdict;
import dev.langchain4j.model.output.structured.Description;

/** Structured answer of the reviewer agent. */
public record ReviewVerdict(
        @Description("ACCEPTED, wenn die Prognose zu den Bewertungen passt und die Sicherheit angemessen ist; sonst REVISE") Verdict verdict,
        @Description("Begründung in höchstens zwei kurzen Sätzen auf Deutsch; bei REVISE konkret, was zu ändern ist") String reason) {
}
