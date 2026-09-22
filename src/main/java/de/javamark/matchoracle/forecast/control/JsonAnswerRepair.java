package de.javamark.matchoracle.forecast.control;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Salvages one specific way the hosted model breaks a JSON answer: it ends the German
 * summary with a typographic quote instead of {@code "}. Under schema-constrained decoding
 * the string is then still open, the model cannot stop and pads with invisible characters
 * — or drifts into new text — until the token limit. The repair is deliberately narrow:
 * cut at a typographic quote, close the string and whatever brackets are still open, and
 * take the first cut that parses. Only applied when the original does not parse.
 */
final class JsonAnswerRepair {

    private static final Logger LOG = Logger.getLogger(JsonAnswerRepair.class);
    private static final ObjectMapper JSON = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    private static final String TYPOGRAPHIC_QUOTES = "“”„";

    private JsonAnswerRepair() {
    }

    /** The text itself if it parses (or cannot be helped), otherwise the repaired JSON. */
    static String repair(String text) {
        if (text == null || parses(text)) {
            return text;
        }
        for (int cut = 0; cut < text.length(); cut++) {
            if (TYPOGRAPHIC_QUOTES.indexOf(text.charAt(cut)) < 0) {
                continue;
            }
            String candidate = closed(text.substring(0, cut));
            if (candidate != null && parses(candidate)) {
                LOG.warnf("Repaired a broken JSON answer (%d chars dropped after a typographic quote): %s", text.length() - cut, candidate);
                return candidate;
            }
        }
        return text;
    }

    /** The prefix with its open string closed and its open brackets closed, or null if the cut is not inside a string. */
    private static String closed(String prefix) {
        Deque<Character> open = new ArrayDeque<>();
        boolean inString = false;
        for (int i = 0; i < prefix.length(); i++) {
            char c = prefix.charAt(i);
            if (inString) {
                if (c == '\\') i++;
                else if (c == '"') inString = false;
            } else if (c == '"') {
                inString = true;
            } else if (c == '{' || c == '[') {
                open.push(c);
            } else if (c == '}' || c == ']') {
                open.poll();
            }
        }
        if (!inString) {
            return null;
        }
        StringBuilder sb = new StringBuilder(prefix).append('"');
        while (!open.isEmpty()) {
            sb.append(open.pop() == '{' ? '}' : ']');
        }
        return sb.toString();
    }

    private static boolean parses(String text) {
        try {
            JSON.readTree(text);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
