package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.control.Stadiums.Stadium;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.Team;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Story r060 "Auswärtsfahrten und Derbys": straight-line distances between home grounds, summed
 * per club over its away matches of the season — those played and those still to come — and the
 * pairings whose grounds lie at most {@link Stadiums#DERBY_KM} apart. A club without a ground has
 * no distance at all, so it never distorts a ranking.
 */
public final class TravelCalculator {

    private TravelCalculator() {
    }

    /** {@code longest}: the farthest single away trip, null without any. {@code known}: the club has a ground. */
    public record Trips(Team team, boolean known, int travelled, int ahead, Longest longest) {
        public int total() {
            return travelled + ahead;
        }
    }

    public record Longest(String city, int km) {
    }

    /** Two clubs whose grounds are close, with their meetings this season in kickoff order. */
    public record Derby(Team first, Team second, int km, List<Match> meetings) {
    }

    /**
     * @param matches every match of the league season, played or not
     * @param ground  a club's home ground, empty when unknown
     * @return every club of the season, the longest total first; clubs without a ground last
     */
    public static List<Trips> trips(List<Match> matches, Function<Team, Optional<Stadium>> ground) {
        List<Trips> result = new ArrayList<>();
        for (Team team : teams(matches)) {
            Optional<Stadium> home = ground.apply(team);
            if (home.isEmpty()) {
                result.add(new Trips(team, false, 0, 0, null));
                continue;
            }
            int travelled = 0, ahead = 0;
            Longest longest = null;
            for (Match m : matches) {
                if (!m.awayTeam.equals(team)) {
                    continue;
                }
                Optional<Stadium> away = ground.apply(m.homeTeam);
                if (away.isEmpty()) {
                    continue;
                }
                int km = Stadiums.km(home.get(), away.get());
                if (m.isPlayed()) {
                    travelled += km;
                } else {
                    ahead += km;
                }
                if (longest == null || km > longest.km()) {
                    longest = new Longest(away.get().city(), km);
                }
            }
            result.add(new Trips(team, true, travelled, ahead, longest));
        }
        result.sort(Comparator.comparing((Trips t) -> !t.known()).thenComparing(Trips::total, Comparator.reverseOrder())
                .thenComparing(t -> t.team().name));
        return List.copyOf(result);
    }

    /** Every pairing of the season with grounds at most {@link Stadiums#DERBY_KM} apart, the closest first. */
    public static List<Derby> derbies(List<Match> matches, Function<Team, Optional<Stadium>> ground) {
        List<Team> teams = new ArrayList<>(teams(matches));
        List<Derby> derbies = new ArrayList<>();
        for (int i = 0; i < teams.size(); i++) {
            for (int j = i + 1; j < teams.size(); j++) {
                Team a = teams.get(i), b = teams.get(j);
                Optional<Stadium> ga = ground.apply(a), gb = ground.apply(b);
                if (ga.isEmpty() || gb.isEmpty()) {
                    continue;
                }
                int km = Stadiums.km(ga.get(), gb.get());
                if (km > Stadiums.DERBY_KM) {
                    continue;
                }
                List<Match> meetings = matches.stream()
                        .filter(m -> (m.homeTeam.equals(a) && m.awayTeam.equals(b)) || (m.homeTeam.equals(b) && m.awayTeam.equals(a)))
                        .sorted(Comparator.comparing(m -> m.kickoff)).toList();
                derbies.add(new Derby(a, b, km, meetings));
            }
        }
        derbies.sort(Comparator.comparingInt(Derby::km).thenComparing(d -> d.first().name));
        return List.copyOf(derbies);
    }

    /** The trip of one match: the distance between both grounds, empty when one is unknown. */
    public static Optional<Integer> km(Match m, Function<Team, Optional<Stadium>> ground) {
        return ground.apply(m.homeTeam).flatMap(h -> ground.apply(m.awayTeam).map(a -> Stadiums.km(h, a)));
    }

    private static Set<Team> teams(List<Match> matches) {
        Set<Team> teams = new LinkedHashSet<>();
        matches.stream().sorted(Comparator.comparing(m -> m.homeTeam.name)).forEach(m -> {
            teams.add(m.homeTeam);
            teams.add(m.awayTeam);
        });
        return teams;
    }

    /** For callers that look grounds up by club: the OpenLigaDB id of a {@link Team}. */
    public static Function<Team, Optional<Stadium>> byExternalId() {
        return t -> Stadiums.of(t.externalId);
    }

    static Function<Team, Optional<Stadium>> of(Map<Team, Stadium> grounds) {
        return t -> Optional.ofNullable(grounds.get(t));
    }
}
