package de.javamark.matchoracle.matchday.control;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

/**
 * A club crest ready to be served by the application. Raster images (PNG, JPEG, GIF) are scaled
 * down to {@link #MAX_EDGE} pixels and re-encoded as PNG, which also drops any metadata. SVG is
 * kept as it is; it is only ever served with a sandboxing Content-Security-Policy. Anything else
 * is not a crest.
 */
public record CrestImage(String contentType, String extension, byte[] data) {

    /** The largest crest on a page is 5.5rem (88 CSS px); twice that covers high-density screens. */
    static final int MAX_EDGE = 192;
    static final int MAX_BYTES = 2_000_000;

    public static Optional<CrestImage> of(byte[] raw) {
        if (raw == null || raw.length == 0 || raw.length > MAX_BYTES) {
            return Optional.empty();
        }
        if (isSvg(raw)) {
            return Optional.of(new CrestImage("image/svg+xml", "svg", raw));
        }
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(raw));
            if (image == null) {
                return Optional.empty();
            }
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            ImageIO.write(scaledDown(image), "png", png);
            return Optional.of(new CrestImage("image/png", "png", png.toByteArray()));
        } catch (IOException | RuntimeException e) {
            return Optional.empty();
        }
    }

    /** The file name pages link to: changes whenever the image does, so it can be cached for good. */
    public String fileName() {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(data);
            return HexFormat.of().formatHex(hash, 0, 6) + "." + extension;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    static boolean isSvg(byte[] raw) {
        String head = new String(raw, 0, Math.min(raw.length, 1024), StandardCharsets.UTF_8)
                .replace("﻿", "").stripLeading();
        boolean xmlStart = head.startsWith("<?xml") || head.startsWith("<svg") || head.startsWith("<!--") || head.startsWith("<!DOCTYPE svg");
        return xmlStart && new String(raw, StandardCharsets.UTF_8).contains("<svg");
    }

    private static BufferedImage scaledDown(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage current = toArgb(image);
        // halve step by step: a single big bicubic step leaves crests visibly jagged
        while (Math.max(width, height) > MAX_EDGE) {
            double factor = Math.max(0.5, (double) MAX_EDGE / Math.max(width, height));
            width = Math.max(1, (int) Math.round(width * factor));
            height = Math.max(1, (int) Math.round(height * factor));
            BufferedImage next = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = next.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(current, 0, 0, width, height, null);
            g.dispose();
            current = next;
        }
        return current;
    }

    private static BufferedImage toArgb(BufferedImage image) {
        if (image.getType() == BufferedImage.TYPE_INT_ARGB) {
            return image;
        }
        BufferedImage argb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = argb.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return argb;
    }
}
