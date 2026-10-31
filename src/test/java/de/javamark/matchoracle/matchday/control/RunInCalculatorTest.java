package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.control.RunInCalculator.ClubRunIn;
import de.javamark.matchoracle.matchday.control.RunInCalculator.RunIn;
import de.javamark.matchoracle.matchday.control.RunInCalculator.State;
import de.javamark.matchoracle.matchday.entity.League;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.Team;
import org.junit.jupiter.api.Test;

import java.util.List;

import static de.javamark.matchoracle.matchday.control.MatchFixtures.played;
import static de.javamark.matchoracle.matchday.control.MatchFixtures.team;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Story r045 "Restprogramm": columns from the first open matchday, "gespielt"/"spielfrei", average and rank. */
class RunInCalculatorTest {

    private final Team a = team("A"), b = team("B"), c = team("C"), d = team("D");
    private final StandingsCalculator standings = new StandingsCalculator();

    private static Match open(int md, Team home, Team away) {
        Match m = played(md, home, 0, away, 0);
        m.fullTimeScore = null;
        return m;
    }

    private static ClubRunIn of(RunIn r, Team t) {
        return r.clubs().stream().filter(x -> x.team() == t).findFirst().orElseThrow();
    }

    @Test
    void cellsRanksAndTheFirstOpenMatchday() {
        List<Match> done = List.of(played(1, a, 3, b, 0), played(1, c, 1, d, 0), played(2, a, 1, c, 0), played(2, b, 1, d, 0));
        // table: A 6, C 3 (goal difference 0), B 3 (-2), D 0
        Match catchUp = played(4, b, 2, a, 2); // a matchday-4 match already played early
        List<Match> all = new java.util.ArrayList<>(done);
        all.add(catchUp);
        all.add(open(3, d, a));
        all.add(open(3, b, c));
        all.add(open(4, c, d));

        RunIn r = RunInCalculator.of(standings.standings(League.BUNDESLIGA_1, 2025, 99, done), all);

        assertEquals(List.of(3, 4), r.matchdays());
        ClubRunIn ofA = of(r, a);
        assertEquals(State.OPEN, ofA.cells().get(0).state());
        assertEquals(d, ofA.cells().get(0).fixture().opponent());
        assertEquals(4, ofA.cells().get(0).fixture().opponentPosition());
        assertEquals(State.PLAYED, ofA.cells().get(1).state());
        // D plays A (place 1) and C: the hardest run-in, rank 1
        assertEquals(1, of(r, d).rank());
        // A only faces D (place 4): the easiest
        assertEquals(4.0, ofA.averagePosition());
    }

    @Test
    void aMatchdayWithoutAMatchForTheClubIsFree() {
        List<Match> done = List.of(played(1, a, 1, b, 0), played(1, c, 1, d, 0));
        List<Match> all = new java.util.ArrayList<>(done);
        all.add(open(2, a, c)); // B and D have no match on matchday 2

        RunIn r = RunInCalculator.of(standings.standings(League.BUNDESLIGA_1, 2025, 99, done), all);

        assertEquals(State.FREE, of(r, b).cells().getFirst().state());
        assertEquals(null, of(r, b).rank());
    }
}
