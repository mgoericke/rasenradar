package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.entity.KickoffSlot;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.SpotlightPhase;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Spec 01, "Blickfang der Startseite": decides which phase the landing page's spotlight is in
 * and which matches it shows. Pure — the clock, the live window and the hold threshold come
 * from the caller, so every rule of the spec is testable without a database.
 */
public final class SpotlightSelector {

    /** The spotlight's content: its phase and the matches it shows, by kickoff. */
    public record Selection(SpotlightPhase phase, Instant kickoff, List<Match> matches) {
    }

    private SpotlightSelector() {
    }

    /**
     * @param matches     candidates from both Bundesligas, usually the matchdays around the displayed one
     * @param liveWindow  how long after kickoff a match without result counts as "läuft"
     * @param resultsHold the results of the last block stay until the next block is closer than this
     */
    public static Optional<Selection> select(List<Match> matches, Instant now, Duration liveWindow, Duration resultsHold) {
        List<Match> live = matches.stream().filter(m -> m.isLive(now, liveWindow))
                .sorted(Comparator.comparing(m -> m.kickoff)).toList();
        if (!live.isEmpty()) {
            return Optional.of(new Selection(SpotlightPhase.LIVE, live.getFirst().kickoff, live));
        }

        TreeMap<Instant, List<Match>> slots = matches.stream()
                .collect(Collectors.groupingBy(m -> m.kickoff, TreeMap::new, Collectors.toList()));
        Optional<KickoffSlot> next = Optional.ofNullable(slots.higherEntry(now))
                .map(e -> new KickoffSlot(e.getKey(), e.getValue()));
        Optional<KickoffSlot> last = Optional.ofNullable(slots.floorEntry(now))
                .map(e -> new KickoffSlot(e.getKey(), e.getValue()));

        // Nothing coming up (end of season, summer break): the spotlight disappears rather than showing stale results.
        if (next.isEmpty()) {
            return Optional.empty();
        }
        boolean nextIsClose = Duration.between(now, next.get().kickoff()).compareTo(resultsHold) <= 0;
        if (last.isPresent() && !nextIsClose) {
            return last.map(slot -> new Selection(SpotlightPhase.RESULTS, slot.kickoff(), slot.matches()));
        }
        return next.map(slot -> new Selection(SpotlightPhase.PREVIEW, slot.kickoff(), slot.matches()));
    }
}
