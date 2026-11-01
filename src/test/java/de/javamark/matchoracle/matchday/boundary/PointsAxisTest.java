package de.javamark.matchoracle.matchday.boundary;

import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.PointsAxis;
import de.javamark.matchoracle.matchday.control.ReachCalculator.ClubReach;
import de.javamark.matchoracle.matchday.control.ReachCalculator.Reach;
import de.javamark.matchoracle.matchday.control.ReachCalculator.Threshold;
import de.javamark.matchoracle.matchday.entity.League;
import de.javamark.matchoracle.matchday.entity.Team;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Story r022 "Punktestrahl": one row per points total, the gap as space, zone lines under their place. */
class PointsAxisTest {

    private static Reach reach(int... points) {
        List<ClubReach> clubs = new ArrayList<>();
        for (int i = 0; i < points.length; i++) {
            Team t = new Team();
            t.name = "Verein " + (i + 1);
            clubs.add(new ClubReach(t, i + 1, points[i], 5, 5, points[i] + 15, null, false));
        }
        return new Reach(clubs, List.of(new Threshold(1, "Platz 1", points[0]),
                new Threshold(4, "Platz 4 · Europapokal", points[3])), 5);
    }

    @Test
    void clubsOnEqualPointsShareARowAndAGapBecomesSpace() {
        PointsAxis axis = PointsAxis.of(reach(15, 15, 11, 10, 9, 7, 0), League.BUNDESLIGA_1, "bl1");

        assertEquals(List.of(15, 11, 10, 9, 7, 0), axis.rows().stream().map(r -> r.points()).toList());
        assertEquals(2, axis.rows().get(0).clubs().size());
        assertEquals(3, axis.rows().get(1).gap());               // 12, 13, 14 missing
        assertEquals(PointsAxis.MAX_GAP, axis.rows().getLast().gap());
        assertEquals("Platz 4 · Europapokal", axis.rows().get(2).lines().getFirst().label());
        assertEquals("zone-top", axis.rows().get(2).lines().getFirst().zone());
    }

    @Test
    void theLeadCountsTheClubsAtMostThreePointsBehindTheFirstLine() {
        assertEquals("2 Mannschaften liegen höchstens drei Punkte hinter Platz 4.",
                PointsAxis.lead(reach(15, 15, 11, 10, 9, 7, 1)));
        assertEquals("Noch keine Abstände.", PointsAxis.lead(reach(0, 0, 0, 0, 0)));
    }
}
