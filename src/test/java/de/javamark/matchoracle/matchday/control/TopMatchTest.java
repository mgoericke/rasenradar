package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.Team;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static de.javamark.matchoracle.matchday.control.MatchFixtures.played;
import static de.javamark.matchoracle.matchday.control.MatchFixtures.team;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** The top match of the start page. */
class TopMatchTest {

    private static final Duration WINDOW = Duration.ofMinutes(150);
    private static final Instant NOW = Instant.parse("2026-11-07T15:00:00Z");

    private final Team first = team("Erster"), second = team("Zweiter"), third = team("Dritter"),
            tenth = team("Zehnter"), eleventh = team("Elfter"), last = team("Letzter");
    private final Map<Team, Integer> places = Map.of(first, 1, second, 2, third, 3, tenth, 10, eleventh, 11, last, 18);

    @Test
    void aRunningMatchComesFirstEvenIfLowerInTheTable() {
        Match running = open(tenth, eleventh, NOW.minus(Duration.ofMinutes(30)));
        Match later = open(first, second, NOW.plus(Duration.ofHours(3)));
        assertEquals(Optional.of(running), TopMatch.pick(List.of(later, running), places, NOW, WINDOW));
    }

    @Test
    void amongOpenMatchesOfTheNextDayTheBestPairingWins() {
        Match early = open(tenth, last, NOW.plus(Duration.ofHours(1)));
        Match top = open(second, first, NOW.plus(Duration.ofHours(4)));
        Match nextWeek = open(first, third, NOW.plus(Duration.ofDays(7)));
        assertEquals(Optional.of(top), TopMatch.pick(List.of(early, top, nextWeek), places, NOW, WINDOW));
    }

    @Test
    void onATieTheEarlierKickoffWins() {
        Match a = open(first, last, NOW.plus(Duration.ofHours(2)));
        Match b = open(tenth, eleventh, NOW.plus(Duration.ofHours(1)));
        // 1 + 18 = 19 against 10 + 11 = 21: the place sum decides before the kickoff
        assertEquals(Optional.of(a), TopMatch.pick(List.of(b, a), places, NOW, WINDOW));
        Match c = open(second, last, NOW.plus(Duration.ofHours(3)));
        Match d = open(third, team("Unbekannt"), NOW.plus(Duration.ofHours(1)));
        assertEquals(Optional.of(c), TopMatch.pick(List.of(c, d), places, NOW, WINDOW), "a club without a place counts as last");
    }

    @Test
    void withNothingOpenTheBestPlayedMatchStays() {
        Match low = played(8, tenth, 1, last, 0);
        Match high = played(8, first, 2, third, 2);
        assertEquals(Optional.of(high), TopMatch.pick(List.of(low, high), places, NOW, WINDOW));
        assertEquals(Optional.empty(), TopMatch.pick(List.of(), places, NOW, WINDOW));
    }

    private static Match open(Team home, Team away, Instant kickoff) {
        Match m = new Match();
        m.homeTeam = home;
        m.awayTeam = away;
        m.kickoff = kickoff;
        return m;
    }
}
