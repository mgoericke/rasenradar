package de.javamark.matchoracle.forecast.control;

import de.javamark.matchoracle.matchday.boundary.MatchSituation;
import de.javamark.matchoracle.matchday.boundary.MatchSituation.Balance;
import de.javamark.matchoracle.matchday.boundary.MatchSituation.Result;
import de.javamark.matchoracle.matchday.boundary.MatchSituation.TeamSituation;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spec 02: form from one or two matches is noisy and must not be read like a settled trend —
 * the fact sheet flags it explicitly so the assessors and the forecaster treat it with caution.
 */
class FactSheetTest {

    private static final Balance NO_MATCHES = new Balance(0, 0, 0, 0, 0, 0, 0);
    private static final Instant KICKOFF = Instant.parse("2026-09-20T13:30:00Z");

    @Test
    void flagsFormBasedOnFewerThanThreeMatchesAsThin() {
        String facts = FactSheet.render(situation(team(1)));

        assertTrue(facts.contains("kaum belastbar"), facts);
    }

    @Test
    void doesNotFlagFormOnceThreeOrMoreMatchesArePlayed() {
        String facts = FactSheet.render(situation(team(3)));

        assertFalse(facts.contains("kaum belastbar"), facts);
    }

    @Test
    void doesNotFlagATeamWithNoMatchesAtAllSeparatelyFromTheExistingNote() {
        String facts = FactSheet.render(situation(team(0)));

        assertTrue(facts.contains("noch keine Spiele in dieser Saison"), facts);
        assertFalse(facts.contains("kaum belastbar"), facts);
    }

    private static TeamSituation team(int matches) {
        List<Result> lastMatches = java.util.stream.IntStream.range(0, matches)
                .mapToObj(i -> new Result(KICKOFF, "Gegner " + i, true, 1, 0, "WIN"))
                .toList();
        return new TeamSituation(1, "Testverein", "Test", 5, 6, lastMatches, NO_MATCHES, NO_MATCHES, false, List.of());
    }

    private static MatchSituation situation(TeamSituation home) {
        TeamSituation away = new TeamSituation(2, "Gastverein", "Gast", 8, 3, List.of(), NO_MATCHES, NO_MATCHES, false, List.of());
        return new MatchSituation(1, "bl1", 2026, 4, KICKOFF, false, null, false, home, away, List.of(), false);
    }
}
