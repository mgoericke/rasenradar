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
import de.javamark.matchoracle.matchday.entity.Standings;
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
    private static final DateTimeFormatter WEEKDAY = DateTimeFormatter.ofPattern("EE", Locale.GERMAN);

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
                    String liveScore, boolean watched, String decision, String homeTier, String awayTier,
                    long homeId, long awayId, String weekday) {
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
                    decisionLabel(m.decision, m.penaltyScore), homeTier, awayTier,
                    m.homeTeam.id, m.awayTeam.id, WEEKDAY.format(m.kickoff.atZone(DISPLAY_ZONE)));
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
                       String goals, String difference, int points, String zone, String teamShort, List<ResultRow> form,
                       long teamId) {
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
                    form.stream().map(v -> ResultRow.of(v, league.sourceShortcut())).toList(), p.team().id);
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
                        List<RoundSection> rounds, String dateRange, boolean current, Map<Long, String> stakes,
                        LiveTable live, List<SplitTable> splits, Map<Long, LeadLine> leadLines) {

        /** Story r070: the "Es geht um" line of an open match, or null. */
        public String stakeOf(long matchId) {
            return stakes.get(matchId);
        }

        /** Story r053: the course of a finished match, null when there is none to draw. */
        public LeadLine leadLineOf(long matchId) {
            return leadLines.get(matchId);
        }

        /**
         * Story r078: the table as "teamId:place,…" for nav.js, which works out in the browser which
         * open match helps "Mein Verein" — the club itself is never sent to the server.
         */
        public String placesByTeam() {
            return standings.stream().map(r -> r.teamId() + ":" + r.position()).collect(java.util.stream.Collectors.joining(","));
        }

        public String livePollLink() {
            return MatchdayPageModels.livePollLink(java.util.stream.Stream.concat(
                    days.stream().flatMap(d -> d.matches().stream()),
                    rounds.stream().flatMap(r -> r.matches().stream())), false);
        }

        /** Story r096: once every match of the matchday is played, starred matches go back to their day. */
        public boolean over() {
            return days.stream().flatMap(d -> d.matches().stream()).allMatch(MatchRow::played);
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
        /** "1. Liga", "2. Liga" — the short name in the matchday head and the league switch (design r102). */
        public String shortName() {
            return badge + ". Liga";
        }
    }

    /**
     * Design r102, "‹ Aktueller Spieltag ›": how many matchdays the start page stands away from the
     * current one, and where the arrows lead — null at the end of the season either way.
     */
    record MatchdayStep(int offset, Integer previous, Integer next) {
        static String link(int offset) {
            return offset == 0 ? "/" : "/?versatz=" + offset;
        }

        /** "Aktueller Spieltag", "Ein Spieltag zurück", "In 3 Spieltagen" — the label between the arrows. */
        public String label() {
            if (offset == 0) return "Aktueller Spieltag";
            if (offset == -1) return "Ein Spieltag zurück";
            if (offset == 1) return "Nächster Spieltag";
            return offset < 0 ? -offset + " Spieltage zurück" : "In " + offset + " Spieltagen";
        }

        public String previousLink() {
            return previous == null ? null : link(previous);
        }

        public String nextLink() {
            return next == null ? null : link(next);
        }
    }

    /**
     * The start page's stage: one match on the pitch, the second place the styleguide allows it.
     * {@code label} is "Topspiel" or "Mein Verein", empty for a match the visitor browsed to;
     * {@code reason} names both places, empty without. {@code status} stands above the score
     * ("Endstand", "Läuft", "Anstoß"). {@code kiLink} loads the KI line, null where there is no
     * KI-Vorschau. {@code browse} is the way through the start page's matches, null without.
     */
    record TopStage(String label, String leagueName, String when, String reason, String status, MatchRow match,
                    StageTeam home, StageTeam away, String kiLink, StageBrowse browse) {
        /** Both clubs' facts, home first; empty without a table. */
        public List<StageTeam> clubs() {
            return home == null ? List.of() : List.of(home, away);
        }
    }

    /** Design r102: a club's facts on the stage — place and points before the match, the form, newest first. */
    record StageTeam(Integer position, int points, List<FormChip> form) {
    }

    /** One result of the form as a letter: S, U or N, with what it was in words. */
    record FormChip(String letter, String result, String title) {
        static FormChip of(TeamMatchView v) {
            String letter = switch (v.result()) {
                case WIN -> "S";
                case DRAW -> "U";
                case LOSS -> "N";
            };
            return new FormChip(letter, v.result().name().toLowerCase(),
                    DAY_SHORT.format(v.match().kickoff.atZone(DISPLAY_ZONE)) + " " + (v.home() ? "gegen " : "bei ")
                            + v.opponent().name + " " + v.goalsFor() + ":" + v.goalsAgainst());
        }
    }

    /** Design r102: browsing the stage through the start page's matches — "Spiel 11 von 18". */
    record StageBrowse(long previous, long next, int position, int count) {
        /**
         * The way from {@code current} through {@code order}, round at both ends. {@code position} is 0
         * when the stage shows a match that is not on the page ("Mein Verein" in another competition) —
         * the arrows then lead to the page's first and last match. Null with fewer than two matches.
         */
        static StageBrowse of(List<Long> order, long current) {
            int count = order.size();
            int at = order.indexOf(current);
            if (count < 2) {
                return null;
            }
            if (at < 0) {
                return new StageBrowse(order.getLast(), order.getFirst(), 0, count);
            }
            return new StageBrowse(order.get((at - 1 + count) % count), order.get((at + 1) % count), at + 1, count);
        }
    }

    /**
     * Story r044: a played match for the card "Seit dem letzten Besuch". The browser compares
     * {@code endsAt} (kickoff plus the usual length of a match, epoch seconds) with the visit it
     * remembers — the visit itself never reaches the server.
     */
    record SinceRow(String badge, MatchRow match, long endsAt) {
    }

    /** Story r044: about when a match is over — 90 minutes, half time and stoppage time. */
    static final Duration MATCH_LENGTH = Duration.ofMinutes(115);

    /** Story r044: how far back the card can reach; an older visit sees the last two weeks. */
    static final Duration SINCE_REACH = Duration.ofDays(14);

    /** Story r044: the table of final results as "teamId:place:name;…" for the browser's comparison. */
    static String finalPlaces(Standings standings) {
        return standings.positions().stream()
                .map(p -> {
                    String name = p.team().shortName == null || p.team().shortName.isBlank() ? p.team().name : p.team().shortName;
                    return p.team().id + ":" + p.position() + ":" + name.replace(";", ",").replace(":", " ");
                })
                .collect(java.util.stream.Collectors.joining(";"));
    }

    /** Story r041: the table counted from home, away or the last five matches; {@code note} says what counts. */
    record SplitTable(String key, String label, String note, List<StandingRow> rows, List<String> moves) {
        SplitTable(String key, String label, String note, List<StandingRow> rows) {
            this(key, label, note, rows, List.of());
        }

        /** Story r071: the places gained or lost against the real table, one per row; empty for the plain splits. */
        public String move(int index) {
            return moves.isEmpty() ? null : moves.get(index);
        }
    }

    /** Story r071: "+2", "−1", "0" — places a team stands higher (+) or lower (−) than in the real table. */
    static String placesMoved(int realPosition, int scenarioPosition) {
        int d = realPosition - scenarioPosition;
        return d > 0 ? "+" + d : d < 0 ? "\u2212" + (-d) : "0";
    }

    /**
     * Story r059, "Wenn es so bleibt": the table with running matches at their current score.
     * {@code ended} once nothing runs any more — the page then falls back to the normal table.
     */
    record LiveTable(String link, boolean ended, List<LiveRow> rows) {
    }

    /** {@code move}: places up (positive) or down against the table before kickoff; {@code live} is empty for a club not playing. */
    record LiveRow(int position, int move, long teamId, String team, String teamShort, String icon, String teamLink,
                   int played, String difference, int points, String live, String liveClass) {

        public String moveLabel() {
            return move > 0 ? "▲ " + move : move < 0 ? "▼ " + (-move) : "–";
        }

        public String moveDescription() {
            int n = Math.abs(move);
            return move == 0 ? "unverändert" : n + (n == 1 ? " Platz " : " Plätze ") + (move > 0 ? "hoch" : "runter");
        }
    }

    /** A match in the stream, with the league it belongs to — the stream mixes both — and its lead line once finished. */
    record StreamRow(String league, String badge, MatchRow match, LeadLine lead) {
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
                       List<StreamDayView> nextDays, String forecastPath, String nextForecastPath, String leaderboardLink,
                       List<SinceRow> recent, String finalPlaces, TopStage top, MatchdayStep step) {

        public String livePollLink() {
            return MatchdayPageModels.livePollLink(days.stream().flatMap(d -> d.slots().stream())
                    .flatMap(s -> s.rows().stream()).map(StreamRow::match), false);
        }

        /** "Bundesliga, 9.–11. Oktober" — the period, since each league keeps its own matchday counter (spec 01). */
        public String heading() {
            return dateRange == null ? "Bundesliga" : "Bundesliga, " + dateRange;
        }

        /** Whether the page shows the current matchdays — "Seit dem letzten Besuch" and the next matchday belong only there. */
        public boolean current() {
            return step == null || step.offset() == 0;
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

    /** Story r024: how many scorers the club page shows above its tabs. */
    static final int TOP_CLUB_SCORERS = 5;

    /** Story r024: how many final results the form strip in the club head shows, oldest left as in the table. */
    static final int CLUB_FORM_LENGTH = 5;

    /** Story r035: how many meetings to list on the match page; the totals count all of them. */
    static final int RECENT_MEETINGS = 5;
    private static final DateTimeFormatter MONTH_YEAR = DateTimeFormatter.ofPattern("MMM yyyy", Locale.GERMAN);

    /**
     * Story r035: the all-time head-to-head of a match's two clubs. Totals and the biggest win
     * count final results only; the recent list shows provisional ones too, marked as such.
     */
    record HeadToHeadSummary(String team, String opponent, String since, int wins, int draws, int losses,
                             List<Meeting> recent, BiggestWin biggestWin, boolean recentProvisional) {

        static HeadToHeadSummary of(Team team, Team opponent, List<TeamMatchView> meetingsNewestFirst,
                                    Integer earliestSeason, String leagueShortcut) {
            List<TeamMatchView> settled = meetingsNewestFirst.stream()
                    .filter(v -> v.match().resultStatus != ResultStatus.PROVISIONAL).toList();
            Balance balance = Balance.EMPTY;
            for (TeamMatchView v : settled) {
                balance = balance.plus(v);
            }
            List<Meeting> recent = meetingsNewestFirst.stream().limit(RECENT_MEETINGS)
                    .map(v -> Meeting.of(v, leagueShortcut)).toList();
            return new HeadToHeadSummary(team.shortName, opponent.shortName,
                    earliestSeason == null ? null : seasonLabel(earliestSeason),
                    balance.wins(), balance.draws(), balance.losses(), recent, BiggestWin.of(settled),
                    recent.stream().anyMatch(Meeting::provisional));
        }

        public int total() {
            return wins + draws + losses;
        }

        /** Share of a count in the totals, for the bar's segment widths. */
        public double share(int count) {
            return total() == 0 ? 0 : Math.round(count * 1000.0 / total()) / 10.0;
        }
    }

    /** One meeting in the head-to-head list: month and year, who was at home, the score as played. */
    record Meeting(String month, String homeTeam, String awayTeam, String score, TeamResult result,
                   boolean provisional, String link) {
        static Meeting of(TeamMatchView v, String leagueShortcut) {
            Match m = v.match();
            return new Meeting(MONTH_YEAR.format(m.kickoff.atZone(DISPLAY_ZONE)), m.homeTeam.shortName, m.awayTeam.shortName,
                    m.fullTimeScore.home + ":" + m.fullTimeScore.away, v.result(),
                    m.resultStatus == ResultStatus.PROVISIONAL, "/" + leagueShortcut + "/matches/" + m.id);
        }
    }

    /**
     * The widest winning margin between the two clubs; on a tie the one with more goals for
     * the winner, then the more recent one. {@code null} when every meeting was a draw.
     */
    record BiggestWin(String winner, String loser, String score, int year, String link) {
        static BiggestWin of(List<TeamMatchView> settledNewestFirst) {
            TeamMatchView best = null;
            for (TeamMatchView v : settledNewestFirst) {
                int margin = Math.abs(v.goalsFor() - v.goalsAgainst());
                if (margin == 0) {
                    continue;
                }
                if (best == null || margin > Math.abs(best.goalsFor() - best.goalsAgainst())
                        || margin == Math.abs(best.goalsFor() - best.goalsAgainst())
                        && Math.max(v.goalsFor(), v.goalsAgainst()) > Math.max(best.goalsFor(), best.goalsAgainst())) {
                    best = v;
                }
            }
            if (best == null) {
                return null;
            }
            Match m = best.match();
            boolean homeWon = m.fullTimeScore.home > m.fullTimeScore.away;
            Team winner = homeWon ? m.homeTeam : m.awayTeam;
            Team loser = homeWon ? m.awayTeam : m.homeTeam;
            int high = Math.max(m.fullTimeScore.home, m.fullTimeScore.away);
            int low = Math.min(m.fullTimeScore.home, m.fullTimeScore.away);
            return new BiggestWin(winner.shortName, loser.shortName, high + ":" + low,
                    m.kickoff.atZone(DISPLAY_ZONE).getYear(), "/" + m.matchday.league.sourceShortcut() + "/matches/" + m.id);
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
                     HeadToHeadSummary headToHead,
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

    /**
     * Story r064: the key under the season wall — "3 Siege, 3 Remis, 1 Niederlage, 27 offen". A
     * provisional result is named apart and counts neither as a result nor as open.
     */
    static String wallKey(List<ScheduleRow> schedule) {
        int w = 0, d = 0, l = 0, open = 0, provisional = 0;
        for (ScheduleRow r : schedule) {
            if (r.score() == null) open++;
            else if (r.provisional()) provisional++;
            else switch (r.result()) { case WIN -> w++; case DRAW -> d++; case LOSS -> l++; }
        }
        String key = w + (w == 1 ? " Sieg, " : " Siege, ") + d + " Remis, " + l + (l == 1 ? " Niederlage, " : " Niederlagen, ") + open + " offen";
        return provisional == 0 ? key : key + ", " + provisional + " vorläufig";
    }

    /** Story r063: a scorer's profile, ready to print; {@code id} is the popover's element id. */
    record ScorerProfileView(String id, String name, String summary, String share, String split, String late,
                             List<ScorerGoalRow> goals) {
        static ScorerProfileView of(String id, de.javamark.matchoracle.matchday.control.ScorerProfileCalculator.Profile p,
                                    String club, String shortcut) {
            long pens = p.penalties();
            String summary = p.count() + (p.count() == 1 ? " Tor in " : " Tore in ") + p.matches() + (p.matches() == 1 ? " Spiel" : " Spielen")
                    + (pens == 0 ? "" : ", davon " + pens + (pens == 1 ? " Elfmeter" : " Elfmeter"));
            String share = p.sharePercent() == null ? null
                    : p.sharePercent() + " % der Tore von " + club + ", " + p.finalGoals() + " von " + p.clubFinalGoals();
            String split = "Heim " + p.homeGoals() + " : Auswärts " + (p.count() - p.homeGoals())
                    + " · 1. Halbzeit " + p.firstHalf() + " : 2. Halbzeit " + p.secondHalf();
            String late = p.late() == 0 ? null
                    : p.late() + (p.late() == 1 ? " Tor" : " Tore") + " ab der " + de.javamark.matchoracle.matchday.control.ScorerProfileCalculator.LATE + ". Minute";
            List<ScorerGoalRow> rows = p.goals().stream().map(g -> new ScorerGoalRow(g.match().matchday.number,
                    (g.home() ? "gegen " : "bei ") + shortOrName(g.opponent()), g.minute() == null ? "–" : g.minute() + "'",
                    g.forAfter() + ":" + g.againstAfter(), g.forFinal() + ":" + g.againstFinal(), g.penalty(), g.provisional(),
                    "/" + shortcut + "/matches/" + g.match().id)).toList();
            return new ScorerProfileView(id, p.name(), summary, share, split, late, rows);
        }
    }

    record ScorerGoalRow(int matchday, String opponent, String minute, String scoreAfter, String finalScore, boolean penalty,
                         boolean provisional, String link) {
    }

    /** Story r088: one month of the club calendar, Monday first; each week has seven days. */
    record CalendarMonth(String label, List<List<CalendarDay>> weeks) {
    }

    /**
     * Story r088: a day of the club calendar. {@code inMonth} false for the padding days of the first
     * and last week; {@code matches} the club's matches that day (two on one day are possible in a cup).
     */
    record CalendarDay(int day, boolean inMonth, boolean today, List<ScheduleRow> matches) {
        public ScheduleRow match() {
            return matches.isEmpty() ? null : matches.getFirst();
        }

        /** "7. November: gegen Leipzig, 18:30 Uhr" or "… bei Leipzig, 2:1 Sieg". */
        public String label(String month) {
            ScheduleRow r = match();
            if (r == null) return null;
            String result = r.score() == null ? r.time() + " Uhr"
                    : r.score() + (r.provisional() ? " vorläufig" : switch (r.result()) { case WIN -> " Sieg"; case DRAW -> " Remis"; case LOSS -> " Niederlage"; });
            return day + ". " + month + ": " + (r.home() ? "gegen " : "bei ") + r.opponent() + ", " + result;
        }
    }

    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMAN);

    /** Story r088: the club's season as month grids, from the first to the last month with a match. */
    static List<CalendarMonth> clubCalendar(List<Match> schedule, Team team, String shortcut, java.time.LocalDate today) {
        if (schedule.isEmpty()) return List.of();
        Map<java.time.LocalDate, List<ScheduleRow>> byDay = new java.util.TreeMap<>();
        for (Match m : schedule) {
            byDay.computeIfAbsent(m.kickoff.atZone(DISPLAY_ZONE).toLocalDate(), d -> new java.util.ArrayList<>())
                    .add(ScheduleRow.of(m, team, shortcut));
        }
        java.time.YearMonth first = java.time.YearMonth.from(((java.util.TreeMap<java.time.LocalDate, List<ScheduleRow>>) byDay).firstKey());
        java.time.YearMonth last = java.time.YearMonth.from(((java.util.TreeMap<java.time.LocalDate, List<ScheduleRow>>) byDay).lastKey());
        List<CalendarMonth> months = new java.util.ArrayList<>();
        for (java.time.YearMonth ym = first; !ym.isAfter(last); ym = ym.plusMonths(1)) {
            java.time.LocalDate start = ym.atDay(1).with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
            java.time.LocalDate end = ym.atEndOfMonth().with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY));
            List<List<CalendarDay>> weeks = new java.util.ArrayList<>();
            List<CalendarDay> week = new java.util.ArrayList<>();
            for (java.time.LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                boolean in = java.time.YearMonth.from(d).equals(ym);
                week.add(new CalendarDay(d.getDayOfMonth(), in, d.equals(today), in ? byDay.getOrDefault(d, List.of()) : List.of()));
                if (week.size() == 7) { weeks.add(List.copyOf(week)); week = new java.util.ArrayList<>(); }
            }
            months.add(new CalendarMonth(MONTH_LABEL.format(ym), List.copyOf(weeks)));
        }
        return List.copyOf(months);
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

    /**
     * The season page of a Bundesliga: figures of the whole season so far, from final results only.
     * {@code basedOn} counts the final matches, {@code leagues} is the switch between the two Bundesligas.
     */
    record SeasonPage(Nav nav, String season, List<NavLeague> leagues, int basedOn, HomeAdvantageView homeAdvantage,
                      List<StreakRow> streaksGoing, List<StreakRow> streaksStuck, ScoreGridView scores,
                      List<SpecialGoalCard> specialGoals, ComebackView comebacks, CrossTable crossTable,
                      ReachView reach, RunInView runIn, GoalTimingView goalTiming, LastYearView lastYear) {
    }

    /** Story r098: the league's home/draw/away shares and the two club lists, ready to print. */
    record HomeAdvantageView(int homeWinPercent, int drawPercent, int awayWinPercent, String lead,
                             List<HomeAwayRow> strongestAtHome, List<HomeAwayRow> preferAway) {
        static HomeAdvantageView of(de.javamark.matchoracle.matchday.control.HomeAdvantageCalculator.HomeAdvantage h, String shortcut) {
            Double ratio = h.homeToAwayRatio();
            String lead = ratio == null ? null
                    : "Wer zu Hause spielt, gewinnt " + decimal(ratio) + "-mal so oft wie der Gast.";
            return new HomeAdvantageView(h.homeWinPercent(), h.drawPercent(), h.awayWinPercent(), lead,
                    h.strongestAtHome().stream().map(c -> HomeAwayRow.of(c, shortcut)).toList(),
                    h.preferAway().stream().map(c -> HomeAwayRow.of(c, shortcut)).toList());
        }
    }

    /** One club in a Heimvorteil list: points per game at home and away, bar widths in percent of 3 points. */
    record HomeAwayRow(String team, String teamLink, String icon, String home, String away, int homeWidth, int awayWidth,
                       int homeGames, int awayGames) {
        static HomeAwayRow of(de.javamark.matchoracle.matchday.control.HomeAdvantageCalculator.ClubSplit c, String shortcut) {
            Team t = c.team();
            // one decimal reads best; when both round to the same figure, a second one shows the gap the order rests on
            boolean same = decimal(c.homePerGame()).equals(decimal(c.awayPerGame()));
            String home = same ? String.format(Locale.GERMAN, "%.2f", c.homePerGame()) : decimal(c.homePerGame());
            String away = same ? String.format(Locale.GERMAN, "%.2f", c.awayPerGame()) : decimal(c.awayPerGame());
            return new HomeAwayRow(t.shortName == null || t.shortName.isBlank() ? t.name : t.shortName,
                    MatchdayPageModels.teamLink(t, shortcut), MatchdayPageModels.crestLink(t), home, away,
                    (int) Math.round(c.homePerGame() / 3 * 100), (int) Math.round(c.awayPerGame() / 3 * 100),
                    c.homeGames(), c.awayGames());
        }
    }

    /** Story r068: one running streak — the club, the sentence and its results, oldest first. */
    record StreakRow(String team, String teamLink, String icon, String sentence, List<ResultRow> results) {
        static StreakRow of(de.javamark.matchoracle.matchday.control.StreakCalculator.Streak s, String shortcut) {
            Team t = s.team();
            return new StreakRow(t.shortName == null || t.shortName.isBlank() ? t.name : t.shortName,
                    MatchdayPageModels.teamLink(t, shortcut), MatchdayPageModels.crestLink(t),
                    streakSentence(s.kind(), s.length(), s.wholeSeason()),
                    s.matches().stream().map(v -> ResultRow.of(v, shortcut)).toList());
        }
    }

    /** "3 Siege in Folge", "in allen 7 Saisonspielen ungeschlagen" — the number always in the sentence. */
    static String streakSentence(de.javamark.matchoracle.matchday.control.StreakCalculator.Kind kind, int n, boolean wholeSeason) {
        if (wholeSeason) {
            return switch (kind) {
                case WINS -> "alle " + n + " Saisonspiele gewonnen";
                case UNBEATEN -> "in allen " + n + " Saisonspielen ungeschlagen";
                case CLEAN_SHEETS -> "in allen " + n + " Saisonspielen ohne Gegentor";
                case SCORING -> "in allen " + n + " Saisonspielen getroffen";
                case LOSSES -> "alle " + n + " Saisonspiele verloren";
                case WINLESS -> "in allen " + n + " Saisonspielen ohne Sieg";
                case GOALLESS -> "in allen " + n + " Saisonspielen ohne eigenes Tor";
            };
        }
        return switch (kind) {
            case WINS -> n + " Siege in Folge";
            case UNBEATEN -> "seit " + n + " Spielen ungeschlagen";
            case CLEAN_SHEETS -> "seit " + n + " Spielen ohne Gegentor";
            case SCORING -> "seit " + n + " Spielen mit eigenem Tor";
            case LOSSES -> n + " Niederlagen in Folge";
            case WINLESS -> "seit " + n + " Spielen ohne Sieg";
            case GOALLESS -> "seit " + n + " Spielen ohne eigenes Tor";
        };
    }

    /** Story r076: the score grid, home goals down, away goals across, plus the two lead sentences. */
    record ScoreGridView(List<String> heads, List<List<ScoreCell>> rows, String lead, String fourGoals) {
        static ScoreGridView of(de.javamark.matchoracle.matchday.control.ScoreFrequencyCalculator.ScoreFrequency f, String shortcut) {
            int cap = de.javamark.matchoracle.matchday.control.ScoreFrequencyCalculator.CAP;
            List<String> heads = new java.util.ArrayList<>();
            for (int i = 0; i <= cap; i++) heads.add(i == cap ? cap + "+" : String.valueOf(i));
            int max = Math.max(1, f.max());
            List<List<ScoreCell>> rows = new java.util.ArrayList<>();
            for (int h = 0; h <= cap; h++) {
                List<ScoreCell> row = new java.util.ArrayList<>();
                for (int a = 0; a <= cap; a++) {
                    int n = f.count(h, a);
                    // the shade grows with the count; above half strength the figure turns light
                    int shade = n == 0 ? 0 : 12 + (int) Math.round(68.0 * n / max);
                    String kind = h > a ? "home" : h == a ? "draw" : "away";
                    List<ScoreMatch> list = f.cells().get(h).get(a).stream().map(m -> new ScoreMatch(
                            shortOrName(m.homeTeam) + " – " + shortOrName(m.awayTeam) + " " + m.fullTimeScore.home + ":" + m.fullTimeScore.away,
                            m.matchday.number + ". Spieltag", "/" + shortcut + "/matches/" + m.id)).toList();
                    row.add(new ScoreCell("score-" + h + "-" + a, heads.get(h) + ":" + heads.get(a), n, kind, shade, shade > 50, list));
                }
                rows.add(row);
            }
            String lead = null;
            if (!f.mostFrequent().isEmpty()) {
                String scores = joinGerman(f.mostFrequent());
                String times = f.mostFrequentCount() + "-mal";
                lead = "Am häufigsten in " + f.matches() + (f.matches() == 1 ? " Spiel: " : " Spielen: ") + scores
                        + " (" + (f.mostFrequent().size() > 1 ? "je " : "") + times + ").";
            }
            int four = f.matchesWithFourGoals();
            String fourGoals = four == 0 ? "In keinem Spiel erzielte eine Mannschaft vier oder mehr Tore."
                    : "In " + four + (four == 1 ? " Spiel" : " Spielen") + " erzielte eine Mannschaft vier oder mehr Tore.";
            return new ScoreGridView(heads, rows, lead, fourGoals);
        }
    }

    /** One cell of the score grid: its count, the tendency it stands for, the shade and the matches behind it. */
    record ScoreCell(String id, String label, int count, String kind, int shade, boolean light, List<ScoreMatch> matches) {
        public String accessibleLabel() {
            return label + ": " + count + (count == 1 ? " Spiel" : " Spiele");
        }
    }

    record ScoreMatch(String title, String matchday, String link) {
    }

    /** "0:0", "0:0 und 2:1", "0:0, 2:1 und 1:0". */
    static String joinGerman(List<String> items) {
        if (items.size() == 1) return items.getFirst();
        return String.join(", ", items.subList(0, items.size() - 1)) + " und " + items.getLast();
    }

    private static String shortOrName(Team t) {
        return t.shortName == null || t.shortName.isBlank() ? t.name : t.shortName;
    }

    /**
     * Story r084: one card of "Tore der besonderen Art". {@code others} are the clubs below the top
     * places, each rendered hidden — the "Mein Verein" style in the head shows the chosen one.
     */
    record SpecialGoalCard(String title, String description, int total, List<SpecialGoalClub> top, List<SpecialGoalClub> others,
                           String latest, String latestLink) {
        static SpecialGoalCard of(de.javamark.matchoracle.matchday.control.SpecialGoalsCalculator.Card c, String shortcut) {
            String title = switch (c.kind()) {
                case LATE -> "Ab der 90. Minute";
                case EARLY -> "In den ersten fünf Minuten";
                case PENALTY -> "Vom Punkt";
                case OWN_GOAL -> "Geschenkt";
            };
            String description = switch (c.kind()) {
                case LATE -> "Tore ab der 90. Minute, Nachspielzeit eingeschlossen";
                case EARLY -> "Tore in den Minuten 1 bis 5";
                case PENALTY -> "verwandelte Elfmeter, für die schießende Mannschaft";
                case OWN_GOAL -> "Eigentore, für die Mannschaft, der sie nützen";
            };
            var top = c.top().stream().map(cc -> SpecialGoalClub.of(cc, shortcut)).toList();
            var others = c.clubs().stream().filter(cc -> cc.rank() > de.javamark.matchoracle.matchday.control.SpecialGoalsCalculator.PLACES)
                    .map(cc -> SpecialGoalClub.of(cc, shortcut)).toList();
            String latest = null, link = null;
            if (c.latest() != null) {
                var g = c.latest().goal();
                Match m = c.latest().match();
                latest = "Zuletzt: " + (g.scorerName == null || g.scorerName.isBlank() ? "unbekannt" : g.scorerName)
                        + (g.minute == null ? "" : ", " + g.minute + ". Minute") + ", " + shortOrName(m.homeTeam) + " – " + shortOrName(m.awayTeam);
                link = "/" + shortcut + "/matches/" + m.id;
            }
            return new SpecialGoalCard(title, description, c.total(), top, others, latest, link);
        }
    }

    record SpecialGoalClub(long teamId, int rank, String team, String icon, String teamLink, int count) {
        static SpecialGoalClub of(de.javamark.matchoracle.matchday.control.SpecialGoalsCalculator.ClubCount cc, String shortcut) {
            Team t = cc.team();
            return new SpecialGoalClub(t.id == null ? 0 : t.id, cc.rank(), shortOrName(t), MatchdayPageModels.crestLink(t),
                    MatchdayPageModels.teamLink(t, shortcut), cc.count());
        }
    }

    /** Story r091: one row per club, the lead sentence on the biggest comeback and the basis. */
    record ComebackView(int matches, int excluded, boolean any, String lead, String leadLink, List<ComebackRow> rows) {
        static ComebackView of(de.javamark.matchoracle.matchday.control.ComebackCalculator.Comebacks c, String shortcut) {
            int scale = Math.max(1, c.clubs().stream().mapToInt(r -> Math.max(r.won(), r.dropped())).max().orElse(1));
            var rows = c.clubs().stream().map(r -> new ComebackRow(shortOrName(r.team()), MatchdayPageModels.crestLink(r.team()),
                    MatchdayPageModels.teamLink(r.team(), shortcut), r.won(), r.dropped(),
                    (int) Math.round(100.0 * r.won() / scale), (int) Math.round(100.0 * r.dropped() / scale))).toList();
            String lead = null, link = null;
            var b = c.biggest();
            if (b != null) {
                Match m = b.match();
                lead = "Größte Aufholjagd: " + shortOrName(b.team()) + " lag " + b.deficit() + (b.deficit() == 1 ? " Tor" : " Tore")
                        + " zurück und holte noch " + (b.won() ? "den Sieg" : "ein Unentschieden") + " – "
                        + shortOrName(m.homeTeam) + " – " + shortOrName(m.awayTeam) + " " + m.fullTimeScore.home + ":" + m.fullTimeScore.away;
                link = "/" + shortcut + "/matches/" + m.id;
            }
            boolean any = c.clubs().stream().anyMatch(r -> r.won() > 0);
            return new ComebackView(c.matches(), c.excluded(), any, lead, link, rows);
        }
    }

    record ComebackRow(String team, String icon, String teamLink, int won, int dropped, int wonWidth, int droppedWidth) {
        /** "6 geholt, 0 verspielt" — what a screen reader hears per club. */
        public String sentence() {
            return won + " geholt, " + dropped + " verspielt";
        }
    }

    /**
     * Story r072: the cross table — home teams down, away teams across, both in table order. The lead
     * counts final results only; a provisional score is shown in its cell, marked, and not counted.
     */
    record CrossTable(List<CrossTeam> teams, List<List<CrossCell>> rows, String lead) {
        static CrossTable of(List<Team> order, List<Match> seasonMatches, String shortcut,
                             de.javamark.matchoracle.matchday.control.HomeAdvantageCalculator.HomeAdvantage shares) {
            Map<String, Match> byPair = new java.util.HashMap<>();
            for (Match m : seasonMatches) {
                byPair.put(m.homeTeam.id + "-" + m.awayTeam.id, m);
            }
            List<CrossTeam> teams = order.stream().map(t -> new CrossTeam(t.name, shortOrName(t),
                    MatchdayPageModels.teamLink(t, shortcut), MatchdayPageModels.crestLink(t))).toList();
            List<List<CrossCell>> rows = new java.util.ArrayList<>();
            for (Team home : order) {
                List<CrossCell> row = new java.util.ArrayList<>();
                for (Team away : order) {
                    if (home == away || (home.id != null && home.id.equals(away.id))) {
                        row.add(new CrossCell("self", null, null, null, false));
                        continue;
                    }
                    Match m = byPair.get(home.id + "-" + away.id);
                    String pairing = shortOrName(home) + " – " + shortOrName(away);
                    if (m == null) {
                        row.add(new CrossCell("open", null, null, pairing + ": noch offen", false));
                    } else if (m.fullTimeScore == null) {
                        row.add(new CrossCell("open", null, "/" + shortcut + "/matches/" + m.id, pairing + ": noch offen", false));
                    } else {
                        int h = m.fullTimeScore.home, a = m.fullTimeScore.away;
                        String kind = h > a ? "home" : h == a ? "draw" : "away";
                        String word = h > a ? "Heimsieg" : h == a ? "Remis" : "Auswärtssieg";
                        boolean provisional = m.resultStatus == ResultStatus.PROVISIONAL;
                        row.add(new CrossCell(kind, h + ":" + a, "/" + shortcut + "/matches/" + m.id,
                                pairing + " " + h + ":" + a + ", " + word + (provisional ? ", vorläufig" : ""), provisional));
                    }
                }
                rows.add(row);
            }
            String lead = shares.matches() == 0 ? null
                    : "Bisher " + shares.matches() + (shares.matches() == 1 ? " Spiel: " : " Spiele: ") + shares.homeWinPercent()
                    + " % Heimsiege, " + shares.drawPercent() + " % Remis, " + shares.awayWinPercent() + " % Auswärtssiege.";
            return new CrossTable(teams, rows, lead);
        }
    }

    record CrossTeam(String name, String shortName, String link, String icon) {
    }

    /** {@code kind}: home, draw, away, open or self (the diagonal). */
    record CrossCell(String kind, String score, String link, String label, boolean provisional) {
    }

    /**
     * Story r058: every club on one points scale — today's points (filled), what is still possible
     * (outlined), the pace value (a neutral tick) and the reference places as thin lines.
     */
    record ReachView(String lead, List<ReachLine> lines, List<ReachRow> rows) {
        static ReachView of(de.javamark.matchoracle.matchday.control.ReachCalculator.Reach r, String shortcut) {
            if (r.clubs().isEmpty()) {
                return new ReachView(null, List.of(), List.of());
            }
            int scale = Math.max(1, r.clubs().stream().mapToInt(c -> Math.max(c.maximum(), c.pace() == null ? 0 : c.pace())).max().orElse(1));
            List<ReachLine> lines = r.thresholds().stream()
                    .map(t -> new ReachLine(t.label(), t.points(), percent(t.points(), scale))).toList();
            List<ReachRow> rows = r.clubs().stream().map(c -> new ReachRow(c.position(), shortOrName(c.team()),
                    MatchdayPageModels.crestLink(c.team()), MatchdayPageModels.teamLink(c.team(), shortcut),
                    c.points(), c.maximum(), c.pace(), c.titleOutOfReach(),
                    percent(c.points(), scale), percent(c.maximum(), scale), c.pace() == null ? null : percent(c.pace(), scale))).toList();
            var leader = r.clubs().getFirst();
            long out = r.clubs().stream().filter(de.javamark.matchoracle.matchday.control.ReachCalculator.ClubReach::titleOutOfReach).count();
            String lead = (r.remainingMatchdays() == 0 ? "Alle Spiele sind gespielt. "
                    : "Noch bis zu " + r.remainingMatchdays() + (r.remainingMatchdays() == 1 ? " Spiel" : " Spiele") + " je Verein. ")
                    + shortOrName(leader.team()) + " führt mit " + leader.points() + (leader.points() == 1 ? " Punkt" : " Punkten")
                    + (out == 0 ? "; jeder Verein kann Platz 1 rechnerisch noch erreichen."
                       : "; " + out + (out == 1 ? " Verein kann" : " Vereine können") + " Platz 1 rechnerisch nicht mehr erreichen.")
                    + " Eine Rechnung, keine Vorhersage.";
            return new ReachView(lead, lines, rows);
        }

        private static int percent(int value, int scale) {
            return (int) Math.round(100.0 * value / scale);
        }
    }

    record ReachLine(String label, int points, int at) {
    }

    record ReachRow(int position, String team, String icon, String teamLink, int points, int maximum, Integer pace,
                    boolean titleOutOfReach, int pointsAt, int maximumAt, Integer paceAt) {
    }

    /** Story r045: the run-in grid — clubs in table order, the next matchdays across. */
    record RunInView(List<Integer> matchdays, List<RunInRow> rows) {
        static RunInView of(de.javamark.matchoracle.matchday.control.RunInCalculator.RunIn r, String shortcut) {
            int clubs = r.clubs().size();
            long ranked = r.clubs().stream().filter(c -> c.rank() != null).count();
            List<RunInRow> rows = r.clubs().stream().map(c -> new RunInRow(c.position(), shortOrName(c.team()),
                    MatchdayPageModels.crestLink(c.team()), MatchdayPageModels.teamLink(c.team(), shortcut),
                    c.cells().stream().map(cell -> RunInCell.of(cell, clubs, shortcut)).toList(),
                    c.averagePosition() == null ? "–" : "Ø " + decimal(c.averagePosition()),
                    c.rank() == null ? "keine Spiele mehr" : c.rank() + ". schwerstes von " + ranked)).toList();
            return new RunInView(r.matchdays(), rows);
        }
    }

    record RunInRow(int position, String team, String icon, String teamLink, List<RunInCell> cells, String average, String rank) {
    }

    /** {@code state}: open, played or free; {@code shade} in percent of ink — the stronger the opponent, the darker. */
    record RunInCell(String state, String opponent, String icon, String venue, int opponentPosition, int shade, boolean light,
                     String link, String label) {
        static RunInCell of(de.javamark.matchoracle.matchday.control.RunInCalculator.Cell c, int clubs, String shortcut) {
            return switch (c.state()) {
                case PLAYED -> new RunInCell("played", null, null, null, 0, 0, false, null, c.matchday() + ". Spieltag: gespielt");
                case FREE -> new RunInCell("free", null, null, null, 0, 0, false, null, c.matchday() + ". Spieltag: spielfrei");
                case OPEN -> {
                    var f = c.fixture();
                    int shade = clubs <= 1 ? 10 : 10 + (int) Math.round(65.0 * (clubs - f.opponentPosition()) / (clubs - 1));
                    String venue = f.home() ? "H" : "A";
                    yield new RunInCell("open", shortOrName(f.opponent()), MatchdayPageModels.crestLink(f.opponent()), venue,
                            f.opponentPosition(), shade, shade > 50, "/" + shortcut + "/matches/" + f.match().id,
                            c.matchday() + ". Spieltag: " + (f.home() ? "zu Hause gegen " : "bei ") + shortOrName(f.opponent())
                                    + ", Platz " + f.opponentPosition());
                }
            };
        }
    }

    /** Story r016: below this many goals no sentence names a strongest phase — decided 31 Oct 2026. */
    static final int GOAL_TIMING_MINIMUM = 5;

    /**
     * Story r016 "Wann fallen die Tore?": the sentence over the chart and the numbers as rows — for a
     * club (scored and conceded) or for a whole league (all goals, {@code conceded} unused).
     */
    record GoalTimingView(String sentence, boolean empty, List<GoalTimingRow> rows) {

        static GoalTimingView ofTeam(String team, List<GoalTiming> timings) {
            return of(timings, (phase, count, total, tie) ->
                    team + " trifft am häufigsten " + phase + " (" + (tie ? "je " : "") + count + " von " + total + " Toren).");
        }

        static GoalTimingView ofLeague(List<GoalTiming> timings) {
            return of(timings, (phase, count, total, tie) ->
                    "Die meisten Tore fallen " + phase + " (" + (tie ? "je " : "") + count + " von " + total + ").");
        }

        private interface Sentence {
            String of(String phase, int count, int total, boolean tie);
        }

        private static GoalTimingView of(List<GoalTiming> timings, Sentence sentence) {
            int total = timings.stream().mapToInt(GoalTiming::scored).sum();
            int max = timings.stream().mapToInt(t -> Math.max(t.scored(), t.conceded())).max().orElse(0);
            List<GoalTimingRow> rows = timings.stream().map(t -> new GoalTimingRow(t.label().replace("-", "–"), t.scored(), t.conceded(),
                    max == 0 ? 0 : (int) Math.round(100.0 * t.scored() / max),
                    max == 0 ? 0 : (int) Math.round(100.0 * t.conceded() / max))).toList();
            if (total == 0) {
                return new GoalTimingView("Noch kein Tor in dieser Saison.", true, rows);
            }
            if (total < GOAL_TIMING_MINIMUM) {
                return new GoalTimingView("Noch zu wenig Tore für eine Aussage.", false, rows);
            }
            int best = timings.stream().mapToInt(GoalTiming::scored).max().orElse(0);
            List<String> phases = timings.stream().filter(t -> t.scored() == best).map(t -> phase(t.label())).toList();
            String phase = phases.size() == 1 ? phases.getFirst()
                    : String.join(", ", phases.subList(0, phases.size() - 1)) + " sowie " + phases.getLast();
            return new GoalTimingView(sentence.of(phase, best, total, phases.size() > 1), false, rows);
        }

        /** "76.-90." → "zwischen der 76. und der 90. Minute". */
        static String phase(String label) {
            String[] p = label.split("-");
            return "zwischen der " + p[0] + " und der " + p[1] + " Minute";
        }
    }

    record GoalTimingRow(String label, int scored, int conceded, int scoredWidth, int concededWidth) {
    }

    /**
     * Story r053 "Spielverlauf als Führungslinie": the lead of a finished match from minute 0 to the
     * end, up for every home goal and down for every away goal (an own goal counts for the side that
     * benefits — the source's running score already says so). {@code path} is an SVG path in a
     * 100 × 24 box; the lead is clamped at two goals, the goal list underneath stays exact.
     */
    record LeadLine(String path, String label, List<GoalRow> goals) {
        static final int CLAMP = 2;
        private static final double MID = 12, STEP = 5, LAST_MINUTE = 95;

        /** Null for an unplayed match or one whose goal list does not add up to its final score. */
        static LeadLine of(Match m) {
            if (m.fullTimeScore == null || m.goals.size() != m.fullTimeScore.home + m.fullTimeScore.away
                    || m.goals.stream().anyMatch(g -> g.minute == null || g.scoreAfter == null)) {
                return null;
            }
            StringBuilder path = new StringBuilder("M0 12");
            List<GoalRow> rows = new java.util.ArrayList<>();
            Score before = new Score(0, 0);
            for (Goal g : m.goals) {
                boolean forHome = g.scoreAfter.home > before.home;
                before = g.scoreAfter;
                int lead = Math.max(-CLAMP, Math.min(CLAMP, g.scoreAfter.home - g.scoreAfter.away));
                path.append(" H").append(x(g.minute)).append(" V").append(fmt(MID - STEP * lead));
                rows.add(GoalRow.of(g, forHome));
            }
            path.append(" H100");
            String label = m.goals.isEmpty() ? "Spielverlauf: keine Tore"
                    : "Spielverlauf: " + rows.stream().map(r -> r.minute() + " " + r.score()).collect(java.util.stream.Collectors.joining(", "));
            return new LeadLine(path.toString(), label, List.copyOf(rows));
        }

        private static String x(int minute) {
            return fmt(Math.min(100, 100.0 * minute / LAST_MINUTE));
        }

        private static String fmt(double v) {
            return String.format(Locale.ROOT, "%.1f", v);
        }

        /** Where the half-time tick sits in the same box. */
        public String halfTime() {
            return x(45);
        }
    }

    /** Story r081: one row per club with last season to compare, the newcomers apart, a lead sentence. */
    record LastYearView(String lead, List<LastYearRow> rows, List<String> newcomers, boolean available) {
        static LastYearView of(de.javamark.matchoracle.matchday.control.LastYearCalculator.LastYear y, String shortcut) {
            int scale = Math.max(1, y.clubs().stream().mapToInt(c -> Math.abs(c.difference())).max().orElse(1));
            List<LastYearRow> rows = y.clubs().stream().map(c -> new LastYearRow(shortOrName(c.team()),
                    MatchdayPageModels.crestLink(c.team()), MatchdayPageModels.teamLink(c.team(), shortcut),
                    c.otherLeague() == null ? null : "(" + (c.otherLeague() == League.BUNDESLIGA_1 ? "1. Liga" : "2. Liga") + ")",
                    c.points() + " statt " + c.pointsThen() + " nach " + c.games() + (c.games() == 1 ? " Spiel" : " Spielen"),
                    (c.difference() > 0 ? "+" : c.difference() < 0 ? "\u2212" : "±") + Math.abs(c.difference()),
                    c.difference() > 0 ? "up" : c.difference() < 0 ? "down" : "same",
                    (int) Math.round(50.0 * Math.abs(c.difference()) / scale))).toList();
            String lead = null;
            if (!y.clubs().isEmpty()) {
                long better = y.clubs().stream().filter(c -> c.difference() > 0).count();
                var top = y.clubs().getFirst();
                lead = better + " von " + y.clubs().size() + " Vereinen haben mehr Punkte als nach gleich vielen Spielen im Vorjahr"
                        + (top.difference() > 0 ? "; am meisten zugelegt: " + shortOrName(top.team()) + " (+" + top.difference() + ")." : ".");
            }
            return new LastYearView(lead, rows, y.newcomers().stream().map(MatchdayPageModels::shortOrName).toList(), !y.clubs().isEmpty());
        }
    }

    /** {@code width}: the bar in percent of the row's half — to the right for more points, to the left for fewer. */
    record LastYearRow(String team, String icon, String teamLink, String otherLeague, String figures, String difference,
                       String direction, int width) {
    }

    /** One decimal with a comma, as German readers expect it: 1.8 → "1,8". */
    static String decimal(double value) {
        return String.format(Locale.GERMAN, "%.1f", value);
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
    /**
     * Story r003: one club of the two Bundesligas as "Mein Verein". The choice lives in the browser
     * only (localStorage, see nav.js); the server just lists the clubs and resolves the club's link.
     */
    record MyClubChoice(long id, String name, String shortName, String crest) {
        static MyClubChoice of(Team t) {
            return new MyClubChoice(t.id, t.name, t.shortName == null || t.shortName.isBlank() ? t.name : t.shortName, crestLink(t));
        }

        /** League-free, so the link survives promotion and relegation (MatchdayPages.myClub). */
        public String link() {
            return "/mein-verein/" + id;
        }
    }

    /**
     * Story r092: the bar with the next match of "Mein Verein" on every page. Server-made and
     * fetched by nav.js every minute, so the countdown moves on without a page reload.
     */
    record NextMatchBar(String title, String when, String countdown, String opponentCrest, String link,
                        boolean live, String score, boolean provisional) {

        private static final DateTimeFormatter WEEKDAY_DATE = DateTimeFormatter.ofPattern("EE, d.M.", Locale.GERMAN);

        /**
         * The match the bar shows: the running one, one just ended with a provisional result while
         * still in the live window, else the next one not yet played. {@code null} when none is left.
         */
        static Match pick(List<Match> byKickoff, Instant now, Duration liveWindow) {
            for (Match m : byKickoff) {
                boolean inWindow = now.isBefore(m.kickoff.plus(liveWindow));
                if (!m.isPlayed() && (inWindow || m.kickoff.isAfter(now))) return m;
                if (m.isPlayed() && m.resultStatus == ResultStatus.PROVISIONAL && inWindow && !now.isBefore(m.kickoff)) return m;
            }
            return null;
        }

        static NextMatchBar of(Match m, Team club, Instant now, Duration liveWindow) {
            boolean home = m.homeTeam.equals(club);
            Team opponent = home ? m.awayTeam : m.homeTeam;
            String clubName = shortOrName(club);
            String opponentName = shortOrName(opponent);
            boolean live = isLive(m.kickoff, m.isPlayed(), now, liveWindow);
            Score shown = m.isPlayed() ? m.fullTimeScore : live ? m.liveScore : null;
            return new NextMatchBar(clubName + (home ? " gegen " : " bei ") + opponentName,
                    when(m.kickoff, now), live || m.isPlayed() ? null : countdown(m.kickoff, now),
                    crestLink(opponent), "/" + m.matchday.league.sourceShortcut() + "/matches/" + m.id,
                    live, shown == null ? null : shown.home + ":" + shown.away,
                    m.isPlayed() && m.resultStatus == ResultStatus.PROVISIONAL);
        }

        private static String shortOrName(Team t) {
            return t.shortName == null || t.shortName.isBlank() ? t.name : t.shortName;
        }

        /** "heute 15:30", "morgen 20:30", "Sa., 8.11. 15:30". */
        static String when(Instant kickoff, Instant now) {
            java.time.ZonedDateTime k = kickoff.atZone(DISPLAY_ZONE);
            long days = java.time.temporal.ChronoUnit.DAYS.between(now.atZone(DISPLAY_ZONE).toLocalDate(), k.toLocalDate());
            String day = days == 0 ? "heute" : days == 1 ? "morgen" : WEEKDAY_DATE.format(k);
            return day + " " + TIME.format(k);
        }

        /** "in 42 Min", "in 13 Std 26 Min", "in 3 Tagen" (calendar days once a day or more is left). */
        static String countdown(Instant kickoff, Instant now) {
            Duration d = Duration.between(now, kickoff);
            if (d.isNegative() || d.isZero()) return null;
            long minutes = (d.toSeconds() + 59) / 60;
            if (minutes < 60) return "in " + minutes + " Min";
            if (minutes < 24 * 60) return "in " + minutes / 60 + " Std" + (minutes % 60 == 0 ? "" : " " + minutes % 60 + " Min");
            long days = java.time.temporal.ChronoUnit.DAYS.between(now.atZone(DISPLAY_ZONE).toLocalDate(), kickoff.atZone(DISPLAY_ZONE).toLocalDate());
            return days == 1 ? "in einem Tag" : "in " + days + " Tagen";
        }
    }

    record MyClubLeague(String name, List<MyClubChoice> clubs) {
    }

    record MyClubPage(Nav nav, List<MyClubLeague> leagues) {
    }

    record TeamPage(Nav nav, String season, int seasonYear, boolean currentSeason, String name, String icon,
                    Integer position, String zone, BalanceRow total, boolean standingsProvisional,
                    List<SeasonChip> otherSeasons, List<ScheduleRow> schedule, BalanceRow home, BalanceRow away,
                    List<ScorerRow> scorers, String goalTimingJson, String chartJson, String outlookLink, DataInfo data,
                    boolean hasTable, String cupRunOutcome, String tier, MyClubChoice myClub,
                    boolean noMatchesLeft, List<ResultRow> form, String calendarLink, GoalTimingView goalTiming,
                    Map<String, ScorerProfileView> scorerProfiles, List<CalendarMonth> calendar) {

        /** Story r063: the profile behind a name in the scorer list, null for a name without one. */
        public ScorerProfileView profileOf(String scorer) {
            return scorerProfiles.get(scorer);
        }

        public List<BalanceRow> balances() {
            return List.of(home, away);
        }

        /** Story r024: the first open match of the season, or the last played one once none is left. */
        public ScheduleRow next() {
            return schedule.stream().filter(r -> r.score() == null).findFirst()
                    .orElse(schedule.isEmpty() ? null : schedule.get(schedule.size() - 1));
        }

        /** Story r024: open matches in kickoff order — the next one first. */
        public List<ScheduleRow> upcoming() {
            return schedule.stream().filter(r -> r.score() == null).toList();
        }

        /** Story r024: played matches, newest first. */
        public List<ScheduleRow> results() {
            return schedule.stream().filter(r -> r.score() != null).toList().reversed();
        }

        /** Story r064: the season wall — the latest played matchday first, then the open ones in date order. */
        public List<ScheduleRow> wall() {
            List<ScheduleRow> tiles = new java.util.ArrayList<>(results());
            tiles.addAll(upcoming());
            return tiles;
        }

        public String wallKey() {
            return MatchdayPageModels.wallKey(schedule);
        }

        /** Story r024: the head of the scorer list sits above the tabs; the rest folds away. */
        public List<ScorerRow> topScorers() {
            return scorers.subList(0, Math.min(TOP_CLUB_SCORERS, scorers.size()));
        }

        public List<ScorerRow> moreScorers() {
            return scorers.subList(Math.min(TOP_CLUB_SCORERS, scorers.size()), scorers.size());
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
