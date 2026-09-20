package de.javamark.matchoracle.season.control;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Spec 04: remaining-season simulation, one Poisson-drawn scoreline per fixture per run. */
class SeasonSimulatorTest {

    private final SeasonSimulator simulator = new SeasonSimulator();

    @Test
    void withNoRemainingFixturesTheCurrentTableDecidesWithCertainty() {
        List<SeasonSimulator.TeamState> teams = List.of(
                new SeasonSimulator.TeamState(1L, 30, 20, 40),
                new SeasonSimulator.TeamState(2L, 20, 0, 25));

        List<SeasonSimulator.Outcome> outcomes = simulator.simulate(teams, List.of(), List.of("Meisterschaft"),
                position -> position == 1 ? List.of("Meisterschaft") : List.of(), new Random(1));

        Map<Long, Double> byTeam = outcomes.stream()
                .collect(Collectors.toMap(SeasonSimulator.Outcome::teamId, o -> o.probabilities().get("Meisterschaft")));
        assertEquals(1.0, byTeam.get(1L), 1e-9);
        assertEquals(0.0, byTeam.get(2L), 1e-9);
    }

    @Test
    void aBigLeadWithNoFixturesLeftIsCertain() {
        List<SeasonSimulator.TeamState> teams = List.of(
                new SeasonSimulator.TeamState(1L, 60, 40, 70),
                new SeasonSimulator.TeamState(2L, 30, 0, 30));

        List<SeasonSimulator.Outcome> outcomes = simulator.simulate(teams, List.of(), List.of("Meisterschaft"),
                position -> position == 1 ? List.of("Meisterschaft") : List.of(), new Random(42));

        double leaderTitleChance = outcomes.stream().filter(o -> o.teamId() == 1L).findFirst().orElseThrow()
                .probabilities().get("Meisterschaft");
        assertEquals(1.0, leaderTitleChance, 1e-9);
    }

    /**
     * A small, still-closeable lead with one head-to-head fixture left: the leader only loses the
     * title if the trailer wins outright (a draw or leader win both leave the leader ahead) — a
     * real, RNG-dependent event, unlike a lead too big for any single result to close.
     */
    @Test
    void aCloseableLeadGivesAHighButNotCertainTitleProbability() {
        List<SeasonSimulator.TeamState> teams = List.of(
                new SeasonSimulator.TeamState(1L, 32, 10, 40),
                new SeasonSimulator.TeamState(2L, 30, 8, 35));
        List<SeasonSimulator.Fixture> fixtures = List.of(new SeasonSimulator.Fixture(1L, 2L, 1.6, 1.0));

        List<SeasonSimulator.Outcome> outcomes = simulator.simulate(teams, fixtures, List.of("Meisterschaft"),
                position -> position == 1 ? List.of("Meisterschaft") : List.of(), new Random(42));

        double leaderTitleChance = outcomes.stream().filter(o -> o.teamId() == 1L).findFirst().orElseThrow()
                .probabilities().get("Meisterschaft");
        assertTrue(leaderTitleChance > 0.7 && leaderTitleChance < 0.95, "title chance " + leaderTitleChance);
    }

    @Test
    void probabilitiesForEveryDeclaredGoalArePresentEvenAtZero() {
        List<SeasonSimulator.TeamState> teams = List.of(new SeasonSimulator.TeamState(1L, 0, 0, 0));

        List<SeasonSimulator.Outcome> outcomes = simulator.simulate(teams, List.of(), List.of("Meisterschaft", "Abstieg"),
                position -> List.of("Meisterschaft"), new Random(1));

        Map<String, Double> probabilities = outcomes.get(0).probabilities();
        assertEquals(1.0, probabilities.get("Meisterschaft"), 1e-9);
        assertEquals(0.0, probabilities.get("Abstieg"), 1e-9);
    }

    @Test
    void poissonSamplesAverageCloseToTheirExpectedValue() {
        Random random = new Random(7);
        long sum = 0;
        int samples = 50_000;
        for (int i = 0; i < samples; i++) {
            sum += SeasonSimulator.poisson(2.3, random);
        }
        double average = sum / (double) samples;
        assertTrue(Math.abs(average - 2.3) < 0.05, "average " + average);
    }
}
