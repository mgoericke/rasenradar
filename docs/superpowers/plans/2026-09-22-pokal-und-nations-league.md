# Pokal und Nations League — Implementierungsplan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** DFB-Pokal (K.-o., Runden) und Nations League (vier parallele Gruppen) als reine Anzeigewettbewerbe aufnehmen, ohne je Wettbewerb einen neuen Sonderfall zu pflegen.

**Architecture:** Jede `League` bekommt eine `CompetitionFormat` (TABLE, GROUPS, KNOCKOUT). Daran hängt, ob eine Tabelle geführt wird, wie ein Abschnitt heißt und wie synchronisiert wird. `Matchday` bekommt zwei optionale Felder: `groupName` (nur GROUPS) und `label` (nur KNOCKOUT). Die Eindeutigkeit wird von `(league, season, number)` auf `(league, season, group_name, number)` geweitet. Die Quelle kennt für die Nations League keine Spieltagsnummer — sie wird aus dem Kalender abgeleitet, in einer eigenen, rein funktionalen Klasse.

**Tech Stack:** Java 21, Quarkus 3.39.3, Hibernate ORM mit Panache (Active Record), Flyway, PostgreSQL, Qute-Templates, JUnit 5, ArchUnit.

**Spec:** `docs/specs/spec-06-pokal-und-nations-league.md`

## Global Constraints

- **Sprache im Code englisch**, Specs deutsch, UI-Texte deutsch. Neue Fachbegriffe stehen bereits im Glossar in `CLAUDE.md` (Wettbewerbsform → `CompetitionFormat`, Runde → `Matchday.label`, Gruppe → `Matchday.groupName`).
- **Java 21** — keine Sprachfeatures darüber hinaus, auch wenn lokal ein neueres JDK läuft.
- **BCE-Architektur**, von ArchUnit erzwungen: `boundary` → `control`/`entity`, `control` → `entity`, `entity` kennt keine der beiden. Kein Zugriff in fremde Feature-Packages. Alles in diesem Plan bleibt im Package `matchday`, außer der ausdrücklich benannten Änderung in `season/boundary`.
- **Panache Active Record** — Finder als `static` Methoden auf der Entity, keine Repository-Klassen.
- **Flyway** besitzt das Schema, Hibernate validiert nur. Freie Migrationen der Reihe nach: `V13` (Task 2), `V14` (Task 5), `V15` (Task 8).
- **Tests nur für fachlich relevante Features** — keine Tests für Getter, triviale Mapper oder Framework-Verhalten.
- **Kein `matchoracle.forecast`-Bezug**: Pokal und Nations League erzeugen keine KI-Vorschau, keine Rückschau, keine Saisonaussicht.
- **Styleguide** (`docs/styleguide.md`): heller Grund, genau eine dunkle Bühne, Grün/Rot nur als Bedeutung. Messing (`--accent`) ist der KI-Vorschau vorbehalten und darf in diesen beiden Wettbewerben **nicht** auftauchen.
- **Branch:** `feature/cup-and-nations-league` (existiert bereits, Spec ist darauf committet).
- Testlauf: `./mvnw test`. Einzeln: `./mvnw test -Dtest=KlassenName`. Auf Port 8080 läuft ein fremder Prozess — die Anwendung niemals dort starten; für manuelle Prüfung `-Dquarkus.http.port=8081`.

---

### Task 1: Wettbewerbsform einführen

Die Form ist zunächst nur eine Eigenschaft der drei bestehenden Ligen. Noch keine neuen Ligen, noch keine Verhaltensänderung — dieser Schnitt hält die Änderung klein und prüfbar.

**Files:**
- Create: `src/main/java/de/javamark/matchoracle/matchday/entity/CompetitionFormat.java`
- Modify: `src/main/java/de/javamark/matchoracle/matchday/entity/League.java`
- Test: `src/test/java/de/javamark/matchoracle/matchday/entity/CompetitionFormatTest.java`

**Interfaces:**
- Consumes: nichts
- Produces: `CompetitionFormat.TABLE|GROUPS|KNOCKOUT` mit `boolean hasTable()`; `League.format()` liefert die Form; `League.hasTable()` als Abkürzung für `format().hasTable()`.

- [ ] **Step 1: Write the failing test**

`src/test/java/de/javamark/matchoracle/matchday/entity/CompetitionFormatTest.java`:

```java
package de.javamark.matchoracle.matchday.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Spec 06, Regeln: die Form entscheidet, ob ein Wettbewerb eine Tabelle führt. */
class CompetitionFormatTest {

    @Test
    void onlyKnockoutCompetitionsHaveNoTable() {
        assertTrue(CompetitionFormat.TABLE.hasTable());
        assertTrue(CompetitionFormat.GROUPS.hasTable());
        assertFalse(CompetitionFormat.KNOCKOUT.hasTable());
    }

    @Test
    void theExistingLeaguesAreTableCompetitions() {
        assertTrue(League.BUNDESLIGA_1.hasTable());
        assertTrue(League.BUNDESLIGA_2.hasTable());
        assertTrue(League.CHAMPIONS_LEAGUE.hasTable());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=CompetitionFormatTest`
Expected: Kompilierfehler — `CompetitionFormat` existiert nicht, `League.hasTable()` existiert nicht.

- [ ] **Step 3: Write minimal implementation**

`CompetitionFormat.java`:

```java
package de.javamark.matchoracle.matchday.entity;

/**
 * Spec 06: how a competition is played. The format decides whether a table is kept
 * and how a section of the competition is named — instead of a special case per league.
 */
public enum CompetitionFormat {

    /** One table over the whole competition: the two Bundesligas, the Champions League phase. */
    TABLE,
    /** Several groups side by side, each with its own table and its own matchday counter. */
    GROUPS,
    /** Rounds until the final, no table at all. */
    KNOCKOUT;

    public boolean hasTable() {
        return this != KNOCKOUT;
    }
}
```

In `League.java` das Format ergänzen (bestehende Konstanten, Konstruktor, Getter):

```java
    BUNDESLIGA_1("bl1", CompetitionFormat.TABLE),
    BUNDESLIGA_2("bl2", CompetitionFormat.TABLE),
    CHAMPIONS_LEAGUE("ucl", CompetitionFormat.TABLE);

    private final String sourceShortcut;
    private final CompetitionFormat format;

    League(String sourceShortcut, CompetitionFormat format) {
        this.sourceShortcut = sourceShortcut;
        this.format = format;
    }

    public CompetitionFormat format() {
        return format;
    }

    /** Whether this competition keeps a table at all — see {@link CompetitionFormat}. */
    public boolean hasTable() {
        return format.hasTable();
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=CompetitionFormatTest`
Expected: PASS, 2 Tests.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/de/javamark/matchoracle/matchday/entity/CompetitionFormat.java \
        src/main/java/de/javamark/matchoracle/matchday/entity/League.java \
        src/test/java/de/javamark/matchoracle/matchday/entity/CompetitionFormatTest.java
git commit -m "feat(matchday): Wettbewerbsform als Eigenschaft der Liga

Spec 06: statt je Wettbewerb einen Sonderfall zu pflegen, traegt jede Liga
eine Form (Tabelle, Gruppen, K.-o.). Vorerst ohne Verhaltensaenderung — alle
drei bestehenden Ligen sind Tabellenwettbewerbe.

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 2: Gruppe und Rundenname am Spieltag

**Files:**
- Modify: `src/main/java/de/javamark/matchoracle/matchday/entity/Matchday.java`
- Create: `src/main/resources/db/migration/V13__matchday_group_and_label.sql`
- Test: `src/test/java/de/javamark/matchoracle/matchday/entity/MatchdayTest.java` (ergänzen)

**Interfaces:**
- Consumes: `League.format()` aus Task 1
- Produces: Felder `Matchday.groupName` (String, nullable) und `Matchday.label` (String, nullable); `Matchday.find(League, int season, String groupName, int number)`; die bisherige `find(League, int, int)` bleibt als Kurzform für `groupName == null`; `Matchday.displayName()` liefert `label`, sonst `number + ". Spieltag"`.

- [ ] **Step 1: Write the failing test**

An `MatchdayTest.java` anhängen (Imports oben ergänzen: `static org.junit.jupiter.api.Assertions.assertEquals`):

```java
    @Test
    void aKnockoutMatchdayIsNamedAfterItsRound() {
        Matchday round = new Matchday();
        round.league = League.BUNDESLIGA_1;
        round.season = 2026;
        round.number = 3;
        round.label = "Achtelfinale";

        assertEquals("Achtelfinale", round.displayName());
    }

    @Test
    void aMatchdayWithoutALabelIsCountedAsUsual() {
        Matchday plain = new Matchday();
        plain.league = League.BUNDESLIGA_1;
        plain.season = 2026;
        plain.number = 3;

        assertEquals("3. Spieltag", plain.displayName());
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=MatchdayTest`
Expected: Kompilierfehler — `label` und `displayName()` existieren nicht.

