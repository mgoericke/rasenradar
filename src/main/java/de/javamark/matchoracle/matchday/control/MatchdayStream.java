package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.entity.KickoffSlot;
import de.javamark.matchoracle.matchday.entity.Match;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Spec 01, "Spieltags-Strom der Startseite": the displayed matchdays of both Bundesligas as one
 * chronological stream — by day, then by kickoff block across both leagues — instead of one list
 * per league. Pure, so the rules are testable without a database.
 */
public final class MatchdayStream {

    private static final ZoneId MATCH_ZONE = ZoneId.of("Europe/Berlin");

    /** One calendar day of the stream (Europe/Berlin) and its kickoff blocks in order. */
    public record StreamDay(LocalDate date, List<KickoffSlot> slots) {
    }

    private MatchdayStream() {
    }

    /** Within a block, the 1. Bundesliga comes first — the order the leagues have everywhere else. */
    public static List<StreamDay> of(List<Match> matches) {
        Map<LocalDate, List<Match>> byDay = matches.stream()
                .collect(Collectors.groupingBy(m -> m.kickoff.atZone(MATCH_ZONE).toLocalDate(), TreeMap::new, Collectors.toList()));
        List<StreamDay> days = new ArrayList<>();
        byDay.forEach((date, ofDay) -> {
            List<KickoffSlot> slots = ofDay.stream()
                    .collect(Collectors.groupingBy(m -> m.kickoff, TreeMap::new, Collectors.toList()))
                    .entrySet().stream()
                    .map(e -> new KickoffSlot(e.getKey(), e.getValue().stream()
                            .sorted(Comparator.comparing((Match m) -> m.matchday.league.ordinal()).thenComparing(m -> m.id == null ? 0 : m.id))
                            .toList()))
                    .toList();
            days.add(new StreamDay(date, slots));
        });
        return days;
    }

    /** The next matchday follows only once the displayed one has started — before that it is the next one itself. */
    public static boolean showsNext(List<Match> displayed) {
        return displayed.stream().anyMatch(Match::isPlayed);
    }
}
