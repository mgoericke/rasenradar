package de.javamark.matchoracle.review.control;

import de.javamark.matchoracle.review.entity.AccuracyReport;
import de.javamark.matchoracle.review.entity.ContrarianReport;
import de.javamark.matchoracle.review.entity.ForecastEvaluation;
import de.javamark.matchoracle.review.entity.Outcome;
import de.javamark.matchoracle.review.entity.RecordedForecast;
import de.javamark.matchoracle.review.entity.Situation;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Spec 03, rules: too few cases yield explicitly nothing instead of a thin number. */
class ReviewServiceTest {

    @Test
    void hitRateIsWithheldBelowTheRequiredNumberOfEvaluations() {
        AccuracyReport report = ReviewService.accuracy("bl1", false, evaluations(9, 6), Map.of(), 10);

        assertFalse(report.reliable());
        assertNull(report.hitRate());
        assertEquals(9, report.evaluated());
        assertEquals(6, report.hits());
    }

    @Test
    void hitRateIsReportedOnceEnoughEvaluationsExist() {
        AccuracyReport report = ReviewService.accuracy("bl1", false, evaluations(10, 6), Map.of(), 10);

        assertTrue(report.reliable());
        assertEquals(0.6, report.hitRate(), 1e-9);
        assertEquals(1, report.perMatchday().size());
        assertEquals(10, report.perMatchday().get(0).evaluated());
    }

    /** Spec 03, step 7: each matchday point carries its own rule-based recap text. */
    @Test
    void perMatchdayPointsIncludeARuleBasedRecap() {
        AccuracyReport report = ReviewService.accuracy("bl1", false, evaluations(10, 6), Map.of(), 10);

        assertTrue(report.perMatchday().get(0).recap().contains("6 von 10 Tendenzen getroffen"));
    }

    /** Spec 03: the oracle is measured against the baseline forecast on the same matches — a skill score of 0 means no better than plain statistics. */
    @Test
    void comparesTheOracleWithTheBaselineOnTheSameMatches() {
        List<ForecastEvaluation> evaluations = evaluations(10, 6);
        Map<Long, Situation> situations = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            evaluations.get(i).matchId = i;
            // the baseline tips home in every match; oracle hit 6, baseline hits whenever the result was a home win
            situations.put((long) i, situation(evaluations.get(i).actualOutcome, 0.5, 0.3, 0.2));
        }

        AccuracyReport report = ReviewService.accuracy("bl1", false, evaluations, situations, 10);

