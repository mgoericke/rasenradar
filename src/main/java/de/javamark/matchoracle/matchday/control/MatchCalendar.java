package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.Team;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

/**
 * Story r027: open matches as an iCalendar file (RFC 5545) — one event per match, two hours long,
 * with a reminder an hour before kickoff and the match page as link. Times are UTC, so every
 * calendar shows them in its own zone. The event id is the match's, so a second download updates
 * the events instead of duplicating them; the sequence follows the source's last change.
 */
public final class MatchCalendar {

    static final Duration LENGTH = Duration.ofHours(2);
    private static final DateTimeFormatter UTC = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);
    private static final int LINE_OCTETS = 75;

    private MatchCalendar() {
    }

    /** The calendar named {@code name} with one event per match; {@code link} gives a match's absolute page URL. */
    public static String of(String name, List<Match> matches, Function<Match, String> link, Instant now) {
        StringBuilder ics = new StringBuilder();
        line(ics, "BEGIN:VCALENDAR");
        line(ics, "VERSION:2.0");
        line(ics, "PRODID:-//Rasen-Radar//Spielplan//DE");
        line(ics, "CALSCALE:GREGORIAN");
        line(ics, "METHOD:PUBLISH");
        line(ics, "X-WR-CALNAME:" + text(name));
        for (Match m : matches) {
            String title = shortName(m.homeTeam) + " – " + shortName(m.awayTeam);
            String url = link.apply(m);
            line(ics, "BEGIN:VEVENT");
            line(ics, "UID:match-" + m.id + "@rasen-radar.de");
            line(ics, "DTSTAMP:" + UTC.format(now));
            line(ics, "SEQUENCE:" + (m.sourceLastChangedAt == null ? 0 : m.sourceLastChangedAt.getEpochSecond() / 60));
            line(ics, "DTSTART:" + UTC.format(m.kickoff));
            line(ics, "DTEND:" + UTC.format(m.kickoff.plus(LENGTH)));
            line(ics, "SUMMARY:" + text(title));
            line(ics, "DESCRIPTION:" + text(m.homeTeam.name + " gegen " + m.awayTeam.name + "\n" + url));
            line(ics, "URL:" + url);
            line(ics, "BEGIN:VALARM");
            line(ics, "ACTION:DISPLAY");
            line(ics, "TRIGGER:-PT1H");
            line(ics, "DESCRIPTION:" + text("In einer Stunde: " + title));
            line(ics, "END:VALARM");
            line(ics, "END:VEVENT");
        }
        line(ics, "END:VCALENDAR");
        return ics.toString();
    }

    private static String shortName(Team team) {
        return team.shortName == null || team.shortName.isBlank() ? team.name : team.shortName;
    }

    /** Escapes a TEXT value: backslash, semicolon, comma and line breaks. */
    static String text(String value) {
        return value.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\n", "\\n");
    }

    /** Appends one content line, folded after 75 octets without splitting a character, ended by CRLF. */
    static void line(StringBuilder ics, String content) {
        int octets = 0;
        int limit = LINE_OCTETS;
        for (int i = 0; i < content.length(); ) {
            int cp = content.codePointAt(i);
            int size = new String(Character.toChars(cp)).getBytes(StandardCharsets.UTF_8).length;
            if (octets + size > limit) {
                ics.append("\r\n ");
                octets = 0;
                limit = LINE_OCTETS - 1;
            }
            ics.appendCodePoint(cp);
            octets += size;
            i += Character.charCount(cp);
        }
        ics.append("\r\n");
    }
}
