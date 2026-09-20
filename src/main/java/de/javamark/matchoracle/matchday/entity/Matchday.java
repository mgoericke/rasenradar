package de.javamark.matchoracle.matchday.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * One round of matches of a league in a season. The unique constraint on
 * (league, season, number) makes the per-league matchday counter explicit.
 * It is also the unit of synchronisation with the external source.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"league", "season", "number"}))
public class Matchday extends PanacheEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public League league;

    /** Season by its starting year, e.g. 2025 for 2025/26. */
    @Column(nullable = false)
    public int season;

    @Column(nullable = false)
    public int number;

    /** Last change reported by the external source; null if never synced. */
    public Instant sourceLastChangedAt;

    /** When this matchday was last successfully checked against the source — the age of the data. */
    public Instant lastCheckedAt;

    public static Optional<Matchday> find(League league, int season, int number) {
        return find("league = ?1 and season = ?2 and number = ?3", league, season, number)
                .firstResultOptional();
    }

    /** Latest known season of a league. */
    public static Optional<Integer> latestSeason(League league) {
        Integer season = getEntityManager()
                .createQuery("select max(m.season) from Matchday m where m.league = :league", Integer.class)
                .setParameter("league", league)
                .getSingleResult();
        return Optional.ofNullable(season);
    }

    /**
     * The current matchday: the first one of the latest season with an unplayed match,
     * or the last matchday if the season is complete.
     */
    public static Optional<Matchday> findCurrent(League league) {
        return latestSeason(league).flatMap(season -> {
            Integer number = getEntityManager()
                    .createQuery("select min(m.matchday.number) from Match m"
                            + " where m.matchday.league = :league and m.matchday.season = :season and m.fullTimeScore is null", Integer.class)
                    .setParameter("league", league).setParameter("season", season)
                    .getSingleResult();
            if (number == null) {
                number = getEntityManager()
                        .createQuery("select max(m.number) from Matchday m where m.league = :league and m.season = :season", Integer.class)
                        .setParameter("league", league).setParameter("season", season)
                        .getSingleResult();
            }
            return find(league, season, number);
        });
    }

    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Europe/Berlin");

    /**
     * The matchday a viewer should see by default. Like {@link #findCurrent}, except a matchday
     * that just finished keeps being shown through the rest of its last match's day — the next
     * one only takes over the day after, so a viewer isn't dropped onto an empty upcoming
     * matchday the instant the final whistle blows (spec 01, "aktueller Spieltag").
     */
    public static Optional<Matchday> findDisplayed(League league, Instant now) {
        return findCurrent(league).map(current -> {
            if (current.number <= 1 || isFullyPlayed(current)) {
                return current;
            }
            return find(league, current.season, current.number - 1)
                    .filter(Matchday::isFullyPlayed)
                    .filter(previous -> stillShowing(lastKickoff(previous), now))
                    .orElse(current);
        });
    }

    private static boolean isFullyPlayed(Matchday matchday) {
        List<Match> matches = Match.findByMatchday(matchday);
        return !matches.isEmpty() && matches.stream().allMatch(Match::isPlayed);
    }

    private static Instant lastKickoff(Matchday matchday) {
        return Match.findByMatchday(matchday).stream().map(m -> m.kickoff).max(Instant::compareTo).orElseThrow();
    }

    /** True through the calendar day (Europe/Berlin) of a finished matchday's last kickoff, false from the day after. */
    static boolean stillShowing(Instant lastKickoffOfPreviousMatchday, Instant now) {
        return !now.atZone(DISPLAY_ZONE).toLocalDate().isAfter(lastKickoffOfPreviousMatchday.atZone(DISPLAY_ZONE).toLocalDate());
    }
}