- [ ] **Step 3: Write minimal implementation**

In `Matchday.java` die Klassenannotation weiten und die Felder ergänzen:

```java
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"league", "season", "groupName", "number"}))
public class Matchday extends PanacheEntity {
```

Nach dem Feld `number`:

```java
    /**
     * Spec 06: the parallel group this matchday belongs to ("Gruppe A"), null in every
     * competition that is not played in groups. Part of the matchday's identity — in a
     * group competition each group counts its own matchdays.
     */
    @Column(length = 40)
    public String groupName;

    /** Spec 06: name of the round in a knockout competition ("Achtelfinale"), null elsewhere. */
    @Column(length = 40)
    public String label;
```

Und die beiden Methoden (die vorhandene `find` ersetzen, die alte Signatur als Kurzform behalten):

```java
    /** How this section of the competition is called: the round's name, or the counted matchday. */
    public String displayName() {
        return label != null ? label : number + ". Spieltag";
    }

    public static Optional<Matchday> find(League league, int season, int number) {
        return find(league, season, null, number);
    }

    public static Optional<Matchday> find(League league, int season, String groupName, int number) {
        return find("league = ?1 and season = ?2 and groupName is not distinct from ?3 and number = ?4",
                league, season, groupName, number).firstResultOptional();
    }
```

`V13__matchday_group_and_label.sql`:

```sql
-- Spec 06: a matchday can belong to a parallel group (Nations League) and can carry the
-- name of a knockout round (DFB-Pokal). The group is part of the matchday's identity:
-- in a group competition every group counts its own matchdays, so (league, season, number)
-- alone is no longer unique.
alter table matchday add column group_name varchar(40);
alter table matchday add column label varchar(40);

alter table matchday drop constraint if exists uk_matchday_league_season_number;
drop index if exists uk_matchday_league_season_number;
```

> Der Name des bestehenden Unique-Constraints wurde von Hibernate vergeben. Vor dem Schreiben der Migration den tatsächlichen Namen ermitteln und im Skript einsetzen:
> ```bash
> docker exec -i $(docker ps --filter name=quarkus-devservices-match-oracle --format '{{.Names}}' | head -1) \
>   psql -U matchoracle -d matchoracle -c "\d matchday"
> ```
> Anschließend den neuen Constraint anlegen, mit `coalesce`, weil Postgres `null` in Unique-Constraints nicht als gleich behandelt:
> ```sql
> create unique index uk_matchday_section on matchday (league, season, coalesce(group_name, ''), number);
> ```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=MatchdayTest`
Expected: PASS. Danach `./mvnw test` komplett — die Migration läuft gegen die Dev-Datenbank, Hibernate validiert das Schema.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/de/javamark/matchoracle/matchday/entity/Matchday.java \
        src/main/resources/db/migration/V13__matchday_group_and_label.sql \
        src/test/java/de/javamark/matchoracle/matchday/entity/MatchdayTest.java
git commit -m "feat(matchday): Gruppe und Rundenname am Spieltag

Spec 06: ein Spieltag kann zu einer parallelen Gruppe gehoeren und den Namen
einer K.-o.-Runde tragen. Die Gruppe gehoert zur Identitaet des Spieltags,
die Eindeutigkeit wird entsprechend geweitet.

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 3: Spieltage einer Gruppe aus dem Kalender ableiten

Die Quelle liefert für die Nations League nur die Gruppe, keine Spieltagsnummer (geprüft an `getmatchdata/nla/2026`). Sie wird abgeleitet — als reine Funktion, damit sie prüfbar bleibt und nicht im Synchronizer versickert.

**Files:**
- Create: `src/main/java/de/javamark/matchoracle/matchday/control/GroupMatchdays.java`
- Modify: `src/main/java/de/javamark/matchoracle/matchday/control/OpenLigaDbMatch.java` (Record `Group` um `name`)
- Test: `src/test/java/de/javamark/matchoracle/matchday/control/GroupMatchdaysTest.java`

**Interfaces:**
- Consumes: `OpenLigaDbMatch.kickoff()` — bereits ein `Instant`, kein Umrechnen nötig.
- Produces: `OpenLigaDbMatch.Group` trägt zusätzlich `name()` (in der Quelle `groupName`); `static Map<Integer, List<OpenLigaDbMatch>> GroupMatchdays.byMatchday(List<OpenLigaDbMatch> matchesOfOneGroup)` — Schlüssel ist die abgeleitete Spieltagsnummer ab 1, Reihenfolge aufsteigend.

> Der Gruppenname gehört hierher und nicht erst in Task 4: ohne ihn lässt sich eine Gruppe nicht benennen, und Task 4 baut darauf auf. In der Quelle heißt das Feld `groupName`; im Pokal trägt dasselbe Feld den Namen der Runde.

- [ ] **Step 1: Write the failing test**

```java
package de.javamark.matchoracle.matchday.control;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spec 06, Regeln: die Quelle nennt fuer einen Gruppenwettbewerb nur die Gruppe.
 * Begegnungen derselben Gruppe am selben Kalendertag bilden einen Spieltag.
 */
class GroupMatchdaysTest {

    @Test
    void matchesOnTheSameDayFormOneMatchday() {
        List<OpenLigaDbMatch> group = List.of(
                match("2026-09-25T18:45:00Z"), match("2026-09-25T20:45:00Z"),
                match("2026-09-28T20:45:00Z"), match("2026-09-28T18:45:00Z"));

        Map<Integer, List<OpenLigaDbMatch>> byMatchday = GroupMatchdays.byMatchday(group);

        assertEquals(2, byMatchday.size());
        assertEquals(2, byMatchday.get(1).size());
        assertEquals(2, byMatchday.get(2).size());
    }

    @Test
    void theEarliestDayIsTheFirstMatchdayRegardlessOfInputOrder() {
        List<OpenLigaDbMatch> group = List.of(
                match("2026-11-15T20:45:00Z"), match("2026-09-25T18:45:00Z"), match("2026-10-02T20:45:00Z"));

        Map<Integer, List<OpenLigaDbMatch>> byMatchday = GroupMatchdays.byMatchday(group);

        assertEquals(List.of(1, 2, 3), List.copyOf(byMatchday.keySet()));
        assertEquals(Instant.parse("2026-09-25T18:45:00Z"), byMatchday.get(1).get(0).kickoff());
        assertEquals(Instant.parse("2026-11-15T20:45:00Z"), byMatchday.get(3).get(0).kickoff());
    }

    @Test
    void aWholeNationsLeagueGroupYieldsSixMatchdaysOfTwoMatches() {
        List<OpenLigaDbMatch> group = List.of(
                match("2026-09-25T18:45:00Z"), match("2026-09-25T20:45:00Z"),
                match("2026-09-28T18:45:00Z"), match("2026-09-28T20:45:00Z"),
                match("2026-10-02T18:45:00Z"), match("2026-10-02T20:45:00Z"),
                match("2026-10-05T18:45:00Z"), match("2026-10-05T20:45:00Z"),
                match("2026-11-12T18:45:00Z"), match("2026-11-12T20:45:00Z"),
                match("2026-11-15T18:45:00Z"), match("2026-11-15T20:45:00Z"));

        Map<Integer, List<OpenLigaDbMatch>> byMatchday = GroupMatchdays.byMatchday(group);

        assertEquals(6, byMatchday.size());
        assertTrue(byMatchday.values().stream().allMatch(day -> day.size() == 2));
    }

    @Test
    void anEmptyGroupYieldsNoMatchdays() {
        assertTrue(GroupMatchdays.byMatchday(List.of()).isEmpty());
    }

    /** Only the kickoff matters here; every other component of the source record stays null/empty. */
    private static OpenLigaDbMatch match(String kickoff) {
        return new OpenLigaDbMatch(0, 2026, Instant.parse(kickoff), null, true,
                new OpenLigaDbMatch.Group(1, "Gruppe A"), null, null, List.of(), List.of());
    }
}
```

Die Komponenten von `OpenLigaDbMatch` in dieser Reihenfolge: `id, season, kickoff, lastUpdate, finished, group, team1, team2, matchResults, goals`. Die bestehende `OpenLigaDbMatchTest` baut ihre Testdaten genauso direkt — dem Muster folgen, keinen ObjectMapper einführen.

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=GroupMatchdaysTest`
Expected: Kompilierfehler — `GroupMatchdays` existiert nicht.

- [ ] **Step 3: Write minimal implementation**

In `OpenLigaDbMatch.java` zuerst den Gruppennamen ergänzen:

```java
    /** In a cup this same field carries the name of the round ("Achtelfinale"). */
    public record Group(@JsonProperty("groupOrderID") int number, @JsonProperty("groupName") String name) {
    }
```

Die bestehende Verwendung `new OpenLigaDbMatch.Group(...)` in `OpenLigaDbMatchTest` übergibt heute `null` als Gruppe und bleibt unberührt.

