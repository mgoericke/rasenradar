package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.control.ScorerProfileCalculator.Profile;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.Team;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static de.javamark.matchoracle.matchday.control.MatchFixtures.goal;
import static de.javamark.matchoracle.matchday.control.MatchFixtures.played;
import static de.javamark.matchoracle.matchday.control.MatchFixtures.provisional;
import static de.javamark.matchoracle.matchday.control.MatchFixtures.team;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Story r063: a scorer's profile from the goal list only — own goals never count, the share only from final results. */
class ScorerProfileCalculatorTest {

    private final Team bremen = team("Bremen");
    private final Team koeln = team("Köln");

    @Test
    void goalsMatchesPenaltiesAndTheShareOfTheClubsGoals() {
        Match a = played(1, bremen, 3, koeln, 1);
        goal(a, "Füllkrug", 1, 0).minute = 10;
        goal(a, "Füllkrug", 2, 0).minute = 55;
        a.goals.get(1).penalty = true;
        goal(a, "Hein", 2, 1).minute = 60;          // Köln
        goal(a, "Ducksch", 3, 1).minute = 85;
        Match b = played(2, koeln, 1, bremen, 1);
        goal(b, "Kainz", 1, 0).minute = 30;          // Köln
        goal(b, "Hein", 1, 1).minute = 88;           // a Köln own goal: counts for Bremen, not for the scorer
        b.goals.get(1).ownGoal = true;

        Map<String, Profile> profiles = ScorerProfileCalculator.of(bremen, List.of(a, b));
        Profile f = profiles.get("Füllkrug");

        assertEquals(2, f.count());
        assertEquals(1, f.matches());
        assertEquals(1, f.penalties());
        assertEquals(2, f.homeGoals());
        assertEquals(1, f.firstHalf());
        assertEquals(1, f.secondHalf());
        assertEquals(50, f.sharePercent()); // 2 of Bremen's 4 goals (the own goal included)
        assertFalse(profiles.containsKey("Hein"));
        // newest first: the 55th-minute goal, then the 10th
        assertEquals(55, f.goals().getFirst().minute());
        assertEquals(2, f.goals().getFirst().forAfter());
        assertEquals(3, f.goals().getFirst().forFinal());
    }

    @Test
    void aProvisionalGoalIsListedButNotCountedInTheShare() {
        Match done = played(1, bremen, 1, koeln, 0);
        goal(done, "Ducksch", 1, 0).minute = 20;
        Match running = provisional(2, koeln, 0, bremen, 1);
        goal(running, "Ducksch", 0, 1).minute = 70;

        Profile d = ScorerProfileCalculator.of(bremen, List.of(done, running)).get("Ducksch");

        assertEquals(2, d.count());
        assertEquals(1, d.finalGoals());
        assertEquals(100, d.sharePercent());
    }
}
