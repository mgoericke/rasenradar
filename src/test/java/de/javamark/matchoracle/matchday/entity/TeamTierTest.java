package de.javamark.matchoracle.matchday.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Spec 06: eine Überraschung ist eine Begegnung, die eine klassentiefere Mannschaft gewinnt. */
class TeamTierTest {

    @Test
    void theTiersAreOrderedFromTheFirstDivisionDownwards() {
        assertTrue(Tier.FIRST.level() < Tier.SECOND.level());
        assertTrue(Tier.SECOND.level() < Tier.THIRD.level());
        assertTrue(Tier.THIRD.level() < Tier.LOWER.level());
    }

    @Test
    void everyTierHasAShortLabelForTheFixtureList() {
        assertEquals("1. Liga", Tier.FIRST.label());
        assertEquals("2. Liga", Tier.SECOND.label());
        assertEquals("3. Liga", Tier.THIRD.label());
        assertEquals("Amateur", Tier.LOWER.label());
    }

    @Test
    void theLowerRankedWinnerMakesAnUpset() {
        assertTrue(TeamTier.isUpset(Tier.LOWER, Tier.FIRST));
        assertTrue(TeamTier.isUpset(Tier.THIRD, Tier.SECOND));
    }

    @Test
    void neitherAFavouriteWinNorAnEqualPairingIsAnUpset() {
        assertFalse(TeamTier.isUpset(Tier.FIRST, Tier.LOWER));
        assertFalse(TeamTier.isUpset(Tier.SECOND, Tier.SECOND));
    }

    @Test
    void theGapMeasuresHowBigTheUpsetWas() {
        assertEquals(3, TeamTier.gap(Tier.LOWER, Tier.FIRST));
        assertEquals(1, TeamTier.gap(Tier.THIRD, Tier.SECOND));
        assertEquals(0, TeamTier.gap(Tier.FIRST, Tier.FIRST));
    }
}
