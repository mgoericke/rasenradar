package de.javamark.matchoracle.matchday.boundary;

import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.BalanceRow;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.DataInfo;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.DayGroup;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.GoalRow;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.LandingLeagueSection;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.LandingPage;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.Spotlight;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.LeagueScorerRow;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.LeagueScorersPage;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.MatchPage;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.MatchRow;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.MatchdayPage;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.Nav;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.ResultRow;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.ScheduleRow;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.ScorerRow;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.SeasonChip;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.StandingRow;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.TeamPage;
import jakarta.ws.rs.QueryParam;
import de.javamark.matchoracle.matchday.entity.Tier;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.UpsetRow;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.RoundSection;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.KnockoutPage;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.GroupsPage;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.TeamSituation;
import de.javamark.matchoracle.matchday.control.FormCalculator;
import de.javamark.matchoracle.matchday.control.GoalTimingCalculator;
import de.javamark.matchoracle.matchday.control.HeadToHeadCalculator;
import de.javamark.matchoracle.matchday.control.ScorerCalculator;
import de.javamark.matchoracle.matchday.control.StandingsCalculator;
import de.javamark.matchoracle.matchday.entity.Form;
import de.javamark.matchoracle.matchday.entity.Goal;
import de.javamark.matchoracle.matchday.entity.HeadToHead;
import de.javamark.matchoracle.matchday.entity.Balance;
import de.javamark.matchoracle.matchday.entity.CompetitionFormat;
import de.javamark.matchoracle.matchday.entity.TeamTier;
import de.javamark.matchoracle.matchday.entity.League;
import de.javamark.matchoracle.matchday.entity.LeagueSeason;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.Matchday;
import de.javamark.matchoracle.matchday.entity.Standings;
import de.javamark.matchoracle.matchday.entity.Team;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** HTML pages for the viewer (spec 01). The JSON API lives in {@link MatchdayResource}. */
@Path("/")
@Produces(MediaType.TEXT_HTML)
@Transactional(Transactional.TxType.SUPPORTS)
public class MatchdayPages {

    @CheckedTemplate
    static class Templates {
        static native TemplateInstance home(LandingPage page);
        static native TemplateInstance matchday(MatchdayPage page);
        static native TemplateInstance match(MatchPage page);
        static native TemplateInstance team(TeamPage page);
        static native TemplateInstance knockout(KnockoutPage page);
        static native TemplateInstance groups(GroupsPage page);
        static native TemplateInstance leagueScorers(LeagueScorersPage page);
    }

    @Inject
    FormCalculator formCalculator;

    @Inject
    StandingsCalculator standingsCalculator;

    @Inject
    HeadToHeadCalculator headToHeadCalculator;

    @Inject
    ScorerCalculator scorerCalculator;

    @Inject
    GoalTimingCalculator goalTimingCalculator;

    /** How long after kickoff a match still counts as "läuft" in the UI — never a live score, just the label (Spec 1). */
    @ConfigProperty(name = "matchoracle.matchday.live-window", defaultValue = "PT2H30M")
    Duration liveWindow;

    /** One match together with the league shortcut it needs for its links — the landing page's spotlight candidates. */
    record CandidateMatch(Match match, String shortcut) {
    }

    /** Spec 1: a currently live match takes priority over a merely upcoming one; earliest kickoff breaks ties either way. */
    static Optional<CandidateMatch> pickSpotlight(List<CandidateMatch> candidates, Instant now, Duration liveWindow) {
        Comparator<CandidateMatch> byKickoff = Comparator.comparing(c -> c.match().kickoff);
        return candidates.stream().filter(c -> MatchdayPageModels.isLive(c.match().kickoff, c.match().isPlayed(), now, liveWindow))
                .min(byKickoff)
                .or(() -> candidates.stream().filter(c -> !c.match().isPlayed() && c.match().kickoff.isAfter(now)).min(byKickoff));
    }

