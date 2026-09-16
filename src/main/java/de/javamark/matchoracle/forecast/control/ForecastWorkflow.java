package de.javamark.matchoracle.forecast.control;

import de.javamark.matchoracle.forecast.entity.Verdict;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.ExitCondition;
import dev.langchain4j.agentic.declarative.LoopAgent;
import dev.langchain4j.agentic.declarative.ParallelAgent;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;
import dev.langchain4j.service.V;

/**
 * Spec 02, steps 3-6 as one declarative workflow:
 * three assessors in parallel, then forecaster and reviewer in a loop of at most
 * two rounds — i.e. exactly one revision if the reviewer says REVISE.
 */
public interface ForecastWorkflow {

    /** The three assessors in parallel, each wrapped so a failure yields a failed assessment instead of aborting. */
    interface Assessors {
        @ParallelAgent(outputKey = "assessments",
                subAgents = {ResilientAssessors.Form.class, ResilientAssessors.HeadToHead.class, ResilientAssessors.Context.class})
        void assess(@V("facts") String facts, @V("parameters") String parameters);
    }

    /** Plain Java agent between the rounds: hands the reviewer's objection to the next draft. One @Agent method per class. */
    class ReviewNoteWriter {
        @Agent(description = "Übergibt die Anmerkung des Prüfers an den nächsten Entwurf", outputKey = "reviewNote")
        public static String reviewNote(@V("verdict") ReviewVerdict verdict) {
            return verdict.verdict() == Verdict.REVISE ? verdict.reason() : "";
        }
    }

    /**
     * The exit condition is evaluated after every sub-agent, so "verdict" must exist
     * before the reviewer has spoken: the caller seeds it with {@link #NOT_REVIEWED}.
     * Whether a forecast was revised follows from a non-empty "reviewNote".
     */
    interface ReviewLoop {
        @LoopAgent(outputKey = "draft", maxIterations = 2, subAgents = {Forecaster.class, Reviewer.class, ReviewNoteWriter.class})
        ForecastDraft forecastAndReview(@V("facts") String facts);

        @ExitCondition
        static boolean accepted(@V("verdict") ReviewVerdict verdict) {
            return verdict != null && verdict.verdict() == Verdict.ACCEPTED;
        }
    }

    ReviewVerdict NOT_REVIEWED = new ReviewVerdict(Verdict.REVISE, "");

    @SequenceAgent(outputKey = "draft", subAgents = {Assessors.class, ReviewLoop.class})
    ResultWithAgenticScope<ForecastDraft> run(@V("facts") String facts, @V("baseline") String baseline, @V("parameters") String parameters,
                                              @V("retrospective") String retrospective, @V("reviewNote") String reviewNote,
                                              @V("verdict") ReviewVerdict verdict);
}
