package de.javamark.matchoracle.review.control;

import de.javamark.matchoracle.matchday.boundary.MatchSituation;
import de.javamark.matchoracle.matchday.boundary.MatchdayFacade;
import de.javamark.matchoracle.matchday.boundary.ScorelineForecast;
import de.javamark.matchoracle.review.entity.AccuracyReport;
import de.javamark.matchoracle.review.entity.Baseline;
import de.javamark.matchoracle.review.entity.ForecastEvaluation;
import de.javamark.matchoracle.review.entity.Outcome;
import de.javamark.matchoracle.review.entity.RecordedForecast;
import de.javamark.matchoracle.review.entity.Retrospective;
import de.javamark.matchoracle.review.entity.SimilarCase;
import de.javamark.matchoracle.review.entity.Situation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Spec 03: records situations, evaluates forecasts, finds similar cases, reports the hit rate. */
@ApplicationScoped
public class ReviewService {

    private static final Logger LOG = Logger.getLogger(ReviewService.class);

    @Inject
    MatchdayFacade matchday;

    /** Spec 03, interview: fewer similar cases than this yield no retrospective at all. */
    @ConfigProperty(name = "matchoracle.review.min-similar-cases", defaultValue = "3")
    int minSimilarCases;

    @ConfigProperty(name = "matchoracle.review.max-similar-cases", defaultValue = "5")
    int maxSimilarCases;

    /** Spec 03, interview: a hit rate is only reported from this many evaluations per league. */
    @ConfigProperty(name = "matchoracle.review.min-evaluations", defaultValue = "10")
    int minEvaluations;

    /** The form window used for the situation features; kept fixed so situations stay comparable. */
    @ConfigProperty(name = "matchoracle.review.form-matches", defaultValue = "5")
    int formMatches;

    // --- recording ---------------------------------------------------------------

    /** Spec 03, step 3: stores the situation of a completed match once; ignored if already known or not final. */
    @Transactional
    public Optional<Situation> recordSituation(long matchId) {
        Optional<Situation> existing = Situation.findByMatch(matchId);
        if (existing.isPresent()) {
            return existing;
        }
        return matchday.situationOf(matchId, formMatches)
                .filter(MatchSituation::resultFinal)
                .map(s -> {
                    Situation situation = situationFrom(s);
                    situation.persist();
                    return situation;
                });
    }

    /** Bundesliga base rates of recent seasons — the "always home" yardstick's probabilities. */
    static final double LEAGUE_HOME_WIN_RATE = 0.44, LEAGUE_DRAW_RATE = 0.25, LEAGUE_AWAY_WIN_RATE = 0.31;
    /** How many of the most recent evaluations the plain hit/miss timeline shows. */
    static final int RECENT_RESULTS_LIMIT = 12;

    Situation situationFrom(MatchSituation s) {
        Situation situation = new Situation();
        ScorelineForecast sf = matchday.scorelineForecast(s);
        Baseline baseline = new Baseline(sf.homeWin(), sf.draw(), sf.awayWin());
        situation.baselineHomeWin = baseline.homeWin();
        situation.baselineDraw = baseline.draw();
        situation.baselineAwayWin = baseline.awayWin();
        situation.matchId = s.matchId();
        situation.league = s.league();
        situation.season = s.season();
        situation.matchday = s.matchday();
        situation.kickoff = s.kickoff();
        situation.homeTeam = s.homeTeam().name();
        situation.awayTeam = s.awayTeam().name();
        situation.positionGap = s.homeTeam().position() == null || s.awayTeam().position() == null
                ? null : s.homeTeam().position() - s.awayTeam().position();
        situation.homeFormPoints = formPoints(s.homeTeam());
        situation.awayFormPoints = formPoints(s.awayTeam());
        situation.formMatches = s.homeTeam().lastMatches().size();
        situation.homeHomePpg = ppg(s.homeTeam().homeBalance());
        situation.awayAwayPpg = ppg(s.awayTeam().awayBalance());
        situation.homePromoted = s.homeTeam().promoted();
        situation.awayPromoted = s.awayTeam().promoted();
        situation.homeGoals = s.result().home();
        situation.awayGoals = s.result().away();
        return situation;
    }