    /** Landing page: current matchday of both Bundesligas in short form, so a first visit shows both leagues. */
    @GET
    public TemplateInstance home() {
        Instant now = Instant.now();
        List<LandingLeagueSection> sections = new ArrayList<>();
        List<CandidateMatch> candidates = new ArrayList<>();
        for (League l : List.of(League.BUNDESLIGA_1, League.BUNDESLIGA_2)) {
            Matchday.findDisplayed(l, now).ifPresent(matchday -> {
                String shortcut = matchday.league.sourceShortcut();
                List<Match> matches = Match.findByMatchday(matchday);
                sections.add(landingSection(matchday, matches, shortcut, now));
                matches.forEach(m -> candidates.add(new CandidateMatch(m, shortcut)));
            });
        }
        Spotlight spotlight = pickSpotlight(candidates, now, liveWindow).map(c -> spotlight(c, now)).orElse(null);
        return Templates.home(new LandingPage(Nav.page("home"), spotlight, sections, MatchdayPageModels.LEADERBOARD_LINK));
    }

    private Spotlight spotlight(CandidateMatch c, Instant now) {
        Match m = c.match();
        String base = "/" + c.shortcut() + "/matches/" + m.id;
        return new Spotlight(base, base + "/forecast", m.homeTeam.name, m.awayTeam.name, m.homeTeam.iconUrl, m.awayTeam.iconUrl,
                MatchdayPageModels.time(m.kickoff), MatchdayPageModels.dayLabel(m.kickoff),
                MatchdayPageModels.isLive(m.kickoff, m.isPlayed(), now, liveWindow));
    }

    private static final int LANDING_TABLE_ROWS = 6;

    private LandingLeagueSection landingSection(Matchday matchday, List<Match> matches, String shortcut, Instant now) {
        matches = new ArrayList<>(matches);
        matches.sort((a, b) -> a.kickoff.compareTo(b.kickoff));

        Map<String, List<MatchRow>> byDay = new LinkedHashMap<>();
        for (Match m : matches) {
            byDay.computeIfAbsent(MatchdayPageModels.dayLabel(m.kickoff), k -> new ArrayList<>()).add(MatchRow.of(m, shortcut, now, liveWindow));
        }
        List<DayGroup> days = byDay.entrySet().stream().map(e -> new DayGroup(e.getKey(), e.getValue())).toList();

        Standings standings = standingsCalculator.standingsBefore(matchday.league, matchday.season, matchday.number + 1);
        List<StandingRow> tableTop = standings.positions().stream()
                .limit(LANDING_TABLE_ROWS).map(p -> StandingRow.of(p, matchday.league)).toList();

        String base = "/" + shortcut + "/" + matchday.season + "/" + matchday.number;
        return new LandingLeagueSection(MatchdayPageModels.badge(matchday.league), MatchdayPageModels.leagueName(matchday.league),
                "/" + shortcut, base + "/forecasts", base + "/forecasts/markers", matchday.number, days,
                tableTop, standings.includesProvisional());
    }

    /**
     * Spec 06: a knockout competition has no "current matchday" worth landing on — the whole
     * edition is the page. An older edition is reached with ?saison=2023.
     */
    @GET
    @Path("/{league}")
    public TemplateInstance currentMatchday(@PathParam("league") String league,
                                            @QueryParam("saison") Integer season) {
        League l = league(league);
        if (l.format() == CompetitionFormat.KNOCKOUT) {
            int edition = season != null ? season
                    : Matchday.latestSeason(l).orElseThrow(() -> new NotFoundException("no season known yet"));
            return Templates.knockout(knockoutPage(l, edition));
        }
        if (l.format() == CompetitionFormat.GROUPS) {
            Matchday.SeasonMatchday current = Matchday.currentNumber(l)
                    .orElseThrow(() -> new NotFoundException("no matchday known yet"));
            return Templates.groups(groupsPage(l, current.season(), current.number()));
        }
        Matchday matchday = Matchday.findDisplayed(l, Instant.now())
                .orElseThrow(() -> new NotFoundException("no matchday known yet"));
        return Templates.matchday(matchdayPage(matchday));
    }

    @GET
    @Path("/{league}/{season}/{number}")
    public TemplateInstance matchday(@PathParam("league") String league, @PathParam("season") int season, @PathParam("number") int number) {
        League l = league(league);
        if (l.format() == CompetitionFormat.GROUPS) {
            return Templates.groups(groupsPage(l, season, number));
        }
        Matchday matchday = Matchday.find(l, season, number)
                .orElseThrow(() -> new NotFoundException("matchday not found"));
        return Templates.matchday(matchdayPage(matchday));
    }

    @GET
    @Path("/{league}/matches/{id}")
    public TemplateInstance match(@PathParam("league") String league, @PathParam("id") long id) {
        Match match = Match.<Match>findByIdOptional(id)
                .filter(m -> m.matchday.league == league(league))
                .orElseThrow(() -> new NotFoundException("match not found"));
        return Templates.match(matchPage(match));
    }