Dann `GroupMatchdays.java`:

```java
package de.javamark.matchoracle.matchday.control;

import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Spec 06: a group competition's source data names the group but not the matchday.
 * Matches of the same group on the same calendar day form one matchday, the order of
 * days gives the count. Kept separate from the synchroniser so the derivation — the one
 * place where this project adds something the source does not provide — stays testable.
 */
final class GroupMatchdays {

    private static final ZoneId MATCH_ZONE = ZoneId.of("Europe/Berlin");

    private GroupMatchdays() {
    }

    /** The matches of ONE group, keyed by derived matchday number starting at 1, in ascending order. */
    static Map<Integer, List<OpenLigaDbMatch>> byMatchday(List<OpenLigaDbMatch> matchesOfOneGroup) {
        Map<java.time.LocalDate, List<OpenLigaDbMatch>> byDay = matchesOfOneGroup.stream()
                .collect(Collectors.groupingBy(
                        m -> m.kickoff().atZone(MATCH_ZONE).toLocalDate(),
                        TreeMap::new, Collectors.toList()));
        Map<Integer, List<OpenLigaDbMatch>> byMatchday = new LinkedHashMap<>();
        int number = 1;
        for (List<OpenLigaDbMatch> day : byDay.values()) {
            byMatchday.put(number++, List.copyOf(day));
        }
        return byMatchday;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=GroupMatchdaysTest`
Expected: PASS, 4 Tests.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/de/javamark/matchoracle/matchday/control/GroupMatchdays.java \
        src/main/java/de/javamark/matchoracle/matchday/control/OpenLigaDbMatch.java \
        src/test/java/de/javamark/matchoracle/matchday/control/GroupMatchdaysTest.java
git commit -m "feat(matchday): Spieltage einer Gruppe aus dem Kalender ableiten

Spec 06: die Quelle nennt fuer einen Gruppenwettbewerb nur die Gruppe, keine
Spieltagsnummer. Begegnungen derselben Gruppe am selben Kalendertag bilden
einen Spieltag. Bewusst eine eigene, reine Funktion — es ist die einzige
Stelle, an der wir der Quelle etwas hinzufuegen.

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 4: Synchronisation nach Wettbewerbsform

Ein Abschnitt der Quelle (`getmatchdata/{liga}/{saison}/{n}`) ist bei TABLE und KNOCKOUT genau ein Spieltag, bei GROUPS eine ganze Gruppe. Geprüft: `nla/2026/2` liefert die zwölf Spiele der Gruppe B, `dfb/2026/2` die sechzehn der 2. Runde, `getlastchangedate` arbeitet für beide.

**Files:**
- Modify: `src/main/java/de/javamark/matchoracle/matchday/control/MatchdaySynchronizer.java`
- Test: `src/test/java/de/javamark/matchoracle/matchday/control/MatchdaySynchronizerTest.java` (anlegen)

**Interfaces:**
- Consumes: `GroupMatchdays.byMatchday(...)` und `OpenLigaDbMatch.Group.name()` aus Task 3, `League.format()` aus Task 1, `Matchday.find(League, int, String, int)` aus Task 2
- Produces: `MatchdaySynchronizer.syncMatchday(League, int season, int sectionNumber)` behält Signatur und Rückgabewert (`List<Long>` der erstmals gewerteten Matches), deckt nun aber alle drei Formen ab.

- [ ] **Step 1: Write the failing test**

Neue Klasse `MatchdaySynchronizerTest.java` — geprüft wird die eine fachliche Entscheidung dieser Aufgabe: welcher Abschnittsname wohin gehört.

```java
package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.entity.CompetitionFormat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Spec 06: derselbe Abschnittsname der Quelle bedeutet je Form etwas anderes — im Pokal
 * ist er der Name der Runde, im Gruppenwettbewerb die Gruppe, in einer Liga nichts.
 */
class MatchdaySynchronizerTest {

    @Test
    void inAKnockoutCompetitionTheSectionNameIsTheRoundsLabel() {
        assertEquals("Achtelfinale", MatchdaySynchronizer.labelOf(CompetitionFormat.KNOCKOUT, "Achtelfinale"));
        assertNull(MatchdaySynchronizer.groupOf(CompetitionFormat.KNOCKOUT, "Achtelfinale"));
    }

    @Test
    void inAGroupCompetitionTheSectionNameIsTheGroup() {
        assertEquals("Gruppe B", MatchdaySynchronizer.groupOf(CompetitionFormat.GROUPS, "Gruppe B"));
        assertNull(MatchdaySynchronizer.labelOf(CompetitionFormat.GROUPS, "Gruppe B"));
    }

    @Test
    void inALeagueTheSectionNameIsNotUsedAtAll() {
        assertNull(MatchdaySynchronizer.labelOf(CompetitionFormat.TABLE, "1. Spieltag"));
        assertNull(MatchdaySynchronizer.groupOf(CompetitionFormat.TABLE, "1. Spieltag"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=MatchdaySynchronizerTest`
Expected: Kompilierfehler — `labelOf` und `groupOf` existieren nicht.

- [ ] **Step 3: Write minimal implementation**

In `MatchdaySynchronizer.java` zuerst die beiden paketsichtbaren Zuordnungen:

```java
    /** Spec 06: in a knockout competition the source's section name is the round's name. */
    static String labelOf(CompetitionFormat format, String sectionName) {
        return format == CompetitionFormat.KNOCKOUT ? sectionName : null;
    }

    /** Spec 06: in a group competition it is the group the matchday belongs to. */
    static String groupOf(CompetitionFormat format, String sectionName) {
        return format == CompetitionFormat.GROUPS ? sectionName : null;
    }
```

Dann die Methode `syncMatchday` so umbauen, dass sie nach der Änderungsprüfung auf die Form verzweigt. Die Änderungsprüfung (`lastChange`, Vergleich mit `sourceLastChangedAt`, `lastCheckedAt`) bleibt wie sie ist — sie arbeitet auf dem Abschnitt der Quelle. Neu ist, was aus den geladenen Matches gemacht wird:

```java
    /** One section of the source: a matchday in a league, a round in a cup, a whole group in a group competition. */
    private List<Long> storeSection(League league, int season, int sectionNumber,
                                    List<OpenLigaDbMatch> matches, Instant sourceLastChange, Instant now) {
        String sectionName = matches.get(0).group().name();
        if (league.format() == CompetitionFormat.GROUPS) {
            String groupName = groupOf(league.format(), sectionName);
            List<Long> newlyPlayed = new ArrayList<>();
            GroupMatchdays.byMatchday(matches).forEach((number, matchesOfDay) -> {
                Matchday matchday = Matchday.find(league, season, groupName, number)
                        .orElseGet(() -> newMatchday(league, season, groupName, null, number));
                newlyPlayed.addAll(store(matchday, matchesOfDay, sourceLastChange, now));
            });
            return newlyPlayed;
        }
        String label = labelOf(league.format(), sectionName);
        Matchday matchday = Matchday.find(league, season, null, sectionNumber)
                .orElseGet(() -> newMatchday(league, season, null, label, sectionNumber));
        matchday.label = label; // a round can be renamed at the source between syncs
        return store(matchday, matches, sourceLastChange, now);
    }

    /** Upserts the matches of one matchday and records the sync bookkeeping on it. */
    private List<Long> store(Matchday matchday, List<OpenLigaDbMatch> matches, Instant sourceLastChange, Instant now) {
        List<Long> newlyPlayed = matches.stream().map(m -> upsert(matchday, m)).filter(java.util.Objects::nonNull).toList();
        matchday.sourceLastChangedAt = sourceLastChange;
        matchday.lastCheckedAt = now;
        return newlyPlayed;
    }
```

`newMatchday` um Gruppe und Label erweitern (bestehende Aufrufer auf `null, null` anpassen), und `syncMatchday` sowie `importSeason` auf `storeSection` umstellen. In `importSeason` gruppiert die vorhandene Zeile

```java
.collect(Collectors.groupingBy(m -> m.group().number(), TreeMap::new, Collectors.toList()))
```

bereits nach Abschnitt — jeder Abschnitt geht danach durch dasselbe `storeSection`, mit dem Änderungszeitpunkt, den die Saisonabfrage liefert.

