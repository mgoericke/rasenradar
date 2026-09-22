package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.entity.CompetitionFormat;
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
import java.util.ArrayList;
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

    /** Checks the current matchday (+/- window) of both leagues and reloads what changed. Returns the ids of matches that got a score for the first time. */
    @ActivateRequestContext // Panache queries outside a transaction need a request context
    public List<Long> syncCurrentMatchdays() {
        List<Long> newlyPlayed = new ArrayList<>();
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
                    newlyPlayed.addAll(syncMatchday(league, season, n));
                }
            } catch (RuntimeException e) {
                // source down or answering garbage: keep the last known state (spec 01, rules)
                LOG.warnf(e, "%s: sync failed, keeping last known state", league);
            }
        }
        return newlyPlayed;
    }

    /**
     * Reloads one section of the source if it reports a change since the last sync. A section is
     * a matchday in a league, a round in a cup, and a whole group in a group competition — see
     * {@link #storeSection}. Returns the ids of matches that got a score for the first time.
     */
    @Transactional
    public List<Long> syncMatchday(League league, int season, int sectionNumber) {
        Instant sourceLastChange = OpenLigaDb.toInstant(client.lastChange(league.sourceShortcut(), season, sectionNumber));
        Instant now = Instant.now();
        if (isUnchanged(league, season, sectionNumber, sourceLastChange, now)) {
            LOG.debugf("%s %d/%d unchanged since %s", league, season, sectionNumber, sourceLastChange);
            return List.of();
        }
        List<OpenLigaDbMatch> matches = client.matchday(league.sourceShortcut(), season, sectionNumber);
        if (matches.isEmpty()) {
            return List.of();
        }
        List<Long> newlyPlayed = storeSection(league, season, sectionNumber, matches, sourceLastChange, now);
        LOG.infof("%s %d/%d: synced %d matches (source changed %s)", league, season, sectionNumber, matches.size(), sourceLastChange);
        return newlyPlayed;
    }

    /**
     * True when the source reports no change for this section since the last sync. In a group
     * competition one section covers several matchdays; they all carry the same bookkeeping,
     * so any one of them answers for the section.
     */
    private boolean isUnchanged(League league, int season, int sectionNumber, Instant sourceLastChange, Instant now) {
        List<Matchday> known = Matchday.findSection(league, season, sectionNumber);
        if (known.isEmpty() || known.stream().anyMatch(m -> m.sourceLastChangedAt == null
                || sourceLastChange.isAfter(m.sourceLastChangedAt))) {
            return false;
        }
        known.forEach(m -> m.lastCheckedAt = now);
        return true;
    }

    /** Spec 06: in a knockout competition the source's section name is the round's name. */
    static String labelOf(CompetitionFormat format, String sectionName) {
        return format == CompetitionFormat.KNOCKOUT ? sectionName : null;
    }

    /** Spec 06: in a group competition it is the group the matchday belongs to. */
    static String groupOf(CompetitionFormat format, String sectionName) {
        return format == CompetitionFormat.GROUPS ? sectionName : null;
    }

    /**
     * One section of the source: a matchday in a league, a round in a cup, a whole group in a
     * group competition. Only the last one turns into several matchdays — the source names the
     * group but not the matchday, so it is derived (spec 06, see {@link GroupMatchdays}).
     */
    private List<Long> storeSection(League league, int season, int sectionNumber,
                                    List<OpenLigaDbMatch> matches, Instant sourceLastChange, Instant now) {
        String sectionName = matches.get(0).group().name();
        if (league.format() == CompetitionFormat.GROUPS) {
            String groupName = groupOf(league.format(), sectionName);
            List<Long> newlyPlayed = new ArrayList<>();
            GroupMatchdays.byMatchday(matches).forEach((number, matchesOfDay) -> {
                Matchday matchday = Matchday.find(league, season, groupName, number)
                        .orElseGet(() -> newMatchday(league, season, groupName, null, number, sectionNumber));
                newlyPlayed.addAll(store(matchday, matchesOfDay, sourceLastChange, now));
            });
            return newlyPlayed;
        }
        String label = labelOf(league.format(), sectionName);
        Matchday matchday = Matchday.find(league, season, null, sectionNumber)
                .orElseGet(() -> newMatchday(league, season, null, label, sectionNumber, sectionNumber));
        matchday.label = label; // a round can be renamed at the source between syncs
        return store(matchday, matches, sourceLastChange, now);
    }

    /** Upserts the matches of one matchday and records the sync bookkeeping on it. */
    private List<Long> store(Matchday matchday, List<OpenLigaDbMatch> matches, Instant sourceLastChange, Instant now) {
        List<Long> newlyPlayed = matches.stream().map(m -> upsert(matchday, m))
                .filter(java.util.Objects::nonNull).toList();
        matchday.sourceLastChangedAt = sourceLastChange;
        matchday.lastCheckedAt = now;
        return newlyPlayed;
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
        Map<Integer, List<OpenLigaDbMatch>> bySection = matches.stream()
                .collect(Collectors.groupingBy(m -> m.group().number(), TreeMap::new, Collectors.toList()));
        Instant now = Instant.now();
        bySection.forEach((sectionNumber, matchesOfSection) -> {
            Instant sourceLastChange = matchesOfSection.stream()
                    .map(m -> OpenLigaDb.toInstant(m.lastUpdate()))
                    .max(Instant::compareTo).orElse(null);
            storeSection(league, season, sectionNumber, matchesOfSection, sourceLastChange, now);
        });
        LOG.infof("%s %d: imported %d matches in %d sections", league, season, matches.size(), bySection.size());
    }

    private static Matchday newMatchday(League league, int season, String groupName, String label,
                                        int number, int sectionNumber) {
        Matchday matchday = new Matchday();
        matchday.league = league;
        matchday.season = season;
        matchday.groupName = groupName;
        matchday.label = label;
        matchday.number = number;
        matchday.sectionNumber = sectionNumber;
        matchday.persist();
        return matchday;
    }

    /** Creates or updates one match. Results and goals are only touched if the source changed the match. Returns its id if this call gave it a score for the first time. */
    Long upsert(Matchday matchday, OpenLigaDbMatch source) {
        Instant sourceLastUpdate = OpenLigaDb.toInstant(source.lastUpdate());
        Match match = Match.findByExternalId(source.id()).orElse(null);
        boolean isNew = match == null;
        if (isNew) {
            match = new Match();
            match.externalId = source.id();
        } else if (match.sourceLastChangedAt != null && !sourceLastUpdate.isAfter(match.sourceLastChangedAt)) {
            return null;
        }
        if (match.resultStatus == ResultStatus.FINAL) {
            // a FINAL result is used for review and learning; we take the correction but keep the status
            LOG.warnf("Match %d changed at the source after being FINAL", source.id());
        }
        boolean wasPlayed = match.isPlayed();
        match.matchday = matchday;
        match.homeTeam = upsert(source.team1());
        match.awayTeam = upsert(source.team2());
        match.kickoff = source.kickoff();
        match.halfTimeScore = source.result(OpenLigaDbMatch.Result.HALF_TIME).map(r -> new Score(r.pointsTeam1(), r.pointsTeam2())).orElse(null);
        match.fullTimeScore = source.finalResult().map(r -> new Score(r.pointsTeam1(), r.pointsTeam2())).orElse(null);
        replaceGoals(match, source.goalsOrEmpty());
        match.sourceLastChangedAt = sourceLastUpdate;
        if (isNew) {
            // persist last: Hibernate checks not-null properties at persist time
            match.persist();
        }
        return !wasPlayed && match.isPlayed() ? match.id : null;
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
