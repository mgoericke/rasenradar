package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.entity.Goal;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.ResultStatus;
import de.javamark.matchoracle.matchday.entity.Score;

import java.util.ArrayList;
import java.util.List;

/**
 * Story r071 "Schluss nach 80": the season's final matches as if they had ended earlier — at half
 * time, or after 80 minutes from the goal list. Returns detached copies that only carry what a table
 * needs; nothing is ever stored. A match whose earlier score is not known is left out and counted.
 */
public final class ScenarioScores {

    /** Decided 31 Oct 2026: goals up to and including minute 80 count. */
    public static final int CUT_OFF = 80;

    private ScenarioScores() {
    }

    /** The rescored matches and how many final matches could not be rescored. */
    public record Rescored(List<Match> matches, int excluded) {
    }

    public static Rescored atHalfTime(List<Match> matches) {
        List<Match> result = new ArrayList<>();
        int excluded = 0;
        for (Match m : finals(matches)) {
            if (m.halfTimeScore == null) {
                excluded++;
                continue;
            }
            result.add(copy(m, m.halfTimeScore.home, m.halfTimeScore.away));
        }
        return new Rescored(List.copyOf(result), excluded);
    }

    public static Rescored afterEighty(List<Match> matches) {
        List<Match> result = new ArrayList<>();
        int excluded = 0;
        for (Match m : finals(matches)) {
            boolean complete = m.goals.size() == m.fullTimeScore.home + m.fullTimeScore.away
                    && m.goals.stream().allMatch(g -> g.minute != null && g.scoreAfter != null);
            if (!complete) {
                excluded++;
                continue;
            }
            Score at = new Score(0, 0);
            for (Goal g : m.goals) {
                if (g.minute <= CUT_OFF) at = g.scoreAfter;
            }
            result.add(copy(m, at.home, at.away));
        }
        return new Rescored(List.copyOf(result), excluded);
    }

    private static List<Match> finals(List<Match> matches) {
        return matches.stream().filter(m -> m.fullTimeScore != null && m.resultStatus == ResultStatus.FINAL).toList();
    }

    private static Match copy(Match original, int home, int away) {
        Match m = new Match();
        m.id = original.id;
        m.matchday = original.matchday;
        m.kickoff = original.kickoff;
        m.homeTeam = original.homeTeam;
        m.awayTeam = original.awayTeam;
        m.fullTimeScore = new Score(home, away);
        m.resultStatus = ResultStatus.FINAL;
        return m;
    }
}
