package de.javamark.matchoracle.review.boundary;

import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.Nav;
import de.javamark.matchoracle.review.control.ReviewService;
import de.javamark.matchoracle.review.entity.AccuracyReport;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.stream.Collectors;

/** Spec 03, step 6: the hit rate, publicly visible. */
@Path("/trefferbilanz")
@Produces(MediaType.TEXT_HTML)
public class ReviewPages {

    @CheckedTemplate
    static class Templates {
        static native TemplateInstance accuracy(AccuracyPage page);
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

    record LeagueAccuracy(String shortcut, String name, boolean backtest, int evaluated, int hits, Integer hitRatePercent, Integer averageConfidencePercent,
                          String averageBrier, int required, List<AccuracyReport.MatchdayPoint> perMatchday, String chartJson,
                          List<YardstickRow> yardsticks, Integer skillScorePercent, int baselineEvaluated,
                          List<AccuracyReport.RecentResult> recent, int recentHits, int recentTotal) {

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
                    YardstickRow.of("Basisprognose (Statistik)", r.baseline(), false),
                    YardstickRow.of("Immer Heimsieg", r.alwaysHome(), false));
            return new LeagueAccuracy(shortcut, name, r.backtest(), r.evaluated(), r.hits(),
                    r.hitRate() == null ? null : (int) Math.round(r.hitRate() * 100),
                    r.averageConfidence() == null ? null : (int) Math.round(r.averageConfidence() * 100),
                    oracleBrier, r.required(), r.perMatchday(),
                    "{\"labels\":[" + labels + "],\"rates\":[" + rates + "],\"baselineRates\":[" + baselineRates + "],\"counts\":[" + counts + "]}",
                    yardsticks, r.skillScore() == null ? null : (int) Math.round(r.skillScore() * 100), r.baseline().evaluated(),
                    r.recent(), (int) r.recent().stream().filter(x -> x.level() != AccuracyReport.HitLevel.MISS).count(), r.recent().size());
        }
    }

    record AccuracyPage(Nav nav, List<LeagueAccuracy> leagues) {
    }

    @GET
    public TemplateInstance accuracy() {
        return Templates.accuracy(new AccuracyPage(Nav.page("accuracy"), List.of(
                LeagueAccuracy.of("bl1", "1. Bundesliga", review.accuracy("bl1", false)),
                LeagueAccuracy.of("bl2", "2. Bundesliga", review.accuracy("bl2", false)),
                LeagueAccuracy.of("bl1", "1. Bundesliga", review.accuracy("bl1", true)),
                LeagueAccuracy.of("bl2", "2. Bundesliga", review.accuracy("bl2", true)))));
    }
}
