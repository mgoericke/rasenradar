package de.javamark.matchoracle.matchday.boundary;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/** A deploy that only changes a script must still get a new version, or Cloudflare keeps the old one. */
class AssetVersionTest {

    private static final Pattern VERSIONED_LINK = Pattern.compile("/([\\w.-]+)\\?v=\\{assetVersion}");

    @Test
    void everyFileTheTemplatesVersionIsPartOfTheHash() throws IOException {
        Set<String> linked = new TreeSet<>();
        try (Stream<Path> templates = Files.walk(Path.of("src/main/resources/templates"))) {
            for (Path template : templates.filter(Files::isRegularFile).toList()) {
                Matcher m = VERSIONED_LINK.matcher(Files.readString(template));
                while (m.find()) {
                    linked.add(m.group(1));
                }
            }
        }
        assertEquals(linked, new TreeSet<>(AssetVersion.VERSIONED));
    }

    @Test
    void aChangeInAScriptAloneChangesTheVersion() {
        String all = AssetVersion.of(AssetVersion.VERSIONED);
        assertEquals(all, AssetVersion.of(AssetVersion.VERSIONED));
        assertNotEquals(all, AssetVersion.of(List.of("app.css")));
    }
}
