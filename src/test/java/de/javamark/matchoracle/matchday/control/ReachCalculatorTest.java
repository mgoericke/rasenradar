package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.control.ReachCalculator.ClubReach;
import de.javamark.matchoracle.matchday.control.ReachCalculator.Reach;
import de.javamark.matchoracle.matchday.entity.League;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.Team;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static de.javamark.matchoracle.matchday.control.MatchFixtures.played;
import static de.javamark.matchoracle.matchday.control.MatchFixtures.team;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Story r058 "Wer kann noch?": maximum, pace and "kann Platz 1 nicht mehr erreichen". */
class ReachCalculatorTest {

    private final Team bremen = team("Bremen");
    private final Team koeln = team("Köln");
    private final Team kiel = team("Kiel");
    private final StandingsCalculator standings = new StandingsCalculator();

    private static Match open(int md, Team home, Team away) {
        Match m = played(md, home, 0, away, 0);
        m.fullTimeScore = null;
        return m;
    }

    private static ClubReach of(Reach r, Team t) {
        return r.clubs().stream().filter(c -> c.team() == t).findFirst().orElseThrow();
    }

    @Test
    void maximumIsThreePointsForEveryOpenMatchAndPaceProjectsOverAllMatches() {
        List<Match> playedMatches = List.of(played(1, bremen, 2, koeln, 0), played(2, koeln, 1, bremen, 1));
        List<Match> all = new ArrayList<>(playedMatches);
        all.add(open(3, bremen, koeln));
        all.add(open(4, koeln, bremen));

        Reach r = ReachCalculator.of(standings.standings(League.BUNDESLIGA_1, 2025, 99, playedMatches), all);

        ClubReach b = of(r, bremen);
        assertEquals(4, b.points());
        assertEquals(2, b.remaining());
        assertEquals(10, b.maximum());
        assertEquals(8, b.pace()); // 2 points per match over 4 matches
        ClubReach k = of(r, koeln);
        assertEquals(1, k.points());
        assertEquals(7, k.maximum());
        assertFalse(k.titleOutOfReach()); // 7 is more than Bremen's 4 today
        assertEquals(2, r.remainingMatchdays());
    }

    @Test
    void aClubWhoseMaximumIsBelowTheLeadersPointsTodayCannotReachPlaceOne() {
        List<Match> playedMatches = List.of(played(1, bremen, 2, koeln, 0), played(2, bremen, 2, kiel, 0),
                played(3, kiel, 1, bremen, 3), played(4, koeln, 0, kiel, 0));
        List<Match> all = new ArrayList<>(playedMatches);
        all.add(open(5, koeln, bremen));

        Reach r = ReachCalculator.of(standings.standings(League.BUNDESLIGA_1, 2025, 99, playedMatches), all);

        // Bremen 9; Kiel 1 point, no match left → 1 < 9
        assertTrue(of(r, kiel).titleOutOfReach());
        assertFalse(of(r, bremen).titleOutOfReach());
        assertEquals("Platz 1", r.thresholds().getFirst().label());
        assertEquals(9, r.thresholds().getFirst().points());
    }
}
