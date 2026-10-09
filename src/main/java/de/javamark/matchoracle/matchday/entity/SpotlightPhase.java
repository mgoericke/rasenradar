package de.javamark.matchoracle.matchday.entity;

/** Spec 01, "Blickfang der Startseite": which question the landing page's spotlight answers right now. */
public enum SpotlightPhase {
    /** Nothing is running: the next block, with the KI-Vorschau's guide value. */
    PREVIEW,
    /** At least one match is running: every running match of both leagues, without a score. */
    LIVE
}
