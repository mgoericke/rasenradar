package de.javamark.matchoracle.matchday.entity;

import java.util.List;
import java.util.Map;

/**
 * Spec 06: how a knockout edition's field thins out from round to round, and what kind of
 * edition it was. A table says who is best; this says whether the outsiders had a year.
 */
public record CupFunnel(List<Round> rounds, Figures figures) {

    /** One round: how many clubs played in it, and which divisions they came from. */
    public record Round(String name, int clubs, Map<Tier, Integer> byTier) {

        /** Share of the widest round, so the bars of an edition are comparable with each other. */
        public int percentOf(int widest) {
            return widest == 0 ? 0 : Math.round(100f * clubs / widest);
        }
    }

    /** The edition in numbers — only played matches count. */
    public record Figures(int matches, int goals, int shootouts, int extraTime, int upsets) {
    }

    /** The widest round, i.e. the first one — the reference every bar is drawn against. */
    public int widest() {
        return rounds.stream().mapToInt(Round::clubs).max().orElse(0);
    }
}
