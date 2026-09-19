package de.javamark.matchoracle.matchday.boundary;

import io.quarkus.qute.TemplateGlobal;
import org.eclipse.microprofile.config.ConfigProvider;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Values available in every Qute template without being passed in explicitly — currently the
 * absolute origin for canonical/Open Graph URLs, and a cache-busting asset version, since
 * {@code base.html} is shared across all features. */
@TemplateGlobal
public final class SiteGlobals {

    private static volatile String assetVersion;

    private SiteGlobals() {
    }

    static String siteBaseUrl() {
        return ConfigProvider.getConfig().getValue("matchoracle.site.base-url", String.class);
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
