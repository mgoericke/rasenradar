package de.javamark.matchoracle.season.control;

import de.javamark.matchoracle.matchday.boundary.MatchSituation;
import de.javamark.matchoracle.matchday.boundary.MatchdayFacade;
import de.javamark.matchoracle.matchday.boundary.ScorelineForecast;
import de.javamark.matchoracle.season.entity.SeasonOutlook;
import de.javamark.matchoracle.season.entity.SeasonThreshold;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Spec 04, step 4: recomputes and fully replaces a league-season's outlook once its matchday is done. */
@ApplicationScoped
public class SeasonOutlookService {

    @Inject
    MatchdayFacade matchday;

    @Inject
    SeasonSimulator simulator;

    public void recompute(String league, int season) {
        recompute(league, season, new Random());
    }

    @Transactional
    void recompute(String league, int season, Random random) {
        List<MatchdayFacade.TeamState> teams = matchday.currentSeasonState(league);
        List<String> allGoals = matchday.placementGoals(league);
        if (teams.isEmpty() || allGoals.isEmpty()) {
            return;
        }
        Map<Long, MatchdayFacade.TeamState> byId = teams.stream()
                .collect(Collectors.toMap(MatchdayFacade.TeamState::teamId, Function.identity()));

        List<MatchdayFacade.Fixture> open = matchday.remainingFixtures(league);
        Map<Long, Long> remaining = open.stream()
                .flatMap(f -> Stream.of(f.homeTeamId(), f.awayTeamId()))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        List<SeasonSimulator.Fixture> fixtures = open.stream()
                .map(f -> toSimFixture(f, byId))
                .toList();
        List<SeasonSimulator.TeamState> simTeams = teams.stream()
                .map(t -> new SeasonSimulator.TeamState(t.teamId(), t.points(), t.goalDifference(), t.goalsFor()))
                .toList();

        SeasonSimulator.Result result = simulator.simulateSeason(simTeams, fixtures, allGoals,
                position -> matchday.placementGoalsAt(league, position), random);

        SeasonOutlook.deleteByLeagueSeason(league, season);
        Instant now = Instant.now();
        for (SeasonSimulator.Outcome outcome : result.outcomes()) {
            SeasonOutlook row = new SeasonOutlook();
            row.league = league;
            row.season = season;
            row.teamId = outcome.teamId();
            row.computedAt = now;
            row.points = byId.get(outcome.teamId()).points();
            row.remaining = remaining.getOrDefault(outcome.teamId(), 0L).intValue();
            row.probabilities.putAll(outcome.probabilities());
            row.persist();
        }
        SeasonThreshold.deleteByLeagueSeason(league, season);
        result.medianPointsAtPosition().forEach((position, points) -> {
            SeasonThreshold threshold = new SeasonThreshold();
            threshold.league = league;
            threshold.season = season;
            threshold.position = position;
            threshold.points = points;
            threshold.persist();
        });
    }

    private SeasonSimulator.Fixture toSimFixture(MatchdayFacade.Fixture fixture, Map<Long, MatchdayFacade.TeamState> byId) {
        MatchSituation.Balance homeRecord = byId.get(fixture.homeTeamId()).homeRecord();
        MatchSituation.Balance awayRecord = byId.get(fixture.awayTeamId()).awayRecord();
        ScorelineForecast forecast = matchday.scorelineForecast(homeRecord, awayRecord);
        return new SeasonSimulator.Fixture(fixture.homeTeamId(), fixture.awayTeamId(),
                forecast.expectedHomeGoals(), forecast.expectedAwayGoals());
    }
}
