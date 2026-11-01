package de.javamark.matchoracle.season.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Story r039: the median final points total at one table position, from the same simulation run
 * as {@link SeasonOutlook} — replaced wholesale with it, no history kept.
 */
@Entity
public class SeasonThreshold extends PanacheEntity {

    @Column(nullable = false, length = 20)
    public String league;

    @Column(nullable = false)
    public int season;

    /** Counted from 1. */
    @Column(nullable = false)
    public int position;

    @Column(nullable = false)
    public int points;

    /** Position to its median points; empty before the first run. */
    public static Map<Integer, Integer> byPosition(String league, int season) {
        return SeasonThreshold.<SeasonThreshold>list("league = ?1 and season = ?2", league, season).stream()
                .collect(Collectors.toMap(t -> t.position, t -> t.points));
    }

    public static void deleteByLeagueSeason(String league, int season) {
        delete("league = ?1 and season = ?2", league, season);
    }
}
