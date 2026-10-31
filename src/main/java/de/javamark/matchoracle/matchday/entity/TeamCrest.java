package de.javamark.matchoracle.matchday.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * A club crest downloaded once from the source's URL, so that pages never make a visitor's
 * browser contact the crest hosts. A row without data records a failed attempt; it is retried
 * after {@link #RETRY_AFTER}.
 */
@Entity
public class TeamCrest extends PanacheEntityBase {

    public static final Duration RETRY_AFTER = Duration.ofDays(1);

    @Id
    public Long teamId;

    @Column(nullable = false, columnDefinition = "text")
    public String sourceUrl;

    @Column(length = 40)
    public String contentType;

    public byte[] data;

    @Column(nullable = false)
    public Instant fetchedAt;

    /**
     * Teams whose crest is still to be fetched: none stored for the current source URL, and no
     * attempt within {@link #RETRY_AFTER}. A failed attempt keeps an earlier crest in place.
     */
    public static List<Team> teamsToFetch(Instant now) {
        return Team.find("""
                select t from Team t where t.iconUrl is not null and t.iconUrl <> '' and not exists (
                    select 1 from TeamCrest c where c.teamId = t.id
                        and ((c.sourceUrl = t.iconUrl and c.data is not null) or c.fetchedAt > ?1))""",
                now.minus(RETRY_AFTER)).list();
    }
}
