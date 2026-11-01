package de.javamark.matchoracle.matchday.boundary;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

/**
 * Design r102: the search in the navigation — clubs, the start page's matches and the scorers of both
 * Bundesligas. Every word of the query has to appear; umlauts match with and without dots ("munchen",
 * "muenchen" and "München" find the same club). A hit whose name starts with the first word comes first,
 * otherwise the order of the index stands: clubs, then matches, then scorers.
 */
final class SiteSearch {

    /** One thing to find: {@code kind} is "Verein", "Spiel" or "Torjäger", {@code keys} what is searched. */
    record Hit(String kind, String label, String hint, String link, String icon, String keys) {
        static Hit of(String kind, String label, String hint, String link, String icon, String... words) {
            return new Hit(kind, label, hint, link, icon, SiteSearch.keys(String.join(" ", words)));
        }
    }

    private SiteSearch() {
    }

    static List<Hit> find(List<Hit> index, String query, int limit) {
        List<String> words = query == null ? List.of()
                : Arrays.stream(fold(query).split("\\s+")).filter(w -> !w.isEmpty()).toList();
        if (words.isEmpty()) {
            return List.of();
        }
        String first = words.getFirst();
        return IntStream.range(0, index.size()).boxed()
                .filter(i -> words.stream().allMatch(index.get(i).keys()::contains))
                .sorted(Comparator.comparingInt((Integer i) -> startsWith(index.get(i).label(), first) ? 0 : 1)
                        .thenComparingInt(i -> i))
                .limit(limit)
                .map(index::get)
                .toList();
    }

    /** A name word that starts with the first query word, e.g. "bay" in "FC Bayern München". */
    private static boolean startsWith(String label, String word) {
        return Arrays.stream(fold(label).split("[\\s–-]+")).anyMatch(w -> w.startsWith(word));
    }

    /** What is searched: the words without dots and accents, and once more with umlauts spelt out. */
    static String keys(String text) {
        String spelt = text.toLowerCase(Locale.GERMAN).replace("ä", "ae").replace("ö", "oe").replace("ü", "ue");
        return fold(text) + " " + fold(spelt);
    }

    static String fold(String text) {
        return Normalizer.normalize(text.toLowerCase(Locale.GERMAN), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replace("ß", "ss");
    }
}
