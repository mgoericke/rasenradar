package de.javamark.matchoracle.matchday.control;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Stories r034 and r060: the home ground of every club, from {@code stadiums.jsonl} on the class
 * path (one JSON object per line, keyed by the OpenLigaDB team id). Read once; a club without a
 * line simply has no ground — no weather, no distance, no error. Covers the 36 clubs of 2026/27.
 */
public final class Stadiums {

    /** Derby threshold of story r060: grounds at most this far apart, as the crow flies. */
    public static final int DERBY_KM = 60;

    public record Stadium(int teamId, String team, String stadium, String city, double lat, double lon) {
    }

    private static final Map<Integer, Stadium> BY_TEAM = load();

    private Stadiums() {
    }

    /** @param externalId the club's OpenLigaDB team id ({@code Team.externalId}) */
    public static Optional<Stadium> of(int externalId) {
        return Optional.ofNullable(BY_TEAM.get(externalId));
    }

    /** Great-circle distance in whole kilometres ("Luftlinie"). */
    public static int km(Stadium a, Stadium b) {
        double earth = 6371.0;
        double dLat = Math.toRadians(b.lat() - a.lat());
        double dLon = Math.toRadians(b.lon() - a.lon());
        double h = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(a.lat())) * Math.cos(Math.toRadians(b.lat())) * Math.pow(Math.sin(dLon / 2), 2);
        return (int) Math.round(2 * earth * Math.asin(Math.sqrt(h)));
    }

    private static Map<Integer, Stadium> load() {
        ObjectMapper json = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        Map<Integer, Stadium> map = new HashMap<>();
        var in = Stadiums.class.getResourceAsStream("/stadiums.jsonl");
        if (in == null) {
            return Map.of();
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    Stadium s = json.readValue(line, Stadium.class);
                    map.put(s.teamId(), s);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("stadiums.jsonl unreadable", e);
        }
        return Map.copyOf(map);
    }
}
