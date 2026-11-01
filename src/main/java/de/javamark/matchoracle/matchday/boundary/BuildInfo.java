package de.javamark.matchoracle.matchday.boundary;

import io.quarkus.qute.TemplateGlobal;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

/** The version stamp in the footer. The build in GitHub Actions writes {@code build-info.properties}
 * before packaging; without it (dev mode, a local build) the footer says so instead. Only the
 * parameterless {@link #build()} lives here — every static method of a template global becomes a
 * template variable. */
@TemplateGlobal
public final class BuildInfo {

    private static volatile BuildStamp build;

    private BuildInfo() {
    }

    static BuildStamp build() {
        BuildStamp stamp = build;
        if (stamp == null) {
            stamp = BuildStamp.of(load());
            build = stamp;
        }
        return stamp;
    }

    private static Properties load() {
        Properties properties = new Properties();
        try (InputStream in = BuildInfo.class.getResourceAsStream("/build-info.properties")) {
            if (in != null) {
                properties.load(in);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return properties;
    }
}