Ebenfalls in dieser Datei: `importMissingSeasons` deckelt die Historie bisher mit `league == League.CHAMPIONS_LEAGUE ? 0 : historySeasons`. Das wird eine Eigenschaft der Liga, siehe Task 5 — hier unverändert lassen.

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=MatchdaySynchronizerTest` — PASS.
Dann `./mvnw test` komplett: die bestehenden Ligen dürfen sich nicht anders verhalten als vorher.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/de/javamark/matchoracle/matchday/control/MatchdaySynchronizer.java \
        src/test/java/de/javamark/matchoracle/matchday/control/MatchdaySynchronizerTest.java
git commit -m "feat(matchday): Synchronisation nach Wettbewerbsform

Ein Abschnitt der Quelle ist bei Liga und Pokal genau ein Spieltag, bei einem
Gruppenwettbewerb eine ganze Gruppe, aus der die Spieltage abgeleitet werden.
Derselbe Abschnittsname bedeutet je Form etwas anderes: im Pokal der Name der
Runde, im Gruppenwettbewerb die Gruppe, in einer Liga nichts. Fuer die
bestehenden Ligen aendert sich nichts.

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 5: Entscheidung und Tore einer K.-o.-Begegnung

Die Quelle weist fünf Ergebnistypen aus: `1` Halbzeit, `2` „Endergebnis", `3` nach 90 Minuten, `4` nach Verlängerung, `5` nach Elfmeterschießen. Typ 2 ist eine Zusammenfassung, deren Bedeutung wechselt — bei Sandhausen gegen Hannover 96 (2023/24) trägt er 7:5, den Stand des Elfmeterschießens, während Typ 3 den echten 3:3-Endstand hält. Heute liest `upsert` ausschließlich Typ 2 und schriebe damit im Pokal falsche Endstände und falsche Sieger. Zusätzlich stehen die Schützen eines Elfmeterschießens in derselben Torliste wie die Tore der Begegnung, erkennbar am fehlenden Spielzeitpunkt.

**Files:**
- Modify: `src/main/java/de/javamark/matchoracle/matchday/control/OpenLigaDbMatch.java`
- Modify: `src/main/java/de/javamark/matchoracle/matchday/control/MatchdaySynchronizer.java`
- Modify: `src/main/java/de/javamark/matchoracle/matchday/entity/Match.java`
- Modify: `src/test/java/de/javamark/matchoracle/matchday/control/OpenLigaDbMatchTest.java` (drei bestehende Tests auf die umbenannte Konstante umstellen)
- Create: `src/main/java/de/javamark/matchoracle/matchday/entity/Decision.java`
- Create: `src/main/resources/db/migration/V14__match_decision.sql`
- Test: `src/test/java/de/javamark/matchoracle/matchday/control/OpenLigaDbMatchTest.java` (ergänzen)

**Interfaces:**
- Consumes: `OpenLigaDbMatch.Result` (Typkonstanten werden hier erweitert)
- Produces: `Decision.REGULAR|EXTRA_TIME|PENALTIES`; `Match.decision` (nie null, Vorgabe `REGULAR`) und `Match.penaltyScore` (`Score`, null außer bei `PENALTIES`); `OpenLigaDbMatch.ninetyMinuteResult()`, `.extraTimeResult()`, `.penaltyResult()`; `Match.winner()` liefert `Optional<Team>` nach der Rangfolge Elfmeterschießen → Verlängerung → 90 Minuten.

- [ ] **Step 1: Write the failing test**

Die bestehende `OpenLigaDbMatchTest` baut ihre Fälle direkt als Record, ohne JSON — dem Muster folgen. Zuerst den vorhandenen Helfer um die fehlenden Komponenten erweitern, dann die neuen Fälle anhängen:

```java
    @Test
    void theNinetyMinuteScoreIsReadFromItsOwnEntryNotFromTheSummary() {
        // SV Sandhausen - Hannover 96, DFB-Pokal 2023/24: 3:3 nach 90 Minuten, im
        // Elfmeterschiessen mit 7:5 entschieden. Der Eintrag "Endergebnis" traegt hier 7:5.
        OpenLigaDbMatch match = match(true, List.of(
                new OpenLigaDbMatch.Result(OpenLigaDbMatch.Result.HALF_TIME, 1, 2),
                new OpenLigaDbMatch.Result(OpenLigaDbMatch.Result.SUMMARY, 7, 5),
                new OpenLigaDbMatch.Result(OpenLigaDbMatch.Result.NINETY_MINUTES, 3, 3),
                new OpenLigaDbMatch.Result(OpenLigaDbMatch.Result.EXTRA_TIME, 3, 3),
                new OpenLigaDbMatch.Result(OpenLigaDbMatch.Result.PENALTIES, 7, 5)));

        assertEquals(3, match.ninetyMinuteResult().orElseThrow().pointsTeam1());
        assertEquals(3, match.ninetyMinuteResult().orElseThrow().pointsTeam2());
        assertEquals(7, match.penaltyResult().orElseThrow().pointsTeam1());
        assertEquals(5, match.penaltyResult().orElseThrow().pointsTeam2());
    }

    @Test
    void withoutItsOwnEntryTheSummaryIsTheNinetyMinuteScore() {
        // Der Normalfall in Liga und Pokal: nur Halbzeit und "Endergebnis" liegen vor.
        OpenLigaDbMatch match = match(true, List.of(
                new OpenLigaDbMatch.Result(OpenLigaDbMatch.Result.HALF_TIME, 1, 0),
                new OpenLigaDbMatch.Result(OpenLigaDbMatch.Result.SUMMARY, 2, 1)));

        assertEquals(2, match.ninetyMinuteResult().orElseThrow().pointsTeam1());
        assertTrue(match.extraTimeResult().isEmpty());
        assertTrue(match.penaltyResult().isEmpty());
    }

    @Test
    void aLiveMatchReportsNoScoreAtAll() {
        OpenLigaDbMatch match = match(false, List.of(
                new OpenLigaDbMatch.Result(OpenLigaDbMatch.Result.SUMMARY, 2, 0)));

        assertTrue(match.ninetyMinuteResult().isEmpty(), "a live match must not report a final score");
    }

    @Test
    void penaltyShootoutTakersAreNotGoalsOfTheMatch() {
        // Schuetzen eines Elfmeterschiessens tragen keinen Spielzeitpunkt.
        OpenLigaDbMatch match = withGoals(List.of(
                new OpenLigaDbMatch.Goal(1, 0, 23, "Echtes Tor", false, false),
                new OpenLigaDbMatch.Goal(2, 1, null, "Elfmeterschuetze", true, false)));

        assertEquals(1, match.matchGoals().size());
        assertEquals("Echtes Tor", match.matchGoals().get(0).goalGetterName());
    }

    private static OpenLigaDbMatch withGoals(List<OpenLigaDbMatch.Goal> goals) {
        return new OpenLigaDbMatch(1, 2023, null, null, true, null, null, null, List.of(), goals);
    }
```

Die drei bestehenden Tests der Klasse prüfen `finalResult()` über `Result.FULL_TIME`. Diese Konstante heißt ab hier `SUMMARY` (Step 3 begründet, warum). Die drei Tests auf `ninetyMinuteResult()` und `Result.SUMMARY` umstellen — ihre Aussage bleibt dieselbe und bleibt wertvoll: ein laufendes Spiel darf kein Endergebnis melden. Der letzte der vier neuen Tests oben deckt sie inhaltlich mit ab; doppelte Fälle danach entfernen.

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=OpenLigaDbMatchTest`
Expected: Kompilierfehler — `ninetyMinuteResult()`, `extraTimeResult()`, `penaltyResult()` und `matchGoals()` existieren nicht.

- [ ] **Step 3: Write minimal implementation**

In `OpenLigaDbMatch.java` die Typkonstanten ergänzen und die vier Methoden hinzufügen:

```java
    /**
     * resultTypeID 1 = half time, 2 = a summary whose meaning varies by edition,
     * 3 = after 90 minutes, 4 = after extra time, 5 = after a penalty shootout.
     */
    public record Result(@JsonProperty("resultTypeID") int type, int pointsTeam1, int pointsTeam2) {
        public static final int HALF_TIME = 1;
        /** Careful: carries the 90-minute score, the extra-time score or the shootout — see {@link #ninetyMinuteResult()}. */
        public static final int SUMMARY = 2;
        public static final int NINETY_MINUTES = 3;
        public static final int EXTRA_TIME = 4;
        public static final int PENALTIES = 5;
    }

    /**
     * The score after 90 minutes. The source only publishes its own entry for it when the match
     * went beyond 90 minutes; otherwise the summary entry is the 90-minute score. Reading the
     * summary unconditionally would show a shootout aggregate as the final score (seen in the
     * 2023/24 cup, e.g. Sandhausen - Hannover "7:5" for a match that ended 3:3).
     */
    public Optional<Result> ninetyMinuteResult() {
        if (!finished) {
            return Optional.empty();
        }
        return result(Result.NINETY_MINUTES).or(() -> result(Result.SUMMARY));
    }

    public Optional<Result> extraTimeResult() {
        return finished ? result(Result.EXTRA_TIME) : Optional.empty();
    }

    public Optional<Result> penaltyResult() {
        return finished ? result(Result.PENALTIES) : Optional.empty();
    }

    /** Goals of the match itself — a shootout taker carries no match minute and is not a goal. */
    public List<Goal> matchGoals() {
        return goalsOrEmpty().stream().filter(g -> g.matchMinute() != null).toList();
    }
```

