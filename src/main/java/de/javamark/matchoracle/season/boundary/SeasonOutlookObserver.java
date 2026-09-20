package de.javamark.matchoracle.season.boundary;

import de.javamark.matchoracle.matchday.boundary.MatchPlayed;
import de.javamark.matchoracle.matchday.boundary.MatchdayFacade;
import de.javamark.matchoracle.matchday.boundary.ResultFinalized;
import de.javamark.matchoracle.season.control.SeasonOutlookService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * Spec 04, step 1: once a whole matchday is played, the affected league's outlook is recomputed —
 * on the first score (even provisional, so a viewer sees it without a day's delay) and again once
 * results go FINAL, in case a provisional score got corrected in between.
 */
@ApplicationScoped
public class SeasonOutlookObserver {

    @Inject
    MatchdayFacade matchday;

    @Inject
    SeasonOutlookService outlook;

    void onMatchPlayed(@Observes MatchPlayed event) {
        matchday.matchdayJustPlayed(event.matchId()).ifPresent(ref -> outlook.recompute(ref.league(), ref.season()));
    }

    void onResultFinalized(@Observes ResultFinalized event) {
        matchday.matchdayJustCompleted(event.matchId()).ifPresent(ref -> outlook.recompute(ref.league(), ref.season()));
    }
}
