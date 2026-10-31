package de.javamark.matchoracle.forecast.boundary;

import io.quarkus.qute.TemplateExtension;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Story r099: the small, fixed vocabulary of the KI pages (spec 05) explained in one plain sentence
 * each — the one place for these texts. Templates mark a term with {@code {#term key="…"}…{/term}};
 * the explainer page lists all of them, so every explanation is reachable without script.
 */
public final class Glossary {

    public record Entry(String key, String term, String explanation) {
        public String anchor() {
            return "begriff-" + key;
        }
    }

    private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();

    static {
        add("tendenz", "Tendenz",
                "Der Ausgang, den die KI für am wahrscheinlichsten hält – Heimsieg, Unentschieden oder Auswärtssieg –, unabhängig vom genauen Ergebnis.");
        add("sicherheit", "Sicherheit",
                "Wie wahrscheinlich die KI ihre eigene Tendenz hält, in Prozent. 44 % heißt: etwas wahrscheinlicher als jede der beiden anderen – sicher ist das nicht.");
        add("trefferquote", "Trefferquote",
                "Der Anteil der Vorschauen, deren Tendenz eingetroffen ist. Es zählen nur endgültige Ergebnisse.");
        add("reine-statistik", "Reine Statistik",
                "Die Basisprognose: eine Rechnung aus Torschnitt und Heimvorteil, ohne KI. An ihr zeigt sich, ob die KI mehr kann als einfache Statistik.");
        add("direkte-duelle", "Direkte Duelle",
                "Die bisherigen Spiele der beiden Mannschaften gegeneinander, soweit sie in den Spieldaten stehen.");
        add("umfeld", "Umfeld",
                "Was rund um ein Spiel in den Spieldaten steht: Tabellenlage, Heim- und Auswärtsbilanz. Aufstellungen oder Verletzte kennt das System nicht.");
    }

    private Glossary() {
    }

    private static void add(String key, String term, String explanation) {
        ENTRIES.put(key, new Entry(key, term, explanation));
    }

    /** All terms in a fixed order, for the explainer page. */
    public static Iterable<Entry> entries() {
        return ENTRIES.values();
    }

    /** {@code {glossary:explain('sicherheit')}} — fails loudly on an unknown key, so a typo never ships silently. */
    @TemplateExtension(namespace = "glossary")
    static String explain(String key) {
        Entry e = ENTRIES.get(key);
        if (e == null) {
            throw new IllegalArgumentException("unknown glossary term: " + key);
        }
        return e.explanation();
    }

    @TemplateExtension(namespace = "glossary")
    static Iterable<Entry> all() {
        return entries();
    }
}
