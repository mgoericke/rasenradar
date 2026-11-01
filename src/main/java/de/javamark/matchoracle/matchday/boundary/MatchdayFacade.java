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
import de.javamark.matchoracle.matchday.entity.PlacementGoal;
import de.javamark.matchoracle.matchday.entity.ResultStatus;
import de.javamark.matchoracle.matchday.entity.StandingPosition;
import de.javamark.matchoracle.matchday.entity.Standings;
import de.javamark.matchoracle.matchday.entity.Team;
import de.javamark.matchoracle.matchday.entity.TeamMatchView;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Java entry point for other features (forecast, review). Cross-feature access goes through here, never through entities. */
@ApplicationScoped
public class MatchdayFacade {

    public record LeagueSeasonRef(String league, int season) {
    }

    public record Fixture(long homeTeamId, long awayTeamId) {
    }

    /** A match's score as far as it is known; {@code provisional} until the result is FINAL (spec 01). */
    public record MatchScore(int homeGoals, int awayGoals, boolean provisional) {
    }

    public record TeamState(long teamId, int points, int goalDifference, int goalsFor, Balance homeRecord, Balance awayRecord) {
    }

    /** The two clubs of a match with the league season it belongs to; ids of all their matches in it. */
    public record MatchClubs(String league, int season, Club home, Club away) {
    }

    public record Club(long id, String name, List<Long> matchIds) {
    }

    /**
     * Story r007, one club in the table "nach KI-Vorschau": {@code change} is places up (positive) or
     * down against the table now.
     */
    public record ProjectedClub(long teamId, String name, String shortName, String crest, String link,
                                int position, int played, int goalDifference, int points, int change) {

        public String changeLabel() {
            return change > 0 ? "+" + change : change < 0 ? "−" + (-change) : "0";
        }

        public String changeDescription() {
            int n = Math.abs(change);
            return change == 0 ? "unverändert" : n + (n == 1 ? " Platz " : " Plätze ") + (change > 0 ? "hoch" : "runter");
        }
    }

    /**
     * Story r007: the table if every open match of the matchday ended at its forecast's Richtwert.
     * {@code assumed} names those matches, {@code withoutForecast} the open ones that have none and
     * stay out; running matches count at their current score, which makes it {@code provisional}.
     */
    public record Projection(List<ProjectedClub> clubs, List<String> assumed, List<String> withoutForecast,
                             boolean provisional) {
    }

    @ConfigProperty(name = "matchoracle.matchday.live-window", defaultValue = "PT2H30M")
    Duration liveWindow;

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

    /** Story N01: the clubs of a match and every match each of them plays in that league season, for the review's AI record per club. */
    @Transactional(Transactional.TxType.SUPPORTS)
    public Optional<MatchClubs> clubsOf(long matchId) {
        return Match.<Match>findByIdOptional(matchId).map(m -> {
            League league = m.matchday.league;
            int season = m.matchday.season;
            return new MatchClubs(league.sourceShortcut(), season,
                    club(m.homeTeam, league, season), club(m.awayTeam, league, season));
        });
    }

    private static Club club(Team team, League league, int season) {
        return new Club(team.id, team.shortName == null ? team.name : team.shortName,
                Match.findByTeam(team, league, season).stream().map(m -> m.id).toList());
    }

    /** Ids of all matches whose result is FINAL — the review derives a situation for each of them. */
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<Long> finalMatchIds() {
        return Match.getEntityManager()
                .createQuery("select m.id from Match m where m.resultStatus = :status", Long.class)
                .setParameter("status", ResultStatus.FINAL)
                .getResultList();
    }

