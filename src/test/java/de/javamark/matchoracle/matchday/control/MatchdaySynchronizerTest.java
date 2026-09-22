package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.entity.CompetitionFormat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Spec 06: derselbe Abschnittsname der Quelle bedeutet je Form etwas anderes — im Pokal
 * ist er der Name der Runde, im Gruppenwettbewerb die Gruppe, in einer Liga nichts.
 */
class MatchdaySynchronizerTest {

    @Test
    void inAKnockoutCompetitionTheSectionNameIsTheRoundsLabel() {
        assertEquals("Achtelfinale", MatchdaySynchronizer.labelOf(CompetitionFormat.KNOCKOUT, "Achtelfinale"));
        assertNull(MatchdaySynchronizer.groupOf(CompetitionFormat.KNOCKOUT, "Achtelfinale"));
    }

    @Test
    void inAGroupCompetitionTheSectionNameIsTheGroup() {
        assertEquals("Gruppe B", MatchdaySynchronizer.groupOf(CompetitionFormat.GROUPS, "Gruppe B"));
        assertNull(MatchdaySynchronizer.labelOf(CompetitionFormat.GROUPS, "Gruppe B"));
    }

    @Test
    void inALeagueTheSectionNameIsNotUsedAtAll() {
        assertNull(MatchdaySynchronizer.labelOf(CompetitionFormat.TABLE, "1. Spieltag"));
        assertNull(MatchdaySynchronizer.groupOf(CompetitionFormat.TABLE, "1. Spieltag"));
    }

    @Test
    void aGroupCompetitionCanAlsoHaveKnockoutRounds() {
        // Spec 06: die Nations League spielt erst Gruppen, dann eine Endrunde. Jeder
        // Abschnitt entscheidet fuer sich, was er ist.
        assertEquals("Gruppe B", MatchdaySynchronizer.groupOf(CompetitionFormat.GROUPS, "Gruppe B"));
        assertNull(MatchdaySynchronizer.labelOf(CompetitionFormat.GROUPS, "Gruppe B"));

        assertNull(MatchdaySynchronizer.groupOf(CompetitionFormat.GROUPS, "Halbfinale Hinspiele"));
        assertEquals("Halbfinale Hinspiele", MatchdaySynchronizer.labelOf(CompetitionFormat.GROUPS, "Halbfinale Hinspiele"));
    }

    @Test
    void aSectionWithoutANameIsNeitherGroupNorRound() {
        assertNull(MatchdaySynchronizer.groupOf(CompetitionFormat.GROUPS, null));
        assertNull(MatchdaySynchronizer.labelOf(CompetitionFormat.GROUPS, null));
    }

    @Test
    void aTableCompetitionCanEndInKnockoutRounds() {
        // Spec 06: die Champions League spielt erst eine Ligaphase, dann eine K.-o.-Phase.
        // Ein Abschnitt, der ein Spieltag ist, bleibt einer; alles andere ist eine Runde.
        assertNull(MatchdaySynchronizer.labelOf(CompetitionFormat.TABLE, "7. Spieltag"));
        assertNull(MatchdaySynchronizer.labelOf(CompetitionFormat.TABLE, "1.Spieltag"));

        assertEquals("Viertelfinale Hinspiele", MatchdaySynchronizer.labelOf(CompetitionFormat.TABLE, "Viertelfinale Hinspiele"));
        assertEquals("Finale", MatchdaySynchronizer.labelOf(CompetitionFormat.TABLE, "Finale"));
        assertEquals("Playoffs", MatchdaySynchronizer.labelOf(CompetitionFormat.TABLE, "Playoffs"));
    }

    @Test
    void aTableCompetitionWithoutSectionNamesKeepsCountingMatchdays() {
        // Die beiden Bundesligen liefern keinen brauchbaren Abschnittsnamen.
        assertNull(MatchdaySynchronizer.labelOf(CompetitionFormat.TABLE, null));
        assertNull(MatchdaySynchronizer.groupOf(CompetitionFormat.TABLE, null));
    }
}