    @GET
    @Path("/{league}/teams/{id}")
    public TemplateInstance team(@PathParam("league") String league, @PathParam("id") long id) {
        League l = league(league);
        int season = Matchday.latestSeason(l).orElseThrow(() -> new NotFoundException("no season known yet"));
        return team(league, season, id);
    }

    @GET
    @Path("/{league}/{season}/teams/{id}")
    public TemplateInstance team(@PathParam("league") String league, @PathParam("season") int season, @PathParam("id") long id) {
        Team team = Team.<Team>findByIdOptional(id).orElseThrow(() -> new NotFoundException("team not found"));
        return Templates.team(teamPage(team, league(league), season));
    }

    /** Liga-Torschützenliste: current season, every club. */
    @GET
    @Path("/{league}/torschuetzen")
    public TemplateInstance leagueScorers(@PathParam("league") String league) {
        League l = league(league);
        int season = Matchday.latestSeason(l).orElseThrow(() -> new NotFoundException("no season known yet"));
        List<LeagueScorerRow> rows = new ArrayList<>();
        int rank = 0;
        for (var s : scorerCalculator.leagueScorersFor(l, season)) {
            rows.add(new LeagueScorerRow(++rank, s.name(), s.team(), s.goals(), s.penalties()));
        }
        return Templates.leagueScorers(new LeagueScorersPage(Nav.of(l), MatchdayPageModels.seasonLabel(season), rows));
    }

    private MatchdayPage matchdayPage(Matchday matchday) {
        String shortcut = matchday.league.sourceShortcut();
        List<Match> matches = Match.findByMatchday(matchday);
        matches.sort((a, b) -> a.kickoff.compareTo(b.kickoff));
        Instant now = Instant.now();

        Map<String, List<MatchRow>> byDay = new LinkedHashMap<>();
        for (Match m : matches) {
            byDay.computeIfAbsent(MatchdayPageModels.dayLabel(m.kickoff), k -> new ArrayList<>()).add(MatchRow.of(m, shortcut, now, liveWindow));
        }
        List<DayGroup> days = byDay.entrySet().stream().map(e -> new DayGroup(e.getKey(), e.getValue())).toList();

        // table after this matchday (everything played up to and including it)
        Standings standings = standingsCalculator.standingsBefore(matchday.league, matchday.season, matchday.number + 1);
        List<StandingRow> rows = standings.positions().stream().map(p -> StandingRow.of(p, matchday.league)).toList();

        String base = "/" + shortcut + "/" + matchday.season + "/";
        String prev = matchday.number > 1 ? base + (matchday.number - 1) : null;
        String next = Matchday.find(matchday.league, matchday.season, matchday.number + 1)
                .filter(n -> n.label == null) // spec 06: the pager stays inside the league phase
                .map(n -> base + n.number).orElse(null);

        return new MatchdayPage(Nav.of(matchday.league), MatchdayPageModels.seasonLabel(matchday.season), matchday.season, matchday.number,
                prev, next, days, rows, standings.includesProvisional(), DataInfo.of(matchday, Instant.now()),
                roundSections(matchday.league, matchday.season, shortcut, now));
    }

    private MatchPage matchPage(Match match) {
        Matchday matchday = match.matchday;
        String shortcut = matchday.league.sourceShortcut();
        Standings standings = standingsCalculator.standingsBefore(matchday);
        HeadToHead h2h = headToHeadCalculator.headToHeadBefore(match);

        List<GoalRow> goals = new ArrayList<>();
        int prevHome = 0;
        for (Goal g : match.goals) {
            goals.add(GoalRow.of(g, g.scoreAfter.home > prevHome));
            prevHome = g.scoreAfter.home;
        }

        return new MatchPage(Nav.of(matchday.league), MatchdayPageModels.seasonLabel(matchday.season), matchday.number,
                "/" + shortcut + "/" + matchday.season + "/" + matchday.number,
                "/" + shortcut + "/matches/" + match.id + "/forecast",
                MatchdayPageModels.dayLabel(match.kickoff), MatchRow.of(match, shortcut, Instant.now(), liveWindow), goals,
                situation(match.homeTeam, matchday, standings, shortcut),
                situation(match.awayTeam, matchday, standings, shortcut),
                h2h.matches().stream().map(v -> ResultRow.of(v, shortcut)).toList(),
                BalanceRow.of("Bilanz", h2h.balance()), h2h.includesProvisional(),
                positionChart(matchday, match.homeTeam, match.awayTeam),
                DataInfo.of(matchday, Instant.now()));
    }

