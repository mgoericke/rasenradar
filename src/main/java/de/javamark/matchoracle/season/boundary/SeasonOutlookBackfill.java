package de.javamark.matchoracle.season.boundary;

import de.javamark.matchoracle.matchday.boundary.MatchdayFacade;
import de.javamark.matchoracle.season.control.SeasonOutlookService;
import de.javamark.matchoracle.season.entity.SeasonOutlook;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Spec 04 safety net: a league-season whose matchday completed before this feature existed (or
 * before its next matchday finishes) would otherwise show no outlook until then. On startup,
 * compute it once for any league-season that has a completed matchday but no outlook yet.
 */
@ApplicationScoped
public class SeasonOutlookBackfill {

    private static final List<String> LEAGUES = List.of("bl1", "bl2", "ucl");

    @Inject
    MatchdayFacade matchday;

    @Inject
    SeasonOutlookService outlook;

    void onStart(@Observes StartupEvent event) {
        for (String league : LEAGUES) {
            matchday.mostRecentlyCompletedMatchday(league)
                    .filter(ref -> SeasonOutlook.findByLeagueSeason(ref.league(), ref.season()).isEmpty())
                    .ifPresent(ref -> outlook.recompute(ref.league(), ref.season()));
        }
    }
}
