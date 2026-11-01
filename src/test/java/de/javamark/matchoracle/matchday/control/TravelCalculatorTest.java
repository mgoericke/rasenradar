package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.control.Stadiums.Stadium;
import de.javamark.matchoracle.matchday.control.TravelCalculator.Derby;
import de.javamark.matchoracle.matchday.control.TravelCalculator.Trips;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.Score;
import de.javamark.matchoracle.matchday.entity.Team;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Story r060: kilometres to the away grounds and the derbies of a season. */
class TravelCalculatorTest {

    private static final Stadium KOELN = new Stadium(65, "1. FC Köln", "RheinEnergieStadion", "Köln", 50.9335, 6.8752);
    private static final Stadium LEVERKUSEN = new Stadium(6, "Bayer 04 Leverkusen", "BayArena", "Leverkusen", 51.0383, 7.0022);
    private static final Stadium MUENCHEN = new Stadium(40, "FC Bayern München", "Allianz Arena", "München", 48.2188, 11.6247);

    private static Team team(String name) {
        Team t = new Team();
        t.name = name;
        return t;
    }

    private static Match match(Team home, Team away, boolean played, String kickoff) {
        Match m = new Match();
        m.homeTeam = home;
        m.awayTeam = away;
        m.kickoff = Instant.parse(kickoff);
        if (played) {
            m.fullTimeScore = new Score(1, 0);
        }
        return m;
    }

    private final Team koeln = team("1. FC Köln");
    private final Team leverkusen = team("Bayer 04 Leverkusen");
    private final Team bayern = team("FC Bayern München");
    private final Team unknown = team("Neuling ohne Stadion");
    private final Map<Team, Stadium> grounds = Map.of(koeln, KOELN, leverkusen, LEVERKUSEN, bayern, MUENCHEN);

    @Test
    void koelnAndLeverkusenAreAboutFifteenKilometresApart() {
        assertEquals(15, Stadiums.km(KOELN, LEVERKUSEN));
    }

    @Test
    void tripsAreSplitIntoTravelledAndAheadWithTheLongestNamedAndUnknownClubsLast() {
        List<Match> season = List.of(
                match(bayern, koeln, true, "2026-09-01T18:30:00Z"),
                match(leverkusen, koeln, false, "2026-12-01T18:30:00Z"),
                match(koeln, unknown, true, "2026-09-08T18:30:00Z"),
                match(unknown, koeln, false, "2027-01-08T18:30:00Z"));

        List<Trips> trips = TravelCalculator.trips(season, TravelCalculator.of(grounds));

        Trips k = trips.stream().filter(t -> t.team() == koeln).findFirst().orElseThrow();
        assertEquals(Stadiums.km(KOELN, MUENCHEN), k.travelled());
        assertEquals(15, k.ahead());                         // the trip to the unknown ground counts nothing
        assertEquals("München", k.longest().city());
        Trips last = trips.getLast();
        assertEquals(unknown, last.team());
        assertFalse(last.known());
        assertNull(last.longest());
        assertEquals(koeln, trips.getFirst().team());
    }

    @Test
    void aDerbyIsAPairingWithinSixtyKilometresWithBothMeetings() {
        List<Match> season = List.of(
                match(koeln, leverkusen, true, "2026-09-15T18:30:00Z"),
                match(leverkusen, koeln, false, "2027-02-15T18:30:00Z"),
                match(bayern, koeln, true, "2026-09-01T18:30:00Z"));

        List<Derby> derbies = TravelCalculator.derbies(season, TravelCalculator.of(grounds));

        assertEquals(1, derbies.size());
        assertEquals(15, derbies.getFirst().km());
        assertEquals(2, derbies.getFirst().meetings().size());
        assertTrue(derbies.getFirst().meetings().getFirst().isPlayed());
    }

    @Test
    void theStadiumFileKnowsTheClubsByTheirOpenLigaDbId() {
        assertEquals("München", Stadiums.of(40).orElseThrow().city());
        assertTrue(Stadiums.of(-1).isEmpty());
    }
}