    /** Called by the forecast feature when a forecast is committed; idempotent. */
    @Transactional
    public void recordForecast(long forecastId, long matchId, String league, int season, int matchdayNumber, Instant createdAt,
                               double homeWin, double draw, double awayWin, double confidence,
                               int expectedHomeGoals, int expectedAwayGoals, boolean backtest) {
        if (RecordedForecast.exists(forecastId)) {
            return;
        }
        RecordedForecast r = new RecordedForecast();
        r.forecastId = forecastId;
        r.matchId = matchId;
        r.league = league;
        r.season = season;
        r.matchday = matchdayNumber;
        r.createdAt = createdAt;
        r.homeWin = homeWin;
        r.draw = draw;
        r.awayWin = awayWin;
        r.confidence = confidence;
        r.expectedHomeGoals = expectedHomeGoals;
        r.expectedAwayGoals = expectedAwayGoals;
        r.backtest = backtest;
        r.persist();
        // a backtest (or a late hand-over) concerns a match whose result may already be final: evaluate right away
        recordSituation(matchId).ifPresent(situation -> evaluate(matchId, situation.homeGoals, situation.awayGoals));
    }

    /** Situations recorded before the baseline existed get theirs from the same facts as of before kickoff. */
    @Transactional
    public int deriveMissingBaselines(int limit) {
        int done = 0;
        for (Situation situation : Situation.findWithoutBaseline(limit)) {
            Optional<MatchSituation> facts = matchday.situationOf(situation.matchId, situation.formMatches);
            if (facts.isEmpty()) {
                continue;
            }
            ScorelineForecast sf = matchday.scorelineForecast(facts.get());
            Baseline baseline = new Baseline(sf.homeWin(), sf.draw(), sf.awayWin());
            situation.baselineHomeWin = baseline.homeWin();
            situation.baselineDraw = baseline.draw();
            situation.baselineAwayWin = baseline.awayWin();
            done++;
        }
        return done;
    }

    // --- evaluating ---------------------------------------------------------------

    /**
     * Spec 02, rule: a match can be forecast again automatically before kickoff — only the last
     * one counts. Backtests are unaffected (a distinct, separately-tracked kind of forecast).
     */
    static List<RecordedForecast> forecastsToEvaluate(List<RecordedForecast> recorded) {
        RecordedForecast latestLive = recorded.stream().filter(r -> !r.backtest)
                .max(Comparator.comparing(r -> r.createdAt)).orElse(null);
        return recorded.stream().filter(r -> r.backtest || r == latestLive).toList();
    }

    /** Spec 03, steps 1-2: on a final result, evaluate every recorded forecast of the match once. */
    @Transactional
    public List<ForecastEvaluation> evaluate(long matchId, int homeGoals, int awayGoals) {
        Outcome actual = Outcome.of(homeGoals, awayGoals);
        List<ForecastEvaluation> evaluations = new ArrayList<>();
        for (RecordedForecast r : forecastsToEvaluate(RecordedForecast.findByMatch(matchId))) {
            if (ForecastEvaluation.exists(r.forecastId)) {
                continue;
            }
            ForecastEvaluation e = new ForecastEvaluation();
            e.forecastId = r.forecastId;
            e.matchId = r.matchId;
            e.league = r.league;
            e.season = r.season;
            e.matchday = r.matchday;
            e.evaluatedAt = Instant.now();
            e.predictedOutcome = r.predictedOutcome();
            e.actualOutcome = actual;
            e.tendencyHit = e.predictedOutcome == actual;
            // the expected score is a guide value (see Forecaster), not a formal prediction - matching it exactly is a bonus
            e.scoreHit = r.expectedHomeGoals == homeGoals && r.expectedAwayGoals == awayGoals;
            e.probabilityOfActual = r.probabilityOf(actual);
            e.brierScore = Evaluation.brier(r.homeWin, r.draw, r.awayWin, actual);
            e.confidence = r.confidence;
            e.backtest = r.backtest;
            e.confidenceVerdict = Evaluation.confidenceVerdict(e.tendencyHit, r.confidence);
            e.persist();
            evaluations.add(e);
        }
        if (!evaluations.isEmpty()) {
            LOG.infof("Evaluated %d forecast(s) for match %d", evaluations.size(), matchId);
        }
        return evaluations;
    }

    // --- retrospective --------------------------------------------------------------

    /** Spec 03, step 4: similar completed matches for an upcoming one; empty when the data is too thin. */
    @Transactional
    public Optional<Retrospective> retrospectiveFor(long matchId) {
        return matchday.situationOf(matchId, formMatches).map(s -> {
            Situation target = situationFrom(withoutResult(s));
            // only matches played before this one: a backtest must not look into the future
            List<SimilarCase> cases = similarCases(target, Situation.findBefore(s.kickoff()));
            return cases.size() < minSimilarCases ? null : new Retrospective(cases, RetrospectiveText.render(cases));
        });
    }

