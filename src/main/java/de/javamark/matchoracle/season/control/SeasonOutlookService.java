package de.javamark.matchoracle.season.control;

import de.javamark.matchoracle.matchday.boundary.MatchSituation;
import de.javamark.matchoracle.matchday.boundary.MatchdayFacade;
import de.javamark.matchoracle.matchday.boundary.ScorelineForecast;
import de.javamark.matchoracle.season.entity.SeasonOutlook;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Random;

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
                .collect(java.util.stream.Collectors.toMap(MatchdayFacade.TeamState::teamId, t -> t));

        List<SeasonSimulator.Fixture> fixtures = matchday.remainingFixtures(league).stream()
                .map(f -> toSimFixture(f, byId))
                .toList();
        List<SeasonSimulator.TeamState> simTeams = teams.stream()
                .map(t -> new SeasonSimulator.TeamState(t.teamId(), t.points(), t.goalDifference(), t.goalsFor()))
                .toList();

        List<SeasonSimulator.Outcome> outcomes = simulator.simulate(simTeams, fixtures, allGoals,
                position -> matchday.placementGoalsAt(league, position), random);

        SeasonOutlook.deleteByLeagueSeason(league, season);
        Instant now = Instant.now();
        for (SeasonSimulator.Outcome outcome : outcomes) {
            SeasonOutlook row = new SeasonOutlook();
            row.league = league;
            row.season = season;
            row.teamId = outcome.teamId();
            row.computedAt = now;
            row.probabilities.putAll(outcome.probabilities());
            row.persist();
        }
    }

    private SeasonSimulator.Fixture toSimFixture(MatchdayFacade.Fixture fixture, Map<Long, MatchdayFacade.TeamState> byId) {
        MatchSituation.Balance homeRecord = byId.get(fixture.homeTeamId()).homeRecord();
        MatchSituation.Balance awayRecord = byId.get(fixture.awayTeamId()).awayRecord();
        ScorelineForecast forecast = matchday.scorelineForecast(homeRecord, awayRecord);
        return new SeasonSimulator.Fixture(fixture.homeTeamId(), fixture.awayTeamId(),
                forecast.expectedHomeGoals(), forecast.expectedAwayGoals());
    }
}
