package de.javamark.matchoracle.matchday.entity;

import org.junit.jupiter.api.Test;

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
}
