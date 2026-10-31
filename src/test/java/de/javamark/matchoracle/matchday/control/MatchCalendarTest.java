package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.entity.Match;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static de.javamark.matchoracle.matchday.control.MatchFixtures.matchday;
import static de.javamark.matchoracle.matchday.control.MatchFixtures.team;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchCalendarTest {

    private static Match open(long id, String home, String away, String kickoff) {
        Match m = new Match();
        m.id = id;
        m.matchday = matchday(9);
        m.homeTeam = team(home);
        m.awayTeam = team(away);
        m.kickoff = Instant.parse(kickoff);
        return m;
    }

    @Test
    void oneEventPerMatchWithReminderAndLink() {
        Match m = open(7, "Bremen", "Köln", "2026-11-07T14:30:00Z");
        String ics = MatchCalendar.of("Bremen", List.of(m, open(8, "Köln", "Bremen", "2026-11-14T17:30:00Z")),
                x -> "https://rasen-radar.de/bl1/matches/" + x.id, Instant.parse("2026-11-01T10:00:00Z"));

        assertTrue(ics.startsWith("BEGIN:VCALENDAR\r\n"));
        assertTrue(ics.endsWith("END:VCALENDAR\r\n"));
        assertEquals(3, ics.split("BEGIN:VEVENT", -1).length);
        for (String line : List.of("UID:match-7@rasen-radar.de", "DTSTART:20261107T143000Z", "DTEND:20261107T163000Z",
                "SUMMARY:Bremen – Köln", "TRIGGER:-PT1H", "DESCRIPTION:In einer Stunde: Bremen – Köln",
                "URL:https://rasen-radar.de/bl1/matches/7")) {
            assertTrue(ics.contains(line + "\r\n"), line);
        }
    }

    @Test
    void longLinesAreFoldedWithinSeventyFiveOctets() {
        String content = "SUMMARY:" + "Mönchengladbach, ".repeat(10);
        StringBuilder ics = new StringBuilder();
        MatchCalendar.line(ics, content);
        for (String line : ics.toString().split("\r\n")) {
            assertTrue(line.getBytes(StandardCharsets.UTF_8).length <= 75, line);
        }
        assertEquals(content + "\r\n", ics.toString().replace("\r\n ", ""));
        assertEquals("a\\,b\\;c\\nd", MatchCalendar.text("a,b;c\nd"));
    }
}
