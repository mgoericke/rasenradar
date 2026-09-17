package de.javamark.matchoracle.matchday.boundary;

import io.quarkus.qute.TemplateGlobal;
import org.eclipse.microprofile.config.ConfigProvider;

/** Values available in every Qute template without being passed in explicitly — currently just the
 * absolute origin for canonical/Open Graph URLs, since {@code base.html} is shared across all features. */
@TemplateGlobal
public final class SiteGlobals {

    private SiteGlobals() {
    }

    static String siteBaseUrl() {
        return ConfigProvider.getConfig().getValue("matchoracle.site.base-url", String.class);
    }
}
