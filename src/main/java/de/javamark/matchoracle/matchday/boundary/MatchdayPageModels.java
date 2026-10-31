package de.javamark.matchoracle.matchday.boundary;

import de.javamark.matchoracle.matchday.entity.Balance;
import de.javamark.matchoracle.matchday.entity.Goal;
import de.javamark.matchoracle.matchday.entity.GoalTiming;
import de.javamark.matchoracle.matchday.entity.Decision;
import de.javamark.matchoracle.matchday.entity.League;
import de.javamark.matchoracle.matchday.entity.LeagueScorer;
import de.javamark.matchoracle.matchday.entity.Score;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.Matchday;
import de.javamark.matchoracle.matchday.entity.PlacementGoal;
import de.javamark.matchoracle.matchday.entity.ResultStatus;
import de.javamark.matchoracle.matchday.entity.Scorer;
import de.javamark.matchoracle.matchday.entity.StandingPosition;
import de.javamark.matchoracle.matchday.entity.Team;
import de.javamark.matchoracle.matchday.entity.TeamMatchView;
import de.javamark.matchoracle.matchday.entity.TeamResult;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** View models for the HTML pages: everything pre-formatted so templates stay logic-free. */
public final class MatchdayPageModels {

    static final ZoneId DISPLAY_ZONE = ZoneId.of("Europe/Berlin");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale.GERMAN);
    private static final DateTimeFormatter DAY_SHORT = DateTimeFormatter.ofPattern("d.M.yy", Locale.GERMAN);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.GERMAN);

    private MatchdayPageModels() {
    }

    static String leagueName(League league) {
        return switch (league) {
            case BUNDESLIGA_1 -> "1. Bundesliga";
            case BUNDESLIGA_2 -> "2. Bundesliga";
            case CHAMPIONS_LEAGUE -> "Champions League";
            case DFB_POKAL -> "DFB-Pokal";
            case NATIONS_LEAGUE -> "Nations League";
        };
    }

    /** Short marker in the league badge — "1"/"2" for the Bundesligas, an abbreviation otherwise. */
    static String badge(League league) {
        return switch (league) {
            case BUNDESLIGA_1 -> "1";
            case BUNDESLIGA_2 -> "2";
            case CHAMPIONS_LEAGUE -> "CL";
            case DFB_POKAL -> "DFB";
            case NATIONS_LEAGUE -> "NL";
        };
    }

    static String seasonLabel(int season) {
        return season + "/" + String.format("%02d", (season + 1) % 100);
    }

    /**
     * Whether a match currently counts as "läuft" — inferred purely from its kickoff time, never a
     * live score (Spec 1, Abgrenzung: keine Live-Ergebnisse während laufender Spiele).
     */
    static boolean isLive(Instant kickoff, boolean played, Instant now, Duration liveWindow) {
        return Match.isLive(kickoff, played, now, liveWindow);
    }

    /** How far ahead of kickoff an open page starts asking for the score — so a page opened at 15:25 still goes live. */
    static final Duration WATCH_AHEAD = Duration.ofMinutes(30);

    /**
     * Whether an open page keeps this match's score fresh by itself: from shortly before kickoff
     * until there is a result (or the live window has passed). Outside of that the page stays quiet.
     */
    static boolean isWatched(Instant kickoff, boolean played, Instant now, Duration liveWindow) {
        return !played && kickoff.isBefore(now.plus(WATCH_AHEAD)) && now.isBefore(kickoff.plus(liveWindow));
    }

    /** The live scores fragment for the watched matches among {@code rows}, or null when none is — then the page doesn't poll. */
    static String livePollLink(java.util.stream.Stream<MatchRow> rows, boolean details) {
        String ids = rows.filter(MatchRow::watched).map(r -> "match=" + r.id()).distinct()
                .collect(java.util.stream.Collectors.joining("&"));
        return ids.isEmpty() ? null : LIVE_SCORES_LINK + "?" + ids + (details ? "&details=true" : "");
    }

    /** Polled every minute by a page that shows a running match: fresh score cells, out of band. */
    static final String LIVE_SCORES_LINK = "/live/spielstaende";

    /**
     * The answer to a poll: one score cell per requested match; on the match page also the
     * half-time line and the goals. {@code keepPolling} is false once none of them runs any more.
     */
    record LiveScoresFragment(List<MatchRow> rows, MatchRow detail, List<GoalRow> goals, boolean keepPolling,
                              Countdown countdown) {
    }

    /** How long before kickoff the match head counts down (story r004). */
    static final Duration COUNTDOWN_AHEAD = Duration.ofHours(24);

    /**
     * The countdown in the match head: the label as rendered, the kickoff and the server's clock
     * (epoch seconds), so the page counts on from the server's time and not from a skewed device clock.
     */
    record Countdown(String label, long kickoff, long now) {
        /** Only for a match without a result that kicks off within the next 24 hours, else null. */
        static Countdown of(Instant kickoff, boolean played, Instant now) {
            if (played || !now.isBefore(kickoff) || kickoff.isAfter(now.plus(COUNTDOWN_AHEAD))) {
                return null;
            }
            return new Countdown(countdownLabel(Duration.between(now, kickoff)), kickoff.getEpochSecond(), now.getEpochSecond());
        }
    }

    /** "Anstoß in 2 Std. 14 Min.", "Anstoß in 5 Min." — minutes round up, so it never says 0 before kickoff; match.js says the same. */
    static String countdownLabel(Duration untilKickoff) {
        long minutes = Math.max(1, (untilKickoff.toSeconds() + 59) / 60);
        long hours = minutes / 60;
        long rest = minutes % 60;
        if (hours == 0) return "Anstoß in " + rest + " Min.";
        return "Anstoß in " + hours + " Std." + (rest == 0 ? "" : " " + rest + " Min.");
    }

    /** "vor 12 Minuten", "vor 3 Stunden" — the age of the data. */
    public static String age(Instant checkedAt, Instant now) {
        if (checkedAt == null) {
            return "noch nicht geprüft";
        }
        Duration d = Duration.between(checkedAt, now);
        if (d.toMinutes() < 1) return "gerade eben";
        if (d.toMinutes() < 60) return "vor " + d.toMinutes() + (d.toMinutes() == 1 ? " Minute" : " Minuten");
        if (d.toHours() < 24) return "vor " + d.toHours() + (d.toHours() == 1 ? " Stunde" : " Stunden");
        return "vor " + d.toDays() + (d.toDays() == 1 ? " Tag" : " Tagen");
    }

    public record NavLeague(String shortcut, String name, String badge, boolean active) {
    }

    /**
     * Top navigation in fixed order; only the active marker moves. League pages mark their
     * league, the site-wide pages (hit rate, parameters) mark themselves via {@code page}.
     */
    public record Nav(String leagueShortcut, String leagueName, List<NavLeague> leagues, String page) {
        /** For other features, which only know the shortcut. */
        public static Nav of(String shortcut) {
            return of(League.bySourceShortcut(shortcut).orElse(League.BUNDESLIGA_1));
        }

        public static Nav of(League league) {
            return new Nav(league.sourceShortcut(), MatchdayPageModels.leagueName(league), leagues(league), null);
        }

        /** A page that belongs to no league, e.g. "accuracy" or "parameters"; no league is marked. */
        public static Nav page(String page) {
            return new Nav(League.BUNDESLIGA_1.sourceShortcut(), null, leagues(null), page);
        }

        /**
         * Spec 02 is scoped to the two Bundesligas for now; templates use this to hide the KI-Vorschau
         * CTA elsewhere. Duplicates {@code ForecastService.supportsForecast} — matchday must not
         * depend on forecast, so this stays a plain, independently kept-in-sync check.
         */
        public boolean forecastSupported() {
            return "bl1".equals(leagueShortcut) || "bl2".equals(leagueShortcut);
        }

        private static List<NavLeague> leagues(League active) {
            return java.util.Arrays.stream(League.values())
                    .map(l -> new NavLeague(l.sourceShortcut(), MatchdayPageModels.leagueName(l),
                            MatchdayPageModels.badge(l), l == active))
                    .toList();
        }
    }

    record DataInfo(String source, String age, boolean stale) {
        static DataInfo of(Matchday md, Instant now) {
            boolean stale = md.lastCheckedAt == null || Duration.between(md.lastCheckedAt, now).toHours() >= 1;
            return new DataInfo(MatchdayRepresentations.SOURCE, MatchdayPageModels.age(md.lastCheckedAt, now), stale);
        }
    }

    /**
     * {@code decision}, {@code homeTier} and {@code awayTier} are empty outside a knockout
     * competition — spec 06: there a result carries how it was decided, and each club the
     * division it plays in.
     */
    record MatchRow(long id, String link, String time, String home, String away, String homeShort, String awayShort,
                    String homeIcon, String awayIcon,
                    String score, String halfTime, boolean played, boolean provisional, boolean live,
                    String liveScore, boolean watched, String decision, String homeTier, String awayTier) {
        static MatchRow of(Match m, String leagueShortcut, Instant now, Duration liveWindow) {
            return of(m, leagueShortcut, now, liveWindow, null, null);
        }

        static MatchRow of(Match m, String leagueShortcut, Instant now, Duration liveWindow,
                           String homeTier, String awayTier) {
            return new MatchRow(m.id, "/" + leagueShortcut + "/matches/" + m.id,
                    TIME.format(m.kickoff.atZone(DISPLAY_ZONE)),
                    m.homeTeam.name, m.awayTeam.name, m.homeTeam.shortName, m.awayTeam.shortName,
                    crestLink(m.homeTeam), crestLink(m.awayTeam),
                    m.fullTimeScore == null ? null : m.fullTimeScore.home + ":" + m.fullTimeScore.away,
                    m.halfTimeScore == null ? null : m.halfTimeScore.home + ":" + m.halfTimeScore.away,
                    m.isPlayed(), m.isPlayed() && m.resultStatus == ResultStatus.PROVISIONAL,
                    isLive(m.kickoff, m.isPlayed(), now, liveWindow),
                    m.liveScore == null ? null : m.liveScore.home + ":" + m.liveScore.away,
                    isWatched(m.kickoff, m.isPlayed(), now, liveWindow),
                    decisionLabel(m.decision, m.penaltyScore), homeTier, awayTier);
        }
    }

    /**
     * Spec 06: one round of a knockout competition — the band's sections, final on top.
     * {@code open} marks the round the viewer should land on: the one that is being played,
     * so a finished edition does not open with 32 first-round pairings.
     */
    record RoundSection(String name, int matchCount, String dateRange, List<MatchRow> matches, boolean open) {
    }

    /** Spec 06: a match a lower-division club won, with how far apart the two divisions were. */
    record UpsetRow(String round, String winner, String loser, String winnerTier, String loserTier,
                    String score, String decision, String link, int gap) {
    }

    /** Spec 06: one bar of the funnel — how wide the field still was in that round. */
    record FunnelRow(String round, int clubs, int percent, List<FunnelShare> shares) {
    }

    /** The share of one division in a round; {@code percent} is relative to that round's field. */
    record FunnelShare(String tier, String cssClass, int clubs, int percent) {
    }

    record FunnelFigures(int matches, int goals, int shootouts, int extraTime, int upsets) {
    }

    record KnockoutPage(Nav nav, String season, int seasonYear, List<SeasonChip> otherSeasons,
                        List<RoundSection> rounds, List<UpsetRow> upsets, DataInfo data,
                        List<FunnelRow> funnel, FunnelFigures figures) {

        public String livePollLink() {
            return MatchdayPageModels.livePollLink(rounds.stream().flatMap(r -> r.matches().stream()), false);
        }
    }

    /**
     * Spec 06: one group of a group competition — its own table and its matches of the shown matchday,
     * by day like on the matchday page (the time alone does not say when).
     */
    record GroupSection(String name, List<StandingRow> table, List<DayGroup> days) {
    }

    /** {@code rounds} carries a group competition's final round — empty while it is in the group stage. */
    record GroupsPage(Nav nav, String season, int seasonYear, int number, String prevLink, String nextLink,
                      List<GroupSection> groups, List<RoundSection> rounds, boolean standingsProvisional, DataInfo data) {

        public String livePollLink() {
            return MatchdayPageModels.livePollLink(java.util.stream.Stream.concat(
                    groups.stream().flatMap(g -> g.days().stream()).flatMap(d -> d.matches().stream()),
                    rounds.stream().flatMap(r -> r.matches().stream())), false);
        }
    }

    record DayGroup(String label, List<MatchRow> matches) {
    }

    /**
     * Zones of the table: bl1 1-4 Champions League; bl2 1-2 promotion, 3 promotion play-off; both
     * 16 relegation play-off, 17-18 relegation. Champions League league phase: 1-8 straight to the
     * round of 16, 9-24 into the knockout play-off, 25-36 eliminated.
     */
    /**
     * Spec 06: a knockout result shows the score of the match with how it was decided behind
     * it — "3:3 n. E. 7:5", never the shootout aggregate as the result itself.
     */
    static String decisionLabel(Decision decision, Score penaltyScore) {
        return switch (decision) {
            case REGULAR -> "";
            case EXTRA_TIME -> "n. V.";
            case PENALTIES -> penaltyScore == null ? "n. E."
                    : "n. E. " + penaltyScore.home + ":" + penaltyScore.away;
        };
    }

    /**
     * Spec 06: how a club's cup run ended — the line under the chain of rounds. Empty while
     * the club is still in it.
     */
    static String cupRunOutcome(String lastRoundLabel, boolean wonIt) {
        if (wonIt) {
            return "Pokalsieger";
        }
        if (lastRoundLabel == null) {
            return "";
        }
        // "die Runde", but "das Achtelfinale" — the preposition follows the round's gender
        return lastRoundLabel.endsWith("Runde")
                ? "in der " + lastRoundLabel + " aus"
                : "im " + lastRoundLabel + " aus";
    }

    static String zone(League league, int position) {
        List<PlacementGoal> goals = PlacementGoal.forPosition(league, position);
        if (goals.contains(PlacementGoal.KNOCKOUT_DIRECT) || goals.contains(PlacementGoal.EUROPE) || goals.contains(PlacementGoal.PROMOTION)) {
            return "zone-top";
        }
        if (goals.contains(PlacementGoal.KNOCKOUT_PLAYOFF) || goals.contains(PlacementGoal.PROMOTION_PLAYOFF)) {
            return "zone-top-playoff";
        }
        if (goals.contains(PlacementGoal.RELEGATION_PLAYOFF)) {
            return "zone-bottom-playoff";
        }
        if (goals.contains(PlacementGoal.RELEGATION) || goals.contains(PlacementGoal.ELIMINATION)) {
            return "zone-bottom";
        }
        return "";
    }

    record StandingRow(int position, String team, String teamLink, String icon, int played, int wins, int draws, int losses,
                       String goals, String difference, int points, String zone, String teamShort, List<ResultRow> form) {
        static StandingRow of(StandingPosition p, League league) {
            return of(p, league, List.of());
        }

        /** With the team's last FINAL results, oldest first, for the form column (story r013). */
        static StandingRow of(StandingPosition p, League league, List<TeamMatchView> form) {
            Balance b = p.balance();
            int diff = b.goalDifference();
            return new StandingRow(p.position(), p.team().name, MatchdayPageModels.teamLink(p.team(), league.sourceShortcut()), MatchdayPageModels.crestLink(p.team()),
                    b.played(), b.wins(), b.draws(), b.losses(),
                    b.goalsFor() + ":" + b.goalsAgainst(), (diff > 0 ? "+" : "") + diff, b.points(), MatchdayPageModels.zone(league, p.position()),
                    p.team().shortName == null ? p.team().name : p.team().shortName,
                    form.stream().map(v -> ResultRow.of(v, league.sourceShortcut())).toList());
        }
    }

    static String teamLink(Team team, String leagueShortcut) {
        return "/" + leagueShortcut + "/teams/" + team.id;
    }

    /** Must match {@code CrestResource}'s {@code @Path("/crests/{teamId}/{file}")}; null until the crest is downloaded. */
    static String crestLink(Team team) {
        return team.crestFile == null ? null : "/crests/" + team.id + "/" + team.crestFile;
    }

    /** Must match {@code SeasonOutlookPages}'s {@code @Path("/{league}/{season}/teams/{id}/outlook")} (spec 04). */
    static String outlookLink(String leagueShortcut, int season, long teamId) {
        return "/" + leagueShortcut + "/" + season + "/teams/" + teamId + "/outlook";
    }

    /** {@code rounds} carries a competition's knockout phase — empty where it has none. */
    record MatchdayPage(Nav nav, String season, int seasonYear, int number, String prevLink, String nextLink,
                        List<DayGroup> days, List<StandingRow> standings, boolean standingsProvisional, DataInfo data,
                        List<RoundSection> rounds, String dateRange) {

        public String livePollLink() {
            return MatchdayPageModels.livePollLink(java.util.stream.Stream.concat(
                    days.stream().flatMap(d -> d.matches().stream()),
                    rounds.stream().flatMap(r -> r.matches().stream())), false);
        }
    }

    /**
     * One league's short-form overview on the landing page: same day-grouped fixtures as the full
     * matchday page, plus a short table preview (top {@code tableTop.size()} rows only — the full
     * table lives on the matchday page behind {@code matchdayLink}).
     */
    /** One league in the landing page header and table rail: badge, name, the matchday shown, where its full page is. */
    record LandingLeague(String shortcut, String badge, String name, int matchdayNumber, String matchdayLink,
                         List<StandingRow> table, boolean tableProvisional) {
    }

    /** A match in the stream, with the league it belongs to — the stream mixes both. */
    record StreamRow(String league, String badge, MatchRow match) {
    }

    /** A kickoff block of the stream; {@code key} ties the forecast fragment's per-block summary to it. */
    record StreamSlot(String time, String key, List<StreamRow> rows) {
    }

    record StreamDayView(String label, List<StreamSlot> slots) {
    }

    /**
     * Spec 01, "Spieltags-Strom der Startseite": both Bundesligas as one stream by day and kickoff block,
     * the next matchday below once the shown one has started, the tables and the KI-Bilanz beside it.
     * {@code forecastPath} / {@code nextForecastPath} are the forecast feature's fragments that fill in the
     * KI cells — matchday only knows their address, as with the leaderboard.
     */
    record LandingPage(Nav nav, String dateRange, List<LandingLeague> leagues, List<StreamDayView> days, int liveCount,
                       List<StreamDayView> nextDays, String forecastPath, String nextForecastPath, String leaderboardLink) {

        public String livePollLink() {
            return MatchdayPageModels.livePollLink(days.stream().flatMap(d -> d.slots().stream())
                    .flatMap(s -> s.rows().stream()).map(StreamRow::match), false);
        }

        /** "Bundesliga, 9.–11. Oktober" — the period, since each league keeps its own matchday counter (spec 01). */
        public String heading() {
            return dateRange == null ? "Bundesliga" : "Bundesliga, " + dateRange;
        }
    }

    /** The forecast feature's fragment for the stream's KI cells (guide value, verdict, per-block summary). */
    static final String STREAM_FORECASTS_LINK = "/strom/ki-vorschau";

    /** "9.–11. Oktober", "30. Oktober – 1. November", "9. Oktober" — the days the stream covers. */
    static String dateRange(java.time.LocalDate first, java.time.LocalDate last) {
        DateTimeFormatter dayMonth = DateTimeFormatter.ofPattern("d. MMMM", Locale.GERMAN);
        if (first.equals(last)) return dayMonth.format(first);
        if (first.getMonth() == last.getMonth()) return first.getDayOfMonth() + ".–" + dayMonth.format(last);
        return dayMonth.format(first) + " – " + dayMonth.format(last);
    }

    /** The match ids of the stream (or the next matchday) as the query of a forecast fragment link. */
    static String forecastLink(List<StreamDayView> days) {
        String ids = days.stream().flatMap(d -> d.slots().stream()).flatMap(s -> s.rows().stream())
                .map(r -> "match=" + r.match().id()).collect(java.util.stream.Collectors.joining("&"));
        return ids.isEmpty() ? null : STREAM_FORECASTS_LINK + "?" + ids;
    }

    /** Spec 05, "Wer liegt vorne?": the review feature's own fragment endpoint, loaded via htmx — matchday stays unaware of it otherwise. */
    static final String LEADERBOARD_LINK = "/trefferbilanz/leaderboard";

    /** One entry of a form strip or head-to-head list. */
    record ResultRow(String date, String opponent, boolean home, String score, TeamResult result, boolean provisional, String link) {
        static ResultRow of(TeamMatchView v, String leagueShortcut) {
            return new ResultRow(DAY_SHORT.format(v.match().kickoff.atZone(DISPLAY_ZONE)), v.opponent().name, v.home(),
                    v.goalsFor() + ":" + v.goalsAgainst(), v.result(),
                    v.match().resultStatus == ResultStatus.PROVISIONAL, "/" + leagueShortcut + "/matches/" + v.match().id);
        }

        /** "S", "U", "N" — so the result never rests on its colour alone. */
        public String letter() {
            return switch (result) { case WIN -> "S"; case DRAW -> "U"; case LOSS -> "N"; };
        }

        /** "Sieg 3:1 gegen Werder Bremen", "Niederlage 0:2 bei Werder Bremen". */
        public String description() {
            String word = switch (result) { case WIN -> "Sieg"; case DRAW -> "Unentschieden"; case LOSS -> "Niederlage"; };
            return word + " " + score + (home ? " gegen " : " bei ") + opponent;
        }
    }

    record BalanceRow(String label, int played, int wins, int draws, int losses, String goals, int points) {
        static BalanceRow of(String label, Balance b) {
            return new BalanceRow(label, b.played(), b.wins(), b.draws(), b.losses(), b.goalsFor() + ":" + b.goalsAgainst(), b.points());
        }
    }

    record TeamSituation(String name, String shortName, String link, Integer position, int points, List<TeamResult> form,
                         List<ResultRow> lastMatches, BalanceRow home, BalanceRow away, boolean formProvisional) {
        public List<BalanceRow> balances() {
            return List.of(home, away);
        }
    }

    record GoalRow(String minute, String scorer, String score, boolean forHome, String note) {
        /** {@code forHome}: whether the home side's score went up with this goal. */
        static GoalRow of(Goal g, boolean forHome) {
            String note = g.penalty ? "Elfmeter" : g.ownGoal ? "Eigentor" : null;
            return new GoalRow(g.minute == null ? "–" : g.minute + "'", g.scorerName,
                    g.scoreAfter.home + ":" + g.scoreAfter.away, forHome, note);
        }
    }

    /** Story r005: the page for a match link that leads nowhere, with the way to the league's current matchday. */
    record MatchNotFoundPage(Nav nav, String currentLink) {
    }

    record MatchPage(Nav nav, String season, int number, String matchdayLink, String forecastLink, String day, MatchRow match,
                     List<GoalRow> goals, TeamSituation homeTeam, TeamSituation awayTeam,
                     List<ResultRow> headToHead, BalanceRow headToHeadBalance, boolean headToHeadProvisional,
                     String chartJson, List<StandingRow> table, boolean tableProvisional, DataInfo data,
                     Countdown countdown) {
        public List<TeamSituation> teams() {
            return List.of(homeTeam, awayTeam);
        }

        /** The rail's table marks the two clubs of this match. */
        public boolean playsHere(StandingRow row) {
            return row.teamLink().equals(homeTeam.link()) || row.teamLink().equals(awayTeam.link());
        }

        /** The match page also refreshes the half-time line and the goals while the match runs. */
        public String livePollLink() {
            return MatchdayPageModels.livePollLink(java.util.stream.Stream.of(match), true);
        }
    }

    /** One match of the club's season schedule from the club's point of view; unplayed matches have no score. */
    /** {@code section} is the round's name in a cup ("Achtelfinale") and "3. Spieltag" elsewhere. */
    record ScheduleRow(int matchday, String section, String date, String time, String opponent, String opponentIcon,
                       boolean home, String score, TeamResult result, boolean provisional, String decision, String link) {
        static ScheduleRow of(Match m, Team team, String leagueShortcut) {
            boolean home = m.homeTeam.equals(team);
            Team opponent = home ? m.awayTeam : m.homeTeam;
            String score = null;
            TeamResult result = null;
            if (m.isPlayed()) {
                TeamMatchView v = m.viewedBy(team);
                score = v.goalsFor() + ":" + v.goalsAgainst();
                result = v.result();
            }
            return new ScheduleRow(m.matchday.number, m.matchday.displayName(),
                    DAY_SHORT.format(m.kickoff.atZone(DISPLAY_ZONE)), TIME.format(m.kickoff.atZone(DISPLAY_ZONE)),
                    opponent.name, MatchdayPageModels.crestLink(opponent), home, score, result,
                    m.isPlayed() && m.resultStatus == ResultStatus.PROVISIONAL,
                    decisionLabel(m.decision, m.penaltyScore), "/" + leagueShortcut + "/matches/" + m.id);
        }
    }

    /** A past season of the club with its final position, linking to that season's club page. */
    record SeasonChip(String season, String league, Integer position, String link) {
    }

    record ScorerRow(String name, int goals, int penalties) {
        static ScorerRow of(Scorer s) {
            return new ScorerRow(s.name(), s.goals(), s.penalties());
        }
    }

    record LeagueScorerRow(int rank, String name, String team, String teamShort, String crest, int goals, int penalties) {
    }

    /** How a club appears beside its scorers: short name for narrow tables, crest link or null. */
    record ClubLook(String shortName, String crest) {
    }

    /** {@code leagues}: the switch between the two Bundesligas, empty for other competitions. */
    record LeagueScorersPage(Nav nav, String season, List<LeagueScorerRow> scorers, List<NavLeague> leagues) {
    }

    /** Story r014: how many places the league scorer list shows; everyone tied on the last place comes along. */
    static final int TOP_SCORER_PLACES = 10;

    /**
     * Story r014: the top of the league scorer list. Equal goals share a rank (1, 1, 3) and are ordered
     * by surname; the list ends after {@code places} places, keeping every player tied on the last one.
     */
    static List<LeagueScorerRow> topScorers(List<LeagueScorer> all, int places, Map<String, ClubLook> clubs) {
        List<LeagueScorer> sorted = all.stream()
                .sorted(java.util.Comparator.comparingInt(LeagueScorer::goals).reversed()
                        .thenComparing(sc -> surname(sc.name()), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(LeagueScorer::name).thenComparing(LeagueScorer::team))
                .toList();
        List<LeagueScorerRow> rows = new java.util.ArrayList<>();
        int rank = 0;
        for (int i = 0; i < sorted.size(); i++) {
            LeagueScorer sc = sorted.get(i);
            if (i == 0 || sc.goals() != sorted.get(i - 1).goals()) {
                rank = i + 1;
                if (rank > places) break;
            }
            ClubLook club = clubs.getOrDefault(sc.team(), new ClubLook(sc.team(), null));
            rows.add(new LeagueScorerRow(rank, sc.name(), sc.team(), club.shortName(), club.crest(), sc.goals(), sc.penalties()));
        }
        return rows;
    }

    /** "N. Füllkrug" → "Füllkrug", "Kane" → "Kane": the source gives one name field, the last word is the surname. */
    static String surname(String name) {
        String trimmed = name.strip();
        int space = trimmed.lastIndexOf(' ');
        return space < 0 ? trimmed : trimmed.substring(space + 1);
    }

    /** Chart.js data for the goal-timing bar chart: labels plus a scored/conceded series, as JSON. */
    static String goalTimingJson(List<GoalTiming> timings) {
        String labels = timings.stream().map(t -> '"' + t.label() + '"').collect(java.util.stream.Collectors.joining(","));
        String scored = timings.stream().map(t -> String.valueOf(t.scored())).collect(java.util.stream.Collectors.joining(","));
        String conceded = timings.stream().map(t -> String.valueOf(t.conceded())).collect(java.util.stream.Collectors.joining(","));
        return "{\"labels\":[" + labels + "],\"scored\":[" + scored + "],\"conceded\":[" + conceded + "]}";
    }

    /**
     * {@code hasTable} is false in a knockout competition — then position, zone and the
     * position curve are absent and {@code cupRunOutcome} says how the run ended instead
     * (spec 06). {@code tier} is the club's division, empty outside a cup.
     */
    record TeamPage(Nav nav, String season, int seasonYear, boolean currentSeason, String name, String icon,
                    Integer position, String zone, BalanceRow total, boolean standingsProvisional,
                    List<SeasonChip> otherSeasons, List<ScheduleRow> schedule, BalanceRow home, BalanceRow away,
                    List<ScorerRow> scorers, String goalTimingJson, String chartJson, String outlookLink, DataInfo data,
                    boolean hasTable, String cupRunOutcome, String tier) {
        public List<BalanceRow> balances() {
            return List.of(home, away);
        }

        /**
         * Whether this club page actually carries AI forecasts: they exist for the running season
         * only (a past season's matches are all played) and only where spec 02 reaches. Both halves
         * have to hold, so the question lives here rather than being spelled out per template —
         * a page must never promise a forecast it cannot show.
         */
        public boolean showsForecasts() {
            return currentSeason && nav.forecastSupported();
        }
    }

    static String dayLabel(Instant kickoff) {
        return DAY.format(kickoff.atZone(DISPLAY_ZONE));
    }

    static String time(Instant kickoff) {
        return TIME.format(kickoff.atZone(DISPLAY_ZONE));
    }
}
