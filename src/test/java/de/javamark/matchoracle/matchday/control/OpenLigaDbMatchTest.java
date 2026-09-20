package de.javamark.matchoracle.matchday.control;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The source publishes a "full time" result entry with the live, still-changing score well
 * before a match is actually finished — finalResult() must not be fooled by that.
 */
class OpenLigaDbMatchTest {

    @Test
    void aLiveMatchWithAnInterimFullTimeEntryHasNoFinalResultYet() {
        OpenLigaDbMatch match = match(false, List.of(
                new OpenLigaDbMatch.Result(OpenLigaDbMatch.Result.HALF_TIME, 1, 0),
                new OpenLigaDbMatch.Result(OpenLigaDbMatch.Result.FULL_TIME, 2, 0)));

        assertTrue(match.finalResult().isEmpty(), "a live match must not report a final result");
    }

    @Test
    void aFinishedMatchReportsItsFullTimeResult() {
        OpenLigaDbMatch match = match(true, List.of(
                new OpenLigaDbMatch.Result(OpenLigaDbMatch.Result.HALF_TIME, 1, 0),
                new OpenLigaDbMatch.Result(OpenLigaDbMatch.Result.FULL_TIME, 2, 1)));

        var result = match.finalResult();
        assertTrue(result.isPresent());
        assertEquals(2, result.get().pointsTeam1());
        assertEquals(1, result.get().pointsTeam2());
    }

    @Test
    void aMatchNotYetKickedOffHasNoFinalResult() {
        OpenLigaDbMatch match = match(false, List.of());

        assertTrue(match.finalResult().isEmpty());
    }

    private static OpenLigaDbMatch match(boolean finished, List<OpenLigaDbMatch.Result> results) {
        return new OpenLigaDbMatch(1, 2026, null, null, finished, null, null, null, results, List.of());
    }
}
