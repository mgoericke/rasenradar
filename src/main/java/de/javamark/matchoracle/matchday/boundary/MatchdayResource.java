package de.javamark.matchoracle.matchday.boundary;

import de.javamark.matchoracle.matchday.boundary.MatchdayRepresentations.FormRep;
import de.javamark.matchoracle.matchday.boundary.MatchdayRepresentations.HeadToHeadRep;
import de.javamark.matchoracle.matchday.boundary.MatchdayRepresentations.MatchDetailRep;
import de.javamark.matchoracle.matchday.boundary.MatchdayRepresentations.MatchRep;
import de.javamark.matchoracle.matchday.boundary.MatchdayRepresentations.MatchdayRep;
import de.javamark.matchoracle.matchday.boundary.MatchdayRepresentations.StandingRep;
import de.javamark.matchoracle.matchday.boundary.MatchdayRepresentations.StandingsRep;
import de.javamark.matchoracle.matchday.boundary.MatchdayRepresentations.TeamRep;
import de.javamark.matchoracle.matchday.boundary.MatchdayRepresentations.TeamSituationRep;
import de.javamark.matchoracle.matchday.boundary.MatchdayRepresentations.DataRep;
import de.javamark.matchoracle.matchday.control.FormCalculator;
import de.javamark.matchoracle.matchday.control.HeadToHeadCalculator;
import de.javamark.matchoracle.matchday.control.StandingsCalculator;
import de.javamark.matchoracle.matchday.entity.League;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.Matchday;
import de.javamark.matchoracle.matchday.entity.Standings;
import de.javamark.matchoracle.matchday.entity.Team;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Arrays;

/**
 * Spec 01, actor "viewer": matchdays, matches with their situation, standings.
 * Leagues are addressed as {@code bl1} / {@code bl2}.
 */
@Path("/leagues/{league}")
@Produces(MediaType.APPLICATION_JSON)
@Transactional(Transactional.TxType.SUPPORTS) // read-only: gives Panache a session without a write transaction
public class MatchdayResource {

    @Inject
    FormCalculator formCalculator;

    @Inject
    StandingsCalculator standingsCalculator;

    @Inject
    HeadToHeadCalculator headToHeadCalculator;

    @GET
    @Path("/matchdays/current")
    public MatchdayRep currentMatchday(@PathParam("league") String league) {
        Matchday matchday = Matchday.findCurrent(league(league))
                .orElseThrow(() -> new NotFoundException("no matchday known yet for " + league));
        return MatchdayRep.of(matchday, Match.findByMatchday(matchday));
    }

    @GET
    @Path("/seasons/{season}/matchdays/{number}")
    public MatchdayRep matchday(@PathParam("league") String league, @PathParam("season") int season, @PathParam("number") int number) {
        Matchday matchday = Matchday.find(league(league), season, number)
                .orElseThrow(() -> new NotFoundException("matchday not found"));
        return MatchdayRep.of(matchday, Match.findByMatchday(matchday));
    }

    /** Standings before the given matchday; use the number after the last one for the current table. */
    @GET
    @Path("/seasons/{season}/standings/before/{matchday}")
    public StandingsRep standingsBefore(@PathParam("league") String league, @PathParam("season") int season, @PathParam("matchday") int matchday) {
        return StandingsRep.of(standingsCalculator.standingsBefore(league(league), season, matchday));
    }

    /** Current standings: everything played up to and including the current matchday. */
    @GET
    @Path("/standings")
    public StandingsRep currentStandings(@PathParam("league") String league) {
        Matchday current = Matchday.findCurrent(league(league))
                .orElseThrow(() -> new NotFoundException("no matchday known yet for " + league));
        return StandingsRep.of(standingsCalculator.standingsBefore(current.league, current.season, current.number + 1));
    }

    /** A match with form and standing of both teams and their previous meetings — all "as of before this match". */
    @GET
    @Path("/matches/{id}")
    public MatchDetailRep match(@PathParam("league") String league, @PathParam("id") long id) {
        Match match = Match.<Match>findByIdOptional(id)
                .filter(m -> m.matchday.league == league(league))
                .orElseThrow(() -> new NotFoundException("match not found"));
        Matchday matchday = match.matchday;
        Standings standings = standingsCalculator.standingsBefore(matchday);
        return new MatchDetailRep(MatchRep.of(match), matchday.league, matchday.season, matchday.number, DataRep.of(matchday),
                situation(match.homeTeam, matchday, standings),
                situation(match.awayTeam, matchday, standings),
                HeadToHeadRep.of(headToHeadCalculator.headToHeadBefore(match)));
    }

    private TeamSituationRep situation(Team team, Matchday matchday, Standings standings) {
        StandingRep standing = standings.of(team).map(StandingRep::of).orElse(null);
        FormRep form = FormRep.of(formCalculator.formBefore(team, matchday));
        return new TeamSituationRep(TeamRep.of(team), standing, form);
    }

    private static League league(String shortcut) {
        return Arrays.stream(League.values())
                .filter(l -> l.sourceShortcut().equalsIgnoreCase(shortcut))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("unknown league " + shortcut + ", use bl1 or bl2"));
    }
}
