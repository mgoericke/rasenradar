package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.entity.Goal;
import de.javamark.matchoracle.matchday.entity.League;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.Matchday;
import de.javamark.matchoracle.matchday.entity.ResultStatus;
import de.javamark.matchoracle.matchday.entity.Score;
import de.javamark.matchoracle.matchday.entity.Team;
import io.quarkus.hibernate.orm.panache.Panache;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Spec 01, steps 1-3: keeps matches and results in sync with the external source.
 * A matchday is only reloaded when the source reports a change for it. If the
 * source is unavailable, the last known state simply stays in place.
 */
@ApplicationScoped
public class MatchdaySynchronizer {

    private static final Logger LOG = Logger.getLogger(MatchdaySynchronizer.class);

    @Inject
    @RestClient
    OpenLigaDbClient client;

    /** Matchdays before and after the current one that are checked for changes. */
    @ConfigProperty(name = "matchoracle.matchday.sync-window", defaultValue = "1")
    int syncWindow;

    /** Past seasons kept as history in addition to the current one. */
    @ConfigProperty(name = "matchoracle.matchday.history-seasons", defaultValue = "5")
    int historySeasons;

    /** Checks the current matchday (+/- window) of both leagues and reloads what changed. */
    @ActivateRequestContext // Panache queries outside a transaction need a request context
    public void syncCurrentMatchdays() {
        for (League league : League.values()) {
            try {
                List<OpenLigaDbMatch> current = client.currentMatchday(league.sourceShortcut());
                if (current.isEmpty()) {
                    LOG.warnf("%s: source reports no current matchday", league);
                    continue;
                }
                int season = current.get(0).season();
                int number = current.get(0).group().number();
                importMissingSeasons(league, season);
                refreshTeams(league, season);
                for (int n = Math.max(1, number - syncWindow); n <= number + syncWindow; n++) {
                    syncMatchday(league, season, n);
                }
            } catch (RuntimeException e) {
                // source down or answering garbage: keep the last known state (spec 01, rules)
                LOG.warnf(e, "%s: sync failed, keeping last known state", league);
            }
        }
    }

    /** Reloads one matchday if the source reports a change since the last sync. */
    @Transactional
    public void syncMatchday(League league, int season, int number) {
        Instant sourceLastChange = OpenLigaDb.toInstant(client.lastChange(league.sourceShortcut(), season, number));
        Matchday existing = Matchday.find(league, season, number).orElse(null);
        Instant now = Instant.now();
        if (existing != null && existing.sourceLastChangedAt != null
                && !sourceLastChange.isAfter(existing.sourceLastChangedAt)) {
            LOG.debugf("%s %d/%d unchanged since %s", league, season, number, sourceLastChange);
            existing.lastCheckedAt = now;
            return;
        }
        List<OpenLigaDbMatch> matches = client.matchday(league.sourceShortcut(), season, number);
        if (matches.isEmpty()) {
            return;
        }
        Matchday matchday = existing != null ? existing : newMatchday(league, season, number);
        matches.forEach(m -> upsert(matchday, m));
        matchday.sourceLastChangedAt = sourceLastChange;
        matchday.lastCheckedAt = now;
        LOG.infof("%s %d/%d: synced %d matches (source changed %s)", league, season, number, matches.size(), sourceLastChange);
    }

    /** Names and crests of the season's teams; cheap, and it also covers teams whose matches did not change. */
    @Transactional
    void refreshTeams(League league, int season) {
        client.teams(league.sourceShortcut(), season).forEach(MatchdaySynchronizer::upsert);
    }

    /**
     * Initial import: the current season and the configured number of past seasons
     * are loaded once, as soon as no matchday of that season is known yet.
     */
    void importMissingSeasons(League league, int currentSeason) {
        // Champions League: only the current season — the league-phase field turns over so much
        // each year that past seasons give almost no reusable form/head-to-head data, at the cost
        // of importing dozens of clubs that will not play again.
        int seasons = league == League.CHAMPIONS_LEAGUE ? 0 : historySeasons;
        for (int season = currentSeason - seasons; season <= currentSeason; season++) {
            if (Matchday.count("league = ?1 and season = ?2", league, season) == 0) {
                importSeason(league, season);
            }
        }
    }

