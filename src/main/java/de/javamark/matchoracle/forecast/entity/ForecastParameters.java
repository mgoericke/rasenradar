package de.javamark.matchoracle.forecast.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * The assessment scales (spec 02, rules): changeable at runtime without a
 * deployment, in effect for the next forecast. A single row with id 1.
 */
@Entity
public class ForecastParameters extends PanacheEntityBase {

    public static final long SINGLETON_ID = 1L;

    @Id
    public long id = SINGLETON_ID;

    /** Probability bonus for the home side, e.g. 0.10 = ten percentage points. */
    @Column(nullable = false)
    public double homeAdvantage;

    /** How many recent matches of the current season count as form. */
    @Column(nullable = false)
    public int formMatches;

    /** Probability malus for a newly promoted team, e.g. 0.05 = five percentage points. */
    @Column(nullable = false)
    public double promotedTeamMalus;

    public static ForecastParameters current() {
        return findById(SINGLETON_ID);
    }
}
