package de.javamark.matchoracle.review.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import java.time.Instant;
import java.util.List;

/** A forecast as handed over when it was committed; the review keeps its own copy. */
@Entity
public class RecordedForecast extends PanacheEntity {

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
    public Instant createdAt;
    @Column(nullable = false)
    public double homeWin;
    @Column(nullable = false)
    public double draw;
    @Column(nullable = false)
    public double awayWin;
    @Column(nullable = false)
    public double confidence;
    /** The forecast's own guide value for the final score — a Volltreffer if the match ends exactly like this. */
    @Column(nullable = false)
    public int expectedHomeGoals;
    @Column(nullable = false)
    public int expectedAwayGoals;
    /** Made after the match was played (backtest) — kept apart from live forecasts. */
    @Column(nullable = false)
    public boolean backtest;

    public Outcome predictedOutcome() {
        if (homeWin >= draw && homeWin >= awayWin) return Outcome.HOME_WIN;
        if (awayWin >= draw) return Outcome.AWAY_WIN;
        return Outcome.DRAW;
    }

    public double probabilityOf(Outcome outcome) {
        return switch (outcome) {
            case HOME_WIN -> homeWin;
            case DRAW -> draw;
            case AWAY_WIN -> awayWin;
        };
    }

    public static boolean exists(long forecastId) {
        return count("forecastId", forecastId) > 0;
    }

    public static List<RecordedForecast> findByMatch(long matchId) {
        return list("matchId", matchId);
    }
}