    /** Loads a whole season in one go; used for the initial import. */
    @Transactional
    public void importSeason(League league, int season) {
        List<OpenLigaDbMatch> matches = client.season(league.sourceShortcut(), season);
        Map<Integer, List<OpenLigaDbMatch>> byMatchday = matches.stream()
                .collect(Collectors.groupingBy(m -> m.group().number(), TreeMap::new, Collectors.toList()));
        byMatchday.forEach((number, matchesOfDay) -> {
            Matchday matchday = Matchday.find(league, season, number)
                    .orElseGet(() -> newMatchday(league, season, number));
            matchesOfDay.forEach(m -> upsert(matchday, m));
            matchday.sourceLastChangedAt = matchesOfDay.stream()
                    .map(m -> OpenLigaDb.toInstant(m.lastUpdate()))
                    .max(Instant::compareTo).orElse(null);
            matchday.lastCheckedAt = Instant.now();
        });
        LOG.infof("%s %d: imported %d matches on %d matchdays", league, season, matches.size(), byMatchday.size());
    }

    private static Matchday newMatchday(League league, int season, int number) {
        Matchday matchday = new Matchday();
        matchday.league = league;
        matchday.season = season;
        matchday.number = number;
        matchday.persist();
        return matchday;
    }

    /** Creates or updates one match. Results and goals are only touched if the source changed the match. */
    void upsert(Matchday matchday, OpenLigaDbMatch source) {
        Instant sourceLastUpdate = OpenLigaDb.toInstant(source.lastUpdate());
        Match match = Match.findByExternalId(source.id()).orElse(null);
        boolean isNew = match == null;
        if (isNew) {
            match = new Match();
            match.externalId = source.id();
        } else if (match.sourceLastChangedAt != null && !sourceLastUpdate.isAfter(match.sourceLastChangedAt)) {
            return;
        }
        if (match.resultStatus == ResultStatus.FINAL) {
            // a FINAL result is used for review and learning; we take the correction but keep the status
            LOG.warnf("Match %d changed at the source after being FINAL", source.id());
        }
        match.matchday = matchday;
        match.homeTeam = upsert(source.team1());
        match.awayTeam = upsert(source.team2());
        match.kickoff = source.kickoff();
        match.halfTimeScore = source.result(OpenLigaDbMatch.Result.HALF_TIME).map(r -> new Score(r.pointsTeam1(), r.pointsTeam2())).orElse(null);
        match.fullTimeScore = source.result(OpenLigaDbMatch.Result.FULL_TIME).map(r -> new Score(r.pointsTeam1(), r.pointsTeam2())).orElse(null);
        replaceGoals(match, source.goalsOrEmpty());
        match.sourceLastChangedAt = sourceLastUpdate;
        if (isNew) {
            // persist last: Hibernate checks not-null properties at persist time
            match.persist();
        }
    }

    private static Team upsert(OpenLigaDbMatch.Team source) {
        Team team = Team.findByExternalId(source.teamId()).orElseGet(() -> {
            Team t = new Team();
            t.externalId = source.teamId();
            return t;
        });
        team.name = source.teamName();
        team.shortName = source.shortName();
        team.iconUrl = source.teamIconUrl();
        if (!team.isPersistent()) {
            team.persist();
        }
        return team;
    }

    /**
     * The goal sequence is replaced as a whole; orphanRemoval on Match.goals deletes the old rows.
     * Flushing right after {@code clear()} forces those deletes to execute before the new goals are
     * inserted — without it, Hibernate can insert a new goal at position 0 before deleting the old
     * one at position 0, and the (match_id, position) unique constraint rejects it.
     */
    private static void replaceGoals(Match match, List<OpenLigaDbMatch.Goal> sourceGoals) {
        match.goals.clear();
        Panache.getEntityManager().flush();
        int position = 0;
        for (OpenLigaDbMatch.Goal g : sourceGoals) {
            Goal goal = new Goal();
            goal.match = match;
            goal.position = position++;
            goal.minute = g.matchMinute();
            goal.scorerName = g.goalGetterName();
            goal.scoreAfter = new Score(g.scoreTeam1(), g.scoreTeam2());
            goal.penalty = g.isPenalty();
            goal.ownGoal = g.isOwnGoal();
            match.goals.add(goal);
        }
    }
}
