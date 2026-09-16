package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.ResultStatus;
import de.javamark.matchoracle.matchday.entity.Score;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Plain unit test: no Quarkus, no database — the rule gets "now" passed in. */
class ResultFinalizerTest {

    private static final Instant KICKOFF = Instant.parse("2025-09-13T13:30:00Z");
    private static final Duration FINAL_AFTER = Duration.ofHours(24);

    private ResultFinalizer finalizer;

    @BeforeEach
    void setUp() {
        finalizer = new ResultFinalizer();
        finalizer.finalAfter = FINAL_AFTER;
    }

    @Test
    void becomesFinalAfterPeriodWithoutChange() {
        // the result itself is entered ~2h after kickoff; the clock runs from that change
        Instant resultEntered = KICKOFF.plus(Duration.ofHours(2));
        Match match = playedMatch(KICKOFF, resultEntered);

        assertTrue(finalizer.isDueForFinal(match, resultEntered.plus(FINAL_AFTER)));
    }

    @Test
    void staysProvisionalBeforePeriodHasPassed() {
        Instant resultEntered = KICKOFF.plus(Duration.ofHours(2));
        Match match = playedMatch(KICKOFF, resultEntered);

        assertFalse(finalizer.isDueForFinal(match, resultEntered.plus(FINAL_AFTER).minusSeconds(1)));
    }

    @Test
    void lateCorrectionRestartsTheClock() {
        Instant correction = KICKOFF.plus(Duration.ofHours(20));
        Match match = playedMatch(KICKOFF, correction);

        assertFalse(finalizer.isDueForFinal(match, KICKOFF.plus(Duration.ofHours(30))));
        assertTrue(finalizer.isDueForFinal(match, correction.plus(FINAL_AFTER)));
    }

    @Test
    void neverSyncedMatchCountsFromKickoff() {
        Match match = playedMatch(KICKOFF, null);

        assertTrue(finalizer.isDueForFinal(match, KICKOFF.plus(FINAL_AFTER)));
    }

    @Test
    void matchWithoutResultNeverBecomesFinal() {
        Match match = playedMatch(KICKOFF, null);
        match.fullTimeScore = null;

        assertFalse(finalizer.isDueForFinal(match, KICKOFF.plus(Duration.ofDays(30))));
    }

    @Test
    void alreadyFinalIsNotDueAgain() {
        Match match = playedMatch(KICKOFF, null);
        match.resultStatus = ResultStatus.FINAL;

        assertFalse(finalizer.isDueForFinal(match, KICKOFF.plus(Duration.ofDays(30))));
    }

    private static Match playedMatch(Instant kickoff, Instant lastChanged) {
        Match match = new Match();
        match.kickoff = kickoff;
        match.sourceLastChangedAt = lastChanged;
        match.fullTimeScore = new Score(2, 1);
        return match;
    }
}