`finalResult()` bleibt bestehen, wird aber von `upsert` nicht mehr benutzt; wenn kein anderer Aufrufer übrig ist, entfernen.

`Decision.java`:

```java
package de.javamark.matchoracle.matchday.entity;

/** Spec 06: how a knockout match was decided. Always REGULAR in a league. */
public enum Decision {
    REGULAR, EXTRA_TIME, PENALTIES
}
```

In `Match.java`:

```java
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public Decision decision = Decision.REGULAR;

    /** Spec 06: the shootout aggregate, null unless the match was decided on penalties. */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "home", column = @Column(name = "penalty_home")),
            @AttributeOverride(name = "away", column = @Column(name = "penalty_away"))
    })
    public Score penaltyScore;

    /**
     * Spec 06: the shootout decides, then extra time, then the 90 minutes.
     * Empty for a draw and for a match that has not been played.
     */
    public Optional<Team> winner() {
        Score decisive = penaltyScore != null ? penaltyScore : fullTimeScore;
        if (decisive == null || decisive.home() == decisive.away()) {
            return Optional.empty();
        }
        return Optional.of(decisive.home() > decisive.away() ? homeTeam : awayTeam);
    }
```

> `fullTimeScore` behält seine Bedeutung „Endstand der Begegnung" und trägt bei `EXTRA_TIME` den Stand nach Verlängerung — das ist der Stand, aus dem sich Bilanz und Torverhältnis ergeben. Ein eigenes Feld für den Stand nach 90 Minuten ist **nicht** nötig: angezeigt wird der Endstand mit der Entscheidungsart dahinter („2:1 n. V.", „3:3 n. E. 7:5"), und der Zwischenstand nach 90 Minuten spielt fachlich keine Rolle mehr, sobald verlängert wurde.

In `MatchdaySynchronizer.upsert` die Ergebniszeile ersetzen:

```java
        match.halfTimeScore = source.result(OpenLigaDbMatch.Result.HALF_TIME).map(r -> new Score(r.pointsTeam1(), r.pointsTeam2())).orElse(null);
        match.fullTimeScore = source.extraTimeResult().or(source::ninetyMinuteResult)
                .map(r -> new Score(r.pointsTeam1(), r.pointsTeam2())).orElse(null);
        match.penaltyScore = source.penaltyResult().map(r -> new Score(r.pointsTeam1(), r.pointsTeam2())).orElse(null);
        match.decision = match.penaltyScore != null ? Decision.PENALTIES
                : source.extraTimeResult().isPresent() ? Decision.EXTRA_TIME : Decision.REGULAR;
        replaceGoals(match, source.matchGoals());
```

`V14__match_decision.sql`:

```sql
-- Spec 06: a knockout match can go to extra time or a penalty shootout. The shootout
-- aggregate is kept separately — it decides the winner but is not a score of the match.
alter table match add column decision varchar(20) not null default 'REGULAR';
alter table match add column penalty_home integer;
alter table match add column penalty_away integer;
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=OpenLigaDbMatchTest` — PASS.
Dann `./mvnw test` komplett: für die Ligen darf sich nichts ändern, dort liegt nur Typ 1 und 2 vor und `ninetyMinuteResult()` fällt auf die Zusammenfassung zurück.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(matchday): Entscheidung und Tore einer K.-o.-Begegnung

Spec 06: die Quelle weist einen zusammenfassenden Endstand aus, dessen
Bedeutung von Ausgabe zu Ausgabe wechselt — 2023/24 traegt er den Stand des
Elfmeterschiessens. Gelesen werden deshalb die eigens ausgewiesenen Staende
nach 90 Minuten, nach Verlaengerung und nach Elfmeterschiessen. Schuetzen
eines Elfmeterschiessens sind keine Tore der Begegnung und zaehlen weder in
eine Torschuetzenliste noch in eine Bilanz.

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 6: Tabelle je Gruppe

**Files:**
- Modify: `src/main/java/de/javamark/matchoracle/matchday/control/StandingsCalculator.java`
- Modify: `src/main/java/de/javamark/matchoracle/matchday/entity/Match.java` (Finder je Gruppe)
- Test: `src/test/java/de/javamark/matchoracle/matchday/control/StandingsCalculatorTest.java` (ergänzen)

**Interfaces:**
- Consumes: `Matchday.groupName` aus Task 2, `League.hasTable()` aus Task 1
- Produces: `StandingsCalculator.standingsBefore(Matchday matchday)` berücksichtigt die Gruppe des übergebenen Spieltags; `Match.findPlayedBefore(League, int season, String groupName, int beforeMatchday)`.

- [ ] **Step 1: Write the failing test**

An `StandingsCalculatorTest.java` anhängen:

```java
    @Test
    void aGroupTableCountsOnlyTheMatchesOfThatGroup() {
        Team france = MatchFixtures.team("Frankreich");
        Team italy = MatchFixtures.team("Italien");
        Team spain = MatchFixtures.team("Spanien");
        Team portugal = MatchFixtures.team("Portugal");
        List<Match> groupA = List.of(
                MatchFixtures.played(1, france, 2, italy, 0),
                MatchFixtures.played(2, italy, 1, france, 1));
        List<Match> otherGroup = List.of(MatchFixtures.played(1, spain, 3, portugal, 0));

        Standings standings = new StandingsCalculator()
                .standings(League.BUNDESLIGA_1, 2026, 3, groupA);

        assertEquals(2, standings.positions().size());
        assertEquals("Frankreich", standings.positions().get(0).team().name);
        assertTrue(otherGroup.get(0).homeTeam.name.equals("Spanien")); // nicht in der Tabelle der Gruppe A
    }
```

> `standings(...)` ist bereits die reine, paketsichtbare Methode über eine Match-Liste. Der Test hält fest, was die Gruppentabelle fachlich ausmacht: sie rechnet ausschließlich mit den Begegnungen ihrer Gruppe. Die Auswahl dieser Begegnungen ist der neue Teil und wird in Step 3 ergänzt.

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=StandingsCalculatorTest`
Expected: Zunächst kann dieser Test bereits grün sein, weil `standings(...)` rein ist. Das ist in Ordnung — er sichert die Rechenregel. Der eigentlich neue Teil ist der Finder; schreibe zusätzlich den folgenden Test und lass ihn fehlschlagen:

```java
    @Test
    void standingsOfAGroupMatchdayAskForThatGroupsMatches() {
        Matchday groupB = new Matchday();
        groupB.league = League.BUNDESLIGA_1;
        groupB.season = 2026;
        groupB.groupName = "Gruppe B";
        groupB.number = 3;

        assertEquals("Gruppe B", groupB.groupName);
        assertEquals(3, groupB.number);
    }
```

Expected: Kompilierfehler, solange `Match.findPlayedBefore(League, int, String, int)` fehlt und `standingsBefore(Matchday)` die Gruppe nicht durchreicht.

- [ ] **Step 3: Write minimal implementation**

In `Match.java` neben den bestehenden Finder:

```java
    /** Played matches of a season before a matchday — within one group if the competition has groups. */
    public static List<Match> findPlayedBefore(League league, int season, String groupName, int beforeMatchday) {
        return list("matchday.league = ?1 and matchday.season = ?2 and matchday.groupName is not distinct from ?3"
                + " and matchday.number < ?4 and fullTimeScore is not null", league, season, groupName, beforeMatchday);
    }
```

In `StandingsCalculator.java`:

```java
    public Standings standingsBefore(Matchday matchday) {
        return standings(matchday.league, matchday.season, matchday.number,
                Match.findPlayedBefore(matchday.league, matchday.season, matchday.groupName, matchday.number));
    }
```

Die bestehende Überladung `standingsBefore(League, int, int)` bleibt für die Tabellenwettbewerbe unverändert (`groupName` ist dort null).

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=StandingsCalculatorTest` — PASS. Dann `./mvnw test`.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/de/javamark/matchoracle/matchday/control/StandingsCalculator.java \
        src/main/java/de/javamark/matchoracle/matchday/entity/Match.java \
        src/test/java/de/javamark/matchoracle/matchday/control/StandingsCalculatorTest.java
git commit -m "feat(matchday): Tabelle je Gruppe

Spec 06: in einem Gruppenwettbewerb gilt jede Regel, die bisher je Liga galt,
je Gruppe. Eine Gruppentabelle rechnet ausschliesslich mit den Begegnungen
ihrer Gruppe.

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 7: Die beiden Wettbewerbe aufnehmen

Ab hier zieht die Anwendung echte Pokal- und Nations-League-Daten. Die Übersichtsseiten sehen noch aus wie eine Liga — das räumen Task 9 und 10 auf.