    /** Scores of the given matches that have one, keyed by match id — for the stream's verdict on the KI-Vorschau. */
    @Transactional(Transactional.TxType.SUPPORTS)
    public Map<Long, MatchScore> scoresOf(List<Long> matchIds) {
        Map<Long, MatchScore> scores = new HashMap<>();
        for (Long id : matchIds) {
            Match.<Match>findByIdOptional(id).filter(Match::isPlayed).ifPresent(m -> scores.put(m.id,
                    new MatchScore(m.fullTimeScore.home, m.fullTimeScore.away, m.resultStatus == ResultStatus.PROVISIONAL)));
        }
        return scores;
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

    /**
     * Story r007: the table after the matchday with each open match counted at the score given in
     * {@code richtwerte} (match id → home/away goals). Empty when the league has no table, the
     * matchday is unknown or nothing is open — then there is nothing to assume.
     */
    @Transactional(Transactional.TxType.SUPPORTS)
    public Optional<Projection> projectedStandings(String league, int season, int number, Map<Long, MatchScore> richtwerte) {
        Optional<Matchday> found = League.bySourceShortcut(league).filter(League::hasTable).flatMap(l -> Matchday.find(l, season, number));
        if (found.isEmpty()) {
            return Optional.empty();
        }
        Matchday matchday = found.get();
        List<Match> matches = new ArrayList<>(Match.findByMatchday(matchday));
        matches.sort((a, b) -> a.kickoff.compareTo(b.kickoff));
        Instant now = Instant.now();
        List<StandingsCalculator.Assumed> counted = new ArrayList<>();
        List<String> assumed = new ArrayList<>();
        List<String> without = new ArrayList<>();
        boolean running = false;
        for (Match m : matches) {
            if (m.isPlayed()) continue;
            String pairing = shortName(m.homeTeam) + " – " + shortName(m.awayTeam);
            MatchScore r = richtwerte.get(m.id);
            if (m.isLive(now, liveWindow)) {
                counted.add(StandingsCalculator.Assumed.live(m));
                running = true;
            } else if (r != null) {
                counted.add(new StandingsCalculator.Assumed(m, r.homeGoals(), r.awayGoals()));
                assumed.add(pairing + " " + r.homeGoals() + ":" + r.awayGoals());
            } else if (!m.kickoff.isBefore(now)) {
                without.add(pairing);
            }
        }
        if (assumed.isEmpty()) {
            return Optional.empty();
        }
        Standings current = standingsCalculator.standingsBefore(matchday.league, season, number + 1);
        Standings projected = standingsCalculator.assumedStandings(current, counted, running);
        String shortcut = matchday.league.sourceShortcut();
        List<ProjectedClub> clubs = projected.positions().stream().map(p -> {
            Team t = p.team();
            int was = current.of(t).map(StandingPosition::position).orElse(p.position());
            return new ProjectedClub(t.id, t.name, shortName(t), MatchdayPageModels.crestLink(t),
                    MatchdayPageModels.teamLink(t, shortcut), p.position(), p.balance().played(),
                    p.balance().goalDifference(), p.balance().points(), was - p.position());
        }).toList();
        return Optional.of(new Projection(clubs, List.copyOf(assumed), List.copyOf(without), projected.includesProvisional()));
    }

    private static String shortName(Team team) {
        return team.shortName == null || team.shortName.isBlank() ? team.name : team.shortName;
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

    /** Unplayed matches of the current matchday, for the automatic KI-Vorschau (spec 02, "Terminplanung"). */
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<Long> currentUnplayedMatchIds(String league) {
        return League.bySourceShortcut(league).flatMap(Matchday::findCurrent)
                .map(md -> Match.findByMatchday(md).stream().filter(m -> !m.isPlayed()).map(m -> m.id).toList())
                .orElse(List.of());
    }

    /** Present with the league/season if every result of this match's matchday is now FINAL (spec 03's Rückschau trigger — final results only); empty otherwise. */
    @Transactional(Transactional.TxType.SUPPORTS)
    public Optional<LeagueSeasonRef> matchdayJustCompleted(long matchId) {
        return Match.<Match>findByIdOptional(matchId)
                .filter(m -> Match.findByMatchday(m.matchday).stream().allMatch(mm -> mm.resultStatus == ResultStatus.FINAL))
                .map(m -> new LeagueSeasonRef(m.matchday.league.sourceShortcut(), m.matchday.season));
    }

    /**
     * Present with the league/season if every match of this match's matchday now has a score,
     * FINAL or not — the season outlook's trigger (spec 04). Unlike the Rückschau, the outlook is
     * a forward-looking simulation, not an evaluation of a fixed result, so a provisional score is
     * good enough: showing it a day early beats a "no outlook yet" placeholder.
     */
    @Transactional(Transactional.TxType.SUPPORTS)
    public Optional<LeagueSeasonRef> matchdayJustPlayed(long matchId) {
        return Match.<Match>findByIdOptional(matchId)
                .filter(m -> Match.findByMatchday(m.matchday).stream().allMatch(Match::isPlayed))
                .map(m -> new LeagueSeasonRef(m.matchday.league.sourceShortcut(), m.matchday.season));
    }

    /**
     * Whether a competition has placement goals at all — spec 06: a cup has nothing to simulate,
     * and the Nations League's groups of four would be a decision of their own. Lets the season
     * feature ask without reaching into this feature's entities.
     */
    public boolean hasPlacementGoals(String league) {
        return League.bySourceShortcut(league).map(l -> !PlacementGoal.all(l).isEmpty()).orElse(false);
    }

    /** The most recently fully-played matchday's league/season, if any — backfills a season outlook that was never computed (spec 04's startup safety net). */
    @Transactional(Transactional.TxType.SUPPORTS)
    public Optional<LeagueSeasonRef> mostRecentlyPlayedMatchday(String league) {
        return League.bySourceShortcut(league).flatMap(Matchday::findCurrent).flatMap(current -> {
            Matchday candidate = isFullyPlayed(current) ? current : Matchday.find(current.league, current.season, current.number - 1).orElse(null);
            return candidate != null && isFullyPlayed(candidate)
                    ? Optional.of(new LeagueSeasonRef(candidate.league.sourceShortcut(), candidate.season))
                    : Optional.empty();
        });
    }

    private static boolean isFullyPlayed(Matchday matchday) {
        List<Match> matches = Match.findByMatchday(matchday);
        return !matches.isEmpty() && matches.stream().allMatch(Match::isPlayed);
    }

    /** Every team of a league's current season with its table state and home/away scoring record — the season outlook's starting point (spec 04). */
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<TeamState> currentSeasonState(String league) {
        Optional<League> l = League.bySourceShortcut(league);
        if (l.isEmpty()) {
            return List.of();
        }
        Optional<Matchday> current = Matchday.findCurrent(l.get());
        if (current.isEmpty()) {
            return List.of();
        }
        Standings standings = standingsCalculator.standingsBefore(current.get());
        List<TeamState> states = new ArrayList<>();
        for (StandingPosition p : standings.positions()) {
            Form form = formCalculator.formBefore(p.team(), current.get());
            states.add(new TeamState(p.team().id, p.balance().points(), p.balance().goalDifference(), p.balance().goalsFor(),
                    balance(form.home()), balance(form.away())));
        }
        return states;
    }

    /** Unplayed fixtures of a league's current season — the season outlook's remaining-season input (spec 04). */
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<Fixture> remainingFixtures(String league) {
        Optional<League> l = League.bySourceShortcut(league);
        if (l.isEmpty()) {
            return List.of();
        }
        Optional<Integer> season = Matchday.latestSeason(l.get());
        if (season.isEmpty()) {
            return List.of();
        }
        return Match.findUnplayed(l.get(), season.get()).stream().map(m -> new Fixture(m.homeTeam.id, m.awayTeam.id)).toList();
    }

    /** Same Poisson estimate as the other overloads, from two teams' current home/away records (spec 04, independent of any KI-Vorschau). */
    public ScorelineForecast scorelineForecast(Balance home, Balance away) {
        return scorelineForecast(poissonScoreModel.forecast(entityBalance(home), entityBalance(away)));
    }

    /** Every placement-goal label a league's table can produce (spec 04). */
    public List<String> placementGoals(String league) {
        return League.bySourceShortcut(league)
                .map(l -> PlacementGoal.all(l).stream().map(PlacementGoal::label).toList())
                .orElse(List.of());
    }

    /** The placement-goal labels a final table position satisfies for a league (spec 04). */
    public List<String> placementGoalsAt(String league, int position) {
        return League.bySourceShortcut(league)
                .map(l -> PlacementGoal.forPosition(l, position).stream().map(PlacementGoal::label).toList())
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
