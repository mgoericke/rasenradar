package de.javamark.matchoracle.season.boundary;

import de.javamark.matchoracle.matchday.boundary.MatchdayFacade;
import de.javamark.matchoracle.matchday.boundary.ResultFinalized;
import de.javamark.matchoracle.season.control.SeasonOutlookService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/** Spec 04, step 1: once a whole matchday is done, the affected league's outlook is recomputed. */
@ApplicationScoped
public class SeasonOutlookObserver {

    @Inject
    MatchdayFacade matchday;

    @Inject
    SeasonOutlookService outlook;

    void onResultFinalized(@Observes ResultFinalized event) {
        matchday.matchdayJustCompleted(event.matchId()).ifPresent(ref -> outlook.recompute(ref.league(), ref.season()));
    }
}