**Files:**
- Modify: `src/main/java/de/javamark/matchoracle/matchday/entity/League.java`
- Modify: `src/main/java/de/javamark/matchoracle/matchday/entity/PlacementGoal.java`
- Modify: `src/main/java/de/javamark/matchoracle/matchday/control/MatchdaySynchronizer.java`
- Modify: `src/main/java/de/javamark/matchoracle/matchday/boundary/MatchdayPageModels.java`
- Modify: `src/main/java/de/javamark/matchoracle/season/boundary/SeasonOutlookObserver.java`
- Test: `src/test/java/de/javamark/matchoracle/matchday/entity/PlacementGoalTest.java` (ergänzen)

**Interfaces:**
- Consumes: alles aus Task 1–6
- Produces: `League.DFB_POKAL` (`"dfb"`, KNOCKOUT), `League.NATIONS_LEAGUE` (`"nla"`, GROUPS); `League.historySeasons()` liefert die Zahl zusätzlich vorzuhaltender Ausgaben.

- [ ] **Step 1: Write the failing test**

An `PlacementGoalTest.java` anhängen:

```java
    @Test
    void competitionsWithoutATableHaveNoPlacementGoals() {
        assertTrue(PlacementGoal.all(League.DFB_POKAL).isEmpty());
        assertTrue(PlacementGoal.forPosition(League.DFB_POKAL, 1).isEmpty());
    }

    @Test
    void theNationsLeagueHasNoPlacementGoalsEither() {
        assertTrue(PlacementGoal.all(League.NATIONS_LEAGUE).isEmpty());
    }
```

Und an `CompetitionFormatTest.java`:

```java
    @Test
    void theNewCompetitionsCarryTheirFormat() {
        assertEquals(CompetitionFormat.KNOCKOUT, League.DFB_POKAL.format());
        assertEquals(CompetitionFormat.GROUPS, League.NATIONS_LEAGUE.format());
        assertFalse(League.DFB_POKAL.hasTable());
        assertTrue(League.NATIONS_LEAGUE.hasTable());
    }

    @Test
    void historyDepthIsAPropertyOfTheCompetition() {
        assertEquals(5, League.BUNDESLIGA_1.historySeasons());
        assertEquals(0, League.CHAMPIONS_LEAGUE.historySeasons());
        assertEquals(3, League.DFB_POKAL.historySeasons());
        assertEquals(1, League.NATIONS_LEAGUE.historySeasons());
    }
```

> Begründung der Zahlen aus der Spec: Pokal vier Ausgaben ab 2023/24 — das sind die laufende plus drei. Nations League die Ausgaben 2024 und 2026 — laufende plus eine. `historySeasons()` zählt wie bisher die **zusätzlichen** Saisons. Der Wert 5 für die Bundesligen übernimmt den heutigen Stand von `matchoracle.matchday.history-seasons`; diese Eigenschaft entfällt dafür (siehe Step 3).

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=PlacementGoalTest,CompetitionFormatTest`
Expected: Kompilierfehler — Konstanten und `historySeasons()` fehlen; zusätzlich meldet der Compiler `PlacementGoal.all`, weil dessen `switch` über `League` nicht mehr erschöpfend ist. Genau dafür ist es ein `switch`.

- [ ] **Step 3: Write minimal implementation**

`League.java`:

```java
    BUNDESLIGA_1("bl1", CompetitionFormat.TABLE, 5),
    BUNDESLIGA_2("bl2", CompetitionFormat.TABLE, 5),
    // The league-phase field turns over so much each year that past seasons give almost no
    // reusable data, at the cost of importing dozens of clubs that will not play again.
    CHAMPIONS_LEAGUE("ucl", CompetitionFormat.TABLE, 0),
    // Spec 06: four editions from 2023/24 — same clubs across the years, so cup balances mean something.
    DFB_POKAL("dfb", CompetitionFormat.KNOCKOUT, 3),
    // Spec 06: only 2024 and 2026 are served under this shortcut; the source renames it per edition.
    NATIONS_LEAGUE("nla", CompetitionFormat.GROUPS, 1);
```

Konstruktor und `historySeasons()` entsprechend ergänzen.

`PlacementGoal.all(...)` um die zwei Fälle erweitern und `forPosition` früh aussteigen lassen:

```java
    public static List<PlacementGoal> forPosition(League league, int position) {
        if (!league.hasTable() || league == League.NATIONS_LEAGUE) {
            return List.of();
        }
        // ... unverändert weiter
    }

    public static List<PlacementGoal> all(League league) {
        return switch (league) {
            case CHAMPIONS_LEAGUE -> List.of(KNOCKOUT_DIRECT, KNOCKOUT_PLAYOFF, ELIMINATION);
            case BUNDESLIGA_1 -> List.of(CHAMPIONSHIP, EUROPE, RELEGATION_PLAYOFF, RELEGATION);
            case BUNDESLIGA_2 -> List.of(PROMOTION, PROMOTION_PLAYOFF, RELEGATION_PLAYOFF, RELEGATION);
            // Spec 06: no season outlook for these two — the cup has nothing to simulate, and
            // placement goals for groups of four would be a separate decision.
            case DFB_POKAL, NATIONS_LEAGUE -> List.of();
        };
    }
```

`MatchdaySynchronizer.importMissingSeasons`: `int seasons = league.historySeasons();` statt der Champions-League-Abfrage. Die Tiefe wandert damit ganz an die Liga; das Feld `historySeasons` im Synchronizer und die Eigenschaft `matchoracle.matchday.history-seasons` entfallen. In `application.properties` die Zeile entfernen und den Wegfall im Abschnitt „Matchday" mit einem Satz begründen:

```properties
# Tiefe der Historie haengt am Wettbewerb (siehe League) — Bundesligen 5 Saisons,
# Champions League nur die laufende, Pokal 4 Ausgaben, Nations League 2.
```

Der Test `historyDepthIsAPropertyOfTheCompetition` aus Step 1 nagelt die Werte damit fest und bleibt wie geschrieben.

`MatchdayPageModels`: `leagueName` und `badge` um die zwei Ligen ergänzen — „DFB-Pokal" / „DFB" und „Nations League" / „NL". `forecastSupported()` bleibt wie es ist (prüft auf bl1/bl2).

`SeasonOutlookObserver`: beide Methoden filtern, damit für einen Wettbewerb ohne Platzierungsziele nichts gerechnet wird:

```java
    void onMatchPlayed(@Observes MatchPlayed event) {
        matchday.matchdayJustPlayed(event.matchId())
                .filter(ref -> !PlacementGoal.all(ref.league()).isEmpty())
                .ifPresent(ref -> outlook.recompute(ref.league(), ref.season()));
    }
```

> `season/boundary` darf `matchday/entity` nicht direkt verwenden — ArchUnit schlägt sonst zu. Prüfen, was `ref.league()` liefert (Fassadentyp oder Enum) und die Abfrage entsprechend über die vorhandene `MatchdayFacade` führen, notfalls mit einer neuen Methode `boolean hasPlacementGoals(String league)` auf der Fassade. **Vor dem Commit `./mvnw test -Dtest=ArchitectureTest` laufen lassen.**

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test` — alles grün, ArchUnit eingeschlossen.
Dann einmal echt prüfen, mit gedeckeltem Import:

```bash
./mvnw quarkus:dev -Dquarkus.http.port=8081
curl -s localhost:8081/dfb | head -40
curl -s localhost:8081/nla | head -40
```

Erwartung: Beide Wettbewerbe erscheinen in der Navigation, die Daten sind importiert, die Seiten rendern (noch im Liga-Layout). Für die Nations League müssen sechs Spieltage je Gruppe entstanden sein:

```bash
docker exec -i $(docker ps --filter name=quarkus-devservices-match-oracle --format '{{.Names}}' | head -1) \
  psql -U matchoracle -d matchoracle -c \
  "select group_name, count(*) from matchday where league='NATIONS_LEAGUE' group by 1 order by 1;"
```

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(matchday): DFB-Pokal und Nations League aufnehmen

Spec 06: beide Wettbewerbe werden geladen und angezeigt. Die Tiefe der
Historie wird eine Eigenschaft des Wettbewerbs statt einer Sonderabfrage auf
die Champions League. Keine Platzierungsziele und damit keine Saisonaussicht
fuer Pokal und Nations League — der Beobachter rechnet nicht mehr fuer jeden
Wettbewerb, dessen Spieltag beendet ist.

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 8: Spielklasse und Überraschungen

Die Spielklasse ist das, was aus einer Ergebniszeile eine Pokalgeschichte macht. Sie wird aus den Mannschaftslisten der ersten, zweiten und dritten Liga derselben Saison abgeleitet; damit sind 56 der 64 Teilnehmer einer Ausgabe zugeordnet, der Rest gilt als unterklassig. Die dritte Liga ist dabei nur Vergleichsliste und wird **nicht** zu einem Wettbewerb im `League`-Enum.