    /** Pure selection: the closest cases below the similarity threshold, at most {@code maxSimilarCases}. */
    List<SimilarCase> similarCases(Situation target, List<Situation> candidates) {
        return candidates.stream()
                .map(c -> new SimilarCase(c, Similarity.distance(target, c), c.league.equals(target.league),
                        c.season == target.season, Optional.empty()))
                .filter(c -> Similarity.similar(c.distance()))
                .sorted(Comparator.comparingDouble(SimilarCase::distance))
                .limit(maxSimilarCases)
                .map(c -> new SimilarCase(c.situation(), c.distance(), c.sameLeague(), c.sameSeason(),
                        ForecastEvaluation.findByMatch(c.situation().matchId).stream().findFirst()))
                .toList();
    }

    // --- accuracy -------------------------------------------------------------------

    /** Spec 03, step 6: hit rate per league, live and backtest apart; null hit rate while fewer than {@code minEvaluations} exist. */
    @Transactional
    public AccuracyReport accuracy(String league, boolean backtest) {
        List<ForecastEvaluation> evaluations = ForecastEvaluation.findByLeague(league, backtest);
        Map<Long, Situation> situations = new HashMap<>();
        for (Situation s : Situation.findByMatchIds(evaluations.stream().map(e -> e.matchId).collect(Collectors.toSet()))) {
            situations.put(s.matchId, s);
        }
        return accuracy(league, backtest, evaluations, situations, minEvaluations);
    }

    /** Oracle, baseline and "always home" measured on the same evaluated matches; the skill score is 1 - Brier / baseline Brier. */
    static AccuracyReport accuracy(String league, boolean backtest, List<ForecastEvaluation> evaluations,
                                   Map<Long, Situation> situations, int required) {
        int hits = (int) evaluations.stream().filter(e -> e.tendencyHit).count();
        boolean reliable = evaluations.size() >= required;
        Map<String, int[]> perMatchday = new LinkedHashMap<>();
        Map<String, List<ForecastEvaluation>> evaluationsByMatchday = new LinkedHashMap<>();
        int baselineEvaluated = 0, baselineHits = 0, alwaysHomeHits = 0;
        double baselineBrier = 0, oracleBrierOnSameMatches = 0, alwaysHomeBrier = 0;
        for (ForecastEvaluation e : evaluations) {
            String key = e.season + "/" + e.matchday;
            int[] counts = perMatchday.computeIfAbsent(key, k -> new int[]{e.season, e.matchday, 0, 0, 0});
            evaluationsByMatchday.computeIfAbsent(key, k -> new ArrayList<>()).add(e);
            counts[2]++;
            if (e.tendencyHit) counts[3]++;
            if (e.actualOutcome == Outcome.HOME_WIN) alwaysHomeHits++;
            alwaysHomeBrier += Evaluation.brier(LEAGUE_HOME_WIN_RATE, LEAGUE_DRAW_RATE, LEAGUE_AWAY_WIN_RATE, e.actualOutcome);
            Optional<Baseline> baseline = Optional.ofNullable(situations.get(e.matchId)).flatMap(Situation::baseline);
            if (baseline.isPresent()) {
                Baseline b = baseline.get();
                baselineEvaluated++;
                oracleBrierOnSameMatches += e.brierScore;
                baselineBrier += Evaluation.brier(b.homeWin(), b.draw(), b.awayWin(), e.actualOutcome);
                if (b.predictedOutcome() == e.actualOutcome) {
                    baselineHits++;
                    counts[4]++;
                }
            }
        }
        List<AccuracyReport.MatchdayPoint> points = perMatchday.entrySet().stream()
                .map(en -> {
                    int[] c = en.getValue();
                    return new AccuracyReport.MatchdayPoint(c[0], c[1], c[2], c[3], c[4],
                            MatchdayRecap.render(evaluationsByMatchday.get(en.getKey()), situations));
                }).toList();
        boolean baselineReliable = baselineEvaluated >= required;
        Double meanBaselineBrier = baselineReliable ? baselineBrier / baselineEvaluated : null;
        return new AccuracyReport(league, backtest, evaluations.size(), hits,
                reliable ? (double) hits / evaluations.size() : null,
                reliable ? evaluations.stream().mapToDouble(e -> e.confidence).average().orElse(0) : null,
                reliable ? evaluations.stream().mapToDouble(e -> e.brierScore).average().orElse(0) : null,
                required, points,
                new AccuracyReport.Comparison(baselineEvaluated, baselineHits,
                        baselineReliable ? (double) baselineHits / baselineEvaluated : null, meanBaselineBrier),
                new AccuracyReport.Comparison(evaluations.size(), alwaysHomeHits,
                        reliable ? (double) alwaysHomeHits / evaluations.size() : null,
                        reliable ? alwaysHomeBrier / evaluations.size() : null),
                baselineReliable && meanBaselineBrier > 0 ? 1 - (oracleBrierOnSameMatches / baselineEvaluated) / meanBaselineBrier : null,
                recent(evaluations, situations), calibration(evaluations, required));
    }

