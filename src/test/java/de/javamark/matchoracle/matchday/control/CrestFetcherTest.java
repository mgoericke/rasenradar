package de.javamark.matchoracle.matchday.control;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Die Quelle nennt Wappen-Adressen, die nur ein Browser bekommt; der Abruf fragt passend nach. */
class CrestFetcherTest {

    @Test
    void aWikimediaThumbnailIsFetchedInTheStandardWidth() {
        assertEquals("https://upload.wikimedia.org/wikipedia/commons/thumb/2/2f/VfR_Aalen_Wappen.svg/250px-VfR_Aalen_Wappen.svg.png",
                CrestFetcher.downloadUrl("http://upload.wikimedia.org/wikipedia/commons/thumb/2/2f/VfR_Aalen_Wappen.svg/150px-VfR_Aalen_Wappen.svg.png?utm_source=x"));
    }

    @Test
    void aWikimediaOriginalAndOtherHostsStayAsTheyAre() {
        String original = "https://upload.wikimedia.org/wikipedia/commons/b/be/SV-Werder-Bremen-Logo.svg";
        assertEquals(original, CrestFetcher.downloadUrl(original));
        assertEquals("https://i.imgur.com/v0tkpNx.png", CrestFetcher.downloadUrl("https://i.imgur.com/v0tkpNx.png"));
    }

    @Test
    void kickerIsAskedForPngInsteadOfWebp() {
        assertEquals("https://derivates.kicker.de/image/fetch/f_png/w_60/https://mediadb.kicker.de/x.png",
                CrestFetcher.downloadUrl("https://derivates.kicker.de/image/fetch/f_webp/w_60/https://mediadb.kicker.de/x.png"));
    }
}
