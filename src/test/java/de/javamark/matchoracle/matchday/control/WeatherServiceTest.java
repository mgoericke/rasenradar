package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.control.BrightSkyClient.Hour;
import de.javamark.matchoracle.matchday.control.WeatherService.Weather;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.Score;
import de.javamark.matchoracle.matchday.entity.Team;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Story r034: what the weather line says, and for which matches there is one. */
class WeatherServiceTest {

    @Test
    void rainAndWindAreNamedFromThirtyOn() {
        Weather dry = WeatherService.of(new Hour("t", -2.4, "dry", "cloudy", 29, 29.4));
        assertEquals(-2, dry.temperature());
        assertEquals("bewölkt", dry.sky());
        assertNull(dry.rain());
        assertNull(dry.wind());

        Weather wet = WeatherService.of(new Hour("t", 8.6, "rain", "rain", 30, 30.0));
        assertEquals("Regen", wet.sky());
        assertEquals(30, wet.rain());
        assertEquals(30, wet.wind());
    }

    @Test
    void precipitationGoesBeforeTheCloudCover() {
        assertEquals("Schnee", WeatherService.sky("snow", "cloudy"));
        assertEquals("klar", WeatherService.sky("dry", "clear-night"));
        assertEquals("trocken", WeatherService.sky(null, null));
    }

    @Test
    void onlyAMatchAheadWithinTenDaysAtAKnownGroundGetsWeather() {
        Instant now = Instant.parse("2026-11-01T12:00:00Z");
        Team bayern = new Team();
        bayern.externalId = 40;
        Team nowhere = new Team();
        nowhere.externalId = -1;
        Match soon = new Match();
        soon.homeTeam = bayern;
        soon.kickoff = now.plus(Duration.ofDays(3));
        assertTrue(WeatherService.shown(soon, now));

        Match far = new Match();
        far.homeTeam = bayern;
        far.kickoff = now.plus(Duration.ofDays(11));
        assertFalse(WeatherService.shown(far, now));

        Match running = new Match();
        running.homeTeam = bayern;
        running.kickoff = now.minus(Duration.ofMinutes(30));
        assertFalse(WeatherService.shown(running, now));

        Match played = new Match();
        played.homeTeam = bayern;
        played.kickoff = now.plus(Duration.ofDays(1));
        played.fullTimeScore = new Score(1, 1);
        assertFalse(WeatherService.shown(played, now));

        Match unknownGround = new Match();
        unknownGround.homeTeam = nowhere;
        unknownGround.kickoff = now.plus(Duration.ofDays(1));
        assertFalse(WeatherService.shown(unknownGround, now));
    }
}
