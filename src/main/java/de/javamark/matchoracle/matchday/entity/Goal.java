package de.javamark.matchoracle.matchday.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

@Entity
public class Goal extends PanacheEntity {

    @ManyToOne(optional = false)
    public Match match;

    /** Order within the match */
    @Column(nullable = false)
    public int position;

    /** Null if the source does not report the minute. */
    public Integer minute;

    @Column(length = 100)
    public String scorerName;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "home", column = @Column(name = "score_after_home")),
            @AttributeOverride(name = "away", column = @Column(name = "score_after_away"))
    })
    public Score scoreAfter;

    @Column(nullable = false)
    public boolean penalty;
    @Column(nullable = false)
    public boolean ownGoal;

}