    /** The club in one season: standing, position curve, schedule, home/away balance, scorers and goal timing. */
    /**
     * Spec 06: the knockout rounds of a season as a band — the Champions League after its
     * league phase, the Nations League after its groups. Open is the round being played.
     */
    private List<RoundSection> roundSections(League league, int season, String shortcut, Instant now) {
        List<Matchday> rounds = Matchday.findRounds(league, season);
        if (rounds.isEmpty()) {
            return List.of();
        }
        int open = rounds.stream()
                .filter(r -> Match.findByMatchday(r).stream().anyMatch(m -> !m.isPlayed()))
                .mapToInt(r -> r.number).min()
                .orElse(rounds.get(rounds.size() - 1).number);
        List<RoundSection> sections = new ArrayList<>();
        for (Matchday round : rounds) {
            List<Match> matches = Match.findByMatchday(round);
            matches.sort(Comparator.comparing((Match m) -> m.kickoff));
            sections.add(new RoundSection(round.displayName(), matches.size(), dateRange(matches),
                    matches.stream().map(m -> MatchdayPageModels.MatchRow.of(m, shortcut, now, liveWindow)).toList(),
                    round.number == open));
        }
        return sections;
    }

    /**
     * Spec 06: all groups of a group competition, each with its own table and its matches of
     * the shown matchday. One competition page, not one entry per group.
     */
    private GroupsPage groupsPage(League league, int season, int number) {
        String shortcut = league.sourceShortcut();
        // the groups' matchday of that number, plus the edition's final round — the latter is a
        // phase of its own and does not share the groups' counting, so it always sits at the end
        List<Matchday> groups = Matchday.findAll(league, season, number).stream()
                .filter(m -> m.groupName != null).toList();
        List<Matchday> finalRound = Matchday.findRounds(league, season);
        if (groups.isEmpty() && finalRound.isEmpty()) {
            throw new NotFoundException("matchday not found");
        }
        Instant now = Instant.now();
        boolean provisional = false;
        List<MatchdayPageModels.GroupSection> sections = new ArrayList<>();
        List<RoundSection> rounds = new ArrayList<>();
        for (Matchday group : groups) {
            List<Match> matches = Match.findByMatchday(group);
            matches.sort(Comparator.comparing((Match m) -> m.kickoff));
            // table after this matchday: everything played in this group up to and including it
            Standings standings = standingsCalculator.standingsBefore(withNumber(group, group.number + 1));
            provisional = provisional || standings.includesProvisional();
            sections.add(new MatchdayPageModels.GroupSection(group.groupName,
                    standings.positions().stream().map(p -> StandingRow.of(p, league)).toList(),
                    matches.stream().map(m -> MatchdayPageModels.MatchRow.of(m, shortcut, now, liveWindow)).toList()));
        }
        // spec 06: sections of the final round are rounds, not groups — no table
        rounds.addAll(roundSections(league, season, shortcut, now));
        String base = "/" + shortcut + "/" + season + "/";
        String prev = number > 1 ? base + (number - 1) : null;
        boolean hasNext = Matchday.findAll(league, season, number + 1).stream().anyMatch(m -> m.groupName != null);
        String next = hasNext ? base + (number + 1) : null;
        Matchday reference = groups.isEmpty() ? finalRound.get(0) : groups.get(0);
        return new GroupsPage(Nav.of(league), MatchdayPageModels.seasonLabel(season), season, number,
                prev, next, sections, rounds, provisional, DataInfo.of(reference, now));
    }

    /** A detached stand-in used only to ask for the table *after* a matchday of the same group. */
    private static Matchday withNumber(Matchday group, int number) {
        Matchday reference = new Matchday();
        reference.league = group.league;
        reference.season = group.season;
        reference.groupName = group.groupName;
        reference.number = number;
        return reference;
    }

