package de.javamark.matchoracle.matchday.boundary;

import de.javamark.matchoracle.matchday.boundary.MatchSituation.Balance;
import de.javamark.matchoracle.matchday.boundary.MatchSituation.Meeting;
import de.javamark.matchoracle.matchday.boundary.MatchSituation.Result;
import de.javamark.matchoracle.matchday.boundary.MatchSituation.TeamSituation;
import de.javamark.matchoracle.matchday.control.FormCalculator;
import de.javamark.matchoracle.matchday.control.HeadToHeadCalculator;
import de.javamark.matchoracle.matchday.control.PoissonScoreModel;
import de.javamark.matchoracle.matchday.control.StandingsCalculator;
import de.javamark.matchoracle.matchday.entity.Form;
import de.javamark.matchoracle.matchday.entity.HeadToHead;
import de.javamark.matchoracle.matchday.entity.League;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.Matchday;
import de.javamark.matchoracle.matchday.entity.ResultStatus;
import de.javamark.matchoracle.matchday.entity.Standings;
import de.javamark.matchoracle.matchday.entity.Team;
import de.javamark.matchoracle.matchday.entity.TeamMatchView;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

/** Java entry point for other features (forecast, review). Cross-feature access goes through here, never through entities. */
@ApplicationScoped
public class MatchdayFacade {

    @Inject
    FormCalculator formCalculator;

    @Inject
    StandingsCalculator standingsCalculator;

    @Inject
    HeadToHeadCalculator headToHeadCalculator;

    @Inject
    PoissonScoreModel poissonScoreModel;

    /** The situation before a match, with the form window given by the caller (a forecast parameter). */
    @Transactional(Transactional.TxType.SUPPORTS)
    public Optional<MatchSituation> situationOf(long matchId, int formMatches) {
        return Match.<Match>findByIdOptional(matchId).map(match -> situation(match, formMatches));
    }

    /** A plain statistical estimate from both sides' goal averages — the Rückschau yardstick and the KI-Vorschau's starting point. */
    public ScorelineForecast scorelineForecast(MatchSituation situation) {
        var d = poissonScoreModel.forecast(entityBalance(situation.homeTeam().homeBalance()), entityBalance(situation.awayTeam().awayBalance()));
        return scorelineForecast(d);
    }

    /** Same grid, from expected goals already adjusted by a caller (the forecast agent's read) — consistency guaranteed either way. */
    public ScorelineForecast scorelineForecast(double expectedHomeGoals, double expectedAwayGoals) {
        return scorelineForecast(poissonScoreModel.fromExpectedGoals(expectedHomeGoals, expectedAwayGoals));
    }

    private static ScorelineForecast scorelineForecast(de.javamark.matchoracle.matchday.entity.ScoreDistribution d) {
        return new ScorelineForecast(d.expectedHomeGoals(), d.expectedAwayGoals(), d.homeGoals(), d.awayGoals(),
                d.scoreProbability(), d.homeWin(), d.draw(), d.awayWin());
    }

    private static de.javamark.matchoracle.matchday.entity.Balance entityBalance(Balance b) {
        return new de.javamark.matchoracle.matchday.entity.Balance(b.wins(), b.draws(), b.losses(), b.goalsFor(), b.goalsAgainst());
    }

    /** Ids of all matches whose result is FINAL — the review derives a situation for each of them. */
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<Long> finalMatchIds() {
        return Match.getEntityManager()
                .createQuery("select m.id from Match m where m.resultStatus = :status", Long.class)
                .setParameter("status", ResultStatus.FINAL)
                .getResultList();
    }

    /** The latest known season of a league (the current one), e.g. 2026. */
    @Transactional(Transactional.TxType.SUPPORTS)
    public Optional<Integer> currentSeason(String league) {
        return League.bySourceShortcut(league).flatMap(Matchday::latestSeason);
    }

