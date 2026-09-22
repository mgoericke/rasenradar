package de.javamark.matchoracle.matchday.entity;

/**
 * The competitions in scope. Each one runs its own matchday counter, and each one
 * is played in a {@link CompetitionFormat} that decides whether a table is kept.
 */
public enum League {

    BUNDESLIGA_1("bl1", CompetitionFormat.TABLE, 5),
    BUNDESLIGA_2("bl2", CompetitionFormat.TABLE, 5),
    // Spec 06: four editions from 2023/24 — largely the same clubs across the years, so cup
    // balances over several editions actually mean something.
    DFB_POKAL("dfb", CompetitionFormat.KNOCKOUT, 3),
    // Spec 06: only the 2024 and 2026 editions are served under this shortcut; the source
    // renames the competition from edition to edition. Two years back, not one — the
    // competition is played every other year, so 2025 simply has nothing in it.
    NATIONS_LEAGUE("nla", CompetitionFormat.GROUPS, 2),
    // One edition back, not more: the league-phase field turns over so much each year that
    // older seasons give almost no reusable form or head-to-head data, at the cost of
    // importing dozens of clubs that will not play again. The previous edition earns its
    // keep since the knockout phase is shown — without it the competition has no visible
    // conclusion for most of the year, only a league phase in progress.
    CHAMPIONS_LEAGUE("ucl", CompetitionFormat.TABLE, 1);

    /** League shortcut as used by the external data source (OpenLigaDB). */
    private final String sourceShortcut;

    private final CompetitionFormat format;

    /** Past seasons kept in addition to the current one — a property of the competition, not a setting. */
    private final int historySeasons;

    League(String sourceShortcut, CompetitionFormat format, int historySeasons) {
        this.sourceShortcut = sourceShortcut;
        this.format = format;
        this.historySeasons = historySeasons;
    }

    public int historySeasons() {
        return historySeasons;
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
