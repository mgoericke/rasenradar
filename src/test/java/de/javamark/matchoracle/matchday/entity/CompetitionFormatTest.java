package de.javamark.matchoracle.matchday.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Spec 06, Regeln: die Form entscheidet, ob ein Wettbewerb eine Tabelle führt. */
class CompetitionFormatTest {

    @Test
    void onlyKnockoutCompetitionsHaveNoTable() {
        assertTrue(CompetitionFormat.TABLE.hasTable());
        assertTrue(CompetitionFormat.GROUPS.hasTable());
        assertFalse(CompetitionFormat.KNOCKOUT.hasTable());
    }

    @Test
    void theExistingLeaguesAreTableCompetitions() {
        assertTrue(League.BUNDESLIGA_1.hasTable());
        assertTrue(League.BUNDESLIGA_2.hasTable());
        assertTrue(League.CHAMPIONS_LEAGUE.hasTable());
    }

    @Test
    void theNewCompetitionsCarryTheirFormat() {
        assertEquals(CompetitionFormat.KNOCKOUT, League.DFB_POKAL.format());
        assertEquals(CompetitionFormat.GROUPS, League.NATIONS_LEAGUE.format());
        assertFalse(League.DFB_POKAL.hasTable());
        assertTrue(League.NATIONS_LEAGUE.hasTable());
    }

    @Test
    void historyDepthIsAPropertyOfTheCompetition() {
        assertEquals(5, League.BUNDESLIGA_1.historySeasons());
        assertEquals(0, League.CHAMPIONS_LEAGUE.historySeasons());
        assertEquals(3, League.DFB_POKAL.historySeasons());
        assertEquals(1, League.NATIONS_LEAGUE.historySeasons());
    }
}
