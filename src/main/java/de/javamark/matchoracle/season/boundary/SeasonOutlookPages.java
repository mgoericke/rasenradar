package de.javamark.matchoracle.season.boundary;

import de.javamark.matchoracle.matchday.boundary.MatchdayFacade;
import de.javamark.matchoracle.season.entity.SeasonOutlook;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.util.List;
import java.util.Optional;

/** Spec 04, step 5: the fragment the club page (matchday) loads via htmx — matchday never touches this feature's entities. */
@Path("/")
public class SeasonOutlookPages {

    @Inject
    MatchdayFacade matchday;

    @CheckedTemplate
    static class Templates {
        static native TemplateInstance outlook(OutlookFragment fragment);
    }

    public record GoalRow(String label, int percent) {
    }

    public record OutlookFragment(boolean available, List<GoalRow> goals) {
    }

    @GET
    @Path("/{league}/{season}/teams/{id}/outlook")
    @Produces(MediaType.TEXT_HTML)
    @Transactional(Transactional.TxType.SUPPORTS)
    public Response outlook(@PathParam("league") String league, @PathParam("season") int season, @PathParam("id") long id,
                            @HeaderParam("HX-Request") String htmxRequest) {
        // Opened directly (a shared link, a crawler) the bare fragment would arrive without any
        // layout — send anything but an htmx request to the club page that embeds it.
        if (htmxRequest == null) {
            return Response.seeOther(URI.create("/" + league + "/" + season + "/teams/" + id)).build();
        }
        Optional<SeasonOutlook> outlook = SeasonOutlook.findByTeam(league, season, id);
        List<GoalRow> rows = outlook.map(o -> matchday.placementGoals(league).stream()
                .map(label -> new GoalRow(label, (int) Math.round(o.probabilities.getOrDefault(label, 0.0) * 100)))
                .toList()).orElse(List.of());
        return Response.ok(Templates.outlook(new OutlookFragment(outlook.isPresent(), rows))).build();
    }
}
