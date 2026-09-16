package de.javamark.matchoracle.review.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Spec 03, steps 1-2: how a forecast compared to the final result. Written once, never changed. */
@Entity
public class ForecastEvaluation extends PanacheEntity {

    @Column(nullable = false, unique = true)
    public long forecastId;
    @Column(nullable = false)
    public long matchId;
    @Column(nullable = false, length = 20)
    public String league;
    @Column(nullable = false)
    public int season;
    @Column(nullable = false)
    public int matchday;
    @Column(nullable = false)
    public Instant evaluatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public Outcome predictedOutcome;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public Outcome actualOutcome;
    @Column(nullable = false)
    public boolean tendencyHit;
    /** Whether the forecast's expected score (a guide value, not a formal prediction) matched exactly — a Volltreffer. */
    @Column(nullable = false)
    public boolean scoreHit;
    /** The probability the forecast gave the outcome that actually happened. */
    @Column(nullable = false)
    public double probabilityOfActual;
    /** Brier score over the three outcomes, 0 (perfect) to 2 (worst). */
    @Column(nullable = false)
    public double brierScore;
    @Column(nullable = false)
    public double confidence;
    /** Made after the match was played (backtest) — kept apart from live forecasts. */
    @Column(nullable = false)
    public boolean backtest;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public ConfidenceVerdict confidenceVerdict;

    public static boolean exists(long forecastId) {
        return count("forecastId", forecastId) > 0;
    }

    public static Optional<ForecastEvaluation> findByForecast(long forecastId) {
        return find("forecastId", forecastId).firstResultOptional();
    }

    public static List<ForecastEvaluation> findByLeague(String league, boolean backtest) {
        return list("league = ?1 and backtest = ?2 order by season, matchday, evaluatedAt", league, backtest);
    }

    public static List<ForecastEvaluation> findByMatch(long matchId) {
        return list("matchId", matchId);
    }
}
