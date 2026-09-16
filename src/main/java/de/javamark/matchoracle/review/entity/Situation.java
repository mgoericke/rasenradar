package de.javamark.matchoracle.review.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Spec 03, step 3: the constellation before a completed match, reduced to the
 * features that make two situations comparable, plus the actual outcome.
 */
@Entity
public class Situation extends PanacheEntity {

    @Column(nullable = false, unique = true)
    public long matchId;
    @Column(nullable = false, length = 20)
    public String league;
    @Column(nullable = false)
    public int season;
    @Column(nullable = false)
    public int matchday;
    @Column(nullable = false)
    public Instant kickoff;
    @Column(nullable = false, length = 100)
    public String homeTeam;
    @Column(nullable = false, length = 100)
    public String awayTeam;

    /** Home position minus away position; null before the first matchday. */
    public Integer positionGap;
    @Column(nullable = false)
    public int homeFormPoints;
    @Column(nullable = false)
    public int awayFormPoints;
    /** How many matches the form points cover (the form window in effect). */
    @Column(nullable = false)
    public int formMatches;
    @Column(nullable = false)
    public double homeHomePpg;
    @Column(nullable = false)
    public double awayAwayPpg;
    @Column(nullable = false)
    public boolean homePromoted;
    @Column(nullable = false)
    public boolean awayPromoted;

    /** The baseline forecast for this constellation; null until derived for situations recorded before it existed. */
    public Double baselineHomeWin;
    public Double baselineDraw;
    public Double baselineAwayWin;

    @Column(nullable = false)
    public int homeGoals;
    @Column(nullable = false)
    public int awayGoals;

    public Outcome outcome() {
        return Outcome.of(homeGoals, awayGoals);
    }

    public Optional<Baseline> baseline() {
        return baselineHomeWin == null ? Optional.empty() : Optional.of(new Baseline(baselineHomeWin, baselineDraw, baselineAwayWin));
    }

    public static Optional<Situation> findByMatch(long matchId) {
        return find("matchId", matchId).firstResultOptional();
    }

    public static List<Situation> findByMatchIds(Collection<Long> matchIds) {
        return matchIds.isEmpty() ? List.of() : list("matchId in ?1", matchIds);
    }

    /** Situations recorded before the baseline existed — derived in batches by the catch-up. */
    public static List<Situation> findWithoutBaseline(int limit) {
        return find("baselineHomeWin is null").page(0, limit).list();
    }

    public static List<Long> knownMatchIds() {
        return getEntityManager().createQuery("select s.matchId from Situation s", Long.class).getResultList();
    }

    /** Situations of matches kicked off before the given instant — the candidates for similar cases (no look into the future). */
    public static List<Situation> findBefore(Instant kickoff) {
        return list("kickoff < ?1", kickoff);
    }
}
