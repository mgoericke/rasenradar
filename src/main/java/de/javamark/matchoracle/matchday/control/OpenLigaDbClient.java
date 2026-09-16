package de.javamark.matchoracle.matchday.control;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST client for OpenLigaDB (https://api.openligadb.de). Base URL is configured
 * via {@code quarkus.rest-client.openligadb.url}.
 * <p>
 * Timestamps other than {@code matchDateTimeUTC} are local time (Europe/Berlin)
 * without zone information; see {@link OpenLigaDb#toInstant(LocalDateTime)}.
 */
@RegisterRestClient(configKey = "openligadb")
@Path("/")
public interface OpenLigaDbClient {

    /** Current matchday of the current season. */
    @GET
    @Path("getmatchdata/{league}")
    List<OpenLigaDbMatch> currentMatchday(@PathParam("league") String league);

    /** All matches of one matchday. */
    @GET
    @Path("getmatchdata/{league}/{season}/{matchday}")
    List<OpenLigaDbMatch> matchday(@PathParam("league") String league,
                                   @PathParam("season") int season,
                                   @PathParam("matchday") int matchday);

    /** All matches of a whole season (used for the initial history import). */
    @GET
    @Path("getmatchdata/{league}/{season}")
    List<OpenLigaDbMatch> season(@PathParam("league") String league,
                                 @PathParam("season") int season);

    /** Teams of a season, with their crests. */
    @GET
    @Path("getavailableteams/{league}/{season}")
    List<OpenLigaDbMatch.Team> teams(@PathParam("league") String league, @PathParam("season") int season);

    /** When anything on this matchday last changed at the source. */
    @GET
    @Path("getlastchangedate/{league}/{season}/{matchday}")
    LocalDateTime lastChange(@PathParam("league") String league,
                             @PathParam("season") int season,
                             @PathParam("matchday") int matchday);
}
