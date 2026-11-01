package de.javamark.matchoracle.matchday.boundary;

import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.CupField;
import de.javamark.matchoracle.matchday.entity.Decision;
import de.javamark.matchoracle.matchday.entity.League;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.Matchday;
import de.javamark.matchoracle.matchday.entity.Score;
import de.javamark.matchoracle.matchday.entity.Team;
import de.javamark.matchoracle.matchday.entity.Tier;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** The cup page: which pairing goes on the stage, and who is still in the competition. */
class CupStageTest {

    private static final Team BAYERN = team("FC Bayern München");
    private static final Team MAINZ = team("1. FSV Mainz 05");
    private static final Team BOCHUM = team("VfL Bochum");
    private static final Team ESSEN = team("Rot-Weiss Essen");
    private static final Team JEDDELOH = team("SSV Jeddeloh II");
    private static final Map<Team, Tier> TIERS = Map.of(BAYERN, Tier.FIRST, MAINZ, Tier.FIRST,
            BOCHUM, Tier.SECOND, ESSEN, Tier.THIRD, JEDDELOH, Tier.LOWER);
    private static final Function<Team, Tier> TIER_OF = TIERS::get;

    private static Team team(String name) {
        Team t = new Team();
        t.name = name;
        return t;
    }

    private static Matchday round(int number, String label) {
        Matchday r = new Matchday();
        r.league = League.DFB_POKAL;
        r.season = 2026;
        r.number = number;
        r.label = label;
        return r;
    }

    private static Match match(Matchday round, Team home, Team away, Integer homeGoals, Integer awayGoals, int day) {
        Match m = new Match();
        m.matchday = round;
        m.homeTeam = home;
        m.awayTeam = away;
        m.kickoff = Instant.parse("2026-10-2" + day + "T18:00:00Z");
        if (homeGoals != null) {
            m.fullTimeScore = new Score(homeGoals, awayGoals);
        }
        return m;
    }

    @Test
    void beforeTheRoundTheStageShowsThePairingOfTheHighestDivisions() {
        Matchday r = round(2, "2. Runde");
        Match amateurs = match(r, JEDDELOH, BAYERN, null, null, 7);
        Match top = match(r, MAINZ, BOCHUM, null, null, 8);

        var pick = MatchdayPages.stagePick(List.of(amateurs, top), TIER_OF);

        assertEquals(top, pick.match());
        assertEquals("Spiel der Runde", pick.label());
    }

    @Test
    void afterTheRoundTheBiggestSurpriseWinsElseTheMostGoals() {
        Matchday r = round(2, "2. Runde");
        Match small = match(r, BOCHUM, MAINZ, 2, 0, 7);     // 2. Liga beats 1. Liga
        Match big = match(r, JEDDELOH, ESSEN, 1, 0, 7);     // amateurs beat 3. Liga — also a gap of one
        Match bigger = match(r, ESSEN, BAYERN, 3, 2, 8);    // 3. Liga beats 1. Liga

        assertEquals(bigger, MatchdayPages.stagePick(List.of(small, big, bigger), TIER_OF).match());
        assertEquals("Überraschung der Runde", MatchdayPages.stagePick(List.of(small, big, bigger), TIER_OF).label());

        Match goals = match(r, BAYERN, MAINZ, 4, 3, 7);
        Match quiet = match(r, BOCHUM, ESSEN, 1, 0, 8);
        var noSurprise = MatchdayPages.stagePick(List.of(quiet, goals), TIER_OF);
        assertEquals(goals, noSurprise.match());
        assertEquals("Torreichstes Spiel der Runde", noSurprise.label());
    }

    @Test
    void theFieldKeepsEveryClubThatHasNotLostAndNamesTheRoundTheOthersWentOutIn() {
        Matchday first = round(1, "1. Runde");
        Matchday second = round(2, "2. Runde");
        Match a = match(first, JEDDELOH, BOCHUM, 1, 1, 1);
        a.decision = Decision.PENALTIES;
        a.penaltyScore = new Score(5, 4);                    // amateurs through on penalties
        Match b = match(first, ESSEN, BAYERN, 0, 3, 1);
        Match c = match(second, JEDDELOH, BAYERN, null, null, 7);
        Match d = match(second, MAINZ, MAINZ, null, null, 7);

        CupField field = MatchdayPages.cupField(List.of(second, first), List.of(c, d, a, b), TIER_OF, "dfb");

        assertEquals(List.of("1. Liga", "Amateur"), field.remaining().stream().map(g -> g.tier()).toList());
        assertEquals(3, field.remainingCount());
        assertEquals(List.of("1. Runde"), field.out().stream().map(e -> e.round()).toList());
        assertEquals(List.of("Rot-Weiss Essen", "VfL Bochum"), field.out().getFirst().clubs());
        assertNull(field.winner());
    }

    @Test
    void onceTheFinalIsPlayedTheFieldNamesTheWinner() {
        Matchday fin = round(6, "Endspiel");
        Match f = match(fin, BAYERN, MAINZ, 2, 0, 9);

        CupField field = MatchdayPages.cupField(List.of(fin), List.of(f), TIER_OF, "dfb");

        assertEquals("FC Bayern München", field.winner());
        assertEquals("Endspiel", MatchdayPages.stagePick(List.of(f), TIER_OF).label());
    }
}
