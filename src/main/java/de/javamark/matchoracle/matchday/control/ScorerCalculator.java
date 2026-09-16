package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.entity.Goal;
import de.javamark.matchoracle.matchday.entity.League;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.Scorer;
import de.javamark.matchoracle.matchday.entity.Team;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Who scored for a club: a goal counts for the side whose score went up; own goals are not credited to the scorer. */
@ApplicationScoped
public class ScorerCalculator {

    public List<Scorer> scorersFor(Team team, League league, int season) {
        return scorers(team, Match.findByTeam(team, league, season));
    }

    /** Pure calculation over the club's matches; unplayed matches carry no goals. */
    List<Scorer> scorers(Team team, List<Match> matches) {
        Map<String, int[]> tally = new LinkedHashMap<>();
        for (Match match : matches) {
            boolean home = match.homeTeam.equals(team);
            int before = 0;
            for (Goal goal : match.goals) {
                int after = home ? goal.scoreAfter.home : goal.scoreAfter.away;
                if (after > before && !goal.ownGoal && goal.scorerName != null) {
                    int[] counts = tally.computeIfAbsent(goal.scorerName, k -> new int[2]);
                    counts[0]++;
                    if (goal.penalty) counts[1]++;
                }
                before = after;
            }
        }
        return tally.entrySet().stream()
                .map(e -> new Scorer(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .sorted(Comparator.comparingInt(Scorer::goals).reversed().thenComparing(Scorer::name))
                .toList();
    }
}