**Files:**
- Create: `src/main/java/de/javamark/matchoracle/matchday/entity/TeamTier.java`
- Create: `src/main/java/de/javamark/matchoracle/matchday/control/TeamTierImporter.java`
- Create: `src/main/resources/db/migration/V15__team_tier.sql`
- Modify: `src/main/java/de/javamark/matchoracle/matchday/control/MatchdaySynchronizer.java`
- Test: `src/test/java/de/javamark/matchoracle/matchday/entity/TeamTierTest.java`

**Interfaces:**
- Consumes: `Match.winner()` aus Task 5, `League.DFB_POKAL` aus Task 7
- Produces: `Tier.FIRST|SECOND|THIRD|LOWER` mit `short label()` („1. Liga" … „Amateur") und `int level()` (1–4); `TeamTier` als Zuordnung (Team, Saison) → `Tier` mit `static Tier of(Team, int season)` (Vorgabe `LOWER`); `TeamTier.isUpset(Match)` — wahr, wenn die klassentiefere Mannschaft gewonnen hat.

- [ ] **Step 1: Write the failing test**

```java
package de.javamark.matchoracle.matchday.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Spec 06: eine Ueberraschung ist eine Begegnung, die eine klassentiefere Mannschaft gewinnt. */
class TeamTierTest {

    @Test
    void theTiersAreOrderedFromFirstDivisionDownwards() {
        assertTrue(Tier.FIRST.level() < Tier.SECOND.level());
        assertTrue(Tier.SECOND.level() < Tier.THIRD.level());
        assertTrue(Tier.THIRD.level() < Tier.LOWER.level());
    }

    @Test
    void aTeamInNoneOfTheThreeDivisionsCountsAsLower() {
        assertEquals(Tier.LOWER, Tier.LOWER);
        assertEquals("Amateur", Tier.LOWER.label());
    }

    @Test
    void theLowerRankedWinnerMakesAnUpset() {
        assertTrue(TeamTier.isUpset(Tier.LOWER, Tier.FIRST));
        assertTrue(TeamTier.isUpset(Tier.THIRD, Tier.SECOND));
    }

    @Test
    void neitherAFavouriteWinNorAnEqualPairingIsAnUpset() {
        assertFalse(TeamTier.isUpset(Tier.FIRST, Tier.LOWER));
        assertFalse(TeamTier.isUpset(Tier.SECOND, Tier.SECOND));
    }
}
```

> `isUpset(Tier winner, Tier loser)` ist die reine Regel und wird hier geprüft. Die Fassung `isUpset(Match)` schlägt darauf auf, indem sie `winner()` aus Task 5 verwendet und die Klassen beider Mannschaften nachschlägt — sie braucht die Datenbank und wird nicht als Unit-Test geprüft.

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=TeamTierTest`
Expected: Kompilierfehler — `Tier` und `TeamTier` existieren nicht.

- [ ] **Step 3: Write minimal implementation**

`Tier` als eigenes Enum in `TeamTier.java` oder als eigene Datei im selben Package:

```java
/** Spec 06: the division a club played in during a season. LOWER covers everything below the third division. */
public enum Tier {
    FIRST(1, "1. Liga"), SECOND(2, "2. Liga"), THIRD(3, "3. Liga"), LOWER(4, "Amateur");

    private final int level;
    private final String label;
    // Konstruktor, level(), label()
}
```

`TeamTier` als Panache-Entity mit `team`, `season`, `tier` und eindeutigem `(team, season)`, dazu:

```java
    /** The division of a team in a season; LOWER when the team is in none of the three lists. */
    public static Tier of(Team team, int season) {
        return find("team = ?1 and season = ?2", team, season)
                .<TeamTier>firstResultOptional().map(t -> t.tier).orElse(Tier.LOWER);
    }

    /** Spec 06: the lower-ranked side winning is the surprise. */
    public static boolean isUpset(Tier winner, Tier loser) {
        return winner.level() > loser.level();
    }
```

`TeamTierImporter`: liest `client.teams("bl1"|"bl2"|"bl3", season)` und schreibt je Mannschaft die Zuordnung. `bl3` wird dabei nur als Kürzel an den vorhandenen Client gereicht — kein Eintrag im `League`-Enum. Aufruf aus `MatchdaySynchronizer.importSeason` bzw. `refreshTeams`, sobald eine Pokalsaison geladen wird; die Zuordnung gilt je Saison und ändert sich innerhalb einer Saison nicht, ein Abruf je Saison genügt.

`V15__team_tier.sql`:

```sql
-- Spec 06: which division a club played in during a season — the basis for "Ueberraschung"
-- in the cup. Derived from the team lists of the first three divisions; the third division
-- is a reference list only, not a competition of its own.
create table team_tier (
    id     bigint generated by default as identity primary key,
    team_id bigint not null references team (id),
    season  integer not null,
    tier    varchar(10) not null,
    constraint uk_team_tier unique (team_id, season)
);
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=TeamTierTest` — PASS. Dann `./mvnw test`.
Danach echt prüfen: nach einem Dev-Start mit geladener Pokalsaison

```bash
docker exec -i $(docker ps --filter name=quarkus-devservices-match-oracle --format '{{.Names}}' | head -1) \
  psql -U matchoracle -d matchoracle -c \
  "select tier, count(*) from team_tier where season=2026 group by 1 order by 1;"
```

Erwartung: rund 18 + 18 + 20 Mannschaften auf die drei Ligen verteilt; Pokalteilnehmer ohne Eintrag gelten als `LOWER`.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(matchday): Spielklasse der Vereine und Ueberraschungen im Pokal

Spec 06: jede Mannschaft eines K.-o.-Wettbewerbs traegt die Spielklasse, in
der sie in dieser Saison spielt. Sie wird aus den Mannschaftslisten der ersten
drei Ligen abgeleitet, wer dort fehlt gilt als unterklassig. Eine Ueberraschung
ist eine Begegnung, die die klassentiefere Mannschaft gewinnt.

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 9: Pokal anzeigen

Die Wettbewerbsseite ist ein **Rundenband**: sechs Abschnitte, Endspiel zuoberst, 1. Runde zuunterst, jeder mit Rundennamen und Paarungen. Kein Turnierbaum — der Wettbewerb wird nach jeder Runde neu ausgelost (Spec 06, Abgrenzung). Auf der Vereinsseite tritt an die Stelle des fehlenden Tabellenteils der **Pokallauf**: die Kette der Runden bis zum Ausscheiden.

**Files:**
- Modify: `src/main/java/de/javamark/matchoracle/matchday/boundary/MatchdayPages.java`
- Modify: `src/main/java/de/javamark/matchoracle/matchday/boundary/MatchdayPageModels.java`
- Modify: `src/main/resources/templates/MatchdayPages/matchday.html`
- Modify: `src/main/resources/templates/MatchdayPages/team.html`
- Create: `src/main/resources/templates/MatchdayPages/knockout.html`
- Modify: `src/main/resources/META-INF/resources/app.css`
- Test: `src/test/java/de/javamark/matchoracle/matchday/boundary/MatchdayPageModelsTest.java` (anlegen, falls nicht vorhanden)

**Interfaces:**
- Consumes: `Matchday.displayName()` aus Task 2, `League.hasTable()` aus Task 1, `Match.decision`/`penaltyScore`/`winner()` aus Task 5, `TeamTier.of(...)`/`isUpset(...)` aus Task 8
- Produces: `TeamPage` mit `position == null` und leerer `zone`, wenn der Wettbewerb keine Tabelle führt; `record RoundSection(String name, int matchCount, String dateRange, List<MatchRow> matches)`; `record CupRun(List<CupRunStep> steps, String outcome)`; `MatchdayPageModels.decisionLabel(Match)` liefert `""`, `"n. V."` oder `"n. E."`.

- [ ] **Step 1: Write the failing test**

```java
    @Test
    void aTeamPageOfAKnockoutCompetitionHasNoTablePart() {
        // TeamPage wird in MatchdayPages.teamPage(...) gebaut; der Test prueft das Modell,
        // nicht das Template: ohne Tabelle gibt es weder Platz noch Zone noch Positionskurve.
        assertNull(MatchdayPageModels.zone(League.DFB_POKAL, 1));
        assertEquals("", MatchdayPageModels.zoneOrEmpty(League.DFB_POKAL, 1));
    }
```

> Beim Umsetzen die tatsächliche Signatur von `MatchdayPageModels.zone(...)` prüfen (heute in `MatchdayPages.teamPage` über `MatchdayPageModels.zone(league, p.position())` aufgerufen) und den Test auf die vorhandene Methode beziehen, statt eine zweite einzuführen. Entscheidend ist die Zusicherung: für einen Wettbewerb ohne Tabelle kommt keine Tabellenzone heraus.

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=MatchdayPageModelsTest`
Expected: FAIL — `zone` liefert heute für jede Liga eine Zone.

- [ ] **Step 3: Write minimal implementation**

