package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.control.StreakCalculator.Kind;
import de.javamark.matchoracle.matchday.control.StreakCalculator.Streak;
import de.javamark.matchoracle.matchday.control.StreakCalculator.Streaks;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.Team;
import org.junit.jupiter.api.Test;

import java.util.List;

import static de.javamark.matchoracle.matchday.control.MatchFixtures.played;
import static de.javamark.matchoracle.matchday.control.MatchFixtures.provisional;
import static de.javamark.matchoracle.matchday.control.MatchFixtures.team;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Story r068 "Serien": running streaks from the latest final result backwards, thresholds as decided. */
class StreakCalculatorTest {

    private final Team bremen = team("Bremen");
    private final Team koeln = team("Köln");

    @Test
    void threeWinsInARowAreAStreakTwoAreNot() {
        Streaks three = StreakCalculator.of(List.of(
                played(1, bremen, 0, koeln, 1), played(2, bremen, 2, koeln, 1),
                played(3, koeln, 0, bremen, 1), played(4, bremen, 3, koeln, 0)));
        Streaks two = StreakCalculator.of(List.of(
                played(1, bremen, 0, koeln, 1), played(2, bremen, 2, koeln, 1), played(3, koeln, 0, bremen, 1)));

        Streak bremenStreak = three.going().stream().filter(s -> s.team() == bremen).findFirst().orElseThrow();
        assertEquals(Kind.WINS, bremenStreak.kind());
        assertEquals(3, bremenStreak.length());
        assertFalse(bremenStreak.wholeSeason());
        assertTrue(two.going().stream().noneMatch(s -> s.team() == bremen && s.kind() == Kind.WINS));
    }

    @Test
    void theLongerRunWinsOverTheShorterKindAndCoversTheWholeSeason() {
        // 8 matches unbeaten, the last 3 of them wins: 8/4 unbeaten outranks 3/3 wins
        List<Match> matches = List.of(
                played(1, bremen, 1, koeln, 1), played(2, koeln, 0, bremen, 0), played(3, bremen, 2, koeln, 2),
                played(4, koeln, 1, bremen, 1), played(5, bremen, 0, koeln, 0), played(6, koeln, 0, bremen, 1),
                played(7, bremen, 2, koeln, 0), played(8, koeln, 1, bremen, 3));

        Streak s = StreakCalculator.of(matches).going().stream().filter(x -> x.team() == bremen).findFirst().orElseThrow();

        assertEquals(Kind.UNBEATEN, s.kind());
        assertEquals(8, s.length());
        assertTrue(s.wholeSeason());
        assertEquals(8, s.matches().size());
    }

    @Test
    void theLosingSideIsStuck() {
        Streaks s = StreakCalculator.of(List.of(
                played(1, bremen, 2, koeln, 0), played(2, koeln, 0, bremen, 1), played(3, bremen, 1, koeln, 0)));

        Streak koelnStreak = s.stuck().stream().filter(x -> x.team() == koeln).findFirst().orElseThrow();
        assertEquals(Kind.LOSSES, koelnStreak.kind());
        // three losses without a goal: equal strength, the kind listed first (losses) is shown
        assertEquals(3, koelnStreak.length());
    }

    @Test
    void aProvisionalResultDoesNotBreakOrExtendAStreak() {
        Streaks s = StreakCalculator.of(List.of(
                played(1, bremen, 2, koeln, 0), played(2, koeln, 0, bremen, 1), played(3, bremen, 1, koeln, 0),
                provisional(4, koeln, 3, bremen, 0)));

        Streak bremenStreak = s.going().stream().filter(x -> x.team() == bremen).findFirst().orElseThrow();
        assertEquals(Kind.WINS, bremenStreak.kind());
        assertEquals(3, bremenStreak.length());
    }
}
