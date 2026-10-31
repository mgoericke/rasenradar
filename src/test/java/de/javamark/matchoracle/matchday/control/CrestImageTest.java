package de.javamark.matchoracle.matchday.control;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Wappen werden selbst ausgeliefert: Rasterbilder klein genug für die Seite, SVG unverändert,
 * alles andere (etwa eine HTML-Fehlerseite des Hosts) gar nicht.
 */
class CrestImageTest {

    @Test
    void aLargeRasterCrestIsScaledDownToAPng() throws IOException {
        CrestImage crest = CrestImage.of(jpeg(500, 400)).orElseThrow();

        BufferedImage stored = ImageIO.read(new ByteArrayInputStream(crest.data()));
        assertEquals("image/png", crest.contentType());
        assertEquals(CrestImage.MAX_EDGE, stored.getWidth());
        assertEquals(154, stored.getHeight());
    }

    @Test
    void aSmallRasterCrestKeepsItsSize() throws IOException {
        BufferedImage stored = ImageIO.read(new ByteArrayInputStream(CrestImage.of(jpeg(64, 64)).orElseThrow().data()));
        assertEquals(64, stored.getWidth());
    }

    @Test
    void anSvgCrestIsKeptAsItIs() {
        byte[] svg = "<?xml version=\"1.0\"?>\n<svg xmlns=\"http://www.w3.org/2000/svg\"/>".getBytes(StandardCharsets.UTF_8);
        CrestImage crest = CrestImage.of(svg).orElseThrow();
        assertEquals("image/svg+xml", crest.contentType());
        assertArrayEquals(svg, crest.data());
    }

    @Test
    void anythingElseIsNoCrest() {
        assertTrue(CrestImage.of("<!DOCTYPE html><html><body>Not found</body></html>".getBytes(StandardCharsets.UTF_8)).isEmpty());
        assertTrue(CrestImage.of(new byte[0]).isEmpty());
        assertTrue(CrestImage.of(new byte[CrestImage.MAX_BYTES + 1]).isEmpty());
    }

    @Test
    void theFileNameChangesWithTheImage() throws IOException {
        CrestImage a = CrestImage.of(jpeg(64, 64)).orElseThrow();
        CrestImage b = CrestImage.of(jpeg(65, 64)).orElseThrow();
        assertTrue(a.fileName().matches("[0-9a-f]{12}\\.png"));
        assertEquals(a.fileName(), CrestImage.of(jpeg(64, 64)).orElseThrow().fileName());
        assertNotEquals(a.fileName(), b.fileName());
    }

    private static byte[] jpeg(int width, int height) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "jpg", out);
        return out.toByteArray();
    }
}
