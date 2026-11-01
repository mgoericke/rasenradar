package de.javamark.matchoracle.matchday.boundary;

import io.quarkus.qute.TemplateGlobal;
import org.eclipse.microprofile.config.ConfigProvider;

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

    /** See {@link AssetVersion}. */
    static String assetVersion() {
        String version = assetVersion;
        if (version == null) {
            version = AssetVersion.of(AssetVersion.VERSIONED);
            assetVersion = version;
        }
        return version;
    }
}
