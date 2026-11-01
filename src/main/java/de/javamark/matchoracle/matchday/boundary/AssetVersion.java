package de.javamark.matchoracle.matchday.boundary;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/** A short hash over the content of all {@link #VERSIONED} files, appended as a query parameter so a
 * CDN or browser caching them as immutable (Cloudflare does, with no version in the URL otherwise) is
 * forced to fetch the new version after a deploy instead of serving a stale one for up to a day. Kept
 * out of {@link SiteGlobals}, where every static method would become a template global. */
final class AssetVersion {

    /** Every file the templates load with {@code ?v={assetVersion}} — one changed file is enough for
     * a new version. */
    static final List<String> VERSIONED = List.of("app.css", "nav.js", "match.js", "since.js", "stream.js");

    private AssetVersion() {
    }

    static String of(List<String> files) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String file : files) {
                try (InputStream in = AssetVersion.class.getResourceAsStream("/META-INF/resources/" + file)) {
                    if (in != null) {
                        digest.update(in.readAllBytes());
                    }
                }
            }
            return HexFormat.of().formatHex(digest.digest(), 0, 4);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
