package de.javamark.matchoracle.review.boundary;

import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.Nav;
import de.javamark.matchoracle.review.control.ReviewService;
import de.javamark.matchoracle.review.entity.AccuracyReport;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** Spec 03, step 6: the hit rate, publicly visible. */
@Path("/trefferbilanz")
@Produces(MediaType.TEXT_HTML)
public class ReviewPages {

    @CheckedTemplate
    static class Templates {
        static native TemplateInstance accuracy(AccuracyPage page);
        static native TemplateInstance leaderboard(LeaderboardFragment fragment);
    }

    @Inject
    ReviewService review;

    /** One row of the comparison table: who, how often right, how good the probabilities were. */
    record YardstickRow(String label, Integer hitRatePercent, String brier, boolean oracle) {
        static YardstickRow of(String label, AccuracyReport.Comparison c, boolean oracle) {
            return new YardstickRow(label, c.hitRate() == null ? null : (int) Math.round(c.hitRate() * 100),
                    c.averageBrier() == null ? null : String.format("%.2f", c.averageBrier()), oracle);
        }
    }

    /** One confidence band of the calibration breakdown (Spec 03): its own hit rate, or "too thin" below the threshold. */
    /** One matchday's rule-based recap (Spec 03, step 7), with a ready-formatted heading. */
    record RecapRow(String heading, String text) {
        static RecapRow of(AccuracyReport.MatchdayPoint p) {
            return new RecapRow(p.matchday() + ". Spieltag " + p.season() % 100 + "/" + (p.season() + 1) % 100, p.recap());
        }
    }

    record CalibrationRow(String label, int evaluated, Integer hitRatePercent) {
        static CalibrationRow of(AccuracyReport.CalibrationGroup g) {
            AccuracyReport.Comparison c = g.comparison();
            return new CalibrationRow(g.label(), c.evaluated(), c.hitRate() == null ? null : (int) Math.round(c.hitRate() * 100));
        }

        /** Spec 05, "Kalibrierung in Sätzen": plain-language reading of one confidence band. */
        public String sentence(int required) {
            if (hitRatePercent == null) {
                return label + ": die Datenlage reicht noch nicht (" + evaluated + " von " + required + " bewerteten Vorschauen).";
            }
            return "Wenn das System " + label + " ausweist, trifft die Tendenz in " + hitRatePercent + " von 100 Fällen.";
        }
    }

    record LeagueAccuracy(String shortcut, String name, boolean backtest, int evaluated, int hits, Integer hitRatePercent, Integer averageConfidencePercent,
                          String averageBrier, int required, List<AccuracyReport.MatchdayPoint> perMatchday, String chartJson,
                          List<YardstickRow> yardsticks, Integer skillScorePercent, int baselineEvaluated,
                          List<AccuracyReport.RecentResult> recent, int recentHits, int recentTotal,
                          List<CalibrationRow> calibrationGroups, List<RecapRow> recaps,
                          int contrarianEvaluated, Integer contrarianHitRatePercent) {

        /** For the template: the CSS/label class of one recent dot. */
        public String dotClass(AccuracyReport.RecentResult r) {
            return switch (r.level()) {
                case EXACT -> "is-exact";
                case TENDENCY -> "is-tendency";
                case MISS -> "is-miss";
            };
        }

        /** For the tooltip. */
        public String dotTitle(AccuracyReport.RecentResult r) {
            return switch (r.level()) {
                case EXACT -> r.label() + ": Volltreffer, auch das Ergebnis stimmt";
                case TENDENCY -> r.label() + ": Tendenz getroffen, Ergebnis abweichend";
                case MISS -> r.label() + ": Tendenz verfehlt";
            };
        }

        /** The comparison needs the oracle's rate and enough matches with a baseline. */
        public boolean comparable() {
            return hitRatePercent != null && yardsticks.stream().allMatch(y -> y.hitRatePercent() != null);
        }

        /** Worded for the page: ahead of, level with or behind plain statistics. */
        public String skillVerdict() {
            if (skillScorePercent == null) return "";
            if (skillScorePercent >= 5) return "BETTER";
            if (skillScorePercent <= -5) return "WORSE";
            return "LEVEL";
        }

        public String id() {
            return shortcut + (backtest ? "-backtest" : "-live");
        }

        /** Spec 03, step 6: does the stated confidence match the hit rate? Templates cannot do arithmetic, so it is decided here. */
        public String calibration() {
            if (hitRatePercent == null || averageConfidencePercent == null) return "";
            if (averageConfidencePercent > hitRatePercent + 10) return "OVERCONFIDENT";
            if (hitRatePercent > averageConfidencePercent + 10) return "UNDERCONFIDENT";
            return "OK";
        }

        static LeagueAccuracy of(String shortcut, String name, AccuracyReport r) {
            String labels = r.perMatchday().stream().map(p -> "\"" + p.matchday() + ". Sp. " + p.season() % 100 + "/" + (p.season() + 1) % 100 + "\"").collect(Collectors.joining(","));
            String rates = r.perMatchday().stream().map(p -> String.valueOf(Math.round(100.0 * p.hits() / p.evaluated()))).collect(Collectors.joining(","));
            String counts = r.perMatchday().stream().map(p -> String.valueOf(p.evaluated())).collect(Collectors.joining(","));
            String baselineRates = r.perMatchday().stream().map(p -> String.valueOf(Math.round(100.0 * p.baselineHits() / p.evaluated()))).collect(Collectors.joining(","));
            String oracleBrier = r.averageBrier() == null ? null : String.format("%.2f", r.averageBrier());
            List<YardstickRow> yardsticks = List.of(
                    new YardstickRow("KI-Vorschau", r.hitRate() == null ? null : (int) Math.round(r.hitRate() * 100), oracleBrier, true),
                    YardstickRow.of("Basisprognose (Statistik)", r.baseline(), false));
            return new LeagueAccuracy(shortcut, name, r.backtest(), r.evaluated(), r.hits(),
                    r.hitRate() == null ? null : (int) Math.round(r.hitRate() * 100),
                    r.averageConfidence() == null ? null : (int) Math.round(r.averageConfidence() * 100),
                    oracleBrier, r.required(), r.perMatchday(),
                    "{\"labels\":[" + labels + "],\"rates\":[" + rates + "],\"baselineRates\":[" + baselineRates + "],\"counts\":[" + counts + "]}",
                    yardsticks, r.skillScore() == null ? null : (int) Math.round(r.skillScore() * 100), r.baseline().evaluated(),
                    r.recent(), (int) r.recent().stream().filter(x -> x.level() != AccuracyReport.HitLevel.MISS).count(), r.recent().size(),
                    r.calibration().stream().map(CalibrationRow::of).toList(),
                    reversed(r.perMatchday()).stream().map(RecapRow::of).toList(),
                    r.contrarian().evaluated(),
                    r.contrarian().hitRate() == null ? null : (int) Math.round(r.contrarian().hitRate() * 100));
        }
    }

