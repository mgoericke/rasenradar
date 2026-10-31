package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.entity.League;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.Score;
import de.javamark.matchoracle.matchday.entity.StandingPosition;
import de.javamark.matchoracle.matchday.entity.Standings;
import de.javamark.matchoracle.matchday.entity.Team;
import org.junit.jupiter.api.Test;

import java.util.List;

import static de.javamark.matchoracle.matchday.control.MatchFixtures.played;
import static de.javamark.matchoracle.matchday.control.MatchFixtures.provisional;
import static de.javamark.matchoracle.matchday.control.MatchFixtures.team;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StandingsCalculatorTest {

    private final Team mainz = team("Mainz");
    private final Team leipzig = team("Leipzig");
    private final Team koeln = team("Köln");
    private final Team bremen = team("Bremen");

    private final StandingsCalculator calculator = new StandingsCalculator();

    @Test
    void ranksByPointsThenGoalDifferenceThenGoalsScored() {
        Standings standings = calculator.standings(League.BUNDESLIGA_1, 2025, 3, List.of(
                played(1, mainz, 3, leipzig, 0),   // mainz 3 pts +3, leipzig 0 pts -3
                played(1, koeln, 1, bremen, 1),    // koeln 1, bremen 1
                played(2, leipzig, 4, koeln, 0),   // leipzig 3 pts +1 (4 scored), koeln 1 pt -4
                played(2, bremen, 2, mainz, 0)));  // bremen 4 pts +3, mainz 3 pts +1 (3 scored)

        List<Team> order = standings.positions().stream().map(StandingPosition::team).toList();

        assertEquals(List.of(bremen, leipzig, mainz, koeln), order);
        assertEquals(1, standings.of(bremen).orElseThrow().position());
        assertEquals(4, standings.of(bremen).orElseThrow().balance().points());
    }

    @Test
    void teamsWithEqualRecordsAreSeparatedByGoalsScored() {
        Standings standings = calculator.standings(League.BUNDESLIGA_1, 2025, 2, List.of(
                played(1, mainz, 2, leipzig, 1),    // mainz +1, 2 scored
                played(1, koeln, 3, bremen, 2)));   // koeln +1, 3 scored

        assertEquals(koeln, standings.positions().get(0).team());
        assertEquals(mainz, standings.positions().get(1).team());
    }

    @Test
    void provisionalResultsAreFlagged() {
        Standings standings = calculator.standings(League.BUNDESLIGA_1, 2025, 2,
                List.of(provisional(1, mainz, 1, leipzig, 0)));

        assertTrue(standings.includesProvisional());
    }

    @Test
    void aGroupTableCountsOnlyTheMatchesOfThatGroup() {
        // Spec 06: in einem Gruppenwettbewerb gilt jede Regel je Gruppe. Die Auswahl der
        // Begegnungen macht die Tabelle aus — hier bekommt sie nur die der einen Gruppe.
        Standings groupA = calculator.standings(League.BUNDESLIGA_1, 2026, 3, List.of(
                played(1, mainz, 2, leipzig, 0),
                played(2, leipzig, 1, mainz, 1)));

        assertEquals(List.of(mainz, leipzig), groupA.positions().stream().map(StandingPosition::team).toList());
        assertEquals(2, groupA.positions().size(), "keine Mannschaft einer anderen Gruppe in der Tabelle");
    }

    @Test
    void aKnockoutRoundNeverCountsTowardsATable() {
        // Spec 06: die Champions League spielt nach der Ligaphase eine K.-o.-Phase. Ein
        // Achtelfinale darf keine Tabellenpunkte erzeugen — sonst wuerde die Tabelle der
        // Ligaphase nach ihrem Ende weiterlaufen.
        Match leaguePhase = played(8, mainz, 2, leipzig, 0);
        Match knockout = played(10, mainz, 3, leipzig, 0);
        knockout.matchday.label = "Achtelfinale Hinspiele";

        assertTrue(Match.countsTowardsTable(leaguePhase));
        assertFalse(Match.countsTowardsTable(knockout));
    }

    /** Story r059: a running match counts at its live score, without one at 0:0 — and never twice. */
    @Test
    void liveTableCountsRunningMatchesAtTheirCurrentScore() {
        Standings before = calculator.standings(League.BUNDESLIGA_1, 2025, 2, List.of(
                played(1, mainz, 1, leipzig, 0),
                played(1, koeln, 0, bremen, 0)));
        Match leading = running(leipzig, mainz, new Score(2, 0));
        Match unknown = running(bremen, koeln, null);

        Standings live = calculator.liveStandings(before, List.of(leading, unknown));

        // Leipzig passes Mainz on goal difference (+1 against -1), both on 3 points
        assertEquals(List.of(leipzig, mainz), live.positions().stream().limit(2).map(StandingPosition::team).toList());
        assertEquals(3, live.of(leipzig).orElseThrow().balance().points());
        assertEquals(2, live.of(bremen).orElseThrow().balance().points(), "unknown score counts as 0:0");
        assertEquals(2, live.of(mainz).orElseThrow().balance().played());
        assertTrue(live.includesProvisional());
        assertEquals(1, before.of(mainz).orElseThrow().position(), "the table before kickoff stays as it was");
    }

    /** Story r041: home, away and last-five tables count only those matches, and only final ones. */
    @Test
    void splitTablesCountOnlyTheirMatches() {
        List<Match> played = List.of(
                played(1, mainz, 2, leipzig, 0),
                played(2, leipzig, 3, mainz, 1),
                played(3, mainz, 1, koeln, 1),
                played(4, koeln, 0, mainz, 2),
                played(5, mainz, 0, bremen, 1),
                played(6, bremen, 1, mainz, 1),
                provisional(7, mainz, 5, leipzig, 0));

        Standings home = calculator.splitStandings(League.BUNDESLIGA_1, 2025, 8, played, StandingsCalculator.Split.HOME);
        Standings away = calculator.splitStandings(League.BUNDESLIGA_1, 2025, 8, played, StandingsCalculator.Split.AWAY);
        Standings last = calculator.splitStandings(League.BUNDESLIGA_1, 2025, 8, played, StandingsCalculator.Split.LAST_FIVE);

        // Mainz at home: 2:0, 1:1, 0:1 — the provisional 5:0 does not count
        assertEquals(3, home.of(mainz).orElseThrow().balance().played());
        assertEquals(4, home.of(mainz).orElseThrow().balance().points());
        // Mainz away: 1:3, 2:0, 1:1
        assertEquals(4, away.of(mainz).orElseThrow().balance().points());
        // Köln at home: only the 0:2 against Mainz
        assertEquals(0, home.of(koeln).orElseThrow().balance().points());
        assertEquals(1, home.of(koeln).orElseThrow().balance().played());
        // Mainz's last five final matches: matchdays 2 to 6
        assertEquals(5, last.of(mainz).orElseThrow().balance().played());
        assertEquals(5, last.of(mainz).orElseThrow().balance().points());
        assertEquals(1, last.of(mainz).orElseThrow().position(), "5 points ahead of Bremen's 4");
        assertFalse(home.includesProvisional());
    }

    private static Match running(Team home, Team away, Score liveScore) {
        Match match = new Match();
        match.homeTeam = home;
        match.awayTeam = away;
        match.liveScore = liveScore;
        return match;
    }
}
