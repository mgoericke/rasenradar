package de.javamark.matchoracle.forecast.boundary;

import de.javamark.matchoracle.forecast.boundary.ForecastPages.SlotSummary;
import de.javamark.matchoracle.forecast.boundary.ForecastPages.StreamCell;
import de.javamark.matchoracle.forecast.entity.Forecast;
import de.javamark.matchoracle.matchday.boundary.MatchdayFacade.MatchScore;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Spec 01, "Spieltags-Strom": the verdict on the KI-Vorschau per match and per kickoff block. */
class StreamVerdictTest {

    private static final Instant KICKOFF = Instant.parse("2026-10-10T13:30:00Z");

    @Test
    void beforeTheFinalWhistleThereIsNoVerdict() {
        StreamCell c = StreamCell.of(1, Optional.of(homeWin(1, 2, 0)), null);

        assertNull(c.hit());
        assertEquals("2:0", c.expectedScore());
    }

    @Test
    void theTendencyDecidesNotTheExactScore() {
        assertTrue(StreamCell.of(1, Optional.of(homeWin(1, 2, 0)), new MatchScore(1, 0, false)).hit());
        assertFalse(StreamCell.of(1, Optional.of(homeWin(1, 2, 0)), new MatchScore(2, 2, false)).hit());
    }

    @Test
    void exactOnlyWhenTheGuideScoreMatches() {
        assertTrue(StreamCell.of(1, Optional.of(homeWin(1, 2, 0)), new MatchScore(2, 0, false)).exact());
        assertFalse(StreamCell.of(1, Optional.of(homeWin(1, 2, 0)), new MatchScore(3, 0, false)).exact());
    }

    @Test
    void aProvisionalResultMakesTheVerdictProvisional() {
        assertTrue(StreamCell.of(1, Optional.of(homeWin(1, 2, 0)), new MatchScore(1, 0, true)).provisional());
    }

    @Test
    void aBlockCountsOnlyPlayedMatchesThatHadAForecast() {
        Forecast hit = homeWin(1, 1, 0), miss = homeWin(2, 1, 0), open = homeWin(3, 1, 0);
        List<StreamCell> cells = List.of(
                StreamCell.of(1, Optional.of(hit), new MatchScore(2, 1, false)),
                StreamCell.of(2, Optional.of(miss), new MatchScore(0, 0, true)),
                StreamCell.of(3, Optional.of(open), null),
                StreamCell.of(4, Optional.empty(), new MatchScore(1, 1, false)));

        SlotSummary s = SlotSummary.of(List.of(hit, miss, open), cells).getFirst();

        assertEquals(1, s.hits());
        assertEquals(2, s.judged());
        assertTrue(s.provisional());
    }

    private static Forecast homeWin(long matchId, int home, int away) {
        Forecast f = new Forecast();
        f.id = matchId;
        f.matchId = matchId;
        f.kickoff = KICKOFF;
        f.createdAt = KICKOFF.minus(Duration.ofHours(2));
        f.homeWin = 0.6;
        f.draw = 0.25;
        f.awayWin = 0.15;
        f.confidence = 0.6;
        f.expectedHomeGoals = home;
        f.expectedAwayGoals = away;
        return f;
    }
}
