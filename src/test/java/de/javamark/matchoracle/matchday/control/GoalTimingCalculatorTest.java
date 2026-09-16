package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.entity.GoalTiming;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.Team;
import org.junit.jupiter.api.Test;

import java.util.List;

import static de.javamark.matchoracle.matchday.control.MatchFixtures.goal;
import static de.javamark.matchoracle.matchday.control.MatchFixtures.played;
import static de.javamark.matchoracle.matchday.control.MatchFixtures.team;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Spec 01: when goals fall, bucketed in 15-minute windows, for the club page. */
class GoalTimingCalculatorTest {

    private final Team mainz = team("Mainz");
    private final Team leipzig = team("Leipzig");

    private final GoalTimingCalculator calculator = new GoalTimingCalculator();

    @Test
    void bucketsGoalsInFifteenMinuteWindows() {
        Match match = played(1, mainz, 2, leipzig, 1);
        goal(match, "Burkardt", 1, 0).minute = 10;   // window 1-15, scored
        goal(match, "Openda", 1, 1).minute = 40;     // window 31-45, conceded
        goal(match, "Amiri", 2, 1).minute = 88;      // window 76-90, scored

        List<GoalTiming> timing = calculator.timing(mainz, List.of(match));

        assertEquals(new GoalTiming("1.-15.", 1, 0), timing.get(0));
        assertEquals(new GoalTiming("31.-45.", 0, 1), timing.get(2));
        assertEquals(new GoalTiming("76.-90.", 1, 0), timing.get(5));
    }

    @Test
    void stoppageTimeCountsIntoTheLastWindowOfItsHalf() {
        Match match = played(1, mainz, 1, leipzig, 0);
        goal(match, "Amiri", 1, 0).minute = 93;

        List<GoalTiming> timing = calculator.timing(mainz, List.of(match));

        assertEquals(new GoalTiming("76.-90.", 1, 0), timing.get(5));
    }

    @Test
    void ownGoalOfTheOpponentCountsAsScoredForTheTeamAndConcededForTheOpponent() {
        Match match = played(1, mainz, 1, leipzig, 0);
        goal(match, "Orban", 1, 0).ownGoal = true;
        var withMinute = match.goals.get(0);
        withMinute.minute = 20;

        List<GoalTiming> mainzTiming = calculator.timing(mainz, List.of(match));
        List<GoalTiming> leipzigTiming = calculator.timing(leipzig, List.of(match));

        assertEquals(new GoalTiming("16.-30.", 1, 0), mainzTiming.get(1));
        assertEquals(new GoalTiming("16.-30.", 0, 1), leipzigTiming.get(1));
    }

    @Test
    void goalsWithoutAKnownMinuteAreExcluded() {
        Match match = played(1, mainz, 1, leipzig, 0);
        goal(match, "Amiri", 1, 0);   // minute stays null

        List<GoalTiming> timing = calculator.timing(mainz, List.of(match));

        assertEquals(0, timing.stream().mapToInt(GoalTiming::scored).sum());
    }

    @Test
    void sixWindowsAlwaysReturnedEvenWithoutGoals() {
        List<GoalTiming> timing = calculator.timing(mainz, List.of());

        assertEquals(6, timing.size());
        assertEquals("1.-15.", timing.get(0).label());
        assertEquals("76.-90.", timing.get(5).label());
    }
}
