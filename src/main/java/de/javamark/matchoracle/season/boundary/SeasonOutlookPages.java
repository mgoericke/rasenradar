package de.javamark.matchoracle.season.boundary;

import de.javamark.matchoracle.matchday.boundary.MatchdayFacade;
import de.javamark.matchoracle.season.control.SeasonTargets;
import de.javamark.matchoracle.season.entity.SeasonOutlook;
import de.javamark.matchoracle.season.entity.SeasonThreshold;
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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Spec 04, step 5: the fragment the club page (matchday) loads via htmx — matchday never touches this feature's entities. */
@Path("/")
public class SeasonOutlookPages {

    @Inject
    MatchdayFacade matchday;

    @CheckedTemplate
    static class Templates {
        static native TemplateInstance outlook(OutlookFragment fragment);

        static native TemplateInstance targets(TargetsFragment fragment);
    }

    public record GoalRow(String label, int percent) {
    }

    public record OutlookFragment(boolean available, List<GoalRow> goals) {
    }

    /** Story r039: one goal's line, the rates already written the German way ("1,8"). */
    public record TargetRow(String label, int typicalPoints, int missing, int remaining, String required,
                            String current, int percent, String status) {

        /** "Es fehlen 9 Punkte aus 14 Spielen", in the singular where it is one. */
        public String gap() {
            return (missing == 1 ? "Es fehlt 1 Punkt" : "Es fehlen " + missing + " Punkte")
                    + (remaining == 1 ? " aus dem letzten Spiel" : " aus " + remaining + " Spielen");
        }
    }

    public record TargetsFragment(boolean available, List<TargetRow> targets) {
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

    /** Story r039: "Was braucht {Verein}?" — read from the stored outlook run, never simulated here. */
    @GET
    @Path("/{league}/{season}/teams/{id}/outlook/targets")
    @Produces(MediaType.TEXT_HTML)
    @Transactional(Transactional.TxType.SUPPORTS)
    public Response targets(@PathParam("league") String league, @PathParam("season") int season, @PathParam("id") long id,
                            @HeaderParam("HX-Request") String htmxRequest) {
        if (htmxRequest == null) {
            return Response.seeOther(URI.create("/" + league + "/" + season + "/teams/" + id)).build();
        }
        List<SeasonOutlook> run = SeasonOutlook.findByLeagueSeason(league, season);
        Map<String, Double> probabilities = run.stream().filter(o -> o.teamId == id).findFirst()
                .map(o -> o.probabilities).orElse(Map.of());
        // rows from before the story carry no points yet — treat the run as missing until it is redone
        List<SeasonTargets.Club> clubs = run.stream().anyMatch(o -> o.points == null) ? List.of()
                : run.stream().map(o -> new SeasonTargets.Club(o.teamId, o.points, o.remaining)).toList();
        List<TargetRow> rows = SeasonTargets.of(SeasonTargets.goals(league), id, clubs,
                        SeasonThreshold.byPosition(league, season), probabilities)
                .map(targets -> targets.stream().map(SeasonOutlookPages::row).toList())
                .orElse(List.of());
        return Response.ok(Templates.targets(new TargetsFragment(!rows.isEmpty(), rows))).build();
    }

    private static TargetRow row(SeasonTargets.Target t) {
        return new TargetRow(t.label(), t.typicalPoints(), t.missing(), t.remaining(), rate(t.requiredPerMatch()),
                rate(t.currentPerMatch()), t.percent(), t.status().name().toLowerCase(Locale.ROOT).replace('_', '-'));
    }

    private static String rate(double perMatch) {
        return String.format(Locale.GERMANY, "%.1f", perMatch);
    }
}
