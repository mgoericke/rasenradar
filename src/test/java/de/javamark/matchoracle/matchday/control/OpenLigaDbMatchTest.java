package de.javamark.matchoracle.matchday.control;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Two traps in the source's result data. It publishes a "full time" entry with the live,
 * still-changing score well before a match is finished — a score must not be read from it
 * until then. And the entry it calls "Endergebnis" is a summary whose meaning changes from
 * edition to edition; the scores after 90 minutes, after extra time and after a shootout
 * have their own entries and are the ones to read (spec 06).
 */
class OpenLigaDbMatchTest {

    @Test
    void aLiveMatchWithAnInterimSummaryEntryHasNoScoreYet() {
        OpenLigaDbMatch match = match(false, List.of(
                new OpenLigaDbMatch.Result(OpenLigaDbMatch.Result.HALF_TIME, 1, 0),
                new OpenLigaDbMatch.Result(OpenLigaDbMatch.Result.SUMMARY, 2, 0)));

        assertTrue(match.ninetyMinuteResult().isEmpty(), "a live match must not report a final score");
    }

    @Test
    void aFinishedMatchReportsItsScore() {
        OpenLigaDbMatch match = match(true, List.of(
                new OpenLigaDbMatch.Result(OpenLigaDbMatch.Result.HALF_TIME, 1, 0),
                new OpenLigaDbMatch.Result(OpenLigaDbMatch.Result.SUMMARY, 2, 1)));

        var result = match.ninetyMinuteResult();
        assertTrue(result.isPresent());
        assertEquals(2, result.get().pointsTeam1());
        assertEquals(1, result.get().pointsTeam2());
    }

    @Test
    void aMatchNotYetKickedOffHasNoScore() {
        OpenLigaDbMatch match = match(false, List.of());

        assertTrue(match.ninetyMinuteResult().isEmpty());
    }

    @Test
    void theNinetyMinuteScoreIsReadFromItsOwnEntryNotFromTheSummary() {
        // SV Sandhausen - Hannover 96, DFB-Pokal 2023/24: 3:3 nach 90 Minuten, im
        // Elfmeterschiessen mit 7:5 entschieden. Der Eintrag "Endergebnis" traegt hier 7:5.
        OpenLigaDbMatch match = match(true, List.of(
                new OpenLigaDbMatch.Result(OpenLigaDbMatch.Result.HALF_TIME, 1, 2),
                new OpenLigaDbMatch.Result(OpenLigaDbMatch.Result.SUMMARY, 7, 5),
                new OpenLigaDbMatch.Result(OpenLigaDbMatch.Result.NINETY_MINUTES, 3, 3),
                new OpenLigaDbMatch.Result(OpenLigaDbMatch.Result.EXTRA_TIME, 3, 3),
                new OpenLigaDbMatch.Result(OpenLigaDbMatch.Result.PENALTIES, 7, 5)));

        assertEquals(3, match.ninetyMinuteResult().orElseThrow().pointsTeam1());
        assertEquals(3, match.ninetyMinuteResult().orElseThrow().pointsTeam2());
        assertEquals(7, match.penaltyResult().orElseThrow().pointsTeam1());
        assertEquals(5, match.penaltyResult().orElseThrow().pointsTeam2());
    }

    @Test
    void withoutItsOwnEntryTheSummaryIsTheNinetyMinuteScore() {
        OpenLigaDbMatch match = match(true, List.of(
                new OpenLigaDbMatch.Result(OpenLigaDbMatch.Result.HALF_TIME, 1, 0),
                new OpenLigaDbMatch.Result(OpenLigaDbMatch.Result.SUMMARY, 2, 1)));

        assertEquals(2, match.ninetyMinuteResult().orElseThrow().pointsTeam1());
        assertTrue(match.extraTimeResult().isEmpty());
        assertTrue(match.penaltyResult().isEmpty());
    }

    @Test
    void penaltyShootoutTakersAreNotGoalsOfTheMatch() {
        // Schuetzen eines Elfmeterschiessens tragen keinen Spielzeitpunkt.
        OpenLigaDbMatch match = withGoals(List.of(
                new OpenLigaDbMatch.Goal(1, 0, 23, "Echtes Tor", false, false),
                new OpenLigaDbMatch.Goal(2, 1, null, "Elfmeterschuetze", true, false)));

        assertEquals(1, match.matchGoals().size());
        assertEquals("Echtes Tor", match.matchGoals().get(0).goalGetterName());
    }

    private static OpenLigaDbMatch match(boolean finished, List<OpenLigaDbMatch.Result> results) {
        return new OpenLigaDbMatch(1, 2026, null, null, finished, null, null, null, results, List.of());
    }

    private static OpenLigaDbMatch withGoals(List<OpenLigaDbMatch.Goal> goals) {
        return new OpenLigaDbMatch(1, 2023, null, null, true, null, null, null, List.of(), goals);
    }
}
