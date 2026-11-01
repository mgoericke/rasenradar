package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.ResultStatus;
import de.javamark.matchoracle.matchday.entity.Score;

import java.util.ArrayList;
import java.util.List;

/**
 * Story r071: the season's final matches as if they had ended at half time. ("Schluss nach 80",
 * the same after minute 80, was dropped on 1 Nov 2026.) Returns detached copies that only carry what a table
 * needs; nothing is ever stored. A match whose earlier score is not known is left out and counted.
 */
public final class ScenarioScores {

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
