package de.javamark.matchoracle.matchday.entity;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Spec 04: the same position ranges MatchdayPageModels.zone() colors the table by. */
class PlacementGoalTest {

    @Test
    void bundesliga1ChampionQualifiesForBothChampionshipAndEurope() {
        assertEquals(List.of(PlacementGoal.CHAMPIONSHIP, PlacementGoal.EUROPE), PlacementGoal.forPosition(League.BUNDESLIGA_1, 1));
    }

    @Test
    void bundesliga1FourthPlaceIsEuropeOnly() {
        assertEquals(List.of(PlacementGoal.EUROPE), PlacementGoal.forPosition(League.BUNDESLIGA_1, 4));
    }

    @Test
    void bundesliga1FifthPlaceHasNoGoal() {
        assertTrue(PlacementGoal.forPosition(League.BUNDESLIGA_1, 5).isEmpty());
    }

    @Test
    void bundesliga1SixteenthIsTheRelegationPlayoff() {
        assertEquals(List.of(PlacementGoal.RELEGATION_PLAYOFF), PlacementGoal.forPosition(League.BUNDESLIGA_1, 16));
    }

    @Test
    void bundesliga1SeventeenthIsRelegation() {
        assertEquals(List.of(PlacementGoal.RELEGATION), PlacementGoal.forPosition(League.BUNDESLIGA_1, 17));
    }

    @Test
    void bundesliga2SecondIsDirectPromotion() {
        assertEquals(List.of(PlacementGoal.PROMOTION), PlacementGoal.forPosition(League.BUNDESLIGA_2, 2));
    }

    @Test
    void bundesliga2ThirdIsThePromotionPlayoff() {
        assertEquals(List.of(PlacementGoal.PROMOTION_PLAYOFF), PlacementGoal.forPosition(League.BUNDESLIGA_2, 3));
    }

    @Test
    void bundesliga2SixteenthIsTheRelegationPlayoffToo() {
        assertEquals(List.of(PlacementGoal.RELEGATION_PLAYOFF), PlacementGoal.forPosition(League.BUNDESLIGA_2, 16));
    }

    @Test
    void championsLeagueEighthIsDirectKnockout() {
        assertEquals(List.of(PlacementGoal.KNOCKOUT_DIRECT), PlacementGoal.forPosition(League.CHAMPIONS_LEAGUE, 8));
    }

    @Test
    void championsLeagueNinthIsThePlayoff() {
        assertEquals(List.of(PlacementGoal.KNOCKOUT_PLAYOFF), PlacementGoal.forPosition(League.CHAMPIONS_LEAGUE, 9));
    }

    @Test
    void championsLeagueTwentyFifthIsElimination() {
        assertEquals(List.of(PlacementGoal.ELIMINATION), PlacementGoal.forPosition(League.CHAMPIONS_LEAGUE, 25));
    }

    @Test
    void allListsEveryGoalOfALeagueExactlyOnce() {
        assertEquals(4, PlacementGoal.all(League.BUNDESLIGA_1).size());
        assertEquals(4, PlacementGoal.all(League.BUNDESLIGA_2).size());
        assertEquals(3, PlacementGoal.all(League.CHAMPIONS_LEAGUE).size());
    }

    @Test
    void everyLeaguesGoalsHaveDistinctLabels() {
        for (League league : League.values()) {
            List<String> labels = PlacementGoal.all(league).stream().map(PlacementGoal::label).toList();
            assertEquals(labels.size(), Set.copyOf(labels).size(), "duplicate label in " + league);
        }
    }

    @Test
    void competitionsWithoutATableHaveNoPlacementGoals() {
        assertTrue(PlacementGoal.all(League.DFB_POKAL).isEmpty());
        assertTrue(PlacementGoal.forPosition(League.DFB_POKAL, 1).isEmpty());
    }

    @Test
    void theNationsLeagueHasNoPlacementGoalsEither() {
        assertTrue(PlacementGoal.all(League.NATIONS_LEAGUE).isEmpty());
        assertTrue(PlacementGoal.forPosition(League.NATIONS_LEAGUE, 1).isEmpty());
    }
}
