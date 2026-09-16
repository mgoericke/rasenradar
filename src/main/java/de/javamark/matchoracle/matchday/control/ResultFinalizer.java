package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.ResultStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Spec 01, rule 4: a result is PROVISIONAL at first and becomes FINAL after a
 * fixed period without further change. Only FINAL results may be used for
 * review and learning.
 */
@ApplicationScoped
public class ResultFinalizer {

    private static final Logger LOG = Logger.getLogger(ResultFinalizer.class);

    /** A result is FINAL once kickoff and the last source change are this long ago. */
    @ConfigProperty(name = "matchoracle.matchday.final-after", defaultValue = "PT24H")
    Duration finalAfter;

    /**
     * Pure rule, testable without a database: {@code now} is passed in instead
     * of being read from the clock.
     */
    boolean isDueForFinal(Match match, Instant now) {
        if (match.resultStatus == ResultStatus.FINAL || match.fullTimeScore == null) {
            return false;
        }
        Instant lastActivity = match.sourceLastChangedAt == null
                ? match.kickoff
                : max(match.kickoff, match.sourceLastChangedAt);
        return !lastActivity.plus(finalAfter).isAfter(now);
    }

    /** Marks every provisional result that is due as FINAL. Returns the matches that just became final. */
    @Transactional
    public List<Match> finalizeDueResults(Instant now) {
        List<Match> finalized = new ArrayList<>();
        for (Match match : Match.findProvisionalWithResult()) {
            if (isDueForFinal(match, now)) {
                match.resultStatus = ResultStatus.FINAL;
                finalized.add(match);
            }
        }
        if (!finalized.isEmpty()) {
            LOG.infof("Finalized %d result(s)", finalized.size());
        }
        return finalized;
    }

    private static Instant max(Instant a, Instant b) {
        return a.isAfter(b) ? a : b;
    }
}
