package de.javamark.matchoracle.matchday.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;

/**
 * Spec 06: which division a club played in during a season. Derived from the team lists of
 * the first three divisions — a club in none of them counts as {@link Tier#LOWER}. This is
 * what turns a cup result line into a story: without it "Illertissen 9:8 n. E. Nürnberg"
 * says almost nothing.
 */
@Entity
public class TeamTier extends PanacheEntity {

    @ManyToOne(optional = false)
    public Team team;

    @Column(nullable = false)
    public int season;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    public Tier tier;

    /** The division of a team in a season; LOWER when the team is in none of the three lists. */
    public static Tier of(Team team, int season) {
        return find("team = ?1 and season = ?2", team, season)
                .<TeamTier>firstResultOptional().map(t -> t.tier).orElse(Tier.LOWER);
    }

    /** Spec 06: the lower-ranked side winning is the surprise. */
    public static boolean isUpset(Tier winner, Tier loser) {
        return winner.level() > loser.level();
    }

    /** How clear the surprise was — the distance between the two divisions, 0 when there is none. */
    public static int gap(Tier winner, Tier loser) {
        return Math.max(0, winner.level() - loser.level());
    }
}
