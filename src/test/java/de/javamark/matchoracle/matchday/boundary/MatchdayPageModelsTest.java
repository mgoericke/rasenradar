package de.javamark.matchoracle.matchday.boundary;

import de.javamark.matchoracle.matchday.entity.Decision;
import de.javamark.matchoracle.matchday.entity.League;
import de.javamark.matchoracle.matchday.entity.LeagueScorer;
import de.javamark.matchoracle.matchday.entity.Score;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    // --- live poll (spec 01, "Zwischenstand"): an open page refreshes itself only around a match ---

    private static final Instant KICKOFF = Instant.parse("2026-10-10T13:30:00Z");
    private static final Duration LIVE_WINDOW = Duration.ofMinutes(150);

    @Test
    void aRunningMatchIsWatched() {
        assertTrue(MatchdayPageModels.isWatched(KICKOFF, false, KICKOFF.plus(Duration.ofMinutes(40)), LIVE_WINDOW));
    }

    @Test
    void aPageOpenedShortlyBeforeKickoffAlreadyWatches() {
        assertTrue(MatchdayPageModels.isWatched(KICKOFF, false, KICKOFF.minus(Duration.ofMinutes(10)), LIVE_WINDOW));
        assertFalse(MatchdayPageModels.isWatched(KICKOFF, false, KICKOFF.minus(Duration.ofHours(2)), LIVE_WINDOW));
    }

    @Test
    void aMatchWithAResultOrPastItsLiveWindowIsNoLongerWatched() {
        assertFalse(MatchdayPageModels.isWatched(KICKOFF, true, KICKOFF.plus(Duration.ofMinutes(110)), LIVE_WINDOW));
        assertFalse(MatchdayPageModels.isWatched(KICKOFF, false, KICKOFF.plus(Duration.ofHours(3)), LIVE_WINDOW));
    }

    @Test
    void countdownLabelRoundsMinutesUp() {
        assertEquals("Anstoß in 2 Std. 14 Min.", MatchdayPageModels.countdownLabel(Duration.ofMinutes(133).plusSeconds(20)));
        assertEquals("Anstoß in 3 Std.", MatchdayPageModels.countdownLabel(Duration.ofHours(3)));
        assertEquals("Anstoß in 1 Min.", MatchdayPageModels.countdownLabel(Duration.ofSeconds(5)));
    }

    @Test
    void countdownOnlyWithinADayBeforeKickoff() {
        assertNotNull(MatchdayPageModels.Countdown.of(NOW.plus(Duration.ofHours(24)), false, NOW));
        assertNull(MatchdayPageModels.Countdown.of(NOW.plus(Duration.ofHours(24)).plusSeconds(1), false, NOW));
        assertNull(MatchdayPageModels.Countdown.of(NOW, false, NOW));
        assertNull(MatchdayPageModels.Countdown.of(NOW.plusSeconds(600), true, NOW));
    }

    @Test
    void countdownCarriesTheServerClock() {
        MatchdayPageModels.Countdown c = MatchdayPageModels.Countdown.of(NOW.plusSeconds(600), false, NOW);
        assertEquals(NOW.getEpochSecond(), c.now());
        assertEquals(NOW.getEpochSecond() + 600, c.kickoff());
        assertEquals("Anstoß in 10 Min.", c.label());
    }

    @Test
    void topScorersShareRanksAndKeepEveryoneTiedOnTheLastPlace() {
        List<LeagueScorer> all = List.of(
                new LeagueScorer("H. Kane", "Bayern", 9, 2),
                new LeagueScorer("N. Füllkrug", "Bremen", 9, 3),
                new LeagueScorer("P. Schick", "Leverkusen", 7, 0),
                new LeagueScorer("M. Olise", "Bayern", 5, 0),
                new LeagueScorer("Luis Díaz", "Bayern", 5, 0),
                new LeagueScorer("A. Zweiter", "Mainz", 1, 0));

        var rows = MatchdayPageModels.topScorers(all, 4, Map.of(
                "Bremen", new MatchdayPageModels.ClubLook("Werder", "/crests/1/a.png")));

        assertEquals(List.of("N. Füllkrug", "H. Kane", "P. Schick", "Luis Díaz", "M. Olise"),
                rows.stream().map(MatchdayPageModels.LeagueScorerRow::name).toList());
        assertEquals(List.of(1, 1, 3, 4, 4), rows.stream().map(MatchdayPageModels.LeagueScorerRow::rank).toList());
        assertEquals("Werder", rows.getFirst().teamShort());
        assertEquals("/crests/1/a.png", rows.getFirst().crest());
        assertEquals("Bayern", rows.get(1).teamShort());
    }

    @Test
    void surnameIsTheLastWord() {
        assertEquals("Füllkrug", MatchdayPageModels.surname("N. Füllkrug"));
        assertEquals("Ebnoutalib", MatchdayPageModels.surname("Younes Ebnoutalib"));
        assertEquals("Kane", MatchdayPageModels.surname("Kane"));
    }

    /** Design r102: the stage's arrows go round the start page's matches at both ends. */
    @Test
    void stageBrowsesRoundTheStartPage() {
        var middle = MatchdayPageModels.StageBrowse.of(List.of(11L, 12L, 13L), 12L);
        assertEquals(new MatchdayPageModels.StageBrowse(11L, 13L, 2, 3), middle);
        var first = MatchdayPageModels.StageBrowse.of(List.of(11L, 12L, 13L), 11L);
        assertEquals(13L, first.previous());
        var last = MatchdayPageModels.StageBrowse.of(List.of(11L, 12L, 13L), 13L);
        assertEquals(11L, last.next());
    }

    /** "Mein Verein" in another competition: no position, the arrows lead onto the page at either end. */
    @Test
    void stageOffThePageEntersAtEitherEnd() {
        var off = MatchdayPageModels.StageBrowse.of(List.of(11L, 12L, 13L), 99L);
        assertEquals(new MatchdayPageModels.StageBrowse(13L, 11L, 0, 3), off);
    }

    @Test
    void stageWithoutAnotherMatchHasNoArrows() {
        assertNull(MatchdayPageModels.StageBrowse.of(List.of(11L), 11L));
    }
}
