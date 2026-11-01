package de.javamark.matchoracle.matchday.boundary;

import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.RaceLine;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.SeasonRace;
import de.javamark.matchoracle.matchday.entity.Balance;
import de.javamark.matchoracle.matchday.entity.League;
import de.javamark.matchoracle.matchday.entity.StandingPosition;
import de.javamark.matchoracle.matchday.entity.Standings;
import de.javamark.matchoracle.matchday.entity.Team;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Story r019: the Saisonrennen — every club's place after every matchday. */
class SeasonRaceTest {

    private static final Team BAYERN = team(1, "FC Bayern München", "Bayern");
    private static final Team MAINZ = team(2, "1. FSV Mainz 05", "Mainz");

    private static Team team(long id, String name, String shortName) {
        Team t = new Team();
        t.id = id;
        t.name = name;
        t.shortName = shortName;
        return t;
    }

    private static Standings table(int afterMatchday, Team... order) {
        List<StandingPosition> positions = new ArrayList<>();
        for (int i = 0; i < order.length; i++) {
            positions.add(new StandingPosition(i + 1, order[i], Balance.EMPTY));
        }
        return new Standings(League.BUNDESLIGA_1, 2026, afterMatchday + 1, positions, false);
    }

    @Test
    void oneLinePerClubInTodaysOrderWithItsPlaceAfterEachMatchday() {
        SeasonRace race = SeasonRace.of(List.of(table(1, MAINZ, BAYERN), table(2, BAYERN, MAINZ), table(3, BAYERN, MAINZ)),
                false, "bl1");

        assertTrue(race.drawn());
        assertEquals(List.of("Bayern", "Mainz"), race.lines().stream().map(RaceLine::name).toList());
        RaceLine bayern = race.lines().getFirst();
        assertEquals(List.of("2", "1", "1"), bayern.cells(3));
        assertEquals(SeasonRace.y(1), bayern.endY());
        assertEquals("", bayern.tail());
        assertTrue(SeasonRace.y(1) < SeasonRace.y(2), "place 1 at the top");
    }

    @Test
    void aProvisionalLastMatchdayGetsADashedLastStep() {
        SeasonRace race = SeasonRace.of(List.of(table(1, MAINZ, BAYERN), table(2, BAYERN, MAINZ), table(3, MAINZ, BAYERN)),
                true, "bl1");

        RaceLine mainz = race.lines().getFirst();
        int x2 = SeasonRace.x(2, 3);
        int x3 = SeasonRace.x(3, 3);
        assertEquals(x2 + "," + SeasonRace.y(2) + " " + x3 + "," + SeasonRace.y(1), mainz.tail());
        assertFalse(mainz.solid().contains(x3 + ","), "the solid line stops one matchday earlier");
    }

    @Test
    void afterOnlyOneMatchdayNothingIsDrawn() {
        assertFalse(SeasonRace.of(List.of(table(1, BAYERN, MAINZ)), false, "bl1").drawn());
    }
}
