package de.javamark.matchoracle.matchday.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import java.util.List;
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

    /** Club crest as provided by the source — usually a URL, but at least one is an inline base64 data: URI; may be null. */
    @Column(columnDefinition = "text")
    public String iconUrl;

    /** File name of the crest the application serves itself (content hash plus extension); null until one is stored. */
    @Column(length = 40)
    public String crestFile;

    public static Optional<Team> findByExternalId(int externalId) {
        return find("externalId", externalId).firstResultOptional();
    }

    /** The clubs with matches in a league and season, by name. */
    public static List<Team> findInLeagueSeason(League league, int season) {
        return getEntityManager()
                .createQuery("select distinct t from Match m join m.homeTeam t"
                        + " where m.matchday.league = :league and m.matchday.season = :season order by t.name", Team.class)
                .setParameter("league", league)
                .setParameter("season", season)
                .getResultList();
    }
}