    record AccuracyPage(Nav nav, List<LeagueAccuracy> leagues) {
    }

    /** For the recap list: most recent matchday first, opposite of the chart's chronological order. */
    private static <T> List<T> reversed(List<T> list) {
        List<T> copy = new java.util.ArrayList<>(list);
        java.util.Collections.reverse(copy);
        return copy;
    }

    @GET
    public TemplateInstance accuracy() {
        return Templates.accuracy(new AccuracyPage(Nav.page("accuracy"), List.of(
                LeagueAccuracy.of("bl1", "1. Bundesliga", review.accuracy("bl1", false)),
                LeagueAccuracy.of("bl2", "2. Bundesliga", review.accuracy("bl2", false)),
                LeagueAccuracy.of("bl1", "1. Bundesliga", review.accuracy("bl1", true)),
                LeagueAccuracy.of("bl2", "2. Bundesliga", review.accuracy("bl2", true)))));
    }

    /** One row of the leaderboard: who, and how often they got the tendency right. */
    record LeaderboardRow(String label, boolean oracle, int hitRatePercent) {
    }

    /** Spec 05, "Wer liegt vorne?": the fragment matchday's home page loads via htmx — matchday never touches this feature. */
    record LeaderboardFragment(boolean reliable, int evaluated, int required, List<LeaderboardRow> rows) {
        static LeaderboardFragment of(AccuracyReport r) {
            boolean comparable = r.hitRate() != null && r.baseline().hitRate() != null;
            if (!comparable) {
                return new LeaderboardFragment(false, r.evaluated(), r.required(), List.of());
            }
            List<LeaderboardRow> rows = new java.util.ArrayList<>(List.of(
                    new LeaderboardRow("KI-Vorschau", true, (int) Math.round(r.hitRate() * 100)),
                    new LeaderboardRow("Basisprognose (Statistik)", false, (int) Math.round(r.baseline().hitRate() * 100))));
            rows.sort(Comparator.comparingInt(LeaderboardRow::hitRatePercent).reversed());
            return new LeaderboardFragment(true, r.evaluated(), r.required(), rows);
        }
    }

    /**
     * Loaded via htmx from matchday's home page — never called directly by matchday's Java code.
     * Opened directly (a shared link, a crawler) the bare fragment would arrive without any layout,
     * so anything but an htmx request is sent to the page that shows the same numbers in full.
     */
    @GET
    @Path("/leaderboard")
    public Response leaderboard(@HeaderParam("HX-Request") String htmxRequest) {
        if (htmxRequest == null) {
            return Response.seeOther(URI.create("/trefferbilanz")).build();
        }
        return Response.ok(Templates.leaderboard(LeaderboardFragment.of(review.combinedAccuracy()))).build();
    }
}
