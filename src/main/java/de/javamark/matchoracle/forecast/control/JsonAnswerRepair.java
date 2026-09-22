package de.javamark.matchoracle.forecast.control;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;

import java.util.regex.Pattern;

/**
 * Salvages one specific way the hosted model breaks a JSON answer: it closes the German
 * summary with a typographic quote instead of {@code "}. Under schema-constrained decoding
 * the string is then still open, the model cannot stop and pads with invisible characters
 * until the token limit. The repair is deliberately narrow — invisible characters removed,
 * a typographic quote right before a closing bracket or comma turned into {@code "}, and
 * whatever follows the first complete JSON value cut off — and only applied when the
 * original does not parse.
 */
final class JsonAnswerRepair {

    private static final Logger LOG = Logger.getLogger(JsonAnswerRepair.class);
    private static final ObjectMapper JSON = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    private static final Pattern INVISIBLE = Pattern.compile("[\\u200B-\\u200D\\uFEFF]");
    private static final Pattern TYPOGRAPHIC_CLOSING_QUOTE = Pattern.compile("[\\u201C\\u201D\\u201E]\\s*(?=[}\\],])");

    private JsonAnswerRepair() {
    }

    /** The text itself if it parses (or cannot be helped), otherwise the repaired JSON. */
    static String repair(String text) {
        if (text == null || parses(text)) {
            return text;
        }
        String candidate = INVISIBLE.matcher(text).replaceAll("");
        candidate = TYPOGRAPHIC_CLOSING_QUOTE.matcher(candidate).replaceAll("\"");
        for (int end = candidate.indexOf('}'); end >= 0; end = candidate.indexOf('}', end + 1)) {
            String prefix = candidate.substring(0, end + 1);
            if (parses(prefix)) {
                LOG.warnf("Repaired a broken JSON answer (%d chars cut, typographic quote closed): %s", text.length() - prefix.length(), prefix);
                return prefix;
            }
        }
        return text;
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
