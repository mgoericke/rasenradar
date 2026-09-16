package de.javamark.matchoracle.forecast.control;

import de.javamark.matchoracle.forecast.entity.ForecastParameters;
import de.javamark.matchoracle.matchday.boundary.MatchSituation;
import de.javamark.matchoracle.matchday.boundary.MatchSituation.Balance;
import de.javamark.matchoracle.matchday.boundary.MatchSituation.Meeting;
import de.javamark.matchoracle.matchday.boundary.MatchSituation.Result;
import de.javamark.matchoracle.matchday.boundary.MatchSituation.TeamSituation;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Spec 02, step 2: the known facts of a match, rendered as German text for the
 * agents. Pre-digested numbers, no interpretation — that is the agents' job.
 */
final class FactSheet {

    /** Below this many games, a single outlier result would dominate the whole "form" - too thin to trust. */
    private static final int THIN_FORM_MATCHES = 3;

    private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy, HH:mm", Locale.GERMAN);
    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("d.M.yyyy", Locale.GERMAN);

    private FactSheet() {
    }

    static String render(MatchSituation s) {
        StringBuilder sb = new StringBuilder();
        sb.append("Begegnung: ").append(s.homeTeam().name()).append(" (Heim) gegen ").append(s.awayTeam().name()).append(" (Gast)\n");
        sb.append("Liga: ").append(league(s.league())).append(", Saison ").append(s.season()).append('/').append(s.season() + 1)
                .append(", ").append(s.matchday()).append(". Spieltag\n");
        sb.append("Anstoß: ").append(DATE.format(s.kickoff().atZone(ZONE))).append(" Uhr\n");
        if (s.includesProvisional()) {
            sb.append("Hinweis: Einige der folgenden Ergebnisse sind noch vorläufig.\n");
        }
        sb.append('\n').append(team(s.homeTeam(), s.kickoff()));
        sb.append('\n').append(team(s.awayTeam(), s.kickoff()));
        sb.append("\nDirekte Duelle (aus Sicht von ").append(s.homeTeam().name()).append(", neueste zuerst):\n");
        if (s.previousMeetings().isEmpty()) {
            sb.append("  keine Begegnungen im Datenbestand (laufende und zwei vorangegangene Saisons)\n");
        }
        for (Meeting m : s.previousMeetings()) {
            sb.append("  ").append(SHORT.format(m.kickoff().atZone(ZONE))).append(' ')
                    .append(m.home() ? "Heim" : "Auswärts").append(' ')
                    .append(m.goalsFor()).append(':').append(m.goalsAgainst()).append(' ').append(result(m.result())).append('\n');
        }
        return sb.toString();
    }

    static String parameters(ForecastParameters p) {
        return "Heimvorteil: " + Math.round(p.homeAdvantage * 100) + " Prozentpunkte zugunsten der Heimmannschaft\n"
                + "Formzeitraum: die letzten " + p.formMatches + " Spiele der laufenden Saison\n"
                + "Aufsteiger-Malus: " + Math.round(p.promotedTeamMalus * 100) + " Prozentpunkte für eine aufgestiegene Mannschaft\n";
    }

    private static String team(TeamSituation t, Instant kickoff) {
        StringBuilder sb = new StringBuilder();
        sb.append(t.name()).append(":\n");
        sb.append("  Tabellenplatz: ").append(t.position() == null ? "noch ohne Spiel" : t.position() + ". Platz, " + t.points() + " Punkte").append('\n');
        if (t.promoted()) {
            sb.append("  Aufsteiger: ja (in der Vorsaison nicht in dieser Liga)\n");
        }
        sb.append("  Letzte Spiele (neuestes zuerst):\n");
        if (t.lastMatches().isEmpty()) {
            sb.append("    noch keine Spiele in dieser Saison\n");
        }
        for (Result r : t.lastMatches()) {
            sb.append("    ").append(SHORT.format(r.kickoff().atZone(ZONE))).append(' ')
                    .append(r.home() ? "gegen " : "bei ").append(r.opponent()).append(' ')
                    .append(r.goalsFor()).append(':').append(r.goalsAgainst()).append(' ').append(result(r.result())).append('\n');
        }
        if (!t.lastMatches().isEmpty() && t.lastMatches().size() < THIN_FORM_MATCHES) {
            sb.append("    Achtung: nur ").append(t.lastMatches().size())
                    .append(" Saisonspiel(e) - diese Form ist statistisch kaum belastbar, ein Ausreißer wiegt hier schwer.\n");
        }
        sb.append("  Heimbilanz: ").append(balance(t.homeBalance())).append('\n');
        sb.append("  Auswärtsbilanz: ").append(balance(t.awayBalance())).append('\n');
        sb.append("  Torschnitt zu Hause: ").append(goalAverage(t.homeBalance())).append('\n');
        sb.append("  Torschnitt auswärts: ").append(goalAverage(t.awayBalance())).append('\n');
        t.recentKickoffs().stream().findFirst().ifPresent(last -> {
            long days = Duration.between(last, kickoff).toDays();
            sb.append("  Tage seit dem letzten Spiel: ").append(days).append('\n');
        });
        return sb.toString();
    }

    /** "1.8 erzielt, 0.6 kassiert pro Spiel" — the basis for the expected score. */
    private static String goalAverage(Balance b) {
        if (b.played() == 0) {
            return "noch kein Spiel";
        }
        return String.format(Locale.GERMAN, "%.1f erzielt, %.1f kassiert pro Spiel", (double) b.goalsFor() / b.played(), (double) b.goalsAgainst() / b.played());
    }

    private static String balance(Balance b) {
        return b.played() + " Spiele, " + b.wins() + " Siege, " + b.draws() + " Unentschieden, " + b.losses() + " Niederlagen, Tore "
                + b.goalsFor() + ":" + b.goalsAgainst();
    }

    private static String result(String teamResult) {
        return switch (teamResult) {
            case "WIN" -> "(Sieg)";
            case "LOSS" -> "(Niederlage)";
            default -> "(Unentschieden)";
        };
    }

    private static String league(String shortcut) {
        return "bl1".equals(shortcut) ? "1. Bundesliga" : "2. Bundesliga";
    }
}
