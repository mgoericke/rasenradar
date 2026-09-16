package de.javamark.matchoracle.matchday.entity;

/**
 * The two leagues in scope. Each league runs its own matchday counter.
 */
public enum League {

    BUNDESLIGA_1("bl1"),
    BUNDESLIGA_2("bl2"),
    CHAMPIONS_LEAGUE("ucl");

    /** League shortcut as used by the external data source (OpenLigaDB). */
    private final String sourceShortcut;

    League(String sourceShortcut) {
        this.sourceShortcut = sourceShortcut;
    }

    public String sourceShortcut() {
        return sourceShortcut;
    }

    public static java.util.Optional<League> bySourceShortcut(String shortcut) {
        return java.util.Arrays.stream(values()).filter(l -> l.sourceShortcut.equalsIgnoreCase(shortcut)).findFirst();
    }
}
