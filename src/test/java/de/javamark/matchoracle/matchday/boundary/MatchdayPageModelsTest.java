package de.javamark.matchoracle.matchday.boundary;

import de.javamark.matchoracle.matchday.entity.League;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Spec 1: a match counts as "läuft" purely from its kickoff time — no live score. */
class MatchdayPageModelsTest {

    private static final Instant NOW = Instant.parse("2026-09-20T16:00:00Z");
    private static final Duration WINDOW = Duration.ofMinutes(150);

    @Test
    void notYetKickedOffIsNotLive() {
        assertFalse(MatchdayPageModels.isLive(NOW.plusSeconds(60), false, NOW, WINDOW));
    }

    @Test
    void withinTheWindowAfterKickoffAndUnplayedIsLive() {
        assertTrue(MatchdayPageModels.isLive(NOW.minus(Duration.ofMinutes(80)), false, NOW, WINDOW));
    }

    @Test
    void beyondTheWindowIsNoLongerLive() {
        assertFalse(MatchdayPageModels.isLive(NOW.minus(Duration.ofMinutes(200)), false, NOW, WINDOW));
    }

    @Test
    void aPlayedMatchIsNeverLiveEvenWithinTheWindow() {
        assertFalse(MatchdayPageModels.isLive(NOW.minus(Duration.ofMinutes(80)), true, NOW, WINDOW));
    }

    @Test
    void kickoffThisInstantIsLive() {
        assertTrue(MatchdayPageModels.isLive(NOW, false, NOW, WINDOW));
    }

    @Test
    void zoneColorsBundesliga1sChampionsLeagueSpotsAndTheRelegationLadder() {
        assertEquals("zone-top", MatchdayPageModels.zone(League.BUNDESLIGA_1, 1));
        assertEquals("zone-top", MatchdayPageModels.zone(League.BUNDESLIGA_1, 4));
        assertEquals("", MatchdayPageModels.zone(League.BUNDESLIGA_1, 5));
        assertEquals("zone-bottom-playoff", MatchdayPageModels.zone(League.BUNDESLIGA_1, 16));
        assertEquals("zone-bottom", MatchdayPageModels.zone(League.BUNDESLIGA_1, 18));
    }

    @Test
    void zoneColorsBundesliga2sPromotionAndRelegationSpots() {
        assertEquals("zone-top", MatchdayPageModels.zone(League.BUNDESLIGA_2, 2));
        assertEquals("zone-top-playoff", MatchdayPageModels.zone(League.BUNDESLIGA_2, 3));
        assertEquals("zone-bottom-playoff", MatchdayPageModels.zone(League.BUNDESLIGA_2, 16));
        assertEquals("zone-bottom", MatchdayPageModels.zone(League.BUNDESLIGA_2, 17));
    }

    @Test
    void zoneColorsTheChampionsLeaguePhaseInThirds() {
        assertEquals("zone-top", MatchdayPageModels.zone(League.CHAMPIONS_LEAGUE, 8));
        assertEquals("zone-top-playoff", MatchdayPageModels.zone(League.CHAMPIONS_LEAGUE, 24));
        assertEquals("zone-bottom", MatchdayPageModels.zone(League.CHAMPIONS_LEAGUE, 25));
    }

    @Test
    void outlookLinkMatchesSeasonOutlookPagesRoute() {
        assertEquals("/bl2/2026/teams/28/outlook", MatchdayPageModels.outlookLink("bl2", 2026, 28));
    }
}
