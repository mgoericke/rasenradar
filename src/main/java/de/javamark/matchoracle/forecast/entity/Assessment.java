package de.javamark.matchoracle.forecast.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;

/** One assessor's view of a match (spec 02, step 3), stored with the forecast and never changed. */
@Entity
public class Assessment extends PanacheEntity {

    @ManyToOne(optional = false)
    public Forecast forecast;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public AssessmentKind kind;

    /** Which outcome this assessor leans towards; null if the assessor failed or sees no lean. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    public Outcome lean;

    /** 0..1; null if the assessor failed. */
    public Double confidence;

    @Column(nullable = false, columnDefinition = "text")
    public String summary;

    /** Spec 02, rules: a failed assessment is reported, the forecast is still made. */
    @Column(nullable = false)
    public boolean failed;
}