    /**
     * Spec 06: the round band — every round of one edition, the final on top, each with its
     * pairings; below it the surprises of that edition. No bracket: the competition is drawn
     * anew after every round, so there are no paths to draw.
     */
    private KnockoutPage knockoutPage(League league, int season) {
        String shortcut = league.sourceShortcut();
        List<Matchday> rounds = Matchday.<Matchday>list("league = ?1 and season = ?2", league, season);
        if (rounds.isEmpty()) {
            throw new NotFoundException("no matchdays known for " + shortcut + " " + season);
        }
        rounds.sort(Comparator.comparingInt((Matchday m) -> m.number).reversed());
        Instant now = Instant.now();
        // the round the viewer should land on — same rule the rest of the app follows for the
        // current matchday: the first one still to be played, the last one once the edition is
        // over. A finished edition opens on the final instead of 32 first-round pairings.
        int openRound = rounds.stream()
                .filter(r -> Match.findByMatchday(r).stream().anyMatch(m -> !m.isPlayed()))
                .mapToInt(r -> r.number).min()
                .orElse(rounds.get(0).number);

        List<RoundSection> sections = new ArrayList<>();
        List<UpsetRow> upsets = new ArrayList<>();
        for (Matchday round : rounds) {
            List<Match> matches = Match.findByMatchday(round);
            matches.sort(Comparator.comparing((Match m) -> m.kickoff));
            sections.add(new RoundSection(round.displayName(), matches.size(), dateRange(matches),
                    matches.stream().map(m -> MatchdayPageModels.MatchRow.of(m, shortcut, now, liveWindow,
                            TeamTier.of(m.homeTeam, season).label(), TeamTier.of(m.awayTeam, season).label())).toList(),
                    round.number == openRound));
            matches.forEach(m -> upsetOf(m, round, season, shortcut).ifPresent(upsets::add));
        }
        upsets.sort(Comparator.comparingInt(UpsetRow::gap).reversed());

        List<SeasonChip> otherEditions = Matchday.<Matchday>list("league = ?1", league).stream()
                .map(m -> m.season).distinct().filter(y -> y != season).sorted(Comparator.reverseOrder())
                .map(y -> new SeasonChip(MatchdayPageModels.seasonLabel(y), MatchdayPageModels.leagueName(league), null,
                        "/" + shortcut + "?saison=" + y))
                .toList();

        return new KnockoutPage(Nav.of(league), MatchdayPageModels.seasonLabel(season), season, otherEditions,
                sections, upsets, DataInfo.of(rounds.get(0), now));
    }

    /** Spec 06: the lower-division side winning is the surprise. */
    private static Optional<UpsetRow> upsetOf(Match m, Matchday round, int season, String shortcut) {
        Optional<Team> winner = m.winner();
        if (winner.isEmpty()) {
            return Optional.empty();
        }
        Team won = winner.get();
        Team lost = won.equals(m.homeTeam) ? m.awayTeam : m.homeTeam;
        Tier winnerTier = TeamTier.of(won, season);
        Tier loserTier = TeamTier.of(lost, season);
        if (!TeamTier.isUpset(winnerTier, loserTier)) {
            return Optional.empty();
        }
        return Optional.of(new UpsetRow(round.displayName(), won.name, lost.name,
                winnerTier.label(), loserTier.label(),
                m.fullTimeScore.home + ":" + m.fullTimeScore.away,
                MatchdayPageModels.decisionLabel(m.decision, m.penaltyScore),
                "/" + shortcut + "/matches/" + m.id, TeamTier.gap(winnerTier, loserTier)));
    }

    private static String dateRange(List<Match> matches) {
        if (matches.isEmpty()) {
            return "";
        }
        String first = MatchdayPageModels.dayLabel(matches.get(0).kickoff);
        String last = MatchdayPageModels.dayLabel(matches.get(matches.size() - 1).kickoff);
        return first.equals(last) ? first : first + " – " + last;
    }

