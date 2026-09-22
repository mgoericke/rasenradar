package de.javamark.matchoracle.matchday.control;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Spec 06: a group competition's source data names the group but not the matchday.
 * Matches of the same group on the same calendar day form one matchday, the order of
 * days gives the count. Kept separate from the synchroniser so the derivation — the one
 * place where this project adds something the source does not provide — stays testable.
 */
final class GroupMatchdays {

    private static final ZoneId MATCH_ZONE = ZoneId.of("Europe/Berlin");

    private GroupMatchdays() {
    }

    /** The matches of ONE group, keyed by derived matchday number starting at 1, in ascending order. */
    static Map<Integer, List<OpenLigaDbMatch>> byMatchday(List<OpenLigaDbMatch> matchesOfOneGroup) {
        Map<LocalDate, List<OpenLigaDbMatch>> byDay = matchesOfOneGroup.stream()
                .collect(Collectors.groupingBy(
                        m -> m.kickoff().atZone(MATCH_ZONE).toLocalDate(),
                        TreeMap::new, Collectors.toList()));
        Map<Integer, List<OpenLigaDbMatch>> byMatchday = new LinkedHashMap<>();
        int number = 1;
        for (List<OpenLigaDbMatch> day : byDay.values()) {
            byMatchday.put(number++, List.copyOf(day));
        }
        return byMatchday;
    }
}
