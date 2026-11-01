package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.control.GroupReachCalculator.PlaceRange;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.Team;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static de.javamark.matchoracle.matchday.control.MatchFixtures.played;
import static de.javamark.matchoracle.matchday.control.MatchFixtures.team;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** "Was noch geht": best and worst place over every outcome of the open group matches, ties counted both ways. */
class GroupReachCalculatorTest {

    private final Team france = team("Frankreich");
    private final Team italy = team("Italien");
    private final Team belgium = team("Belgien");
    private final Team turkey = team("Türkei");

    private static Match open(Team home, Team away) {
        Match m = played(6, home, 0, away, 0);
        m.fullTimeScore = null;
        return m;
    }

    private Map<Team, Integer> points(int fr, int it, int be, int tr) {
        Map<Team, Integer> p = new LinkedHashMap<>();
        p.put(france, fr);
        p.put(italy, it);
        p.put(belgium, be);
        p.put(turkey, tr);
        return p;
    }

    @Test
    void aTeamThatCanOnlyDrawLevelWithThirdCanStillBeThirdButNoHigher() {
        // two matchdays left: Türkei at most 6, Belgien already has 6
        Map<Team, PlaceRange> r = GroupReachCalculator.of(points(10, 7, 6, 0),
                List.of(open(turkey, belgium), open(italy, france), open(france, turkey), open(belgium, italy)));

        assertEquals(new PlaceRange(3, 4), r.get(turkey));
        assertEquals(new PlaceRange(1, 3), r.get(france)); // two defeats and France can be overtaken by both
    }

    @Test
    void withNothingOpenEveryPlaceIsSettledAndATieCountsBothWays() {
        Map<Team, PlaceRange> r = GroupReachCalculator.of(points(12, 9, 9, 0), List.of());

        assertTrue(r.get(france).settled());
        assertEquals(new PlaceRange(2, 3), r.get(italy)); // level with Belgien: the tie-breaker decides, not the points
        assertEquals(new PlaceRange(4, 4), r.get(turkey));
    }

    @Test
    void tooManyOpenMatchesGiveNoRange() {
        List<Match> many = java.util.stream.IntStream.range(0, GroupReachCalculator.MAX_OPEN + 1)
                .mapToObj(i -> open(france, italy)).toList();

        assertTrue(GroupReachCalculator.of(points(0, 0, 0, 0), many).isEmpty());
    }
}
