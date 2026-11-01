package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.entity.Decision;
import de.javamark.matchoracle.matchday.entity.Goal;
import de.javamark.matchoracle.matchday.entity.GoalTiming;
import de.javamark.matchoracle.matchday.entity.League;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.ResultStatus;
import de.javamark.matchoracle.matchday.entity.Score;
import de.javamark.matchoracle.matchday.entity.Team;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;

/** When a club's goals fall, in 15-minute windows — Nachspielzeit counts to the last window of its half. */
@ApplicationScoped
public class GoalTimingCalculator {

    private static final String[] LABELS = {"1.-15.", "16.-30.", "31.-45.", "46.-60.", "61.-75.", "76.-90."};

    /** Story r016: final results only — a provisional score's goals may still change. */
    public List<GoalTiming> timingFor(Team team, League league, int season) {
        return timing(team, Match.findByTeam(team, league, season).stream()
                .filter(m -> m.fullTimeScore != null && m.resultStatus == ResultStatus.FINAL).toList());
    }

    /**
     * Story r016: every goal of a league season by window — {@code scored} holds the goals,
     * {@code conceded} stays zero. Final results only; goals without a minute are left out.
     */
    public static List<GoalTiming> league(List<Match> matches) {
        return windows(matches, false);
    }

    /**
     * The same for a cup: a match that went to extra time adds a window "91.-120." for every goal
     * after minute 90 — Nachspielzeit of the second half counts there too, the source does not tell
     * them apart.
     */
    public static List<GoalTiming> cup(List<Match> matches) {
        return windows(matches, true);
    }

    private static List<GoalTiming> windows(List<Match> matches, boolean extraTime) {
        int[] goals = new int[LABELS.length + 1];
        for (Match match : matches) {
            if (match.fullTimeScore == null || match.resultStatus != ResultStatus.FINAL) continue;
            boolean extended = extraTime && match.decision != Decision.REGULAR;
            for (Goal goal : match.matchGoals()) {
                if (goal.minute == null) continue;
                goals[extended && goal.minute > 90 ? LABELS.length : window(goal.minute)]++;
            }
        }
        List<GoalTiming> result = new ArrayList<>();
        for (int i = 0; i < LABELS.length; i++) {
            result.add(new GoalTiming(LABELS[i], goals[i], 0));
        }
        if (extraTime) {
            result.add(new GoalTiming(EXTRA_TIME_LABEL, goals[LABELS.length], 0));
        }
        return result;
    }

    private static final String EXTRA_TIME_LABEL = "91.-120.";

    /** Pure calculation over the club's matches; goals without a known minute are left out. */
    List<GoalTiming> timing(Team team, List<Match> matches) {
        int[] scored = new int[LABELS.length];
        int[] conceded = new int[LABELS.length];
        for (Match match : matches) {
            boolean home = match.homeTeam.equals(team);
            Score before = new Score(0, 0);
            for (Goal goal : match.matchGoals()) {
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
