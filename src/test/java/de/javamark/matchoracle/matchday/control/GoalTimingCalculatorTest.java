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

    @Test
    void inTheCupExtraTimeGoalsGetAWindowOfTheirOwn() {
        Match extra = played(1, mainz, 2, leipzig, 1);
        extra.decision = de.javamark.matchoracle.matchday.entity.Decision.EXTRA_TIME;
        goal(extra, "Burkardt", 1, 0).minute = 30;
        goal(extra, "Openda", 1, 1).minute = 80;
        goal(extra, "Amiri", 2, 1).minute = 112;
        Match regular = played(1, leipzig, 1, mainz, 0);
        goal(regular, "Openda", 1, 0).minute = 93;   // Nachspielzeit stays with 76.-90.

        List<GoalTiming> cup = GoalTimingCalculator.cup(List.of(extra, regular));

        assertEquals(7, cup.size());
        assertEquals(new GoalTiming("76.-90.", 2, 0), cup.get(5));
        assertEquals(new GoalTiming("91.-120.", 1, 0), cup.get(6));
        assertEquals(6, GoalTimingCalculator.league(List.of(extra)).size());
    }
}
