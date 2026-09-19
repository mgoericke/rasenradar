package de.javamark.matchoracle.review.control;

import de.javamark.matchoracle.review.entity.ForecastEvaluation;
import de.javamark.matchoracle.review.entity.Outcome;
import de.javamark.matchoracle.review.entity.Situation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Spec 03, step 7: a rule-based, text-only recap of one completed matchday. */
class MatchdayRecapTest {

    @Test
    void countsTendencyAndExactHitsAndAverageConfidence() {
        ForecastEvaluation hitExact = evaluation(1, true, true, 0.8, 0.1);
        ForecastEvaluation hitTendencyOnly = evaluation(2, true, false, 0.6, 0.2);
        ForecastEvaluation miss = evaluation(3, false, false, 0.4, 0.3);

        String text = MatchdayRecap.render(List.of(hitExact, hitTendencyOnly, miss), Map.of());

        assertTrue(text.contains("2 von 3 Tendenzen getroffen"), text);
        assertTrue(text.contains("1 genaues Ergebnis"), text);
        assertTrue(text.contains("durchschnittliche Sicherheit 60 %"), text);
    }

    @Test
    void omitsExactResultsWhenThereWereNone() {
        String text = MatchdayRecap.render(List.of(evaluation(1, true, false, 0.6, 0.2)), Map.of());

        assertFalse(text.contains("genaues Ergebnis"), text);
        assertFalse(text.contains("genaue Ergebnisse"), text);
    }

    @Test
    void usesThePluralFromTwoExactResultsOn() {
        String text = MatchdayRecap.render(
                List.of(evaluation(1, true, true, 0.6, 0.1), evaluation(2, true, true, 0.6, 0.1)), Map.of());

        assertTrue(text.contains("2 genaue Ergebnisse"), text);
    }

    @Test
    void namesTheMatchWithTheHighestBrierScoreAsTheBiggestMiss() {
        ForecastEvaluation close = evaluation(1, true, false, 0.6, 0.1);
        ForecastEvaluation biggestMiss = evaluation(2, false, false, 0.7, 1.4);
        Map<Long, Situation> situations = Map.of(
                1L, namedSituation("A", "B", 1, 0),
                2L, namedSituation("Eintracht Frankfurt", "SC Freiburg", 0, 3));

        String text = MatchdayRecap.render(List.of(close, biggestMiss), situations);

        assertTrue(text.contains("Eintracht Frankfurt 0:3 SC Freiburg"), text);
    }

    private static ForecastEvaluation evaluation(long matchId, boolean tendencyHit, boolean scoreHit, double confidence, double brierScore) {
        ForecastEvaluation e = new ForecastEvaluation();
        e.matchId = matchId;
        e.tendencyHit = tendencyHit;
        e.scoreHit = scoreHit;
        e.confidence = confidence;
        e.brierScore = brierScore;
        e.predictedOutcome = Outcome.HOME_WIN;
        e.actualOutcome = tendencyHit ? Outcome.HOME_WIN : Outcome.AWAY_WIN;
        return e;
    }

    private static Situation namedSituation(String home, String away, int homeGoals, int awayGoals) {
        Situation s = new Situation();
        s.homeTeam = home;
        s.awayTeam = away;
        s.homeGoals = homeGoals;
        s.awayGoals = awayGoals;
        return s;
    }
}