    /** Played matches of a matchday, for the "backtest the whole matchday" action. */
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<Long> playedMatchIds(String league, int season, int number) {
        return League.bySourceShortcut(league).flatMap(l -> Matchday.find(l, season, number))
                .map(md -> Match.findByMatchday(md).stream().filter(Match::isPlayed).map(m -> m.id).toList())
                .orElse(List.of());
    }

    /** Resolves a league shortcut (bl1/bl2); empty if unknown. Other features never see the enum. */
    public Optional<String> leagueShortcut(String shortcut) {
        return League.bySourceShortcut(shortcut).map(League::sourceShortcut);
    }

    /** All matches of a matchday in kickoff order. */
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<Long> matchIds(String league, int season, int number) {
        return League.bySourceShortcut(league).flatMap(l -> Matchday.find(l, season, number))
                .map(md -> Match.findByMatchday(md).stream().sorted((a, b) -> a.kickoff.compareTo(b.kickoff)).map(m -> m.id).toList())
                .orElse(List.of());
    }

    /** Unplayed matches of a matchday, for the "forecast the whole matchday" action. */
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<Long> unplayedMatchIds(String league, int season, int number) {
        return League.bySourceShortcut(league).flatMap(l -> Matchday.find(l, season, number))
                .map(md -> Match.findByMatchday(md).stream().filter(m -> !m.isPlayed()).map(m -> m.id).toList())
                .orElse(List.of());
    }

    private MatchSituation situation(Match match, int formMatches) {
        Matchday matchday = match.matchday;
        Standings standings = standingsCalculator.standingsBefore(matchday);
        Form homeForm = formCalculator.formBefore(match.homeTeam, matchday, formMatches);
        Form awayForm = formCalculator.formBefore(match.awayTeam, matchday, formMatches);
        HeadToHead h2h = headToHeadCalculator.headToHeadBefore(match);
        boolean provisional = standings.includesProvisional() || homeForm.includesProvisional()
                || awayForm.includesProvisional() || h2h.includesProvisional();
        return new MatchSituation(match.id, matchday.league.sourceShortcut(), matchday.season, matchday.number, match.kickoff, match.isPlayed(),
                match.isPlayed() ? new MatchSituation.Score(match.fullTimeScore.home, match.fullTimeScore.away) : null,
                match.resultStatus == ResultStatus.FINAL,
                team(match.homeTeam, matchday, standings, homeForm),
                team(match.awayTeam, matchday, standings, awayForm),
                h2h.matches().stream().map(v -> new Meeting(v.match().kickoff, v.match().matchday.season, v.home(),
                        v.goalsFor(), v.goalsAgainst(), v.result().name())).toList(),
                provisional);
    }

    private static TeamSituation team(Team team, Matchday matchday, Standings standings, Form form) {
        var standing = standings.of(team);
        // Champions League has no promotion/relegation — a club new to the league phase qualified
        // through its domestic league, it was not "promoted" the way a Bundesliga club is.
        boolean promoted = matchday.league != League.CHAMPIONS_LEAGUE
                && Matchday.latestSeason(matchday.league).isPresent()
                && !Match.playedInLeagueSeason(team, matchday.league, matchday.season - 1);
        return new TeamSituation(team.id, team.name, team.shortName,
                standing.map(s -> s.position()).orElse(null),
                standing.map(s -> s.balance().points()).orElse(0),
                form.lastMatches().stream().map(MatchdayFacade::result).toList(),
                balance(form.home()), balance(form.away()), promoted,
                form.lastMatches().stream().map(v -> v.match().kickoff).toList());
    }

    private static Result result(TeamMatchView v) {
        return new Result(v.match().kickoff, v.opponent().name, v.home(), v.goalsFor(), v.goalsAgainst(), v.result().name());
    }

    private static Balance balance(de.javamark.matchoracle.matchday.entity.Balance b) {
        return new Balance(b.played(), b.wins(), b.draws(), b.losses(), b.goalsFor(), b.goalsAgainst(), b.points());
    }
}
