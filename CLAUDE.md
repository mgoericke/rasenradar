# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Arbeitsweise (wichtig, zuerst lesen)

Der Nutzer ist Fußballfan (Bundesliga) und hat dieses Projekt aus genau diesem Interesse heraus
begonnen — nicht in erster Linie, um Quarkus oder KI-Agenten zu lernen. Er will trotzdem verstehen,
was passiert, mitentscheiden und bei Interesse selbst mitarbeiten.

- **Nicht drauflos generieren.** Bei größeren Schritten kurz erklären, *was* und *warum*; bei genuinen Entscheidungen (Modellierung, Bibliothek, Abwägung) fragen statt annehmen. Kohärente, nachvollziehbare Einheiten statt willkürlicher Zwischenstände.
- Wenn der Nutzer etwas selbst schreibt: danach gemeinsam durchgehen, nicht stillschweigend umschreiben.
- Entscheidungen mit mehreren sinnvollen Optionen (Package-Schnitt, Modellierung, Bibliothek) als Frage stellen, nicht allein treffen.
- **Dependency-Versionen immer aktuell recherchieren** (Context7 / Web), bevor sie in die `pom.xml` kommen. Nie aus dem Gedächtnis eintragen. Quarkus-Extensions kommen über die BOM ohne eigene Version.

## Tech-Stack und Vorgaben

- **Java 21** (`maven.compiler.release=21` in der pom.xml). Lokal ist ein neueres JDK installiert — trotzdem keine Sprachfeatures jenseits von 21 verwenden.
- **Quarkus** (Platform-Version in `quarkus.platform.version`), Maven Wrapper `./mvnw`.
- **Externe Spieldatenquelle: OpenLigaDB** (`https://api.openligadb.de`, Ligen `bl1`/`bl2`, kein API-Key). Mannschaften werden über deren stabile `teamId` identifiziert; `getlastchangedate` pro Spieltag entscheidet, ob nachgeladen wird.
- **LangChain4j** über `quarkus-langchain4j-agentic` und `quarkus-langchain4j-skills` — die Analyse-Agenten aus Spec 2 werden darauf aufgebaut.
- **Sprache im Code: Englisch.** Packages, Klassen, Methoden, Felder, Migrationen, Kommentare — alles englisch. Die Specs bleiben deutsch; die Übersetzung der Fachbegriffe steht in der Tabelle unten und muss konsistent verwendet werden.
- **Hibernate ORM mit Panache, Active-Record-Pattern**: Entities erben von `PanacheEntity` (bzw. `PanacheEntityBase` bei eigener ID) und tragen ihre Finder-Methoden als `static` Methoden selbst (`Match.findByMatchday(...)`). Keine separaten Repository-Klassen (`PanacheRepository`).
- **PostgreSQL** als Datenbank, für die Entwicklung per **Docker Compose** (Quarkus Dev Services / Compose Dev Services), nicht per Testcontainers-Zufallscontainer.
- **Flyway** für Schema-Migrationen (`src/main/resources/db/migration/V<n>__<beschreibung>.sql`). Kein `hibernate.hbm2ddl` in Prod-Profilen.
- **ArchUnit** erzwingt die Architekturregeln als Test (siehe unten).
- **Tests nur für fachlich relevante Features** — keine Tests für Getter, triviale Mapper oder Framework-Verhalten. Was getestet wird: Fachregeln aus den Specs (z. B. `PROVISIONAL`/`FINAL`, Wahrscheinlichkeiten summieren sich zu 1, Reviewer-Schleife läuft höchstens einmal).

## Architektur: BCE (Boundary – Control – Entity)

Jedes fachliche Feature ist ein eigenes Package unter `de.javamark.matchoracle` mit den drei Schichten:

```
de.javamark.matchoracle.<feature>
├── boundary   – Einstiegspunkte: REST-Resources, Scheduler, Event-Listener, Agenten-Fassaden
├── control    – Fachlogik, Services, Berechnungen
└── entity     – Domänenmodell: Panache-Entities (inkl. ihrer statischen Finder), Value Objects, Enums
```

