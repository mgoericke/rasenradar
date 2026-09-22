package de.javamark.matchoracle.forecast.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A committed forecast (spec 02, step 7). Written once, never updated or deleted —
 * there are deliberately no methods that modify a persisted forecast. A new
 * forecast for the same match is a new row.
 */
@Entity
public class Forecast extends PanacheEntity {

    @Column(nullable = false)
    public long matchId;

    @Column(nullable = false)
    public Instant createdAt;

    // snapshot of the match as it was forecast; the league as its shortcut (bl1/bl2) —
    // forecast must not depend on matchday's entities, only on its boundary
    @Column(nullable = false, length = 20)
    public String league;
    @Column(nullable = false)
    public int season;
    @Column(nullable = false)
    public int matchday;
    @Column(nullable = false, length = 100)
    public String homeTeam;
    @Column(nullable = false, length = 100)
    public String awayTeam;
    @Column(nullable = false)
    public Instant kickoff;

    // the forecast; homeWin + draw + awayWin == 1
    @Column(nullable = false)
    public double homeWin;
    @Column(nullable = false)
    public double draw;
    @Column(nullable = false)
    public double awayWin;
    @Column(nullable = false)
    public int expectedHomeGoals;
    @Column(nullable = false)
    public int expectedAwayGoals;
    /** 0..1 */
    @Column(nullable = false)
    public double confidence;
    @Column(nullable = false, columnDefinition = "text")
    public String reasoning;

    // outcome of the review
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public Verdict verdict;
    @Column(columnDefinition = "text")
    public String verdictReason;
    /** Whether the forecaster got one revision round. */
    @Column(nullable = false)
    public boolean revised;
    /** Spec 02, step 6: after the single revision the reviewer still objected. */
    @Column(nullable = false)
    public boolean objectionRemains;

    /**
     * Spec 05, "Gegen den Strom": whether the tendency diverged from the statistical baseline's
     * tendency at commit time. Null for forecasts committed before this existed — the baseline
     * used back then is not reconstructible, so no marker is shown for them.
     */
    public Boolean contrarian;

    // the scales in effect for this forecast
    @Column(nullable = false)
    public double homeAdvantage;
    @Column(nullable = false)
    public int formMatches;
    @Column(nullable = false)
    public double promotedTeamMalus;
    @Column(length = 100)
    public String modelName;

    /** A backtest: made after the match was played, with the facts as of before kickoff. Reported separately. */
    @Column(nullable = false)
    public boolean backtest;

    /** Spec 03, step 5: the retrospective the forecaster saw; null when the data was too thin. */
    @Column(columnDefinition = "text")
    public String retrospective;

    @OneToMany(mappedBy = "forecast", cascade = CascadeType.ALL)
    @OrderBy("kind")
    public List<Assessment> assessments = new ArrayList<>();

    public Outcome tendency() {
        if (homeWin >= draw && homeWin >= awayWin) return Outcome.HOME_WIN;
        if (awayWin >= draw) return Outcome.AWAY_WIN;
        return Outcome.DRAW;
    }

    public Optional<Assessment> assessment(AssessmentKind kind) {
        return assessments.stream().filter(a -> a.kind == kind).findFirst();
    }

    /** Spec 02, rules: made without a single working assessment — kept, but worth a fresh attempt soon. */
    public boolean withoutAnyAssessment() {
        return assessments.stream().allMatch(a -> a.failed);
    }

    /** All forecasts of a match, newest first. */
    public static List<Forecast> findByMatch(long matchId) {
        return list("matchId = ?1 order by createdAt desc", matchId);
    }

    public static Optional<Forecast> findLatestByMatch(long matchId) {
        return find("matchId = ?1 order by createdAt desc", matchId).firstResultOptional();
    }

    /** Latest forecast per match of a matchday, for the overview. */
    public static List<Forecast> findByMatchday(String league, int season, int matchday) {
        return list("league = ?1 and season = ?2 and matchday = ?3 order by kickoff, createdAt desc", league, season, matchday);
    }
}
