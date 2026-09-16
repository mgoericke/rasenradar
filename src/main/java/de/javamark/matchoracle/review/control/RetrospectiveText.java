package de.javamark.matchoracle.review.control;

import de.javamark.matchoracle.review.entity.ForecastEvaluation;
import de.javamark.matchoracle.review.entity.Outcome;
import de.javamark.matchoracle.review.entity.SimilarCase;

import java.util.List;

/** The retrospective as German text for the forecaster (spec 03, step 4). */
final class RetrospectiveText {

    private RetrospectiveText() {
    }

    static String render(List<SimilarCase> cases) {
        long homeWins = cases.stream().filter(c -> c.situation().outcome() == Outcome.HOME_WIN).count();
        long draws = cases.stream().filter(c -> c.situation().outcome() == Outcome.DRAW).count();
        long awayWins = cases.size() - homeWins - draws;
        StringBuilder sb = new StringBuilder();
        sb.append("Vergleichsfälle sind Begegnungen mit ähnlicher Ausgangslage (Tabellenabstand, Form beider Mannschaften, ")
                .append("Heim- und Auswärtsstärke, Aufsteiger-Status) – unabhängig von den beteiligten Vereinen. ")
                .append("Lies sie als Basisrate für diese Konstellation, nicht als Aussage über die Vereine.\n");
        sb.append(cases.size()).append(" vergleichbare frühere Begegnungen: ")
                .append(homeWins).append(" Heimsiege, ").append(draws).append(" Unentschieden, ").append(awayWins).append(" Auswärtssiege.\n");
        for (SimilarCase c : cases) {
            var s = c.situation();
            sb.append("- ").append(s.homeTeam).append(" ").append(s.homeGoals).append(":").append(s.awayGoals).append(" ").append(s.awayTeam)
                    .append(" (").append(s.season).append("/").append(s.season + 1).append(", ").append(s.matchday).append(". Spieltag");
            if (!c.sameLeague()) sb.append(", andere Liga");
            sb.append(")");
            c.evaluation().ifPresent(e -> sb.append(" – damalige Prognose: ").append(describe(e)));
            sb.append('\n');
        }
        long evaluated = cases.stream().filter(c -> c.evaluation().isPresent()).count();
        if (evaluated == 0) {
            sb.append("Zu diesen Fällen gab es noch keine eigene Prognose.\n");
        } else {
            long hits = cases.stream().filter(c -> c.evaluation().map(e -> e.tendencyHit).orElse(false)).count();
            long over = cases.stream().filter(c -> c.evaluation().map(e -> e.confidenceVerdict.name().equals("OVERCONFIDENT")).orElse(false)).count();
            sb.append("Eigene Prognosen zu diesen Fällen: ").append(hits).append(" von ").append(evaluated).append(" Tendenzen getroffen");
            if (over > 0) sb.append(", ").append(over).append("-mal zu sicher");
            sb.append(".\n");
        }
        return sb.toString();
    }

    private static String describe(ForecastEvaluation e) {
        return outcome(e.predictedOutcome) + (e.tendencyHit ? " (getroffen)" : " (verfehlt)")
                + ", Sicherheit " + Math.round(e.confidence * 100) + " %"
                + switch (e.confidenceVerdict) {
                    case OVERCONFIDENT -> " – zu sicher";
                    case UNDERCONFIDENT -> " – zu vorsichtig";
                    case APPROPRIATE -> "";
                };
    }

    private static String outcome(Outcome o) {
        return switch (o) {
            case HOME_WIN -> "Heimsieg";
            case DRAW -> "Unentschieden";
            case AWAY_WIN -> "Auswärtssieg";
        };
    }
}
