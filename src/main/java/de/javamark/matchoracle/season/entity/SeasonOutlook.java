package de.javamark.matchoracle.season.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A team's current season outlook (spec 04) — replaced wholesale on every recomputation,
 * never updated in place, no history kept.
 */
@Entity
public class SeasonOutlook extends PanacheEntity {

    @Column(nullable = false, length = 20)
    public String league;

    @Column(nullable = false)
    public int season;

    @Column(nullable = false)
    public long teamId;

    @Column(nullable = false)
    public Instant computedAt;

    /** Placement-goal label (e.g. "Meisterschaft") to its probability, 0..1. */
    @ElementCollection
    @CollectionTable(name = "season_outlook_probability", joinColumns = @JoinColumn(name = "season_outlook_id"))
    @MapKeyColumn(name = "placement_goal")
    @Column(name = "probability", nullable = false)
    public Map<String, Double> probabilities = new HashMap<>();

    public static List<SeasonOutlook> findByLeagueSeason(String league, int season) {
        return list("league = ?1 and season = ?2", league, season);
    }

    public static Optional<SeasonOutlook> findByTeam(String league, int season, long teamId) {
        return find("league = ?1 and season = ?2 and teamId = ?3", league, season, teamId).firstResultOptional();
    }

    public static void deleteByLeagueSeason(String league, int season) {
        delete("league = ?1 and season = ?2", league, season);
    }
}
