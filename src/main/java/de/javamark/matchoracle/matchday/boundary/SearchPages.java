package de.javamark.matchoracle.matchday.boundary;

import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.Nav;
import de.javamark.matchoracle.matchday.boundary.SiteSearch.Hit;
import de.javamark.matchoracle.matchday.control.ScorerCalculator;
import de.javamark.matchoracle.matchday.entity.League;
import de.javamark.matchoracle.matchday.entity.LeagueScorer;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.Matchday;
import de.javamark.matchoracle.matchday.entity.Team;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Design r102: the search in the navigation. {@code /suche} is a page of its own, so the field works
 * without script; with script the field asks {@code /suche/treffer} while typing and shows the hits
 * below itself.
 */
@Path("/suche")
@Produces(MediaType.TEXT_HTML)
@Transactional(Transactional.TxType.SUPPORTS)
public class SearchPages {

    private static final List<League> LEAGUES = List.of(League.BUNDESLIGA_1, League.BUNDESLIGA_2);
    /** Counting every scorer of a season reads all its goals; the index is rebuilt at most this often. */
    private static final Duration INDEX_AGE = Duration.ofMinutes(10);
    private static final int FIELD_HITS = 8;
    private static final int PAGE_HITS = 40;

    @CheckedTemplate
    static class Templates {
        static native TemplateInstance search(SearchPage page);
        static native TemplateInstance hits(List<Hit> hits, String query);
    }

    record SearchPage(Nav nav, String query, List<Hit> hits) {
    }

    @Inject
    ScorerCalculator scorerCalculator;

    private volatile List<Hit> index;
    private volatile Instant indexedAt = Instant.EPOCH;

    @GET
    public TemplateInstance page(@QueryParam("q") String query) {
        String q = query == null ? "" : query.strip();
        return Templates.search(new SearchPage(Nav.page("search"), q, SiteSearch.find(index(), q, PAGE_HITS)));
    }

    @GET
    @Path("/treffer")
    public Response hits(@QueryParam("q") String query, @HeaderParam("HX-Request") String htmxRequest) {
        String q = query == null ? "" : query.strip();
        if (htmxRequest == null) {
            return Response.seeOther(UriBuilder.fromPath("/suche").queryParam("q", q).build()).build();
        }
        return Response.ok(Templates.hits(SiteSearch.find(index(), q, FIELD_HITS), q).render())
                .header("Cache-Control", "no-store").build();
    }

    private List<Hit> index() {
        Instant now = Instant.now();
        if (index == null || indexedAt.plus(INDEX_AGE).isBefore(now)) {
            index = build(now);
            indexedAt = now;
        }
        return index;
    }

    /** Clubs of both Bundesligas, the start page's matches, then the scorers of both and of the cup — the order hits keep. */
    private List<Hit> build(Instant now) {
        List<Hit> clubs = new ArrayList<>();
        List<Hit> scorers = new ArrayList<>();
        Map<String, String> crests = new HashMap<>();
        for (League l : LEAGUES) {
            String shortcut = l.sourceShortcut();
            String leagueName = MatchdayPageModels.leagueName(l);
            Matchday.latestSeason(l).ifPresent(season -> {
                for (Team t : Team.findInLeagueSeason(l, season)) {
                    String crest = MatchdayPageModels.crestLink(t);
                    crests.put(t.name, crest);
                    clubs.add(Hit.of("Verein", t.name, leagueName + " · Vereinsseite",
                            MatchdayPageModels.teamLink(t, shortcut), crest, t.name, nullToEmpty(t.shortName)));
                }
                for (LeagueScorer s : scorerCalculator.leagueScorersFor(l, season)) {
                    scorers.add(Hit.of("Torjäger", s.name(), s.team() + " · " + s.goals() + (s.goals() == 1 ? " Tor" : " Tore"),
                            "/" + shortcut + "/torschuetzen", crests.get(s.team()), s.name(), s.team()));
                }
            });
        }
        // the cup's scorers after the leagues' — a Bundesliga club's crest where there is one
        Matchday.latestSeason(League.DFB_POKAL).ifPresent(season -> {
            for (LeagueScorer s : scorerCalculator.leagueScorersFor(League.DFB_POKAL, season)) {
                scorers.add(Hit.of("Torjäger", s.name(), s.team() + " · DFB-Pokal · " + s.goals() + (s.goals() == 1 ? " Tor" : " Tore"),
                        "/dfb/torschuetzen", crests.get(s.team()), s.name(), s.team(), "Pokal"));
            }
        });
        clubs.sort(Comparator.comparing(Hit::label));
        List<Hit> matches = MatchdayPages.startPageMatches(now).stream()
                .sorted(Comparator.comparing((Match m) -> m.kickoff).thenComparing(m -> m.id))
                .map(m -> {
                    String shortcut = m.matchday.league.sourceShortcut();
                    String when = MatchdayPageModels.dayLabel(m.kickoff) + ", " + MatchdayPageModels.time(m.kickoff) + " Uhr";
                    String score = m.fullTimeScore == null ? "" : " · " + m.fullTimeScore.home + ":" + m.fullTimeScore.away;
                    return Hit.of("Spiel", m.homeTeam.shortName + " – " + m.awayTeam.shortName, when + score,
                            "/" + shortcut + "/matches/" + m.id, MatchdayPageModels.crestLink(m.homeTeam),
                            m.homeTeam.name, nullToEmpty(m.homeTeam.shortName), m.awayTeam.name, nullToEmpty(m.awayTeam.shortName));
                }).toList();
        List<Hit> all = new ArrayList<>(clubs);
        all.addAll(matches);
        all.addAll(scorers);
        return List.copyOf(all);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