Abhängigkeitsregel (per ArchUnit geprüft):

- `boundary` → darf `control` und `entity` nutzen
- `control` → darf `entity` nutzen, **nicht** `boundary`
- `entity` → darf weder `control` noch `boundary` kennen
- Features greifen nicht in fremde `control`/`entity`-Packages hinein; feature-übergreifende Zusammenarbeit läuft über `boundary` (oder Events)

Die Features ergeben sich aus den Specs in `docs/specs/`:

| Spec | Fachlicher Kern | Package |
|------|-----------------|---------|
| 01 Spieltagsdaten | Begegnungen/Ergebnisse beider Bundesligen aus externer Quelle laden, vorläufig → endgültig, Form/Tabelle/Direktduelle ableiten | `matchday` |
| 02 Spieltag-Prognose | Drei parallele Bewerter-Agenten (Form, Duell, Umfeld) → Prognostiker → Prüfer mit max. einer Überarbeitungsrunde; Prognose ist danach unveränderlich | `forecast` |
| 03 Rückschau und Lernen | Prognosen gegen endgültige Ergebnisse abgleichen, Ausgangslagen speichern, Vergleichsfälle für neue Prognosen liefern, Trefferbilanz | `review` |
| 04 Saisonaussicht | Je Mannschaft Wahrscheinlichkeiten für Platzierungsziele der Restsaison, statistisch simuliert, neu berechnet nach jedem beendeten Spieltag | `season` |
| 05 Prognose nachvollziehbar | Zustandekommen, Prüfer-Einwand, Verlauf, Rangliste und Kalibrierung für den Betrachter lesbar machen — keine neuen KI-Schritte, nur Aufbereitung vorhandener Daten | `forecast`/`review` (Boundary-Ergänzung, kein eigenes Package) |
| 06 Pokal und Nations League | DFB-Pokal (K.-o., Runden) und Nations League (parallele Gruppen) nur zur Ansicht; Wettbewerbsform statt Sonderfällen je Liga | `matchday` (kein eigenes Package) |

Fachliche Abhängigkeiten: `forecast` liest Daten aus `matchday` und die Rückschau aus `review`; `review` reagiert darauf, dass ein Ergebnis in `matchday` endgültig wird und auf festgeschriebene Prognosen aus `forecast`; `season` reagiert ebenfalls darauf, dass ein Ergebnis in `matchday` endgültig wird (eigenständig von `forecast`/`review`, keine Abhängigkeit zwischen den beiden).

### Glossar Spec (deutsch) → Code (englisch)

