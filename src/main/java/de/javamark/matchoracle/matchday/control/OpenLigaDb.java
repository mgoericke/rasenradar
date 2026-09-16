package de.javamark.matchoracle.matchday.control;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Conventions of the OpenLigaDB source that are not part of the domain. */
final class OpenLigaDb {

    /** OpenLigaDB reports change timestamps in this zone without saying so. */
    static final ZoneId SOURCE_ZONE = ZoneId.of("Europe/Berlin");

    private OpenLigaDb() {
    }

    static Instant toInstant(LocalDateTime sourceLocalTime) {
        return sourceLocalTime == null ? null : sourceLocalTime.atZone(SOURCE_ZONE).toInstant();
    }
}
