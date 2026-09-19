package de.javamark.matchoracle.review.control;

import de.javamark.matchoracle.review.entity.ForecastEvaluation;
import de.javamark.matchoracle.review.entity.Situation;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Spec 03, step 7: a rule-based, text-only recap of one completed matchday — no agent, only already-recorded numbers. */
final class MatchdayRecap {

    private MatchdayRecap() {
    }

    static String render(List<ForecastEvaluation> evaluations, Map<Long, Situation> situations) {
        int tendencyHits = (int) evaluations.stream().filter(e -> e.tendencyHit).count();
        int exactHits = (int) evaluations.stream().filter(e -> e.scoreHit).count();
        int averageConfidencePercent = (int) Math.round(evaluations.stream().mapToDouble(e -> e.confidence).average().orElse(0) * 100);
        ForecastEvaluation biggestMiss = evaluations.stream().max(Comparator.comparingDouble(e -> e.brierScore)).orElseThrow();

        StringBuilder text = new StringBuilder()
                .append(tendencyHits).append(" von ").append(evaluations.size()).append(" Tendenzen getroffen");
        if (exactHits > 0) {
            text.append(", ").append(exactHits).append(exactHits == 1 ? " genaues Ergebnis" : " genaue Ergebnisse");
        }
        text.append(", durchschnittliche Sicherheit ").append(averageConfidencePercent).append(" %.");

        Situation s = situations.get(biggestMiss.matchId);
        if (s != null) {
            text.append(" Am deutlichsten daneben lag die Prognose zu ")
                    .append(s.homeTeam).append(' ').append(s.homeGoals).append(':').append(s.awayGoals).append(' ').append(s.awayTeam).append('.');
        }
        return text.toString();
    }
}
