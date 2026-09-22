package de.javamark.matchoracle.matchday.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
public class Match extends PanacheEntity {

    @Column(nullable = false, unique = true)
    public int externalId;

    @Column(nullable = false)
    public Instant kickoff;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public ResultStatus resultStatus = ResultStatus.PROVISIONAL;

    public Instant sourceLastChangedAt;

    @ManyToOne(optional = false)
    public Matchday matchday;

    @ManyToOne(optional = false)
    public Team homeTeam;

    @ManyToOne(optional = false)
    public Team awayTeam;

    // Score is embedded twice, so each embedding maps home/away to its own columns
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "home", column = @Column(name = "half_time_home")),
            @AttributeOverride(name = "away", column = @Column(name = "half_time_away"))
    })
    public Score halfTimeScore;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "home", column = @Column(name = "full_time_home")),
            @AttributeOverride(name = "away", column = @Column(name = "full_time_away"))
    })
    public Score fullTimeScore;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    public List<Goal> goals = new ArrayList<>();

    /** Spec 06: how the match was decided; always REGULAR outside a knockout competition. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public Decision decision = Decision.REGULAR;

    /** Spec 06: the shootout aggregate, null unless the match was decided on penalties. */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "home", column = @Column(name = "penalty_home")),
            @AttributeOverride(name = "away", column = @Column(name = "penalty_away"))
    })
    public Score penaltyScore;

    /**
     * Spec 06: the shootout decides, then extra time, then the 90 minutes. Empty for a
     * draw and for a match that has not been played.
     */
    public Optional<Team> winner() {
        Score decisive = penaltyScore != null ? penaltyScore : fullTimeScore;
        if (decisive == null || decisive.home == decisive.away) {
            return Optional.empty();
        }
        return Optional.of(decisive.home > decisive.away ? homeTeam : awayTeam);
    }

    public static Optional<Match> findByExternalId(int externalId) {
        return find("externalId", externalId).firstResultOptional();
    }

    public static List<Match> findByMatchday(Matchday matchday) {
        return list("matchday", matchday);
    }

    public boolean isPlayed() {
        return fullTimeScore != null;
    }

    public boolean involves(Team team) {
        return homeTeam.equals(team) || awayTeam.equals(team);
    }

    /** This match from one team's point of view; the match must be played and involve the team. */
    public TeamMatchView viewedBy(Team team) {
        if (homeTeam.equals(team)) {
            return new TeamMatchView(this, true, fullTimeScore.home, fullTimeScore.away);
        }
        if (awayTeam.equals(team)) {
            return new TeamMatchView(this, false, fullTimeScore.away, fullTimeScore.home);
        }
        throw new IllegalArgumentException(team.name + " did not play in match " + externalId);
    }

    /** Played matches of a season before the given matchday number, e.g. for standings. */
    /**
     * Spec 06: a round of a knockout phase never counts towards a table — the Champions
     * League's league-phase table does not keep growing once the knockout rounds start.
     * A matchday carrying a round's name ({@code label}) is such a round.
     */
    public static boolean countsTowardsTable(Match match) {
        return match.matchday.label == null;
    }

    public static List<Match> findPlayedBefore(League league, int season, int matchday) {
        return list("matchday.league = ?1 and matchday.season = ?2 and matchday.number < ?3"
                + " and matchday.label is null and fullTimeScore is not null",
                league, season, matchday);
    }

    /** Spec 06: played matches before a matchday within one group — the basis of a group's own table. */
    public static List<Match> findPlayedBefore(League league, int season, String groupName, int matchday) {
        return list("matchday.league = ?1 and matchday.season = ?2 and matchday.groupName is not distinct from ?3"
                + " and matchday.number < ?4 and matchday.label is null and fullTimeScore is not null",
                league, season, groupName, matchday);
    }

    /** Unplayed matches of a season — the season outlook's remaining-fixtures input (spec 04). */
    public static List<Match> findUnplayed(League league, int season) {
        return list("matchday.league = ?1 and matchday.season = ?2 and fullTimeScore is null", league, season);
    }

    /** Played matches of a team in a season before the given matchday number, newest first. */
    public static List<Match> findPlayedByTeamBefore(Team team, League league, int season, int matchday) {
        return list("(homeTeam = ?1 or awayTeam = ?1) and matchday.league = ?2 and matchday.season = ?3"
                        + " and matchday.number < ?4 and fullTimeScore is not null order by kickoff desc",
                team, league, season, matchday);
    }

    /** All matches of a team in a season, played or not, in kickoff order — the club's schedule. */
    public static List<Match> findByTeam(Team team, League league, int season) {
        return list("(homeTeam = ?1 or awayTeam = ?1) and matchday.league = ?2 and matchday.season = ?3 order by kickoff",
                team, league, season);
    }

    /** Every league and season the team has matches in, newest season first — the club's history in the data. */
    public static List<LeagueSeason> findLeagueSeasonsOf(Team team) {
        return getEntityManager()
                .createQuery("select distinct new de.javamark.matchoracle.matchday.entity.LeagueSeason(m.matchday.league, m.matchday.season)"
                        + " from Match m where m.homeTeam = :team or m.awayTeam = :team order by m.matchday.season desc", LeagueSeason.class)
                .setParameter("team", team)
                .getResultList();
    }

    /** Played meetings of two teams in any league and season before the given kickoff, newest first. */
    public static List<Match> findPlayedBetweenBefore(Team a, Team b, Instant kickoff) {
        return list("((homeTeam = ?1 and awayTeam = ?2) or (homeTeam = ?2 and awayTeam = ?1))"
                        + " and kickoff < ?3 and fullTimeScore is not null order by kickoff desc",
                a, b, kickoff);
    }

    /** Whether the team had any match in that league and season — false for a newly promoted (or relegated) team. */
    public static boolean playedInLeagueSeason(Team team, League league, int season) {
        return count("(homeTeam = ?1 or awayTeam = ?1) and matchday.league = ?2 and matchday.season = ?3", team, league, season) > 0;
    }

    /** Played matches whose result is not yet FINAL — the candidates for finalization. */
    public static List<Match> findProvisionalWithResult() {
        return list("resultStatus = ?1 and fullTimeScore is not null", ResultStatus.PROVISIONAL);
    }
}
