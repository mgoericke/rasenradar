package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.entity.Balance;
import de.javamark.matchoracle.matchday.entity.League;
import de.javamark.matchoracle.matchday.entity.StandingPosition;
import de.javamark.matchoracle.matchday.entity.Standings;
import de.javamark.matchoracle.matchday.entity.Team;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Story r070: the "Es geht um" line of an open match, from the table as it stands. */
class StakesTest {

    /** Eighteen sides, 30 points down to 13 — one point apart, goal difference 0. */
    private final List<Team> teams = new ArrayList<>();
    private Standings table = table(new int[]{30, 29, 28, 27, 26, 25, 24, 23, 22, 21, 20, 19, 18, 17, 16, 15, 14, 13});

    @Test
    void topThreeAmongThemselvesIsATopMatch() {
        assertEquals(Optional.of("Spitzenspiel: Erster gegen Zweiter"), Stakes.of(teams.get(1), teams.get(0), table));
        assertEquals(Optional.of("Spitzenspiel: Erster gegen Dritter"), Stakes.of(teams.get(0), teams.get(2), table));
    }

    @Test
    void lastFourAmongThemselvesIsABottomMatch() {
        assertEquals(Optional.of("Kellerduell: 15. gegen 16."), Stakes.of(teams.get(15), teams.get(14), table));
        assertEquals(Optional.of("Kellerduell: 15. gegen 18."), Stakes.of(teams.get(14), teams.get(17), table));
    }

    @Test
    void otherwiseEachSideAWinWouldLiftIsNamed() {
        // 6th (25) wins → 28: past 4th (27) and 5th (26), level with 3rd on points but ahead on goal difference
        assertEquals(Optional.of("Ein Sieg brächte Team 6 auf Platz 3, Team 10 auf Platz 7"),
                Stakes.of(teams.get(5), teams.get(9), table));
    }

    @Test
    void theLeaderCannotClimbSoOnlyTheOtherSideIsNamed() {
        assertEquals(Optional.of("Ein Sieg brächte Team 8 auf Platz 5"), Stakes.of(teams.get(0), teams.get(7), table));
    }

    @Test
    void noLineWhenNeitherSideWouldMove() {
        table = table(new int[]{40, 30, 29, 20, 10, 9, 8, 7, 6, 5});
        // 4th (20) and 5th (10): a win brings 23 or 13 — neither passes anyone
        assertEquals(Optional.empty(), Stakes.of(teams.get(3), teams.get(4), table));
    }

    @Test
    void anEmptyTableHasNothingAtStake() {
        Team a = team("A"), b = team("B");
        assertEquals(Optional.empty(), Stakes.of(a, b, new Standings(League.BUNDESLIGA_1, 2026, 1, List.of(), false)));
    }

    /** Points as wins and draws, goal difference 0, five goals scored each — so only points decide. */
    private Standings table(int[] points) {
        teams.clear();
        List<StandingPosition> positions = new ArrayList<>();
        for (int i = 0; i < points.length; i++) {
            Team t = team("Team " + (i + 1));
            teams.add(t);
            positions.add(new StandingPosition(i + 1, t, new Balance(points[i] / 3, points[i] % 3, 0, 5, 5)));
        }
        return new Standings(League.BUNDESLIGA_1, 2026, 10, positions, false);
    }

    private static Team team(String name) {
        Team t = new Team();
        t.name = name;
        return t;
    }
}
