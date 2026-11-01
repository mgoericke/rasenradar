package de.javamark.matchoracle.matchday.boundary;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Story r085 "Wegweiser": the section row of the season page lists every section in page order,
 * each link jumps to an existing section and its text equals the section heading — so a section that
 * is added, renamed or moved without the row leaves neither a dead nor a wrong link.
 */
class SeasonTocTest {

    private static final Pattern TOC = Pattern.compile("<nav class=\"season-toc\".*?</nav>", Pattern.DOTALL);
    private static final Pattern LINK = Pattern.compile("<a href=\"#([^\"]+)\">([^<]+)</a>");
    private static final Pattern SECTION = Pattern.compile(
            "<section class=\"season-block\" id=\"([^\"]+)\"[^>]*>\\s*<h2 id=\"[^\"]+\">([^<]+)</h2>");

    @Test
    void rowMatchesTheSectionsOfThePage() throws IOException {
        String page = template();
        Matcher toc = TOC.matcher(page);
        assertTrue(toc.find(), "season page has no section row");

        List<String> links = pairs(LINK.matcher(toc.group()));
        List<String> sections = pairs(SECTION.matcher(page));

        assertTrue(sections.size() > 6, "the row is only meant for pages with more than six sections");
        assertEquals(sections, links);
    }

    private static List<String> pairs(Matcher m) {
        List<String> out = new ArrayList<>();
        while (m.find()) out.add(m.group(1) + " → " + m.group(2));
        return out;
    }

    private static String template() throws IOException {
        try (InputStream in = SeasonTocTest.class.getResourceAsStream("/templates/MatchdayPages/season.html")) {
            assertNotNull(in, "season template not on the classpath");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
