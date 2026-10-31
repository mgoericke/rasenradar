package de.javamark.matchoracle.matchday.boundary;

import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.CrossCell;
import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.CrossTable;
import de.javamark.matchoracle.matchday.control.HomeAdvantageCalculator;
import de.javamark.matchoracle.matchday.entity.League;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.Matchday;
import de.javamark.matchoracle.matchday.entity.ResultStatus;
import de.javamark.matchoracle.matchday.entity.Score;
import de.javamark.matchoracle.matchday.entity.Team;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Story r072 "Kreuztabelle": rows are home teams, columns away teams; open, provisional and the diagonal. */
class CrossTableTest {

    private static long ids = 1;

    private static Team team(String name) {
        Team t = new Team();
        t.id = ids++;
        t.name = name;
        t.shortName = name;
        return t;
    }

    private static Match match(Team home, Team away, Integer h, Integer a, ResultStatus status) {
        Matchday md = new Matchday();
        md.league = League.BUNDESLIGA_1;
        md.season = 2026;
        md.number = 1;
        Match m = new Match();
        m.id = ids++;
        m.matchday = md;
        m.homeTeam = home;
        m.awayTeam = away;
        m.kickoff = Instant.parse("2026-09-01T18:30:00Z");
        m.fullTimeScore = h == null ? null : new Score(h, a);
        m.resultStatus = status;
        return m;
    }

    @Test
    void rowsAreHomeTeamsAndOnlyFinalResultsCountInTheLead() {
        Team bremen = team("Bremen"), koeln = team("Köln"), kiel = team("Kiel");
        List<Match> matches = List.of(
                match(bremen, koeln, 2, 1, ResultStatus.FINAL),
                match(koeln, bremen, 0, 3, ResultStatus.PROVISIONAL),
                match(kiel, bremen, null, null, ResultStatus.PROVISIONAL));

        CrossTable x = CrossTable.of(List.of(bremen, koeln, kiel), matches, "bl1", HomeAdvantageCalculator.of(matches));

        CrossCell bremenKoeln = x.rows().get(0).get(1);
        assertEquals("home", bremenKoeln.kind());
        assertEquals("2:1", bremenKoeln.score());
        CrossCell koelnBremen = x.rows().get(1).get(0);
        assertEquals("away", koelnBremen.kind());
        assertTrue(koelnBremen.provisional());
        assertEquals("self", x.rows().get(2).get(2).kind());
        CrossCell kielBremen = x.rows().get(2).get(0);
        assertEquals("open", kielBremen.kind());
        assertTrue(kielBremen.label().endsWith("noch offen"));
        assertNull(kielBremen.score());
        assertEquals("open", x.rows().get(0).get(2).kind()); // not even scheduled in the data
        assertEquals("Bisher 1 Spiel: 100 % Heimsiege, 0 % Remis, 0 % Auswärtssiege.", x.lead());
    }
}