        assertEquals(10, report.baseline().evaluated());
        assertEquals(6, report.baseline().hits());
        assertEquals(0.6, report.baseline().hitRate(), 1e-9);
        // baseline Brier: 6 home wins → 0.25+0.09+0.04 = 0.38; 4 draws → 0.25+0.49+0.04 = 0.78; mean 0.54
        assertEquals(0.54, report.baseline().averageBrier(), 1e-9);
        assertEquals(1 - 0.3 / 0.54, report.skillScore(), 1e-9);
    }

    @Test
    void matchesWithoutABaselineAreLeftOutOfTheComparison() {
        List<ForecastEvaluation> evaluations = evaluations(10, 6);
        Map<Long, Situation> situations = new HashMap<>();
        evaluations.get(0).matchId = 1;
        situations.put(1L, situation(evaluations.get(0).actualOutcome, 0.5, 0.3, 0.2));

        AccuracyReport report = ReviewService.accuracy("bl1", false, evaluations, situations, 10);

        assertEquals(1, report.baseline().evaluated());
        assertNull(report.baseline().hitRate()); // below the required number, nothing instead of a thin number
        assertNull(report.skillScore());
    }

    /** Spec 03: a simple hit/miss timeline of the most recent evaluations — a second, plainer view than the hit rate. */
    @Test
    void recentResultsAreTheLastEvaluationsChronologicallyWithLabelsFromTheSituationWhereKnown() {
        List<ForecastEvaluation> evaluations = evaluations(15, 9); // indices 0-8 hits, 9-14 misses (ascending, chronological)
        for (int i = 0; i < evaluations.size(); i++) {
            evaluations.get(i).matchId = i;
        }
        Map<Long, Situation> situations = new HashMap<>();
        situations.put(5L, namedSituation("Bayern", "Leipzig"));
        situations.put(10L, namedSituation("Dortmund", "Bremen"));

        AccuracyReport report = ReviewService.accuracy("bl1", false, evaluations, situations, 10);

        assertEquals(12, report.recent().size()); // capped, most recent 12 of 15
        assertEquals(AccuracyReport.HitLevel.TENDENCY, report.recent().get(0).level());  // index 3: a hit, no exact-score data set
        assertEquals(AccuracyReport.HitLevel.MISS, report.recent().get(6).level()); // index 9: first miss
        assertEquals("Bayern – Leipzig", report.recent().get(2).label()); // index 5
        assertEquals("Dortmund – Bremen", report.recent().get(7).label()); // index 10
        assertEquals("3. Sp. 26/27", report.recent().get(0).label()); // index 3, no situation known
    }

    /** Spec 03: an exact score match (Volltreffer) is a stronger signal than a bare tendency hit. */
    @Test
    void anExactScoreHitIsDistinguishedFromAPlainTendencyHit() {
        List<ForecastEvaluation> evaluations = evaluations(3, 2); // index 0,1 hits, index 2 miss
        evaluations.get(0).scoreHit = true; // the exact score also matched
        for (int i = 0; i < evaluations.size(); i++) {
            evaluations.get(i).matchId = i;
        }

        AccuracyReport report = ReviewService.accuracy("bl1", false, evaluations, Map.of(), 10);

        assertEquals(AccuracyReport.HitLevel.EXACT, report.recent().get(0).level());
        assertEquals(AccuracyReport.HitLevel.TENDENCY, report.recent().get(1).level());
        assertEquals(AccuracyReport.HitLevel.MISS, report.recent().get(2).level());
    }

    /** Spec 03: is the stated confidence honest? Three broad groups, each judged like the overall hit rate. */
    @Test
    void calibrationGroupsConfidenceIntoThreeBandsWithTheirOwnReliabilityThreshold() {
        List<ForecastEvaluation> evaluations = new ArrayList<>();
        evaluations.addAll(withConfidence(0.3, 10, 5));  // niedrig: 5/10 = 0.5, reliable
        evaluations.addAll(withConfidence(0.6, 10, 7));  // mittel: 7/10 = 0.7, reliable
        evaluations.addAll(withConfidence(0.9, 5, 5));   // hoch: 5/5, below the required 10 -> unreliable

        List<AccuracyReport.CalibrationGroup> groups = ReviewService.calibration(evaluations, 10);

        assertEquals(3, groups.size());
        assertEquals(10, groups.get(0).comparison().evaluated());
        assertEquals(0.5, groups.get(0).comparison().hitRate(), 1e-9);
        assertEquals(10, groups.get(1).comparison().evaluated());
        assertEquals(0.7, groups.get(1).comparison().hitRate(), 1e-9);
        assertEquals(5, groups.get(2).comparison().evaluated());
        assertNull(groups.get(2).comparison().hitRate()); // too few cases in this group
    }

    private static List<ForecastEvaluation> withConfidence(double confidence, int count, int hits) {
        List<ForecastEvaluation> list = evaluations(count, hits);
        list.forEach(e -> e.confidence = confidence);
        return list;
    }

    /** Spec 02, rule: repeated automatic forecasts before kickoff — only the last live one counts. */
    @Test
    void onlyTheLastLiveForecastBeforeKickoffIsEvaluated() {
        RecordedForecast first = recordedForecast(false, Instant.parse("2026-01-01T10:00:00Z"));
        RecordedForecast second = recordedForecast(false, Instant.parse("2026-01-02T10:00:00Z"));
        RecordedForecast backtest = recordedForecast(true, Instant.parse("2026-01-01T09:00:00Z"));

        List<RecordedForecast> toEvaluate = ReviewService.forecastsToEvaluate(List.of(first, second, backtest));

        assertEquals(List.of(second, backtest), toEvaluate);
    }

    @Test
    void aSingleLiveForecastIsStillEvaluated() {
        RecordedForecast only = recordedForecast(false, Instant.parse("2026-01-01T10:00:00Z"));

        assertEquals(List.of(only), ReviewService.forecastsToEvaluate(List.of(only)));
    }

    private static RecordedForecast recordedForecast(boolean backtest, Instant createdAt) {
        RecordedForecast r = new RecordedForecast();
        r.backtest = backtest;
        r.createdAt = createdAt;
        return r;
    }

    private static Situation namedSituation(String home, String away) {
        Situation s = new Situation();
        s.homeTeam = home;
        s.awayTeam = away;
        return s;
    }

    private static Situation situation(Outcome actual, double home, double draw, double away) {
        Situation s = new Situation();
        s.homeGoals = actual == Outcome.HOME_WIN ? 1 : 0;
        s.awayGoals = actual == Outcome.AWAY_WIN ? 1 : 0;
        s.baselineHomeWin = home;
        s.baselineDraw = draw;
        s.baselineAwayWin = away;
        return s;
    }

    /** Spec 05, "Mut-Bilanz": the contrarian hit rate is counted separately, with its own reliability threshold. */
    @Test
    void contrarianHitRateIsCountedAndGatedSeparately() {
        List<ForecastEvaluation> evaluations = new ArrayList<>();
        evaluations.addAll(contrarianEvaluations(3, 2));
        evaluations.addAll(evaluations(10, 6));

        ContrarianReport thin = ReviewService.contrarianReport(evaluations, 10);
        assertFalse(thin.reliable());
        assertEquals(3, thin.evaluated());
        assertEquals(2, thin.hits());

        ContrarianReport reliable = ReviewService.contrarianReport(evaluations, 3);
        assertTrue(reliable.reliable());
        assertEquals(2.0 / 3, reliable.hitRate(), 1e-9);
    }

    private static List<ForecastEvaluation> contrarianEvaluations(int count, int hits) {
        List<ForecastEvaluation> list = evaluations(count, hits);
        list.forEach(e -> e.contrarian = true);
        return list;
    }

    private static List<ForecastEvaluation> evaluations(int count, int hits) {
        List<ForecastEvaluation> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ForecastEvaluation e = new ForecastEvaluation();
            e.season = 2026;
            e.matchday = 3;
            e.tendencyHit = i < hits;
            e.predictedOutcome = Outcome.HOME_WIN;
            e.actualOutcome = e.tendencyHit ? Outcome.HOME_WIN : Outcome.DRAW;
            e.confidence = 0.6;
            e.brierScore = 0.3;
            list.add(e);
        }
        return list;
    }
}
