package de.javamark.matchoracle.matchday.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import java.util.Optional;

/**
 * A team keeps its identity across seasons, promotion and relegation via the
 * source's stable external id. Name and short name may change over time.
 */
@Entity
public class Team extends PanacheEntity {

    @Column(nullable = false, unique = true)
    public int externalId;

    @Column(nullable = false, length = 100)
    public String name;

    @Column(length = 30)
    public String shortName;

    /** Club crest, an external URL from the source; may be null. */
    @Column(length = 500)
    public String iconUrl;

    public static Optional<Team> findByExternalId(int externalId) {
        return find("externalId", externalId).firstResultOptional();
    }
}
