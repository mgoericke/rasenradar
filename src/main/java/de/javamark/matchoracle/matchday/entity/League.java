package de.javamark.matchoracle.matchday.entity;

/**
 * The competitions in scope. Each one runs its own matchday counter, and each one
 * is played in a {@link CompetitionFormat} that decides whether a table is kept.
 */
public enum League {

    BUNDESLIGA_1("bl1", CompetitionFormat.TABLE),
    BUNDESLIGA_2("bl2", CompetitionFormat.TABLE),
    CHAMPIONS_LEAGUE("ucl", CompetitionFormat.TABLE);

    /** League shortcut as used by the external data source (OpenLigaDB). */
    private final String sourceShortcut;

    private final CompetitionFormat format;

    League(String sourceShortcut, CompetitionFormat format) {
        this.sourceShortcut = sourceShortcut;
        this.format = format;
    }

    public String sourceShortcut() {
        return sourceShortcut;
    }

    public CompetitionFormat format() {
        return format;
    }

    /** Whether this competition keeps a table at all — see {@link CompetitionFormat}. */
    public boolean hasTable() {
        return format.hasTable();
    }

    public static java.util.Optional<League> bySourceShortcut(String shortcut) {
        return java.util.Arrays.stream(values()).filter(l -> l.sourceShortcut.equalsIgnoreCase(shortcut)).findFirst();
    }
}
