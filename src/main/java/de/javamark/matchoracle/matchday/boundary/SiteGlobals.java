package de.javamark.matchoracle.matchday.boundary;

import io.quarkus.qute.TemplateGlobal;
import org.eclipse.microprofile.config.ConfigProvider;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Values available in every Qute template without being passed in explicitly — the absolute
 * origin for canonical/Open Graph URLs, a cache-busting asset version, and the visitor-statistics
 * tracker, since {@code base.html} is shared across all features. */
@TemplateGlobal
public final class SiteGlobals {

    private static volatile String assetVersion;

    private SiteGlobals() {
    }

    static String siteBaseUrl() {
        return ConfigProvider.getConfig().getValue("matchoracle.site.base-url", String.class);
    }

    /** Self-hosted Umami. The tracker is only written into the page when a website id is
     * configured: it stays empty everywhere but production, so developing does not count towards
     * the numbers. Set as MATCHORACLE_ANALYTICS_WEBSITE_ID in the deployment stack. */
    static String analyticsWebsiteId() {
        return ConfigProvider.getConfig().getOptionalValue("matchoracle.analytics.website-id", String.class).orElse("");
    }

    static String analyticsScriptUrl() {
        return ConfigProvider.getConfig().getOptionalValue("matchoracle.analytics.script-url", String.class).orElse("");
    }

    /** Both halves have to be there — a tracker without an id collects nothing, an id without a
     * tracker is dead configuration. */
    static boolean analyticsEnabled() {
        return !analyticsWebsiteId().isBlank() && !analyticsScriptUrl().isBlank();
    }

    /** A short hash of {@code app.css}'s content, appended as a query parameter so a CDN or browser
     * caching the file as immutable (Cloudflare does, with no version in the URL otherwise) is forced
     * to fetch the new version after a deploy instead of serving a stale one for up to a day. */
    static String assetVersion() {
        String version = assetVersion;
        if (version == null) {
            version = hashOf("/META-INF/resources/app.css");
            assetVersion = version;
        }
        return version;
    }

    private static String hashOf(String classpathResource) {
        try (InputStream in = SiteGlobals.class.getResourceAsStream(classpathResource)) {
            if (in == null) {
                return "0";
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(in.readAllBytes());
            return HexFormat.of().formatHex(hash, 0, 4);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
