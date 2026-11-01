package de.javamark.matchoracle.matchday.control;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

/**
 * Story r034: Bright Sky (https://brightsky.dev), the open data of the Deutscher Wetterdienst as JSON,
 * no key. Called by the server only, so a visitor's address never reaches it. Base URL and the
 * six-second limit are configured under {@code quarkus.rest-client.brightsky}.
 */
@RegisterRestClient(configKey = "brightsky")
@Path("/")
public interface BrightSkyClient {

    /** Hourly values from {@code date} up to {@code last_date} (both ISO 8601) at a point. */
    @GET
    @Path("weather")
    Answer weather(@QueryParam("lat") double lat, @QueryParam("lon") double lon,
                   @QueryParam("date") String date, @QueryParam("last_date") String lastDate);

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Answer(List<Hour> weather) {
    }

    /** {@code windSpeed} in km/h, {@code precipitationProbability} in percent; either may be missing. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Hour(String timestamp, Double temperature, String condition, String icon,
                @JsonProperty("precipitation_probability") Integer precipitationProbability,
                @JsonProperty("wind_speed") Double windSpeed) {
    }
}
