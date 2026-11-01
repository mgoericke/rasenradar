package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.control.LastYearCalculator.Comparison;
import de.javamark.matchoracle.matchday.control.LastYearCalculator.LastYear;
import de.javamark.matchoracle.matchday.entity.League;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.Team;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static de.javamark.matchoracle.matchday.control.MatchFixtures.played;
import static de.javamark.matchoracle.matchday.control.MatchFixtures.provisional;
import static de.javamark.matchoracle.matchday.control.MatchFixtures.team;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Story r081: points against the same number of games last season, the other league marked, newcomers apart. */
class LastYearCalculatorTest {

    private final Team bremen = team("Bremen"), koeln = team("Köln"), kiel = team("Kiel"), elv = team("Elversberg");

    private static Comparison of(LastYear y, Team t) {
        return y.clubs().stream().filter(c -> c.team() == t).findFirst().orElseThrow();
    }

    @Test
    void comparesAfterTheSameNumberOfGames() {
        List<Match> now = List.of(played(1, bremen, 2, koeln, 0), played(2, koeln, 1, bremen, 1), provisional(3, bremen, 3, koeln, 0));
        // last season Bremen: win, loss, win — only the first two count (two final games this season)
        List<Match> then = List.of(played(1, bremen, 1, kiel, 0), played(2, kiel, 2, bremen, 0), played(3, bremen, 4, kiel, 0));
        List<Match> otherThen = List.of(played(1, koeln, 0, elv, 0), played(2, elv, 0, koeln, 1));

        LastYear y = LastYearCalculator.of(League.BUNDESLIGA_1, now,
                Map.of(League.BUNDESLIGA_1, then, League.BUNDESLIGA_2, otherThen));

        Comparison b = of(y, bremen);
        assertEquals(2, b.games());
        assertEquals(4, b.points());
        assertEquals(3, b.pointsThen());
        assertEquals(1, b.difference());
        assertNull(b.otherLeague());
        Comparison k = of(y, koeln);
        assertEquals(League.BUNDESLIGA_2, k.otherLeague());
        assertEquals(4, k.pointsThen());
        assertEquals(bremen, y.clubs().getFirst().team()); // +1 before Köln's -3
    }

    @Test
    void aClubWithoutLastSeasonIsListedApart() {
        LastYear y = LastYearCalculator.of(League.BUNDESLIGA_1, List.of(played(1, bremen, 1, elv, 0)),
                Map.of(League.BUNDESLIGA_1, List.of(played(1, bremen, 0, koeln, 0))));

        assertEquals(List.of(elv), y.newcomers());
        assertEquals(1, y.clubs().size());
    }
}
