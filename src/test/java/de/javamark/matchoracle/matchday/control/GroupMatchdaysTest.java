package de.javamark.matchoracle.matchday.control;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spec 06, Regeln: die Quelle nennt für einen Gruppenwettbewerb nur die Gruppe.
 * Begegnungen derselben Gruppe am selben Kalendertag bilden einen Spieltag.
 */
class GroupMatchdaysTest {

    @Test
    void matchesOnTheSameDayFormOneMatchday() {
        List<OpenLigaDbMatch> group = List.of(
                match("2026-09-25T18:45:00Z"), match("2026-09-25T20:45:00Z"),
                match("2026-09-28T20:45:00Z"), match("2026-09-28T18:45:00Z"));

        Map<Integer, List<OpenLigaDbMatch>> byMatchday = GroupMatchdays.byMatchday(group);

        assertEquals(2, byMatchday.size());
        assertEquals(2, byMatchday.get(1).size());
        assertEquals(2, byMatchday.get(2).size());
    }

    @Test
    void theEarliestDayIsTheFirstMatchdayRegardlessOfInputOrder() {
        List<OpenLigaDbMatch> group = List.of(
                match("2026-11-15T20:45:00Z"), match("2026-09-25T18:45:00Z"), match("2026-10-02T20:45:00Z"));

        Map<Integer, List<OpenLigaDbMatch>> byMatchday = GroupMatchdays.byMatchday(group);

        assertEquals(List.of(1, 2, 3), List.copyOf(byMatchday.keySet()));
        assertEquals(Instant.parse("2026-09-25T18:45:00Z"), byMatchday.get(1).get(0).kickoff());
        assertEquals(Instant.parse("2026-11-15T20:45:00Z"), byMatchday.get(3).get(0).kickoff());
    }

    @Test
    void aWholeNationsLeagueGroupYieldsSixMatchdaysOfTwoMatches() {
        List<OpenLigaDbMatch> group = List.of(
                match("2026-09-25T18:45:00Z"), match("2026-09-25T20:45:00Z"),
                match("2026-09-28T18:45:00Z"), match("2026-09-28T20:45:00Z"),
                match("2026-10-02T18:45:00Z"), match("2026-10-02T20:45:00Z"),
                match("2026-10-05T18:45:00Z"), match("2026-10-05T20:45:00Z"),
                match("2026-11-12T18:45:00Z"), match("2026-11-12T20:45:00Z"),
                match("2026-11-15T18:45:00Z"), match("2026-11-15T20:45:00Z"));

        Map<Integer, List<OpenLigaDbMatch>> byMatchday = GroupMatchdays.byMatchday(group);

        assertEquals(6, byMatchday.size());
        assertTrue(byMatchday.values().stream().allMatch(day -> day.size() == 2));
    }

    @Test
    void anEmptyGroupYieldsNoMatchdays() {
        assertTrue(GroupMatchdays.byMatchday(List.of()).isEmpty());
    }

    /** Only the kickoff matters here; every other component of the source record stays null/empty. */
    private static OpenLigaDbMatch match(String kickoff) {
        return new OpenLigaDbMatch(0, 2026, Instant.parse(kickoff), null, true,
                new OpenLigaDbMatch.Group(1, "Gruppe A"), null, null, List.of(), List.of());
    }
}
