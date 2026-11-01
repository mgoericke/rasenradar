package de.javamark.matchoracle.season.control;

import de.javamark.matchoracle.season.control.SeasonTargets.Club;
import de.javamark.matchoracle.season.control.SeasonTargets.Status;
import de.javamark.matchoracle.season.control.SeasonTargets.Target;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Story r039: what a club still needs per placement goal. */
class SeasonTargetsTest {

    private static final List<SeasonTargets.Goal> BL1 = SeasonTargets.goals("bl1");

    /** 18 clubs, team id = table place, 3 points apart from 60 down; {@code remaining} matches each. */
    private static List<Club> league(int remaining) {
        List<Club> clubs = new ArrayList<>();
        for (int place = 1; place <= 18; place++) {
            clubs.add(new Club(place, 63 - 3 * place, remaining));
        }
        return clubs;
    }

    private static Map<Integer, Integer> medians() {
        Map<Integer, Integer> m = new HashMap<>();
        m.put(1, 78);
        m.put(4, 60);
        m.put(15, 35);
        return m;
    }

    private static Target goal(List<Target> targets, String label) {
        return targets.stream().filter(t -> t.label().equals(label)).findFirst().orElseThrow();
    }

    @Test
    void midSeasonEveryGoalGetsTypicalPointsMissingAndTheRates() {
        List<Target> targets = SeasonTargets.of(BL1, 4, league(14), medians(),
                Map.of("Europapokal", 0.42, "Abstiegsrelegation", 0.01, "Abstieg", 0.01)).orElseThrow();

        Target europe = goal(targets, "Europapokal");
        assertEquals(60, europe.typicalPoints());
        assertEquals(9, europe.missing());                  // 51 points so far
        assertEquals(14, europe.remaining());
        assertEquals(9 / 14.0, europe.requiredPerMatch(), 1e-9);
        assertEquals(51 / 20.0, europe.currentPerMatch(), 1e-9);
        assertEquals(Status.ON_PACE, europe.status());
        assertEquals(42, europe.percent());
        assertEquals(98, goal(targets, "Klassenerhalt").percent());
    }

    @Test
    void aGoalThatNeedsMoreThanThePaceSoFarStaysOpenAndOneBeyondTheRemainingMatchesSaysSo() {
        List<Target> tenth = SeasonTargets.of(BL1, 10, league(14), medians(), Map.of()).orElseThrow();

        Target europe = goal(tenth, "Europapokal");         // 33 points, 1.65 per match so far
        assertEquals(27, europe.missing());
        assertEquals(Status.OPEN, europe.status());
        assertEquals(Status.BEYOND_TYPICAL, goal(tenth, "Meisterschaft").status());   // 45 missing, 42 possible
    }

    @Test
    void securedAndOutOfReachAreCountedOnPointsAndAgainstTheClub() {
        List<Club> lastMatchday = league(1);

        Map<Integer, Integer> late = new HashMap<>(medians());
        late.put(1, 61);
        List<Target> leader = SeasonTargets.of(BL1, 1, lastMatchday, late, Map.of()).orElseThrow();
        assertEquals(Status.ON_PACE, goal(leader, "Meisterschaft").status());   // second can still draw level
        assertEquals(Status.SECURED, goal(leader, "Europapokal").status());

        List<Target> tenth = SeasonTargets.of(BL1, 10, lastMatchday, medians(), Map.of()).orElseThrow();
        assertEquals(Status.OUT_OF_REACH, goal(tenth, "Europapokal").status());
        assertEquals(Status.SECURED, goal(tenth, "Klassenerhalt").status());
    }

    @Test
    void beforeTheFirstMatchOrWithoutARunThereIsNothingToSay() {
        assertTrue(SeasonTargets.of(BL1, 4, league(34), medians(), Map.of()).isEmpty());
        assertTrue(SeasonTargets.of(BL1, 4, league(14), Map.of(), Map.of()).isEmpty());
        assertTrue(SeasonTargets.of(SeasonTargets.goals("ucl"), 4, league(14), medians(), Map.of()).isEmpty());
    }
}