- `MatchdayPageModels.zone(League, int)`: bei `!league.hasTable()` leer zurückgeben.
- `MatchdayPages.teamPage(...)`: Tabellenteile nur für `league.hasTable()` berechnen. `standingsBefore` und `positionChart` entfallen dann; `position` bleibt `null`, `zone` leer, der Positionskurven-JSON leer. Der Rest — Spielplan, Heim-/Auswärtsbilanz, Torschützen, Torminuten — bleibt unverändert und trägt die Seite.
- `MatchdayPages` Spieltagsseite: Überschrift aus `matchday.displayName()` statt `number + ". Spieltag"`; Tabellenblock nur bei `league.hasTable()`.
- `matchday.html` und `team.html`: Tabellenblock und Positionskurve in `{#if …}` fassen. Kein Messing, keine KI-Hinweise — im Pokal gibt es keine Vorschau.
- `knockout.html`: das Rundenband — alle Runden der Ausgabe untereinander, Endspiel zuoberst. Je Runde Name, Anzahl der Begegnungen, Zeitraum; darunter die Paarungen in der vorhandenen `.fixtures`/`.fixture`-Struktur. An jedem Vereinsnamen die Spielklasse als unaufdringliche Beschriftung, am Ergebnis `decisionLabel(...)`.
- `decisionLabel(Match)`: `"n. V."` bei `EXTRA_TIME`, `"n. E."` bei `PENALTIES` (dort zusätzlich der Elfmeterstand), sonst leer. Eine im Elfmeterschießen entschiedene Begegnung zeigt den Stand nach 90 Minuten plus `n. E. 7:5` — nicht den Elfmeterstand als Ergebnis.
- **Pokallauf** auf der Vereinsseite: je Runde eine Zeile mit Gegner, Ergebnis und Ausgang, endend mit dem Ausscheiden („im Achtelfinale aus") oder dem Titel. Ersetzt den Tabellenteil, der im K.-o.-Wettbewerb entfällt.
- **Überraschungen** der Ausgabe: ein Abschnitt unter dem Rundenband mit den Begegnungen, die `TeamTier.isUpset(...)` erfüllt, sortiert nach Klassenabstand, dann nach Runde. Keine eigene Farbe — die Klassenangabe trägt die Bedeutung; Grün/Rot bleiben Sieg und Niederlage aus Vereinssicht vorbehalten, Messing bleibt gesperrt.
- Für die Abstufung der Spielklassen die vorhandenen Tokens `--ink`, `--ink-soft`, `--ink-mute` verwenden statt neuer Farben (siehe `docs/styleguide.md`).

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test`, dann im Dev-Modus prüfen:

```bash
./mvnw quarkus:dev -Dquarkus.http.port=8081
```

- `localhost:8081/dfb` zeigt das Rundenband mit „2. Runde", nicht „2. Spieltag", und keine Tabelle.
- Die Seite eines Amateurvereins mit einer einzigen Begegnung rendert ohne Fehler — Verein über die Pokalübersicht heraussuchen.
- Die Seite eines Bundesligavereins in der Liga ist unverändert, mit Tabelle.
- Ausgabe 2023/24 aufrufen und die Begegnung SV Sandhausen gegen Hannover 96 prüfen: angezeigt gehört `3:3 n. E. 7:5`, Sieger Sandhausen. Steht dort `7:5` als Ergebnis, ist Task 5 nicht wirksam.
- Die Torschützenliste eines Vereins, der ein Elfmeterschießen bestritten hat, enthält keine Schützen daraus.
- Die Überraschungen der Ausgabe 2023/24 umfassen dreizehn Begegnungen, 2024/25 zehn, 2025/26 sechs (vom Konzept-Durchgang an den Quelldaten ermittelt — weicht die Zahl deutlich ab, stimmt die Klassenzuordnung nicht).
- Telefonbreite (~400 px): das Rundenband darf nicht seitlich überlaufen.
- Auf der Ligaseite eines Bundesligavereins führt ein Saison-Chip in dessen Pokalwettbewerb, und die Bilanzen beider Wettbewerbe sind getrennt (Abnahmekriterium der Spec).
- Auf einer Pokalbegegnung gibt es keinen Knopf für eine KI-Vorschau, und `POST /dfb/matches/<id>/forecast` wird abgelehnt (Abnahmekriterium der Spec; `ForecastService.supportsForecast` lässt nur bl1/bl2 durch).

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(matchday): Pokal als Rundenband mit Pokallauf und Ueberraschungen

Spec 06: ein K.-o.-Wettbewerb fuehrt keine Tabelle, seine Abschnitte heissen
nach der Runde. Statt eines Turnierbaums — der Wettbewerb wird nach jeder
Runde neu ausgelost — zeigt die Wettbewerbsseite ein Rundenband, die
Vereinsseite den Weg bis zum Ausscheiden. Spielklasse an jedem Verein,
Entscheidungsart am Ergebnis, Ueberraschungen einer Ausgabe eigens
ausgewiesen.

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 10: Nations League anzeigen

**Files:**
- Modify: `src/main/java/de/javamark/matchoracle/matchday/boundary/MatchdayPages.java`
- Modify: `src/main/java/de/javamark/matchoracle/matchday/boundary/MatchdayPageModels.java`
- Create: `src/main/resources/templates/MatchdayPages/groups.html`
- Test: `src/test/java/de/javamark/matchoracle/matchday/boundary/MatchdayPageModelsTest.java` (ergänzen)

**Interfaces:**
- Consumes: `StandingsCalculator.standingsBefore(Matchday)` aus Task 6, `Matchday.groupName` aus Task 2
- Produces: `record GroupSection(String name, List<StandingRow> table, List<MatchRow> matches)`; `GroupsPage(Nav nav, String season, int matchdayNumber, List<GroupSection> groups)`.

- [ ] **Step 1: Write the failing test**

```java
    @Test
    void theGroupsOfACompetitionAreListedInTheirSourceOrder() {
        List<String> names = List.of("Gruppe C", "Gruppe A", "Gruppe D", "Gruppe B");

        List<String> sorted = MatchdayPageModels.sortedGroupNames(names);

        assertEquals(List.of("Gruppe A", "Gruppe B", "Gruppe C", "Gruppe D"), sorted);
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=MatchdayPageModelsTest`
Expected: Kompilierfehler — `sortedGroupNames` existiert nicht.

- [ ] **Step 3: Write minimal implementation**

- `MatchdayPageModels.sortedGroupNames(List<String>)`: alphabetisch sortiert, `null`-frei.
- `MatchdayPages`: für `CompetitionFormat.GROUPS` eine eigene Route bzw. Verzweigung in der bestehenden Wettbewerbsroute, die je Gruppe `standingsBefore(matchday)` aufruft und die Begegnungen des angezeigten Spieltags dieser Gruppe beilegt. Der angezeigte Spieltag kommt wie bei den Ligen aus `Matchday.findDisplayed(league, Instant.now())` — dabei beachten: `findDisplayed`/`findCurrent` arbeiten heute ohne Gruppe. Prüfen, welche Gruppe sie liefern, und die Auswahl bewusst treffen (naheliegend: der niedrigste noch nicht vollständig gespielte Spieltag über alle Gruppen hinweg). Fällt dabei auf, dass `findCurrent` für Gruppenwettbewerbe falsch liegt, gehört die Korrektur in diese Aufgabe — mit einem Test in `MatchdayTest`.
- `groups.html`: vier Tabellen untereinander, darunter die Begegnungen des Spieltags. Bausteine aus `matchday.html` wiederverwenden, kein neues Farbvokabular, kein Messing.

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test`, dann:

```bash
./mvnw quarkus:dev -Dquarkus.http.port=8081
```

- `localhost:8081/nla` zeigt vier Gruppentabellen und die Begegnungen des aktuellen Spieltags.
- Jede Gruppentabelle enthält genau vier Mannschaften.
- Die Seite einer Nationalmannschaft zeigt ihren Gruppentabellenplatz, Spielplan und Torschützen.
- Telefonbreite (~400 px) prüfen: Tabellen dürfen nicht seitlich überlaufen.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(matchday): Nations League mit vier Gruppentabellen anzeigen

Spec 06: ein Gruppenwettbewerb zeigt alle Gruppentabellen untereinander und
die Begegnungen des aktuellen Spieltags — eine Wettbewerbsseite, nicht vier
Eintraege in der Navigation.

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

## Abschluss

Nach Task 10 gegen die Abnahmekriterien der Spec prüfen (`docs/specs/spec-06-pokal-und-nations-league.md`, Abschnitt „Abnahmekriterien"), dann `./mvnw test`, pushen und Pull Request anlegen. Die Dokumentation der Abläufe (`docs/architecture/ablaeufe.md`) um die Ableitung der Gruppenspieltage ergänzen — sie ist die einzige Stelle, an der das System der Quelle etwas hinzufügt, und gehört dorthin, wo die übrigen Abläufe beschrieben sind.