    /** Spec 03: is the stated confidence honest? Three broad confidence bands, each judged like the overall hit rate. */
    static List<AccuracyReport.CalibrationGroup> calibration(List<ForecastEvaluation> evaluations, int required) {
        return List.of(
                calibrationGroup("niedrige Sicherheit (unter 50 %)", evaluations, e -> e.confidence < 0.5, required),
                calibrationGroup("mittlere Sicherheit (50 bis 70 %)", evaluations, e -> e.confidence >= 0.5 && e.confidence < 0.7, required),
                calibrationGroup("hohe Sicherheit (70 % oder mehr)", evaluations, e -> e.confidence >= 0.7, required));
    }

    private static AccuracyReport.CalibrationGroup calibrationGroup(String label, List<ForecastEvaluation> evaluations,
                                                                     java.util.function.Predicate<ForecastEvaluation> inGroup, int required) {
        List<ForecastEvaluation> group = evaluations.stream().filter(inGroup).toList();
        int hits = (int) group.stream().filter(e -> e.tendencyHit).count();
        boolean reliable = group.size() >= required;
        Double averageBrier = reliable ? group.stream().mapToDouble(e -> e.brierScore).average().orElse(0) : null;
        return new AccuracyReport.CalibrationGroup(label,
                new AccuracyReport.Comparison(group.size(), hits, reliable ? (double) hits / group.size() : null, averageBrier));
    }

    /**
     * Spec 03, second view: a plain hit/miss timeline of the most recent evaluations, oldest to
     * newest — no rate, no threshold, just what happened. {@code evaluations} is already
     * chronological (query order); the label names the match where its situation is known.
     */
    private static List<AccuracyReport.RecentResult> recent(List<ForecastEvaluation> evaluations, Map<Long, Situation> situations) {
        int from = Math.max(0, evaluations.size() - RECENT_RESULTS_LIMIT);
        return evaluations.subList(from, evaluations.size()).stream()
                .map(e -> new AccuracyReport.RecentResult(recentLabel(e, situations.get(e.matchId)), level(e)))
                .toList();
    }

    private static AccuracyReport.HitLevel level(ForecastEvaluation e) {
        if (e.scoreHit) return AccuracyReport.HitLevel.EXACT;
        if (e.tendencyHit) return AccuracyReport.HitLevel.TENDENCY;
        return AccuracyReport.HitLevel.MISS;
    }

    private static String recentLabel(ForecastEvaluation e, Situation situation) {
        if (situation != null) {
            return situation.homeTeam + " – " + situation.awayTeam;
        }
        return e.matchday + ". Sp. " + e.season % 100 + "/" + (e.season + 1) % 100;
    }

    // --- helpers --------------------------------------------------------------------

    private static int formPoints(MatchSituation.TeamSituation t) {
        return t.lastMatches().stream().mapToInt(r -> switch (r.result()) {
            case "WIN" -> 3;
            case "DRAW" -> 1;
            default -> 0;
        }).sum();
    }

    private static double ppg(MatchSituation.Balance b) {
        return b.played() == 0 ? 0 : (double) b.points() / b.played();
    }

    /** An upcoming match has no result yet; the feature extraction needs a score object though. */
    private static MatchSituation withoutResult(MatchSituation s) {
        return new MatchSituation(s.matchId(), s.league(), s.season(), s.matchday(), s.kickoff(), s.played(),
                new MatchSituation.Score(0, 0), false, s.homeTeam(), s.awayTeam(), s.previousMeetings(), s.includesProvisional());
    }
}
