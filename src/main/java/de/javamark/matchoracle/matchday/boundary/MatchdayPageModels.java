package de.javamark.matchoracle.matchday.boundary;

import de.javamark.matchoracle.matchday.entity.Balance;
import de.javamark.matchoracle.matchday.entity.Goal;
import de.javamark.matchoracle.matchday.entity.League;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.Matchday;
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
        };
    }

    static String seasonLabel(int season) {
        return season + "/" + String.format("%02d", (season + 1) % 100);
    }

    /** "vor 12 Minuten", "vor 3 Stunden" — the age of the data. */
    public static String age(Instant checkedAt, Instant now) {
        if (checkedAt == null) {
            return "noch nicht geprüft";
        }
        Duration d = Duration.between(checkedAt, now);
        if (d.toMinutes() < 1) return "gerade eben";
        if (d.toMinutes() < 60) return "vor " + d.toMinutes() + " Minuten";
        if (d.toHours() < 24) return "vor " + d.toHours() + " Stunden";
        return "vor " + d.toDays() + " Tagen";
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

        private static List<NavLeague> leagues(League active) {
            return java.util.Arrays.stream(League.values())
                    .map(l -> new NavLeague(l.sourceShortcut(), MatchdayPageModels.leagueName(l),
                            l == League.BUNDESLIGA_1 ? "1" : "2", l == active))
                    .toList();
        }
    }

    record DataInfo(String source, String age, boolean stale) {
        static DataInfo of(Matchday md, Instant now) {
            boolean stale = md.lastCheckedAt == null || Duration.between(md.lastCheckedAt, now).toHours() >= 1;
            return new DataInfo(MatchdayRepresentations.SOURCE, MatchdayPageModels.age(md.lastCheckedAt, now), stale);
        }
    }

    record MatchRow(long id, String link, String time, String home, String away, String homeShort, String awayShort,
                    String homeIcon, String awayIcon,
                    String score, String halfTime, boolean played, boolean provisional) {
        static MatchRow of(Match m, String leagueShortcut) {
            return new MatchRow(m.id, "/" + leagueShortcut + "/matches/" + m.id,
                    TIME.format(m.kickoff.atZone(DISPLAY_ZONE)),
                    m.homeTeam.name, m.awayTeam.name, m.homeTeam.shortName, m.awayTeam.shortName,
                    m.homeTeam.iconUrl, m.awayTeam.iconUrl,
                    m.fullTimeScore == null ? null : m.fullTimeScore.home + ":" + m.fullTimeScore.away,
                    m.halfTimeScore == null ? null : m.halfTimeScore.home + ":" + m.halfTimeScore.away,
                    m.isPlayed(), m.isPlayed() && m.resultStatus == ResultStatus.PROVISIONAL);
        }
    }

    record DayGroup(String label, List<MatchRow> matches) {
    }

    /** Zones of the table: bl1 1-4 Champions League; bl2 1-2 promotion, 3 promotion play-off; both 16 relegation play-off, 17-18 relegation. */
    static String zone(League league, int position) {
        if (league == League.BUNDESLIGA_1 && position <= 4) return "zone-top";
        if (league == League.BUNDESLIGA_2 && position <= 2) return "zone-top";
        if (league == League.BUNDESLIGA_2 && position == 3) return "zone-top-playoff";
        if (position == 16) return "zone-bottom-playoff";
        if (position >= 17) return "zone-bottom";
        return "";
    }

    record StandingRow(int position, String team, String teamLink, String icon, int played, int wins, int draws, int losses,
                       String goals, String difference, int points, String zone) {
        static StandingRow of(StandingPosition p, League league) {
            Balance b = p.balance();
            int diff = b.goalDifference();
            return new StandingRow(p.position(), p.team().name, MatchdayPageModels.teamLink(p.team(), league.sourceShortcut()), p.team().iconUrl,
                    b.played(), b.wins(), b.draws(), b.losses(),
                    b.goalsFor() + ":" + b.goalsAgainst(), (diff > 0 ? "+" : "") + diff, b.points(), MatchdayPageModels.zone(league, p.position()));
        }
    }

    static String teamLink(Team team, String leagueShortcut) {
        return "/" + leagueShortcut + "/teams/" + team.id;
    }

    record MatchdayPage(Nav nav, String season, int seasonYear, int number, String prevLink, String nextLink,
                        List<DayGroup> days, List<StandingRow> standings, boolean standingsProvisional, DataInfo data) {
    }

    /** One entry of a form strip or head-to-head list. */
    record ResultRow(String date, String opponent, boolean home, String score, TeamResult result, boolean provisional, String link) {
        static ResultRow of(TeamMatchView v, String leagueShortcut) {
            return new ResultRow(DAY_SHORT.format(v.match().kickoff.atZone(DISPLAY_ZONE)), v.opponent().name, v.home(),
                    v.goalsFor() + ":" + v.goalsAgainst(), v.result(),
                    v.match().resultStatus == ResultStatus.PROVISIONAL, "/" + leagueShortcut + "/matches/" + v.match().id);
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

    record MatchPage(Nav nav, String season, int number, String matchdayLink, String forecastLink, String day, MatchRow match,
                     List<GoalRow> goals, TeamSituation homeTeam, TeamSituation awayTeam,
                     List<ResultRow> headToHead, BalanceRow headToHeadBalance, boolean headToHeadProvisional,
                     String chartJson, DataInfo data) {
        public List<TeamSituation> teams() {
            return List.of(homeTeam, awayTeam);
        }
    }

    /** One match of the club's season schedule from the club's point of view; unplayed matches have no score. */
    record ScheduleRow(int matchday, String date, String time, String opponent, String opponentIcon, boolean home,
                       String score, TeamResult result, boolean provisional, String link) {
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
            return new ScheduleRow(m.matchday.number, DAY_SHORT.format(m.kickoff.atZone(DISPLAY_ZONE)), TIME.format(m.kickoff.atZone(DISPLAY_ZONE)),
                    opponent.name, opponent.iconUrl, home, score, result,
                    m.isPlayed() && m.resultStatus == ResultStatus.PROVISIONAL, "/" + leagueShortcut + "/matches/" + m.id);
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

    record TeamPage(Nav nav, String season, int seasonYear, boolean currentSeason, String name, String icon,
                    Integer position, String zone, BalanceRow total, boolean standingsProvisional,
                    List<SeasonChip> otherSeasons, List<ScheduleRow> schedule, BalanceRow home, BalanceRow away,
                    List<ScorerRow> scorers, String chartJson, DataInfo data) {
        public List<BalanceRow> balances() {
            return List.of(home, away);
        }
    }

    static String dayLabel(Instant kickoff) {
        return DAY.format(kickoff.atZone(DISPLAY_ZONE));
    }
}
