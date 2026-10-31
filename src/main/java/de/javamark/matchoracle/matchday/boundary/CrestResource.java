package de.javamark.matchoracle.matchday.boundary;

import de.javamark.matchoracle.matchday.entity.Team;
import de.javamark.matchoracle.matchday.entity.TeamCrest;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

/**
 * Serves the club crests the application downloaded itself. The file name carries a content
 * hash, so the current one can be cached for good; an outdated name still gets the current
 * crest, but only briefly cached. SVG never runs script: the sandboxing policy applies when a
 * crest is opened on its own, and an {@code <img>} never runs it anyway.
 */
@Path("/crests")
@Transactional(Transactional.TxType.SUPPORTS)
public class CrestResource {

    @GET
    @Path("/{teamId}/{file}")
    public Response crest(@PathParam("teamId") long teamId, @PathParam("file") String file) {
        Team team = Team.findById(teamId);
        TeamCrest crest = TeamCrest.findById(teamId);
        if (team == null || team.crestFile == null || crest == null || crest.data == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        boolean current = team.crestFile.equals(file);
        return Response.ok(crest.data, crest.contentType)
                .header("Cache-Control", current ? "public, max-age=31536000, immutable" : "public, max-age=300")
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; sandbox")
                .build();
    }
}