    private TeamPage teamPage(Team team, League league, int season) {
        String shortcut = league.sourceShortcut();
        List<Match> schedule = Match.findByTeam(team, league, season);
        if (schedule.isEmpty()) {
            throw new NotFoundException(team.name + " has no matches in " + shortcut + " " + season);
        }
        List<Match> played = schedule.stream().filter(Match::isPlayed).toList();
        int lastPlayedMatchday = played.stream().mapToInt(m -> m.matchday.number).max().orElse(0);
        // spec 06: no table in a knockout competition — no position, no zone, no position curve
        boolean hasTable = league.hasTable();
        Standings standings = hasTable
                ? standingsCalculator.standingsBefore(league, season, lastPlayedMatchday + 1)
                : new Standings(league, season, lastPlayedMatchday + 1, List.of(), false);
        var standing = standings.of(team);
        boolean currentSeason = Matchday.latestSeason(league).map(s -> s == season).orElse(false);

        List<SeasonChip> otherSeasons = new ArrayList<>();
        boolean hasPreviousSeasonInLeague = false;
        for (LeagueSeason ls : Match.findLeagueSeasonsOf(team)) {
            if (ls.league() == league && ls.season() == season) continue;
            if (ls.league() == league && ls.season() == season - 1) hasPreviousSeasonInLeague = true;
            int last = Match.findPlayedByTeamBefore(team, ls.league(), ls.season(), Integer.MAX_VALUE).stream()
                    .mapToInt(m -> m.matchday.number).max().orElse(0);
            Integer position = ls.league().hasTable()
                    ? standingsCalculator.standingsBefore(ls.league(), ls.season(), last + 1).of(team).map(p -> p.position()).orElse(null)
                    : null;
            otherSeasons.add(new SeasonChip(MatchdayPageModels.seasonLabel(ls.season()), MatchdayPageModels.leagueName(ls.league()), position,
                    "/" + ls.league().sourceShortcut() + "/" + ls.season() + "/teams/" + team.id));
        }
        String positionChartJson = !hasTable ? null
                : hasPreviousSeasonInLeague
                ? seasonComparisonChart(league, team, season, lastPlayedMatchday)
                : positionChart(league, season, lastPlayedMatchday, List.of(team));

        Matchday reference = (played.isEmpty() ? schedule.get(0) : played.get(played.size() - 1)).matchday;
        return new TeamPage(Nav.of(league), MatchdayPageModels.seasonLabel(season), season, currentSeason, team.name, team.iconUrl,
                standing.map(p -> p.position()).orElse(null),
                standing.map(p -> MatchdayPageModels.zone(league, p.position())).orElse(""),
                BalanceRow.of("Gesamt", standing.map(p -> p.balance()).orElse(Balance.EMPTY)), standings.includesProvisional(),
                otherSeasons,
                schedule.stream().map(m -> ScheduleRow.of(m, team, shortcut)).toList(),
                BalanceRow.of("Heim", Balance.of(team, played.stream().filter(m -> m.homeTeam.equals(team)).toList())),
                BalanceRow.of("Auswärts", Balance.of(team, played.stream().filter(m -> m.awayTeam.equals(team)).toList())),
                scorerCalculator.scorersFor(team, league, season).stream().map(ScorerRow::of).toList(),
                MatchdayPageModels.goalTimingJson(goalTimingCalculator.timingFor(team, league, season)),
                positionChartJson,
                hasTable ? MatchdayPageModels.outlookLink(shortcut, season, team.id) : null,
                DataInfo.of(reference, Instant.now()),
                hasTable, cupRunOutcome(team, league, played), tierLabel(team, league, season));
    }

    /** Spec 06: how a club's cup run ended — empty while it is still in, or outside a cup. */
    private static String cupRunOutcome(Team team, League league, List<Match> played) {
        if (league.format() != CompetitionFormat.KNOCKOUT || played.isEmpty()) {
            return "";
        }
        Match last = played.get(played.size() - 1);
        boolean wonIt = last.winner().map(w -> w.equals(team)).orElse(false);
        boolean wasTheFinal = Matchday.find(league, last.matchday.season, last.matchday.number + 1).isEmpty()
                && lastRoundOf(league, last.matchday.season) == last.matchday.number;
        if (wonIt && !wasTheFinal) {
            return ""; // won its last match but the competition goes on — still in it
        }
        return MatchdayPageModels.cupRunOutcome(last.matchday.displayName(), wonIt && wasTheFinal);
    }

    private static int lastRoundOf(League league, int season) {
        return Matchday.<Matchday>list("league = ?1 and season = ?2", league, season).stream()
                .mapToInt(m -> m.number).max().orElse(0);
    }

    /** Spec 06: the division a club played in, shown in a cup only. */
    private static String tierLabel(Team team, League league, int season) {
        return league.format() == CompetitionFormat.KNOCKOUT ? TeamTier.of(team, season).label() : "";
    }

