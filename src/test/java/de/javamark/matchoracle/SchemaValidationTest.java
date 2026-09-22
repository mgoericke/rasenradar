package de.javamark.matchoracle;

import de.javamark.matchoracle.matchday.entity.Matchday;
import de.javamark.matchoracle.matchday.entity.TeamTier;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Flyway owns the schema and Hibernate only validates it — but until this test nothing in the
 * suite ever started the application, so a migration that had drifted from its entity failed
 * first in dev mode, or would have failed on deploy. Two such mistakes happened while building
 * spec 06: a column the entity expected but the migration did not create, and an id column
 * written as {@code identity} where every other table uses a sequence.
 * <p>
 * Booting once and touching the mappings is enough to catch that class of error.
 */
@QuarkusTest
class SchemaValidationTest {

    @Test
    @Transactional
    void theApplicationStartsAgainstTheMigratedSchema() {
        assertTrue(Matchday.count() >= 0, "the Matchday mapping must match the migrated schema");
        assertTrue(TeamTier.count() >= 0, "the TeamTier mapping must match the migrated schema");
    }
}