| Spec | Code |
|------|------|
| Liga (1./2. Bundesliga) | `League` |
| Saison | `Season` |
| Spieltag | `Matchday` |
| Begegnung | `Match` |
| Mannschaft | `Team` |
| Tor (Minute, Schütze) | `Goal` |
| Halbzeitstand / Endstand | `halfTimeScore` / `fullTimeScore` |
| vorläufig / endgültig | `ResultStatus.PROVISIONAL` / `FINAL` |
| Form | `Form` |
| Direktes Duell | `HeadToHead` |
| Tabellenposition | `StandingPosition` / `Standings` |
| Bilanz (Siege/Unentschieden/Niederlagen, Tore) | `Balance` |
| Spielausgang aus Sicht einer Mannschaft (Sieg/Unentschieden/Niederlage) | `TeamResult.WIN` / `DRAW` / `LOSS` |
| Begegnung aus Sicht einer Mannschaft | `TeamMatchView` |
| Prognose | `Forecast` |
| Formbewerter / Duellbewerter / Umfeldbewerter | `FormAssessor` / `HeadToHeadAssessor` / `ContextAssessor` |
| Bewertung (eines Bewerters) | `Assessment` |
| Prognostiker | `Forecaster` |
| Prüfer, angenommen / überarbeiten | `Reviewer`, `Verdict.ACCEPTED` / `REVISE` |
| Tendenz (Heimsieg / Unentschieden / Auswärtssieg) | `Outcome.HOME_WIN` / `DRAW` / `AWAY_WIN` |
| Sicherheitsgrad | `confidence` |
| Bewertungsmaßstäbe | `ForecastParameters` |
| Rückschau | `Review` (Package) / `Retrospective` (das Ergebnis für den Prognostiker) |
| Ausgangslage | `Situation` |
| Vergleichsfall | `SimilarCase` |
| Trefferbilanz | `HitRate` / `AccuracyReport` |
| Torschütze (Torschützenliste eines Vereins) | `Scorer` / `ScorerCalculator` |
| Basisprognose (Statistik-Maßstab der Rückschau) | `Baseline` / `BaselineForecast` |
| Maßstab in der Trefferbilanz (Basisprognose) | `AccuracyReport.Comparison` |
| Skill-Score | `skillScore` |
| Saison einer Liga (z. B. 2. Bundesliga 2024/25) | `LeagueSeason` |
| Vereinsseite (Spielplan, Saisonverlauf, Bilanz, Torschützen) | `TeamPage` / `ScheduleRow` |
| Champions-League-Ligaphase | `League.CHAMPIONS_LEAGUE` (nur Anzeige, keine KI-Vorschau) |
| Wettbewerbsform (Tabellen-/Gruppen-/K.-o.-Wettbewerb) | `CompetitionFormat.TABLE` / `GROUPS` / `KNOCKOUT` |
| DFB-Pokal | `League.DFB_POKAL` (K.-o., nur Anzeige) |
| Nations League (Liga A) | `League.NATIONS_LEAGUE` (Gruppen, nur Anzeige) |
| Runde (Achtelfinale, Endspiel …) | `Matchday.label` — Name des Abschnitts im K.-o.-Wettbewerb |
| Gruppe (Gruppe A … D) | `Matchday.groupName` — eigene Tabelle und eigener Spieltagszähler je Gruppe |
| Saisonaussicht | `SeasonOutlook` (Package `season`) |
| Platzierungsziel (Meisterschaft, Europapokal, Auf-/Abstieg, Play-off, K.-o.-Runde, Ausscheiden) | `PlacementGoal` — Definition der Schwellenwerte je Liga lebt in `matchday`, da sie dieselbe ist wie bei den Tabellenzonen-Farben |
| Zustandekommen (einer Prognose) | `ForecastTrace` |
| Einwand des Prüfers | `Reviewer.objection` / `Objection` |
| Verlauf vor dem Anstoß | `ForecastHistory` |
| Gegen den Strom (abweichend von Basisprognose) | `contrarian` / `ContrarianOutcome` |
| Aus der Erfahrung (Vergleichsfälle in Prosa) | `ExperienceNote` |
| Rangliste (Prognose / Basisprognose) | `Leaderboard` |
| Kalibrierungssatz | `CalibrationStatement` |
| Mut-Bilanz | `ContrarianReport` |
| Erklärseite „So entsteht eine Prognose" | `HowItWorksPage` |
| „Was das System nicht weiß" | `KnownBlindSpots` |
| Mindestanzahl (Schwelle für Bilanzen) | `minimumSampleSize` (bereits Spec 3) |

Neue Fachbegriffe hier ergänzen, bevor sie im Code verwendet werden.

## Styleguide (`docs/styleguide.md`)

Alle UI-Änderungen folgen `docs/styleguide.md`: heller Grund, genau eine dunkle Bühne
(Top-Navigation und Anzeigetafel), Grün/Rot nur als Bedeutung, Messing (`--accent`) nur
für die KI-Vorschau, Bricolage Grotesque für Display, IBM Plex Sans für Text. Neue Tokens
oder Bausteine dort eintragen, bevor sie ins CSS kommen.

## Specs (`docs/specs/`)

