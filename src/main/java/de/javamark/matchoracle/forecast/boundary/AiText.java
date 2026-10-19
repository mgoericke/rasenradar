package de.javamark.matchoracle.forecast.boundary;

import java.util.regex.Pattern;

/**
 * Model output as a fan reads it. Stored forecasts never change (spec 02), so text written
 * before the prompts were tightened is cleaned when shown: markup the model sometimes emits,
 * the outcome constants it echoed from the draft, and the word it used for the confidence.
 */
final class AiText {

    private static final Pattern BREAK = Pattern.compile("(?i)\\s*</?br\\s*/?>\\s*");
    private static final Pattern BULLET = Pattern.compile("^\\s*[-*•]\\s+");

    private AiText() {
    }

    static String forDisplay(String text) {
        if (text == null) {
            return null;
        }
        String t = BREAK.matcher(text).replaceAll(" ");
        t = t.replaceAll("\\bHOME_WIN\\b", "Heimsieg")
                .replaceAll("\\bAWAY_WIN\\b", "Auswärtssieg")
                .replaceAll("\\bDRAW\\b", "Unentschieden")
                .replace("Konfidenz", "Sicherheit")
                // retrospectives stored before the wording changed address the model directly
                .replace("Lies sie als Basisrate", "Sie gelten als Basisrate");
        return t.replaceAll("[ \\t]{2,}", " ").strip();
    }

    /** One line of a retrospective list; the template draws its own bullet, so the model's goes. */
    static String listItem(String line) {
        String t = forDisplay(line);
        return t == null ? null : BULLET.matcher(t).replaceFirst("");
    }
}
