package de.javamark.matchoracle.matchday.boundary;

import de.javamark.matchoracle.matchday.entity.Decision;
import de.javamark.matchoracle.matchday.entity.League;
import de.javamark.matchoracle.matchday.entity.Score;
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

    // --- Spec 06: Pokal -------------------------------------------------------------

    @Test
    void aRegularResultCarriesNoDecisionLabel() {
        assertEquals("", MatchdayPageModels.decisionLabel(Decision.REGULAR, null));
    }

    @Test
    void extraTimeIsMarkedAtTheResult() {
        assertEquals("n. V.", MatchdayPageModels.decisionLabel(Decision.EXTRA_TIME, null));
    }

    @Test
    void aShootoutShowsItsOwnScoreBesideTheLabel() {
        // Spec 06: angezeigt wird der Endstand, das Elfmeterschiessen steht dahinter —
        // nicht der Elfmeterstand als Ergebnis.
        assertEquals("n. E. 7:5", MatchdayPageModels.decisionLabel(Decision.PENALTIES, new Score(7, 5)));
    }

    @Test
    void aCompetitionWithoutATableHasNoTableZone() {
        assertEquals("", MatchdayPageModels.zone(League.DFB_POKAL, 1));
        assertEquals("", MatchdayPageModels.zone(League.NATIONS_LEAGUE, 1));
    }

    @Test
    void theCupRunSaysWhereATeamWentOut() {
        assertEquals("im Achtelfinale aus", MatchdayPageModels.cupRunOutcome("Achtelfinale", false));
        assertEquals("im Endspiel aus", MatchdayPageModels.cupRunOutcome("Endspiel", false));
    }

    @Test
    void aRoundIsFeminineInGerman() {
        // "im 1. Runde aus" waere falsch — die Runde, aber das Achtelfinale.
        assertEquals("in der 1. Runde aus", MatchdayPageModels.cupRunOutcome("1. Runde", false));
        assertEquals("in der 2. Runde aus", MatchdayPageModels.cupRunOutcome("2. Runde", false));
    }

    @Test
    void winningTheFinalIsNotGoingOut() {
        assertEquals("Pokalsieger", MatchdayPageModels.cupRunOutcome("Endspiel", true));
    }

    @Test
    void aTeamStillInTheCupHasNoOutcomeYet() {
        assertEquals("", MatchdayPageModels.cupRunOutcome(null, false));
    }
}
