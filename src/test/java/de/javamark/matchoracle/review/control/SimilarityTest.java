package de.javamark.matchoracle.review.control;

import de.javamark.matchoracle.review.entity.Situation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Spec 03, interview: what makes two situations similar. */
class SimilarityTest {

    @Test
    void identicalSituationsHaveDistanceZero() {
        Situation a = situation("bl1", -10, 12, 3, 2.5, 0.5, false, false);

        assertEquals(0.0, Similarity.distance(a, a), 1e-9);
        assertTrue(Similarity.similar(0.0));
    }

    @Test
    void aCloseConstellationIsSimilarAnOppositeOneIsNot() {
        Situation favouriteAtHome = situation("bl1", -10, 12, 3, 2.5, 0.5, false, false);
        Situation nearlyTheSame = situation("bl1", -8, 10, 4, 2.2, 0.8, false, false);
        Situation underdogAtHome = situation("bl1", 12, 2, 13, 0.5, 2.6, true, false);

        assertTrue(Similarity.similar(Similarity.distance(favouriteAtHome, nearlyTheSame)));
        assertFalse(Similarity.similar(Similarity.distance(favouriteAtHome, underdogAtHome)));
    }

    @Test
    void theOtherLeagueIsComparableButFurtherAway() {
        Situation a = situation("bl1", -10, 12, 3, 2.5, 0.5, false, false);
        Situation b = situation("bl2", -10, 12, 3, 2.5, 0.5, false, false);

        assertEquals(Similarity.OTHER_LEAGUE_PENALTY, Similarity.distance(a, b), 1e-9);
    }

    private static Situation situation(String league, int gap, int homeForm, int awayForm, double homePpg, double awayPpg,
                                       boolean homePromoted, boolean awayPromoted) {
        Situation s = new Situation();
        s.league = league;
        s.positionGap = gap;
        s.homeFormPoints = homeForm;
        s.awayFormPoints = awayForm;
        s.formMatches = 5;
        s.homeHomePpg = homePpg;
        s.awayAwayPpg = awayPpg;
        s.homePromoted = homePromoted;
        s.awayPromoted = awayPromoted;
        return s;
    }
}
