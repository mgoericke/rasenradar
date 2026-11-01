package de.javamark.matchoracle.matchday.boundary;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/** The footer's version stamp: what the build in GitHub Actions writes, as the visitor reads it. */
class BuildStampTest {

    private static Properties props(String commit, String built, String version) {
        Properties p = new Properties();
        p.setProperty("commit", commit);
        p.setProperty("built", built);
        p.setProperty("version", version);
        return p;
    }

    @Test
    void aBuildOfMainShowsCommitAndBerlinTimeButNoVersion() {
        BuildStamp stamp = BuildStamp.of(props("1fadca6e4b2c9d0f", "2026-11-01T15:29:10Z", "latest"));
        assertEquals("1fadca6", stamp.commit());
        assertEquals("https://github.com/mgoericke/rasenradar/commit/1fadca6e4b2c9d0f", stamp.commitUrl());
        assertEquals("1. November 2026, 16:29 Uhr", stamp.builtAt());
        assertNull(stamp.version());
    }

    @Test
    void aReleaseTagIsShownAsTheVersion() {
        assertEquals("v1.2.0", BuildStamp.of(props("abc1234", "2026-07-01T10:00:00Z", "v1.2.0")).version());
        assertEquals("1. Juli 2026, 12:00 Uhr", BuildStamp.of(props("abc1234", "2026-07-01T10:00:00Z", "v1.2.0")).builtAt());
    }

    @Test
    void withoutABuildFileTheStampIsUnknown() {
        assertFalse(BuildStamp.of(new Properties()).known());
        assertNull(BuildStamp.of(props("abc1234", "kaputt", "latest")).builtAt());
    }
}
