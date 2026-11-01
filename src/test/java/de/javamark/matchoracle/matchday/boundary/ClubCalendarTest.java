package de.javamark.matchoracle.matchday.boundary;

import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.CalendarMonth;
import de.javamark.matchoracle.matchday.entity.League;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.Matchday;
import de.javamark.matchoracle.matchday.entity.ResultStatus;
import de.javamark.matchoracle.matchday.entity.Score;
import de.javamark.matchoracle.matchday.entity.Team;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Story r088: the club calendar — months from first to last match, Monday first, late kickoffs on the Berlin day. */
class ClubCalendarTest {

    private final Team bremen = team(1, "Bremen"), leipzig = team(2, "Leipzig");

    private static Team team(long id, String name) {
        Team t = new Team();
        t.id = id;
        t.name = name;
        t.shortName = name;
        return t;
    }

    private Match match(String kickoff, Team home, Team away, Integer h, Integer a) {
        Matchday md = new Matchday();
        md.league = League.BUNDESLIGA_1;
        md.season = 2026;
        md.number = 1;
        Match m = new Match();
        m.id = 9L;
        m.matchday = md;
        m.kickoff = Instant.parse(kickoff);
        m.homeTeam = home;
        m.awayTeam = away;
        m.fullTimeScore = h == null ? null : new Score(h, a);
        m.resultStatus = ResultStatus.FINAL;
        return m;
    }

    @Test
    void monthsMondayFirstAndAMatchOnItsBerlinDay() {
        // 22:30 UTC on 30 Sep is 00:30 on 1 Oct in Berlin
        List<Match> schedule = List.of(match("2026-08-22T13:30:00Z", bremen, leipzig, 2, 1),
                match("2026-09-30T22:30:00Z", leipzig, bremen, null, null));

        List<CalendarMonth> months = MatchdayPageModels.clubCalendar(schedule, bremen, "bl1", LocalDate.of(2026, 8, 22));

        assertEquals(List.of("August 2026", "September 2026", "Oktober 2026"), months.stream().map(CalendarMonth::label).toList());
        // 1 August 2026 is a Saturday: the first week starts with Monday 27 July, outside the month
        var firstWeek = months.getFirst().weeks().getFirst();
        assertFalse(firstWeek.getFirst().inMonth());
        assertEquals(1, firstWeek.get(5).day());
        var aug22 = months.getFirst().weeks().stream().flatMap(List::stream).filter(d -> d.inMonth() && d.day() == 22).findFirst().orElseThrow();
        assertTrue(aug22.today());
        assertEquals("22. August 2026: gegen Leipzig, 2:1 Sieg", aug22.label("August 2026"));
        var oct1 = months.getLast().weeks().stream().flatMap(List::stream).filter(d -> d.inMonth() && d.day() == 1).findFirst().orElseThrow();
        assertEquals("bei Leipzig", oct1.label("Oktober").split(": ")[1].split(",")[0]);
    }
}
