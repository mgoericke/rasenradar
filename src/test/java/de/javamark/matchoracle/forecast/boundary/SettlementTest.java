package de.javamark.matchoracle.forecast.boundary;

import de.javamark.matchoracle.forecast.boundary.ForecastPages.MarkerView;
import de.javamark.matchoracle.forecast.boundary.ForecastPages.Settlement;
import de.javamark.matchoracle.forecast.entity.Assessment;
import de.javamark.matchoracle.forecast.entity.AssessmentKind;
import de.javamark.matchoracle.forecast.entity.Forecast;
import de.javamark.matchoracle.forecast.entity.Outcome;
import de.javamark.matchoracle.matchday.boundary.MatchdayFacade.MatchScore;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Story r012: a forecast is settled against the FINAL score only — in the list marker and in "Abgerechnet". */
class SettlementTest {

    @Test
    void onlyAFinalScoreSettlesTheMarker() {
        Forecast f = forecast(Outcome.HOME_WIN, Outcome.DRAW, Outcome.AWAY_WIN);

        assertNull(MarkerView.of(f, null).hit());
        assertNull(MarkerView.of(f, new MatchScore(2, 0, true)).hit());
        assertTrue(MarkerView.of(f, new MatchScore(2, 0, false)).hit());
        assertFalse(MarkerView.of(f, new MatchScore(0, 1, false)).hit());
    }

    @Test
    void namesTheAssessorsThatLeanedTheRightWay() {
        Forecast f = forecast(Outcome.AWAY_WIN, Outcome.DRAW, Outcome.AWAY_WIN);

        Settlement s = Settlement.of(f, new MatchScore(0, 1, false));

        assertFalse(s.hit());
        assertFalse(s.exact());
        assertEquals("1:0", s.expectedScore());
        assertEquals("Recht hatten: Form und Umfeld.", s.assessorLine());
    }

    @Test
    void oneNoneOrAllThree() {
        assertEquals("Recht hatte: Direkte Duelle.",
                Settlement.of(forecast(Outcome.AWAY_WIN, Outcome.HOME_WIN, Outcome.DRAW), new MatchScore(1, 0, false)).assessorLine());
        assertEquals("Keiner der drei Bewerter lag richtig.",
                Settlement.of(forecast(Outcome.AWAY_WIN, Outcome.DRAW, Outcome.DRAW), new MatchScore(1, 0, false)).assessorLine());
        assertEquals("Recht hatten: Form, Direkte Duelle und Umfeld.",
                Settlement.of(forecast(Outcome.HOME_WIN, Outcome.HOME_WIN, Outcome.HOME_WIN), new MatchScore(1, 0, false)).assessorLine());
    }

    @Test
    void aFailedAssessorIsNeverRightAndAProvisionalScoreSettlesNothing() {
        Forecast f = forecast(Outcome.HOME_WIN, Outcome.HOME_WIN, Outcome.HOME_WIN);
        f.assessments.getFirst().failed = true;

        assertEquals("Recht hatten: Direkte Duelle und Umfeld.", Settlement.of(f, new MatchScore(1, 0, false)).assessorLine());
        assertTrue(Settlement.of(f, new MatchScore(1, 0, false)).exact());
        assertNull(Settlement.of(f, new MatchScore(1, 0, true)));
    }

    /** A 1:0 home win forecast with the leans of form, head-to-head and context. */
    private static Forecast forecast(Outcome form, Outcome headToHead, Outcome context) {
        Forecast f = new Forecast();
        f.id = 1L;
        f.matchId = 1;
        f.kickoff = Instant.parse("2026-10-10T13:30:00Z");
        f.createdAt = f.kickoff.minusSeconds(7200);
        f.homeWin = 0.5;
        f.draw = 0.3;
        f.awayWin = 0.2;
        f.confidence = 0.5;
        f.expectedHomeGoals = 1;
        f.expectedAwayGoals = 0;
        f.assessments.add(assessment(AssessmentKind.FORM, form));
        f.assessments.add(assessment(AssessmentKind.HEAD_TO_HEAD, headToHead));
        f.assessments.add(assessment(AssessmentKind.CONTEXT, context));
        return f;
    }

    private static Assessment assessment(AssessmentKind kind, Outcome lean) {
        Assessment a = new Assessment();
        a.kind = kind;
        a.lean = lean;
        return a;
    }
}
