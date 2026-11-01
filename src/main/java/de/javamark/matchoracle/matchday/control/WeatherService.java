package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.entity.Match;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Story r034 "Wetter am Spielort": the forecast at the home ground for half-time (kickoff + 45
 * minutes) of a match that is still ahead and within the source's range. One answer per match is
 * kept for every visitor; it is asked again only after {@link #KEEP}, a failed or slow answer after
 * {@link #RETRY} — the page simply shows no weather meanwhile.
 */
@ApplicationScoped
public class WeatherService {

    /** Bright Sky forecasts about ten days ahead. */
    static final Duration RANGE = Duration.ofDays(10);
    static final Duration KEEP = Duration.ofHours(6);
    static final Duration RETRY = Duration.ofMinutes(15);
    public static final int RAIN_FROM = 30;
    public static final int WIND_FROM = 30;

    /** {@code rain} and {@code wind} are null below their thresholds — nothing worth saying. */
    public record Weather(int temperature, String sky, Integer rain, Integer wind) {
    }

    private record Kept(Optional<Weather> weather, Instant until) {
    }

    @Inject
    @RestClient
    BrightSkyClient client;

    private final Map<Long, Kept> kept = new ConcurrentHashMap<>();

    /** Whether a match gets a weather line at all: ahead, not running, in range, home ground known. */
    public static boolean shown(Match m, Instant now) {
        return !m.isPlayed() && m.kickoff.isAfter(now) && m.kickoff.isBefore(now.plus(RANGE))
                && Stadiums.of(m.homeTeam.externalId).isPresent();
    }

    public Optional<Weather> at(Match m, Instant now) {
        if (!shown(m, now)) {
            return Optional.empty();
        }
        Kept k = kept.get(m.id);
        if (k != null && now.isBefore(k.until())) {
            return k.weather();
        }
        Optional<Weather> weather = fetch(m);
        kept.put(m.id, new Kept(weather, now.plus(weather.isPresent() ? KEEP : RETRY)));
        return weather;
    }

    private Optional<Weather> fetch(Match m) {
        var ground = Stadiums.of(m.homeTeam.externalId).orElseThrow();
        Instant halfTime = m.kickoff.plus(Duration.ofMinutes(45)).truncatedTo(ChronoUnit.HOURS);
        try {
            var answer = client.weather(ground.lat(), ground.lon(), halfTime.toString(), halfTime.plus(Duration.ofHours(1)).toString());
            return answer == null || answer.weather() == null ? Optional.empty()
                    : answer.weather().stream().filter(h -> h.temperature() != null).findFirst().map(WeatherService::of);
        } catch (RuntimeException e) {
            Log.debugf("no weather for match %d: %s", m.id, e.getMessage());
            return Optional.empty();
        }
    }

    static Weather of(BrightSkyClient.Hour h) {
        Integer rain = h.precipitationProbability() != null && h.precipitationProbability() >= RAIN_FROM ? h.precipitationProbability() : null;
        Integer wind = h.windSpeed() != null && Math.round(h.windSpeed()) >= WIND_FROM ? (int) Math.round(h.windSpeed()) : null;
        return new Weather((int) Math.round(h.temperature()), sky(h.condition(), h.icon()), rain, wind);
    }

    /** Precipitation and fog first, as the source names them; on a dry hour the cloud cover. */
    static String sky(String condition, String icon) {
        String wet = condition == null ? null : switch (condition) {
            case "rain" -> "Regen";
            case "sleet" -> "Schneeregen";
            case "snow" -> "Schnee";
            case "hail" -> "Hagel";
            case "thunderstorm" -> "Gewitter";
            case "fog" -> "Nebel";
            default -> null;
        };
        if (wet != null) {
            return wet;
        }
        return icon == null ? "trocken" : switch (icon) {
            case "clear-day", "clear-night" -> "klar";
            case "partly-cloudy-day", "partly-cloudy-night" -> "leicht bewölkt";
            case "cloudy" -> "bewölkt";
            case "fog" -> "Nebel";
            case "wind" -> "windig";
            default -> "trocken";
        };
    }
}
