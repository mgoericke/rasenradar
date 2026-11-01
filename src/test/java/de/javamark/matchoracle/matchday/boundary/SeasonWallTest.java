package de.javamark.matchoracle.matchday.boundary;

import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.ScheduleRow;
import de.javamark.matchoracle.matchday.entity.TeamResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Story r064: the key under the season wall — provisional results count neither as a result nor as open. */
class SeasonWallTest {

    private static ScheduleRow row(String score, TeamResult result, boolean provisional) {
        return new ScheduleRow(1, "1. Spieltag", "1.9.26", "15:30", "Köln", null, true, score, result, provisional, null, "/bl1/matches/1");
    }

    @Test
    void countsResultsOpenMatchesAndProvisionalApart() {
        List<ScheduleRow> schedule = List.of(
                row("2:0", TeamResult.WIN, false), row("1:1", TeamResult.DRAW, false), row("0:1", TeamResult.LOSS, false),
                row("3:1", TeamResult.WIN, false), row("2:2", TeamResult.DRAW, true), row(null, null, false), row(null, null, false));

        assertEquals("2 Siege, 1 Remis, 1 Niederlage, 2 offen, 1 vorläufig", MatchdayPageModels.wallKey(schedule));
    }

    @Test
    void singularAndPlural() {
        assertEquals("1 Sieg, 0 Remis, 0 Niederlagen, 0 offen", MatchdayPageModels.wallKey(List.of(row("1:0", TeamResult.WIN, false))));
    }
}
