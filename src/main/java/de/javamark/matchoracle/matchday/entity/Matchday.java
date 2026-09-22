package de.javamark.matchoracle.matchday.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * One round of matches of a competition in a season. The per-competition matchday
 * counter is explicit in the identity (league, season, group, number) — spec 06: in a
 * group competition every group counts its own matchdays. The uniqueness itself is
 * enforced by a unique index in the schema, because null groups have to compare equal.
 */
@Entity
public class Matchday extends PanacheEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public League league;

    /** Season by its starting year, e.g. 2025 for 2025/26. */
    @Column(nullable = false)
    public int season;

    @Column(nullable = false)
    public int number;

    /**
     * Spec 06: the parallel group this matchday belongs to ("Gruppe A"), null in every
     * competition that is not played in groups. Part of the matchday's identity.
     */
    @Column(length = 40)
    public String groupName;

    /** Spec 06: name of the round in a knockout competition ("Achtelfinale"), null elsewhere. */
    @Column(length = 40)
    public String label;

    /**
     * Which section of the external source this matchday came from — the unit the source
     * synchronises and reports changes for. Equal to {@link #number} everywhere except in a
     * group competition, where one section is a whole group and holds several matchdays.
     */
    @Column(nullable = false)
    public int sectionNumber;

    /** Last change reported by the external source; null if never synced. */
    public Instant sourceLastChangedAt;

    /** When this matchday was last successfully checked against the source — the age of the data. */
    public Instant lastCheckedAt;

    /** How this section of the competition is called: the round's name, or the counted matchday. */
    public String displayName() {
        return label != null ? label : number + ". Spieltag";
    }

    /** Short form for the competitions without groups. */
    public static Optional<Matchday> find(League league, int season, int number) {
        return find(league, season, null, number);
    }

    /** Every matchday that came from one section of the source — several only in a group competition. */
    public static List<Matchday> findSection(League league, int season, int sectionNumber) {
        return list("league = ?1 and season = ?2 and sectionNumber = ?3", league, season, sectionNumber);
    }

    public static Optional<Matchday> find(League league, int season, String groupName, int number) {
        return find("league = ?1 and season = ?2 and groupName is not distinct from ?3 and number = ?4",
                league, season, groupName, number).firstResultOptional();
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
     * or the last matchday if the season is complete. In a group competition the groups
     * run side by side and share the counting, so this is the number that applies to all
     * of them — see {@link #findAll} to get every group's matchday of that number.
     */
    public static Optional<Matchday> findCurrent(League league) {
        return currentNumber(league).flatMap(current -> find(league, current.season(), current.number()));
    }

    /** Season and matchday number the competition is currently at. */
    public static Optional<SeasonMatchday> currentNumber(League league) {
        return latestSeason(league).map(season -> {
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
            return new SeasonMatchday(season, number == null ? 1 : number);
        });
    }

    /** A season and a matchday number within it. */
    public record SeasonMatchday(int season, int number) {
    }

    /** Every group's matchday of that number, ordered by group; a single one outside a group competition. */
    public static List<Matchday> findAll(League league, int season, int number) {
        return list("league = ?1 and season = ?2 and number = ?3 order by groupName nulls first",
                league, season, number);
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
