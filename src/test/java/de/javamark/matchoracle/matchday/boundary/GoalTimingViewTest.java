package de.javamark.matchoracle.matchday.boundary;

import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.GoalTimingView;
import de.javamark.matchoracle.matchday.entity.GoalTiming;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Story r016: the sentence over the goal-timing chart — strongest phase, ties, too few goals, none. */
class GoalTimingViewTest {

    private static List<GoalTiming> timings(int... scored) {
        String[] labels = {"1.-15.", "16.-30.", "31.-45.", "46.-60.", "61.-75.", "76.-90."};
        return java.util.stream.IntStream.range(0, 6).mapToObj(i -> new GoalTiming(labels[i], scored[i], 1)).toList();
    }

    @Test
    void namesTheStrongestPhaseOfAClub() {
        assertEquals("Bremen trifft am häufigsten zwischen der 76. und der 90. Minute (6 von 15 Toren).",
                GoalTimingView.ofTeam("Bremen", timings(1, 2, 2, 2, 2, 6)).sentence());
    }

    @Test
    void namesEveryPhaseOnATie() {
        assertEquals("Die meisten Tore fallen zwischen der 1. und der 15. Minute sowie zwischen der 76. und der 90. Minute (je 4 von 10).",
                GoalTimingView.ofLeague(timings(4, 0, 1, 0, 1, 4)).sentence());
    }

    @Test
    void fourGoalsAreTooFewFiveAreEnough() {
        assertEquals("Noch zu wenig Tore für eine Aussage.", GoalTimingView.ofTeam("Kiel", timings(1, 1, 1, 1, 0, 0)).sentence());
        assertTrue(GoalTimingView.ofTeam("Kiel", timings(1, 1, 1, 1, 1, 0)).sentence().startsWith("Kiel trifft"));
    }

    @Test
    void noGoalAtAll() {
        GoalTimingView v = GoalTimingView.ofTeam("Kiel", timings(0, 0, 0, 0, 0, 0));
        assertEquals("Noch kein Tor in dieser Saison.", v.sentence());
        assertTrue(v.empty());
    }
}
