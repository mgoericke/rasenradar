package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.entity.Goal;
import de.javamark.matchoracle.matchday.entity.GoalTiming;
import de.javamark.matchoracle.matchday.entity.League;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.Score;
import de.javamark.matchoracle.matchday.entity.Team;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;

/** When a club's goals fall, in 15-minute windows — Nachspielzeit counts to the last window of its half. */
@ApplicationScoped
public class GoalTimingCalculator {

    private static final String[] LABELS = {"1.-15.", "16.-30.", "31.-45.", "46.-60.", "61.-75.", "76.-90."};

    public List<GoalTiming> timingFor(Team team, League league, int season) {
        return timing(team, Match.findByTeam(team, league, season));
    }

    /** Pure calculation over the club's matches; goals without a known minute are left out. */
    List<GoalTiming> timing(Team team, List<Match> matches) {
        int[] scored = new int[LABELS.length];
        int[] conceded = new int[LABELS.length];
        for (Match match : matches) {
            boolean home = match.homeTeam.equals(team);
            Score before = new Score(0, 0);
            for (Goal goal : match.goals) {
                boolean homeScored = goal.scoreAfter.home > before.home;
                before = goal.scoreAfter;
                if (goal.minute == null) {
                    continue;
                }
                int window = window(goal.minute);
                if (homeScored == home) {
                    scored[window]++;
                } else {
                    conceded[window]++;
                }
            }
        }
        List<GoalTiming> result = new ArrayList<>();
        for (int i = 0; i < LABELS.length; i++) {
            result.add(new GoalTiming(LABELS[i], scored[i], conceded[i]));
        }
        return result;
    }

    private static int window(int minute) {
        return Math.min((minute - 1) / 15, LABELS.length - 1);
    }
}
