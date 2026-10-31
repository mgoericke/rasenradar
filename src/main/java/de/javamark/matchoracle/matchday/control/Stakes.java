package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.entity.Balance;
import de.javamark.matchoracle.matchday.entity.StandingPosition;
import de.javamark.matchoracle.matchday.entity.Standings;
import de.javamark.matchoracle.matchday.entity.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Story r070, "Es geht um": what an open match means in the table as it stands — a top match,
 * a bottom match, or which side a win would lift and to which place. Read from the table alone,
 * the rest of the league unchanged; no probabilities, no forecast.
 */
public final class Stakes {

    /** Places 1–3 make a top match. */
    static final int TOP = 3;
    /** The last four places make a bottom match. */
    static final int BOTTOM = 4;
    private static final List<String> ORDINALS = List.of("Erster", "Zweiter", "Dritter");

    private Stakes() {
    }

    public static Optional<String> of(Team home, Team away, Standings table) {
        Optional<StandingPosition> h = table.of(home);
        Optional<StandingPosition> a = table.of(away);
        if (h.isEmpty() || a.isEmpty()) {
            return Optional.empty();
        }
        int first = Math.min(h.get().position(), a.get().position());
        int second = Math.max(h.get().position(), a.get().position());
        int size = table.positions().size();
        if (second <= TOP) {
            return Optional.of("Spitzenspiel: " + ORDINALS.get(first - 1) + " gegen " + ORDINALS.get(second - 1));
        }
        if (first > size - BOTTOM) {
            return Optional.of("Kellerduell: " + first + ". gegen " + second + ".");
        }
        List<String> climbs = new ArrayList<>();
        climb(h.get(), a.get(), table).ifPresent(climbs::add);
        climb(a.get(), h.get(), table).ifPresent(climbs::add);
        return climbs.isEmpty() ? Optional.empty() : Optional.of("Ein Sieg brächte " + String.join(", ", climbs));
    }

    /** "Bremen auf Platz 3" when a narrow win would lift the side; ties beyond goals scored go its way. */
    private static Optional<String> climb(StandingPosition side, StandingPosition opponent, Standings table) {
        Balance b = side.balance();
        int points = b.points() + 3;
        int difference = b.goalDifference() + 1;
        int goals = b.goalsFor() + 1;
        int place = 1;
        for (StandingPosition other : table.positions()) {
            if (other == side) continue;
            int otherDifference = other.balance().goalDifference() - (other == opponent ? 1 : 0);
            if (ahead(other.balance().points(), otherDifference, other.balance().goalsFor(), points, difference, goals)) {
                place++;
            }
        }
        return place < side.position() ? Optional.of(name(side.team()) + " auf Platz " + place) : Optional.empty();
    }

    private static boolean ahead(int points, int difference, int goals, int byPoints, int byDifference, int byGoals) {
        if (points != byPoints) return points > byPoints;
        if (difference != byDifference) return difference > byDifference;
        return goals > byGoals;
    }

    private static String name(Team team) {
        return team.shortName == null || team.shortName.isBlank() ? team.name : team.shortName;
    }
}
