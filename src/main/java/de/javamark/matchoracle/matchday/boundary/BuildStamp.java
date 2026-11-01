package de.javamark.matchoracle.matchday.boundary;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Properties;

/** Which commit is running and when it was built, read from what the build wrote. A release tag is
 * shown as the version; a build of main (image "latest") shows only commit and time. */
public record BuildStamp(String version, String commit, String commitUrl, String builtAt) {

    static final BuildStamp UNKNOWN = new BuildStamp(null, null, null, null);

    private static final DateTimeFormatter BUILT_AT = DateTimeFormatter.ofPattern("d. MMMM yyyy, HH:mm 'Uhr'", Locale.GERMAN)
            .withZone(ZoneId.of("Europe/Berlin"));

    public boolean known() {
        return commit != null;
    }

    static BuildStamp of(Properties p) {
        String commit = p.getProperty("commit", "").strip();
        if (commit.isEmpty()) {
            return UNKNOWN;
        }
        String version = p.getProperty("version", "").strip();
        return new BuildStamp(version.startsWith("v") ? version : null,
                commit.length() > 7 ? commit.substring(0, 7) : commit,
                "https://github.com/mgoericke/rasenradar/commit/" + commit,
                builtAt(p.getProperty("built", "")));
    }

    private static String builtAt(String iso) {
        try {
            return BUILT_AT.format(Instant.parse(iso.strip()));
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
