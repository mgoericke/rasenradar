package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.control.MatchdayStream.StreamDay;
import de.javamark.matchoracle.matchday.entity.League;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.Matchday;
import de.javamark.matchoracle.matchday.entity.Score;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Spec 01, "Spieltags-Strom der Startseite": one stream by day and kickoff block, across both leagues. */
class MatchdayStreamTest {

    // Friday 18:30 (2. BL) and 20:30 (1. BL), Saturday 13:00 (2. BL) and 15:30 (both) — written in UTC (+2)
    private static final Instant FRI_1830 = Instant.parse("2026-10-09T16:30:00Z");
    private static final Instant FRI_2030 = Instant.parse("2026-10-09T18:30:00Z");
    private static final Instant SAT_1300 = Instant.parse("2026-10-10T11:00:00Z");
    private static final Instant SAT_1530 = Instant.parse("2026-10-10T13:30:00Z");

    @Test
    void bothLeaguesMergeIntoOneStreamByDayAndKickoff() {
        Match bl1Fri = match(League.BUNDESLIGA_1, FRI_2030, false);
        Match bl2Fri = match(League.BUNDESLIGA_2, FRI_1830, false);
        Match bl2Sat = match(League.BUNDESLIGA_2, SAT_1300, false);

        List<StreamDay> days = MatchdayStream.of(List.of(bl1Fri, bl2Sat, bl2Fri));

        assertEquals(List.of(LocalDate.of(2026, 10, 9), LocalDate.of(2026, 10, 10)), days.stream().map(StreamDay::date).toList());
        assertEquals(List.of(FRI_1830, FRI_2030), days.getFirst().slots().stream().map(s -> s.kickoff()).toList());
    }

    @Test
    void aBlockHoldsBothLeaguesWithTheFirstBundesligaFirst() {
        Match bl2 = match(League.BUNDESLIGA_2, SAT_1530, false);
        Match bl1 = match(League.BUNDESLIGA_1, SAT_1530, false);

        var slot = MatchdayStream.of(List.of(bl2, bl1)).getFirst().slots().getFirst();

        assertEquals(List.of(bl1, bl2), slot.matches());
    }

    @Test
    void theNextMatchdayFollowsOnlyOnceTheDisplayedOneHasStarted() {
        assertFalse(MatchdayStream.showsNext(List.of(match(League.BUNDESLIGA_1, SAT_1530, false))));
        assertTrue(MatchdayStream.showsNext(List.of(match(League.BUNDESLIGA_1, FRI_2030, true), match(League.BUNDESLIGA_1, SAT_1530, false))));
    }

    private static Match match(League league, Instant kickoff, boolean played) {
        Matchday md = new Matchday();
        md.league = league;
        Match m = new Match();
        m.matchday = md;
        m.kickoff = kickoff;
        m.fullTimeScore = played ? new Score(1, 0) : null;
        return m;
    }
}
