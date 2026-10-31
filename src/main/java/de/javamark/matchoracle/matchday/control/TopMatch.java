package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.Team;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * The top match of the start page: a running match first, then the next open one, else the last
 * played — and within that group the pairing highest in the table (the lowest sum of both places),
 * the earlier kickoff breaking a tie. A club without a place counts as last. From the table alone,
 * never from a forecast.
 */
public final class TopMatch {

    private TopMatch() {
    }

    /** {@code places}: team → table place, for the league of each candidate. */
    public static Optional<Match> pick(List<Match> candidates, Map<Team, Integer> places, Instant now, Duration liveWindow) {
        Comparator<Match> byTable = Comparator.comparingInt((Match m) -> place(places, m.homeTeam) + place(places, m.awayTeam))
                .thenComparing(m -> m.kickoff);
        Predicate<Match> live = m -> m.isLive(now, liveWindow);
        Predicate<Match> open = m -> !m.isPlayed() && !live.test(m);
        Optional<Match> running = candidates.stream().filter(live).min(byTable);
        if (running.isPresent()) {
            return running;
        }
        // the next open matches: the earliest kickoff day among them, then the best pairing that day
        Optional<Instant> soonest = candidates.stream().filter(open).map(m -> m.kickoff).min(Comparator.naturalOrder());
        if (soonest.isPresent()) {
            Instant dayEnd = soonest.get().plus(Duration.ofHours(24));
            return candidates.stream().filter(open).filter(m -> m.kickoff.isBefore(dayEnd)).min(byTable);
        }
        return candidates.stream().filter(Match::isPlayed).min(byTable);
    }

    private static int place(Map<Team, Integer> places, Team team) {
        return places.getOrDefault(team, 99);
    }
}