Die Specs sind **rein fachlich** (SDD-Stil): keine Frameworks, HTTP-Methoden, Datentypen. Technische Entscheidungen gehören nicht in die Specs, sondern in Code, ADRs oder diese Datei. Jede Spec hat einen Abschnitt „Im Spec-Interview zu klären" — diese offenen Punkte vor der Implementierung des jeweiligen Features mit dem Nutzer klären, nicht stillschweigend annehmen.

Wichtige Fachregeln, die im Code sichtbar sein müssen:

- Jede Liga hat ihren eigenen Spieltagszähler (kein gemeinsamer Zähler).
- Vorläufige Ergebnisse dürfen angezeigt, aber nicht für Rückschau/Lernen verwendet werden.
- Prognosen und deren Bewertungen werden nie geändert oder gelöscht; eine erneute Prognose ist ein neuer Eintrag.
- Bewertungsmaßstäbe (Heimvorteil, Formzeitraum, Aufsteiger) sind zur Laufzeit ohne Deployment änderbar.
- Die Rückschau liefert bei zu dünner Datenlage ausdrücklich *nichts* statt einer Zahl.
- Keine Quoten, keine Wettempfehlungen.

## Befehle

```bash
./mvnw quarkus:dev                         # Dev-Modus mit Live-Reload, Dev UI: http://localhost:8080/q/dev/
./mvnw test                                # Unit-Tests (Surefire)
./mvnw test -Dtest=KlassenName             # einzelne Testklasse
./mvnw test -Dtest=KlassenName#methode     # einzelne Testmethode
./mvnw verify -DskipITs=false              # inkl. Integrationstests (Failsafe, *IT.java)
./mvnw package                             # Build → target/quarkus-app/quarkus-run.jar
./mvnw package -Dnative -Dquarkus.native.container-build=true   # Native Image im Container
```

Im Dev-Modus steht auch die kontinuierliche Testausführung über die Quarkus-Konsole (Taste `r`) zur Verfügung.

```bash
python3 scripts/branch-tokens.py            # Claude-Code-Tokenverbrauch des aktuellen Branches, als Markdown für die PR-Beschreibung
python3 scripts/branch-tokens.py --all      # Rangliste über alle Branches
```

Der Verbrauch wird aus den Sitzungsprotokollen unter `~/.claude/projects/` gelesen; jede Zeile
dort trägt den Branch, unter dem sie entstanden ist. **Was vor dem Branch passiert — Spec-Gespräche,
Planung, Recherche — liegt auf `main` und fehlt dem Feature**; die Zahl taugt zum Vergleich zwischen
Features, nicht als vollständige Kostenzuordnung. Protokolle werden nach 30 Tagen gelöscht.

## Branching-Konvention

**Für jedes neue Feature IMMER einen eigenen Branch anlegen — niemals direkt auf `main` committen.**

Mehrere Features können in einem gemeinsamen Branch entwickelt werden, wenn sie inhaltlich
zusammengehören oder voneinander abhängen.

Branch-Namensschema: `feature/<kurzbeschreibung>`

Commit-Präfixe (scoped, wie in der bisherigen Historie): `feat(scope):`, `fix(scope):`,
`docs(scope):`, `chore(scope):`, `test(scope):` — `scope` ist meist das betroffene Feature
(`matchday`, `forecast`, `review`) oder Thema (`architecture`, `ui`).

Vorgehen für neue Features:
1. `git checkout main && git pull`
2. `git checkout -b feature/<kurzbeschreibung>`
3. Entwickeln und committen
4. `git push -u origin feature/<kurzbeschreibung>`
5. Pull Request erstellen — `.github/workflows/build.yml` führt die Tests aus

## Aktueller Stand

Frisch generiertes Quarkus-Skeleton: noch kein eigener Java-Code, keine Tests, `application.properties` ist leer, kein `docker-compose`, kein Flyway, kein ArchUnit. Diese Bausteine werden schrittweise gemeinsam mit dem Nutzer aufgebaut.