    private TeamSituation situation(Team team, Matchday matchday, Standings standings, String shortcut) {
        Form form = formCalculator.formBefore(team, matchday);
        var standing = standings.of(team);
        return new TeamSituation(team.name, team.shortName, MatchdayPageModels.teamLink(team, shortcut),
                standing.map(s -> s.position()).orElse(null),
                standing.map(s -> s.balance().points()).orElse(0),
                form.results(), chronological(form.lastMatches()).stream().map(v -> ResultRow.of(v, shortcut)).toList(),
                BalanceRow.of("Heim", form.home()), BalanceRow.of("Auswärts", form.away()), form.includesProvisional());
    }

    /** Form is computed newest first; on the page it reads left to right like a timeline. */
    private static <T> List<T> chronological(List<T> newestFirst) {
        List<T> copy = new ArrayList<>(newestFirst);
        Collections.reverse(copy);
        return copy;
    }

    /** Table position of both teams after each matchday before this one, as JSON for the chart. */
    private String positionChart(Matchday matchday, Team home, Team away) {
        return positionChart(matchday.league, matchday.season, matchday.number - 1, List.of(home, away));
    }

    /** Table position of the teams after each of the first {@code matchdays} matchdays of the season. */
    private String positionChart(League league, int season, int matchdays, List<Team> teams) {
        List<String> labels = new ArrayList<>();
        List<List<String>> positions = new ArrayList<>();
        teams.forEach(t -> positions.add(new ArrayList<>()));
        int teamCount = 18;
        for (int n = 1; n <= matchdays; n++) {
            Standings s = standingsCalculator.standingsBefore(league, season, n + 1);
            labels.add("\"" + n + "\"");
            for (int i = 0; i < teams.size(); i++) {
                positions.get(i).add(s.of(teams.get(i)).map(p -> String.valueOf(p.position())).orElse("null"));
            }
            teamCount = Math.max(teamCount, s.positions().size());
        }
        List<String> series = new ArrayList<>();
        for (int i = 0; i < teams.size(); i++) {
            series.add(seriesJson(teams.get(i).shortName, positions.get(i), false));
        }
        return "{\"labels\":[" + String.join(",", labels) + "],\"teams\":[" + String.join(",", series) + "],\"teamCount\":" + teamCount + "}";
    }

    /**
     * Table position of one team across the current season and, aligned by matchday number, the
     * immediately preceding season in the same league — "where did we stand at matchday X last
     * year". Caller has already checked that a previous season in the same league exists.
     */
    private String seasonComparisonChart(League league, Team team, int season, int currentMatchdays) {
        int previousSeason = season - 1;
        int previousMatchdays = Match.findPlayedByTeamBefore(team, league, previousSeason, Integer.MAX_VALUE).stream()
                .mapToInt(m -> m.matchday.number).max().orElse(0);
        int maxMatchdays = Math.max(currentMatchdays, previousMatchdays);

        List<String> labels = new ArrayList<>();
        List<String> current = new ArrayList<>();
        List<String> previous = new ArrayList<>();
        int teamCount = 18;
        for (int n = 1; n <= maxMatchdays; n++) {
            labels.add("\"" + n + "\"");
            if (n <= currentMatchdays) {
                Standings s = standingsCalculator.standingsBefore(league, season, n + 1);
                current.add(s.of(team).map(p -> String.valueOf(p.position())).orElse("null"));
                teamCount = Math.max(teamCount, s.positions().size());
            } else {
                current.add("null");
            }
            if (n <= previousMatchdays) {
                Standings s = standingsCalculator.standingsBefore(league, previousSeason, n + 1);
                previous.add(s.of(team).map(p -> String.valueOf(p.position())).orElse("null"));
                teamCount = Math.max(teamCount, s.positions().size());
            } else {
                previous.add("null");
            }
        }
        List<String> series = List.of(
                seriesJson(MatchdayPageModels.seasonLabel(season), current, false),
                seriesJson(MatchdayPageModels.seasonLabel(previousSeason), previous, true));
        return "{\"labels\":[" + String.join(",", labels) + "],\"teams\":[" + String.join(",", series) + "],\"teamCount\":" + teamCount + "}";
    }

    private static String seriesJson(String name, List<String> positions, boolean dashed) {
        return "{\"name\":\"" + name + "\",\"positions\":[" + String.join(",", positions) + "],\"dashed\":" + dashed + "}";
    }

    private static League league(String shortcut) {
        return Arrays.stream(League.values())
                .filter(l -> l.sourceShortcut().equalsIgnoreCase(shortcut))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("unknown league " + shortcut));
    }
}
