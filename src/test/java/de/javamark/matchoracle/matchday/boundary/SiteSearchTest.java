package de.javamark.matchoracle.matchday.boundary;

import de.javamark.matchoracle.matchday.boundary.SiteSearch.Hit;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Design r102: the search in the navigation — every word must match, umlauts either way, name starts first. */
class SiteSearchTest {

    private static final Hit BAYERN = Hit.of("Verein", "FC Bayern München", "", "/bl1/teams/1", null, "FC Bayern München", "Bayern");
    private static final Hit LEVERKUSEN = Hit.of("Verein", "Bayer 04 Leverkusen", "", "/bl1/teams/2", null, "Bayer 04 Leverkusen", "Leverkusen");
    private static final Hit KOELN = Hit.of("Verein", "1. FC Köln", "", "/bl1/teams/3", null, "1. FC Köln", "Köln");
    private static final Hit MATCH = Hit.of("Spiel", "Bremen – Köln", "", "/bl1/matches/9", null, "SV Werder Bremen", "Bremen", "1. FC Köln", "Köln");
    private static final Hit KANE = Hit.of("Torjäger", "H. Kane", "", "/bl1/torschuetzen", null, "H. Kane", "FC Bayern München");
    private static final List<Hit> INDEX = List.of(BAYERN, LEVERKUSEN, KOELN, MATCH, KANE);

    @Test
    void everyWordHasToMatch() {
        assertEquals(List.of(MATCH), SiteSearch.find(INDEX, "bremen köln", 8));
    }

    @Test
    void umlautsMatchWithAndWithoutDots() {
        assertEquals(SiteSearch.find(INDEX, "köln", 8), SiteSearch.find(INDEX, "koln", 8));
        assertEquals(SiteSearch.find(INDEX, "köln", 8), SiteSearch.find(INDEX, "koeln", 8));
        assertEquals(List.of(BAYERN, KANE), SiteSearch.find(INDEX, "muenchen", 8));
    }

    @Test
    void aNameStartingWithTheWordComesFirst() {
        // "bay" starts "Bayer" and "Bayern"; the scorer only plays for Bayern, so he comes after both clubs
        assertEquals(List.of(BAYERN, LEVERKUSEN, KANE), SiteSearch.find(INDEX, "bay", 8));
        assertEquals(KANE, SiteSearch.find(INDEX, "kane", 8).getFirst());
    }

    @Test
    void anEmptyQueryFindsNothingAndTheLimitHolds() {
        assertTrue(SiteSearch.find(INDEX, "   ", 8).isEmpty());
        assertEquals(1, SiteSearch.find(INDEX, "bay", 1).size());
    }
}
