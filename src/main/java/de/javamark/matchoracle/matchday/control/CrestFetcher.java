package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.entity.Team;
import de.javamark.matchoracle.matchday.entity.TeamCrest;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Downloads club crests from the URLs the source names and stores them, so pages can serve
 * them from our own origin. Each download runs outside a transaction; each result is stored
 * in its own, so one slow or broken host never holds up the rest.
 */
@ApplicationScoped
public class CrestFetcher {

    private static final Logger LOG = Logger.getLogger(CrestFetcher.class);
    private static final Duration PAUSE = Duration.ofMillis(500);

    /** Wikimedia refuses requests without a descriptive User-Agent that names a contact. */
    @ConfigProperty(name = "matchoracle.matchday.crest-user-agent")
    String userAgent;

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    record Pending(long teamId, String sourceUrl) {
    }

    /** Fetches every crest that is missing or due again; returns how many were stored. */
    public int fetchDue() {
        List<Pending> due = QuarkusTransaction.requiringNew().call(() ->
                TeamCrest.teamsToFetch(Instant.now()).stream().map(t -> new Pending(t.id, t.iconUrl)).toList());
        int stored = 0;
        for (Pending pending : due) {
            pause();
            Optional<CrestImage> image = download(pending.sourceUrl()).flatMap(CrestImage::of);
            QuarkusTransaction.requiringNew().run(() -> store(pending, image.orElse(null), Instant.now()));
            if (image.isPresent()) {
                stored++;
            } else {
                LOG.warnf("No usable crest for team %d at %.120s", pending.teamId(), pending.sourceUrl());
            }
        }
        if (!due.isEmpty()) {
            LOG.infof("Crests: %d of %d stored", stored, due.size());
        }
        return stored;
    }

    /** Most crests come from Wikimedia; a short gap keeps a first run of ~180 downloads polite. */
    private static void pause() {
        try {
            Thread.sleep(PAUSE.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Optional<byte[]> download(String url) {
        if (url.startsWith("data:")) {
            int comma = url.indexOf(',');
            if (comma < 0 || !url.substring(0, comma).endsWith(";base64")) {
                return Optional.empty();
            }
            try {
                return Optional.of(Base64.getMimeDecoder().decode(url.substring(comma + 1)));
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(downloadUrl(url.strip())))
                    .timeout(Duration.ofSeconds(20))
                    .header("User-Agent", userAgent)
                    .GET().build();
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body()) {
                if (response.statusCode() != 200) {
                    LOG.warnf("Crest download: HTTP %d for %.120s", response.statusCode(), url);
                    return Optional.empty();
                }
                // read one byte past the limit, so CrestImage can reject an oversized file
                return Optional.of(body.readNBytes(CrestImage.MAX_BYTES + 1));
            }
        } catch (IOException | IllegalArgumentException e) {
            LOG.warnf("Crest download: %s for %.120s", e, url);
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    private static final Pattern WIKIMEDIA_THUMB = Pattern.compile("^https?://upload\\.wikimedia\\.org(/.+/thumb/.+/)\\d+px-([^/?]+)(\\?.*)?$");

    /**
     * Where to actually fetch a crest. Wikimedia answers thumbnails in non-standard widths
     * (150px, 227px, …) with HTTP 400 for anything but a browser, so they are asked for at the
     * standard 250px. kicker.de's image service is asked for PNG instead of WebP, which
     * ImageIO cannot read.
     */
    static String downloadUrl(String url) {
        Matcher thumb = WIKIMEDIA_THUMB.matcher(url);
        if (thumb.matches()) {
            return "https://upload.wikimedia.org" + thumb.group(1) + "250px-" + thumb.group(2);
        }
        if (url.startsWith("https://derivates.kicker.de/image/fetch/")) {
            return url.replace("/f_webp/", "/f_png/");
        }
        return url;
    }

    private static void store(Pending pending, CrestImage image, Instant now) {
        TeamCrest crest = TeamCrest.<TeamCrest>findByIdOptional(pending.teamId()).orElseGet(() -> {
            TeamCrest c = new TeamCrest();
            c.teamId = pending.teamId();
            c.sourceUrl = pending.sourceUrl();
            return c;
        });
        crest.fetchedAt = now;
        if (image != null) {
            crest.sourceUrl = pending.sourceUrl();
            crest.contentType = image.contentType();
            crest.data = image.data();
            Team.<Team>findById(pending.teamId()).crestFile = image.fileName();
        }
        // a failed attempt keeps an earlier crest and only records when it was tried
        if (!crest.isPersistent()) {
            crest.persist();
        }
    }
}
