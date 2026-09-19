# Architekturentscheidungen

Knapper ADR-Stil (Kontext / Entscheidung / Konsequenzen), fortlaufend nach Thema statt nach
Nummer. Belege: Code, `CLAUDE.md`, `docs/specs/*.md`, `docs/kosten-cloud-modelle.md`,
`docs/styleguide.md`, `git log`.

## Feature-Schnitt nach BCE, Zusammenarbeit nur über Boundary

**Kontext:** Drei fachlich unterschiedliche Features (`matchday`, `forecast`, `review`) mit
jeweils eigener Fachlogik und eigenem Datenmodell, aber echten fachlichen Abhängigkeiten
zueinander (siehe unten).

**Entscheidung:** Jedes Feature bekommt ein eigenes Package mit den drei Schichten
`boundary`/`control`/`entity` (Boundary-Control-Entity, `CLAUDE.md`). Innerhalb eines Features
gilt `boundary → control → entity`. Über Feature-Grenzen hinweg darf ausschließlich das
`boundary`-Package des anderen Features angesprochen werden — nie dessen `control` oder
`entity` direkt. `ArchitectureTest` (`src/test/java/de/javamark/matchoracle/ArchitectureTest.java`)
erzwingt das mit drei Regeln: `bce_layers` (Schichtung je Feature), `features_collaborate_only_via_boundary`
(keine Klasse hängt an fremdem `control`/`entity`) und `features_are_free_of_cycles`
(die Feature-Packages als Slices zyklenfrei).

**Konsequenzen:** Fremde Features sehen nur Records/Fassaden (`MatchdayFacade.situationOf`
liefert `MatchSituation`, `ReviewFacade` nimmt `CommittedForecast` entgegen), nie
Panache-Entities eines anderen Features — Kopplung bleibt lose, auch wenn die Fachlogik eng
verzahnt ist. Der Test schlägt sofort fehl, wenn jemand aus Bequemlichkeit direkt auf
`review.entity.Situation` statt auf `ReviewFacade` zugreift.

## Zyklus `matchday`/`review` per CDI-Event statt direktem Aufruf

**Kontext:** `forecast` braucht Daten aus `matchday` und aus `review` (Vergleichsfälle),
`review` braucht Daten aus `matchday` (Ausgangslage) — aber `matchday` muss auch `review`
anstoßen, sobald ein Ergebnis endgültig wird. Ein direkter Aufruf `matchday → review` hätte
zusammen mit `review → matchday` einen Zyklus ergeben, den `features_are_free_of_cycles`
verbietet.

**Entscheidung:** `matchday.boundary` definiert das CDI-Event `ResultFinalized`
(Record mit `matchId`, Toren, Anstoß) und feuert es aus `MatchdaySyncScheduler`, ohne das
Feature `review` zu kennen. `review.boundary.ResultFinalizedObserver` beobachtet das Event.
`forecast → review` bleibt dagegen ein direkter Aufruf (`ReviewFacade.retrospectiveFor`,
`forecastCommitted`), weil `forecast` ohnehin von `review` abhängt und kein Zyklus entsteht.

**Konsequenzen:** Erlaubter Abhängigkeitsgraph ist `forecast → {matchday, review}`,
`review → matchday`, `matchday ⇢ review` nur lose per Event. `matchday` bleibt unabhängig
kompilier- und testbar, auch wenn `review` einmal wegfiele. Nachteil: die Reaktion ist
asynchron im selben Request, nicht sofort nachvollziehbar im Aufruf-Stack — ausgeglichen durch
`ReviewCatchUp`, das nachträglich fehlende Ausgangslagen ergänzt.

## Panache Active Record ohne Repositories, Flyway + Hibernate `validate`

**Kontext:** `CLAUDE.md` gibt Active-Record-Pattern vor (Entities tragen ihre Finder selbst),
kein `PanacheRepository`. Schema-Historie soll nachvollziehbar und reproduzierbar sein.

**Entscheidung:** Alle Entities erben von `PanacheEntity`/`PanacheEntityBase` und tragen
statische Finder (`Match.findByMatchday(...)`, `Team.findByExternalId(...)`). Das Schema
gehört ausschließlich Flyway (`src/main/resources/db/migration/V1`–`V7`); Hibernate läuft mit
`quarkus.hibernate-orm.schema-management.strategy=validate`. Spaltentypen sind `double precision`
(keine `numeric`/`decimal`, z. B. `forecast.home_win`), und jede Entity bekommt eine eigene
Sequenz (`<entity>_seq`, Schrittweite 50) statt einer gemeinsamen, weil Hibernates
`@GeneratedValue(AUTO)` das je Entity erwartet.

**Konsequenzen:** Migrationen sind die einzige Quelle der Wahrheit für das Schema; ein
abweichendes Hibernate-Mapping fällt beim Start auf (`validate` bricht ab statt still
anzupassen). `double precision` passt zu Wahrscheinlichkeiten/Distanzen, die ohnehin nie exakt
sein müssen; ein Check in `V3__forecast_schema.sql` stellt sicher, dass die drei
Wahrscheinlichkeiten einer Prognose zu 1 summieren.

## OpenLigaDB als einzige Datenquelle

**Kontext:** Freie, key-lose Schnittstelle für beide Bundesligen, liefert Begegnungen, Tore,
Mannschaften und einen `getlastchangedate`-Endpunkt je Spieltag.

**Entscheidung:** Einzige externe Quelle (`quarkus.rest-client.openligadb.url`), Mannschaften
werden über ihre stabile `teamId` identifiziert (`Team.externalId`), nicht über den Namen —
Um- oder Neubenennungen ändern nur ein Attribut, nie die Identität. Vor jedem vollen Nachladen
eines Spieltags prüft der Sync `getlastchangedate`; nur bei Änderung wird nachgeladen.
Sync-Takt 15 Minuten (`matchoracle.matchday.sync-interval`), ein Ergebnis gilt 24 Stunden nach
Anstoß und letzter Quelländerung als endgültig (`matchoracle.matchday.final-after`) —
`ResultFinalizer`.

**Konsequenzen:** Kein Vendor-Lock durch API-Key-Verwaltung, aber vollständige Abhängigkeit von
einer einzigen, nicht offiziell garantierten Quelle; fällt sie aus, arbeitet das System mit dem
letzten bekannten Stand weiter (Spec 01, Regeln) und weist dessen Alter aus. Die Frist von 24 h
ist ein Bewertungsmaßstab und ohne Deployment änderbar (Konfigurationswert, nicht Code).

## Ableitungen: Form pro Saison, Direktduelle über alle Saisons, Tabelle „vor Spieltag N“

**Kontext:** Form soll die aktuelle Verfassung einer Mannschaft zeigen, nicht durch
Auf-/Abstieg oder Kaderwechsel verzerrt werden; Direktduelle sollen dagegen die ganze
gehaltene Historie zeigen.

**Entscheidung:** `FormCalculator.form` begrenzt sich auf die laufende Saison — „neue Liga,
neuer Kader“, kein Übertrag über Saisongrenzen (`matchoracle.matchday.form-matches`,
Standard 5). `HeadToHeadCalculator.headToHeadBefore` sucht dagegen über alle gehaltenen
Saisons und Ligen hinweg. Tabellenstände werden nie global vorgehalten, sondern immer als
„Tabelle vor Spieltag N“ berechnet (`StandingsCalculator`), damit Prognose und Rückschau den
Stand zum jeweiligen Zeitpunkt sehen, nicht den heutigen.

**Konsequenzen:** Konsistente, nachvollziehbare Fakten für Bewerter-Agenten und Rückschau;
Kehrseite ist, dass Standings nicht gecacht, sondern bei Bedarf aus den Spielen berechnet
werden — für die gehaltene Datenmenge (drei Saisons) unkritisch.

## Compose Dev Services mit festem Port, ohne Auto-Cleanup

**Kontext:** Lokale Entwicklung mit Postgres über Quarkus Compose Dev Services. Die Voreinstellung
räumt Container und Volumes beim Beenden der JVM auf (Testcontainers/Ryuk) — dabei ging die
Entwicklungsdatenbank zweimal versehentlich verloren.

**Entscheidung:** `compose-devservices.yml` bindet Postgres fest an Port 5432 (damit ein
DB-Client wie IntelliJ oder `psql` jederzeit verbinden kann) und benennt das Volume
`postgres-data`. Im `%dev`-Profil: `quarkus.compose.devservices.stop-services=false`,
`remove-volumes=false`, `ryuk-enabled=false` (`application.properties`, kommentiert mit dem
Grund).

**Konsequenzen:** Die Dev-Datenbank überlebt Neustarts von `quarkus:dev`; Kehrseite ist, dass
sie nicht mehr automatisch aufräumt — manuelles `docker compose -p quarkus-devservices-match-oracle
down` ist nötig, wenn wirklich neu aufgesetzt werden soll. Für Tests bleibt das unkritisch, weil
`%test.quarkus.scheduler.enabled=false` verhindert, dass Tests die externe Quelle anfassen.

## Agentic Workflow: parallele Bewerter, Prüfer-Schleife, ausfallsichere Wrapper

**Kontext:** Spec 02 verlangt drei unabhängige Bewerter (Form, Duell, Umfeld), einen
Prognostiker und einen Prüfer mit höchstens einer Überarbeitungsrunde; fällt ein Bewerter aus,
soll trotzdem prognostiziert werden.

**Entscheidung:** `ForecastWorkflow` ist ein deklarativer `@SequenceAgent` aus einem
`@ParallelAgent` (die drei Bewerter) und einem `@LoopAgent` mit `maxIterations = 2`
(Prognostiker → Prüfer → `ReviewNoteWriter`, endet vorzeitig bei `Verdict.ACCEPTED`). Ein
`@Agent` pro Klasse. Weil das deklarative `@ErrorHandler` in der verwendeten LangChain4j-Version
nicht auf parallele Agenten wirkt, kapselt `ResilientAssessors.guarded` jeden Bewerter: ein
Fehlschlag wird einmal wiederholt, danach liefert er `AssessmentResult.failed(...)` statt den
ganzen Lauf abzubrechen.

**Konsequenzen:** Prognosen entstehen auch bei einem ausgefallenen Bewerter, allerdings mit
breiterer Streuung (der Prognostiker wird per Prompt entsprechend angewiesen) und niedrigerem
Sicherheitsgrad (siehe unten). Die `@ExitCondition` wird nach jedem Sub-Agenten ausgewertet,
weshalb `verdict` vom Aufrufer mit `ForecastWorkflow.NOT_REVIEWED` (`REVISE`, leerer Grund)
vorbelegt werden muss, bevor der Prüfer je gesprochen hat.

## Sicherheitsgrad wird berechnet, nicht erfragt

**Kontext:** Ein vom Modell selbst geschätzter Sicherheitsgrad ist unzuverlässig kalibriert und
schwer mit einem Bewerter-Ausfall zu verrechnen.

**Entscheidung:** `ForecastService.confidence` errechnet den Sicherheitsgrad aus der
Wahrscheinlichkeit der Tendenz (höchster der drei Werte aus `Probabilities`), multipliziert mit
0,75 je ausgefallener Bewertung.

**Konsequenzen:** Sicherheitsgrad ist reproduzierbar und ohne Modellaufruf herleitbar; die
Rückschau kann ihn direkt mit der Trefferquote vergleichen (Kalibrierungsurteil
`OVERCONFIDENT`/`UNDERCONFIDENT`/`APPROPRIATE` in `Evaluation`). Ein Bewerter-Ausfall wirkt sich
spürbar aus (0,75³ ≈ 0,42 bei allen drei ausgefallen), was gewollt ist.

## Modellstrategie: Cloud zuerst, lokaler Fallback, striktes JSON

**Kontext:** Ein Cloud-Modell liefert bessere Urteilskraft für den Prüfer, soll aber nicht die
einzige Option sein (Erreichbarkeit, Kosten, Datenschutz). Frühere Läufe brachen am JSON-Parsing,
weil das Modell typografische Anführungszeichen erzeugte (`fix(forecast): keep Sonnet's JSON
intact`, Commit `56c33c0`).

**Entscheidung:** `FallbackChatModel` versucht zuerst das benannte Modell `cloud`
(Anthropic `claude-sonnet-5`) und bei jeder `RuntimeException` (nicht erreichbar, fehlender/
ungültiger Key, Guthaben leer, Timeout) das lokale Ollama-Modell (`gemma4:12b`). Beide melden
über `supportedCapabilities` strukturierte JSON-Ausgabe (`response-format=json` bei Anthropic,
`format=json` bei Ollama), sodass die Antwortstruktur als Schema statt als Prompt-Beschreibung
übertragen wird — verhindert kaputtes JSON durch Anführungszeichen im Fließtext. Temperatur
0,3 (Cloud) bzw. 0,2 (lokal) hält die Antworten steady. `matchoracle.forecast.cloud-enabled=false`
schaltet die Cloud vollständig ab (z. B. zum Modellvergleich).

**Konsequenzen:** Ein Ausfall des Cloud-Anbieters blockiert keine Prognose. `ModelUsage`
protokolliert, welches Modell tatsächlich geantwortet hat (`Forecast.modelName`), sodass die
Rückschau später nach Modell auswerten könnte. Kostenschätzung: rund 10 $ pro Saison bei Sonnet 5,
siehe `docs/kosten-cloud-modelle.md`; das lokale Modell bleibt kostenlos, aber deutlich
langsamer (~1 min statt Sekunden je Vorschau).

## Rücktests nur für die laufende Saison, getrennt ausgewiesen

**Kontext:** Um die Prognosequalität schneller zu beurteilen als „eine Saison abwarten“,
sollen auch bereits gespielte Begegnungen prognostiziert werden können — aber ohne das
Ergebnis zu leaken und ohne die Trefferbilanz der echten (Live-)Prognosen zu verfälschen.

**Entscheidung:** `POST /{league}/{season}/{number}/backtests` stellt alle gespielten
Begegnungen der laufenden Saison ohne vorhandene Prognose in die Queue (`backtest=true`).
Das Flag wandert von `Forecast` über `RecordedForecast` bis `ForecastEvaluation`
(`V6__backtest_flag.sql`); bei einem Rücktest ruft `ReviewService.recordForecast` sofort
`recordSituation` und `evaluate` auf, statt auf das `ResultFinalized`-Event zu warten, weil das
Ergebnis ja schon feststeht.

**Konsequenzen:** `AccuracyReport` weist Live- und Rücktest-Serie getrennt aus (eigene
Comparison-Datensätze je Serie); eine schöngerechnete Gesamt-Trefferquote entsteht nicht.
Beschränkung auf die laufende Saison, weil Form/Tabelle für ältere Saisons weniger verlässlich
und die Vergleichsfälle dünner sind.

## Rückschau für neue Prognosen: nur Vergangenheit, Mindest-/Höchstzahl, Basisprognose als Maßstab

**Kontext:** Spec 03 verlangt, dass die Rückschau bei zu dünner Datenlage ausdrücklich nichts
liefert statt einer unsicheren Zahl, und dass Vergleichsfälle nur aus der Vergangenheit
stammen dürfen (kein Blick in die Zukunft).

**Entscheidung:** `Similarity` sucht Vergleichsfälle ausschließlich unter
`Situation.findBefore(kickoff)` (nur Spiele vor dem Anstoß der zu prognostizierenden
Begegnung), mindestens 3, höchstens 5 (`matchoracle.review.min-similar-cases`/`max-similar-cases`).
Weniger als 3 Fälle → `Optional.empty()`, der Prognostiker bekommt „Keine belastbare Rückschau
verfügbar“ statt erfundener Fälle. Die Trefferbilanz zeigt Trefferquote/Brier/Skill-Score erst
ab 10 Bewertungen (`matchoracle.review.min-evaluations`), sonst `null` mit Hinweis auf dünne
Datenlage. Als Maßstab dient `BaselineForecast`, eine Poisson-Schätzung aus den
Tor-Durchschnitten vor Anstoß mit Schrumpfung Richtung Liga-Mittelwert (`PRIOR_WEIGHT = 5`
Spiele wiegen wie der Liga-Prior) — nicht nur die KI-Vorschau gegen sich selbst gemessen.
Zusätzlich zeigt eine einfache Trefferverlauf-Sicht (`AccuracyReport.recent`, „plain hit/miss
timeline“, Commit `c8cceab`) die letzten Bewertungen als Treffer/Fehlschlag ohne
Statistik-Schwelle — auch unterhalb von 10 Bewertungen lesbar.

**Konsequenzen:** Ehrlicher Umgang mit Datenmangel statt Pseudo-Genauigkeit (Spec-3-Regel
wörtlich umgesetzt). Der Skill-Score (`1 − Brier_KI / Brier_Basis`) macht sichtbar, ob die
Agenten überhaupt besser sind als eine simple Statistik — ein negativer Wert wäre ein
Warnsignal, keine Ausnahme, die versteckt wird.

## Sprache: deutsche Prompts und UI, englischer Code

**Kontext:** `CLAUDE.md` verlangt englischen Code (Packages, Klassen, Felder, Kommentare), die
Specs und die fachliche Kommunikation sind aber deutsch, ebenso die Zielgruppe der Anwendung.

**Entscheidung:** Prompts an die Agenten, UI-Texte (Qute-Templates) und Fehlermeldungen für
Betrachter sind deutsch; der gesamte Code (inklusive Agenten-Beschreibungen als
Java-Doc-Kommentare, aber `@Agent(description = "...")` teils deutsch, weil das der Prompt-Text
selbst ist) folgt dem Glossar in `CLAUDE.md` (z. B. Prognose → `Forecast`, Prüfer → `Reviewer`,
Ausgangslage → `Situation`).

**Konsequenzen:** Zwei-Sprachen-Konsistenz erfordert Disziplin — neue Fachbegriffe müssen erst
im Glossar ergänzt werden, bevor sie im Code auftauchen (`CLAUDE.md`, Regel am Ende des
Glossars). Reduziert Übersetzungsfehler zwischen Spec und Code.

## UI: serverseitiges Qute + htmx + Chart.js, kein SPA

**Kontext:** Die Anwendung ist überwiegend lesend (Tabellen, Spielpläne, Vorschauen), nur
wenige interaktive Stellen (Vorschau anstoßen, Fortschritt anzeigen). Ein SPA-Framework wäre
für diesen Umfang unverhältnismäßig.

**Entscheidung:** Serverseitiges Rendering mit Qute (`@CheckedTemplate`, logikfreie Templates,
View-Models als Records), htmx 2 für Fragmente (`summary`, `markers`, `progress`) und Polling,
Chart.js 4 für die wenigen Diagramme — beide Bibliotheken lokal unter `META-INF/resources/vendor/`
statt per CDN. Gestaltung nach `docs/styleguide.md`: helle Seite, genau eine dunkle Bühne
(Top-Navigation und Anzeigetafel), Messing (`--accent`) ausschließlich für Wege zur und
Elemente der KI-Vorschau. Trefferbilanz und Bewertungsmaßstäbe liegen in der Fußzeile, nicht in
der Top-Navigation.

**Konsequenzen:** Kein Build-Schritt für ein Frontend-Framework, keine JSON-API-Verdopplung nur
für die UI (die JSON-Endpunkte existieren parallel, aber die HTML-Seiten rendern serverseitig).
Die Fußzeilen-Platzierung von Trefferbilanz/Bewertungsmaßstäben drückt aus, dass es
Hintergrundinformation ist, kein Kernweg des Betrachters (der ist: Liga → Spieltag → Begegnung
→ Vorschau).

## SEO/GEO-Basics: statische Sitemap, `SiteGlobals` in `matchday.boundary`

**Kontext:** Die Marke wurde auf „Rasen-Radar" vereinheitlicht (vorher parallel „Bundesliga
aktuell" im UI, „Rasenradar" in Manifest/Footer/Repo); Domain wird `rasen-radar.de`,
`rasenradar.markserver.de` bleibt als 301-Redirect bestehen. Für Auffindbarkeit (klassische
Suche und KI-Suchmaschinen wie ChatGPT/Perplexity) fehlten technische Grundlagen: Meta-Description,
Canonical-URL, Open-Graph-Tags, `robots.txt`, `sitemap.xml`. `base.html` wird von allen drei
Features eingebunden, ist also naturgemäß fachübergreifend.

**Entscheidung:** `base.html` bekommt `{#insert description}` (analog zum bestehenden
`{#insert title}`), jede Content-Seite überschreibt es mit `{#description}...{/description}`.
Canonical- und Open-Graph-URL werden aus einem neuen `matchoracle.site.base-url` (Config,
`%prod` auf `https://rasen-radar.de`) plus dem aktuellen Pfad (`{inject:vertxRequest.path()}`,
Qutes eingebauter `inject`-Namensraum) zusammengesetzt; `base-url` selbst ist ein Qute-`@TemplateGlobal`
(`SiteGlobals`). Die Klasse liegt in `matchday.boundary`, nicht in einem neuen Top-Level-Package —
dorthin greifen `forecast` und `review` bereits für geteilte Seiten-Bausteine (`MatchdayPageModels.Nav`)
zu, `matchday.boundary` ist also schon der De-facto-Ort für fachübergreifende Template-Belange.
`robots.txt`/`sitemap.xml` sind vorerst statische Dateien unter `META-INF/resources` mit nur den
festen, ligabezogenen Seiten (`/bl1`, `/bl2`, Torschützen, Trefferbilanz, Bewertungsmaßstäbe) —
keine dynamische Erzeugung für die ~36 Vereins- oder die Spieltag-Seiten, bis sich die SEO-Nische
als lohnend erweist.

**Konsequenzen:** Kein neues Package nur für einen Cross-Cutting-Concern, aber `matchday.boundary`
trägt jetzt auch eine Zuständigkeit, die inhaltlich zu keinem der drei Features gehört — bei
weiterem Wachstum (z. B. dynamische Sitemap mit Vereins-/Spieltag-URLs) sollte neu bewertet werden,
ob ein eigenes Package den Schnitt sauberer hält. Die Sitemap muss von Hand erweitert werden, wenn
neue feste Seiten dazukommen; Vereinsseiten tauchen dort vorerst gar nicht auf.

## Prognose-Auslösung: Terminplanung statt Knopfdruck

**Kontext:** Spec 02 sah den Betrachter als Auslöser jeder Prognose vor
(Basic-Auth-geschützter Knopfdruck, siehe „Rate Limiting" in der
Deployment-Doku). Das begrenzt die tatsächliche Aktualität auf die Anwesenheit
eines Menschen und passt nicht zum Anspruch, dass die KI-Vorschau möglichst
zeitnah zum Anstoß mit dem aktuellen Wissensstand entsteht.

**Entscheidung:** Ein systeminterner Akteur „Terminplanung" löst Prognosen aus,
nicht mehr der Betrachter. Zwei Fälle: (1) bevorstehende Begegnungen dürfen bis
zum Anstoß mehrfach neu prognostiziert werden, maßgeblich ist die zuletzt vor
dem Anstoß entstandene Prognose; (2) unprognostiziert gebliebene, aber bereits
fällige Begegnungen der laufenden Saison werden in einem eigenen, wiederkehrenden
Takt nachgeholt, gedeckelt auf eine feste Obergrenze je Lauf. Der Rücktest bleibt
die einzige vom Betrachter selbst angestoßene Prognoseart. Details siehe Spec 02,
Abschnitt „Auslöser".

**Konsequenzen:** Die bestehende Kostenschutz-Kette (Basic Auth, Cloudflare
Rate-Limiting-Regel auf die POST-Route) sichert weiterhin fremden Zugriff auf
die manuellen Auslöser (insbesondere den Rücktest) ab, schützt aber nicht mehr
gegen die eigene Automatik — das übernimmt stattdessen die feste Obergrenze je
Lauf. Die Umsetzung (welcher Scheduler-Typ, welcher Takt, welche Obergrenze
konkret) ist noch offen und wird beim Bau entschieden, nicht in der Spec.

## Landingpage statt Redirect auf /bl1

**Kontext:** `/` leitete bisher direkt auf `/bl1` weiter — kein neutraler Einstiegspunkt,
keine Sicht auf beide Ligen auf einen Blick. Für den ersten Eindruck und die
Auffindbarkeit (siehe SEO/GEO-Basics oben) reicht eine reine Weiterleitung nicht:
Besucher landen sofort auf einer liga-spezifischen Seite ohne Kontext.

**Entscheidung:** `/` rendert jetzt eine eigene Landingpage (`MatchdayPages.home`,
Template `home.html`): je Liga eine Karte mit dem aktuellen Spieltag
(tagesgruppierte Paarungen, dieselben KI-Vorschau-Marker wie auf der vollen
Spieltagsseite) und einer kompakten Tabellen-Vorschau (oberste sechs Plätze, Link
zur vollständigen Tabelle). Zwischen beiden Ansichten schaltet ein CSS-only-Tab um
(siehe Styleguide, „CSS-only Tabs") — kein JavaScript, mit Tastatur bedienbar. Auf
schmalen Bildschirmen fällt das Karten-Grid per `auto-fit`/`minmax` ohne eigene
Media-Query auf eine Spalte zurück.

**Konsequenzen:** `MatchdayPageModels.LandingLeagueSection` und
`MatchdayPages.landingSection` sind ein zusätzlicher, dünner Formatierungspfad
neben `matchdayPage` — beide teilen sich `DayGroup`/`MatchRow`/`StandingRow`,
keine doppelte Fachlogik. Nebeneffekt, der auch der vollen Spieltagsseite
zugutekommt: Mannschaftsnamen in Paarungszeilen kürzen jetzt mit Ellipsis statt in
eine zweite Zeile umzubrechen (brach zuvor das Zeilenraster auf schmalen
Bildschirmen), unter 560 px zusätzlich mit dem kurzen statt vollen Mannschaftsnamen.
Die KI-Vorschau-Marker zeigen dabei den Richtwert („2:1") statt der bisherigen
Toto-Kürzel („KI 1/X/2"), Tendenz und Wahrscheinlichkeit stehen im Tooltip — siehe
auch die angepasste Farbregel im Styleguide. Damit ist ein erster Baustein aus der
Konkurrenzanalyse umgesetzt: Tabs innerhalb einer Karte, nicht als Seiten-weite
Reiter (die „Navigationsstruktur"-Frage unten bleibt davon unberührt offen).

## Saisonaussicht (Spec 04): eigenständige Statistik-Simulation, keine Historie

**Kontext:** Der Diskussionsstand in `docs/specs/spec-04-saisonaussicht.md` hatte sechs offene
Modellierungsfragen: Abhängigkeit von den Einzelspiel-Prognosen aus Spec 2, Ausdrucksform,
Aktualisierungsanlass, Unveränderlichkeit, Rückschau-Bezug und ob die Champions-League-Ligaphase
mit einbezogen wird.

**Entscheidung:** Die Saisonaussicht rechnet eigenständig auf Basis von Tabellenlage und bisheriger
Punkt-/Torausbeute, unabhängig von den KI-Vorschauen aus Spec 2 — läuft ohne KI-Kosten und
unabhängig davon, wie viele Restbegegnungen bereits eine Prognose haben. Ausdrucksform ist eine
einzelne Wahrscheinlichkeit je Platzierungsziel (nicht als Bandbreite). Neuberechnung nach jedem
beendeten Spieltag der jeweiligen Liga; jede Neuberechnung ersetzt den vorherigen Stand vollständig
— eine Momentaufnahme ohne Historie, kein Rückschau-Bezug zu Spec 3. Die Champions-League-Ligaphase
bekommt ebenfalls eine Saisonaussicht, auf derselben statistischen Grundlage. Details siehe
Spec 04, Abschnitt „Im Spec-Interview geklärt".

**Konsequenzen:** Spec 4 hängt an keiner Stelle von Spec 2 ab — beide Features können unabhängig
voneinander ausfallen oder fehlen, ohne einander zu blockieren. Die Platzierungsziele je Liga sind
dieselben Tabellenzonen, die `MatchdayPageModels.zone()` (Spec 1) bereits für die Tabellendarstellung
kennt (Meisterschaft/Europapokal/Abstieg bzw. Auf-/Abstieg, Play-offs) — kein neuer Begriff, nur eine
neue Verwendung. Ohne Historie entfällt jede spätere Möglichkeit zu zeigen, wie sich eine
Saisonaussicht über Zeit entwickelt hat oder wie gut sie am Saisonende lag; das war eine bewusste
Abwägung gegen den zusätzlichen Nutzen einer Historie. Noch nicht umgesetzt — dieser ADR hält den
Ausgang des Spec-Interviews fest, bevor gebaut wird.

## Verschoben / abgelehnt

- **DFB-Pokal:** nicht aufgenommen. Das K.-o.-Format hat keinen Spieltagszähler und keine
  Tabelle — beides trägt das gesamte Datenmodell (`Matchday.number` je Liga/Saison,
  `StandingsCalculator`). Eine Aufnahme würde das Modell verbiegen statt erweitern
  (Spec 01, Abgrenzung: „Keine weiteren Wettbewerbe“).
- **Aufstellungen:** nicht aufgenommen. OpenLigaDB liefert dazu keine Daten — `OpenLigaDbMatch`
  bildet nur Tore (mit Schütze und Minute), keine Kader oder Positionen ab; eine andere Quelle
  wäre nötig und ist nicht vorgesehen (Spec 01, Abgrenzung: „Keine Spielerstatistiken über die
  Torschützen hinaus“).
- **Champions League:** offen, nicht abgelehnt. Die Ligaphase (seit 2024/25 eine gemeinsame
  Tabelle) würde technisch ins bestehende Modell passen (ein Spieltagszähler, eine Tabelle),
  im Unterschied zum Pokal. Bisher nicht umgesetzt, weil Spec 01 den Umfang bewusst auf beide
  Bundesligen begrenzt (Abgrenzung: „Keine weiteren Wettbewerbe (Pokal, europäische
  Wettbewerbe) in dieser Ausbaustufe“) — eine spätere Ausbaustufe müsste das im Spec-Interview
  klären, unter anderem wie K.-o.-Runden nach der Ligaphase gehandhabt würden.
- **Navigationsstruktur — mehr Übersichten als Reiter statt Fußzeilen-Links:** offen, nicht
  entschieden. Aktuell: Top-Navigation nur Liga-Wechsel (bl1/bl2), Trefferbilanz und
  Bewertungsmaßstäbe liegen in der Fußzeile (siehe „UI: serverseitiges Qute…" oben). Eine
  Konkurrenzanalyse zeigte eine Gliederung mit klar getrennten Reitern (Start, Tabelle,
  Spieltag, Prognose, Trefferbilanz, Lernkurve, Historie, Statistik) als lernenswertes Vorbild
  für die Struktur — nicht für Optik/Wortwahl. Für uns nicht 1:1 übertragbar, weil `matchday.html`
  Tabelle und Spieltag heute bewusst auf einer Seite zusammenführt (Kernweg „Liga → Spieltag →
  Begegnung → Vorschau", siehe UI-ADR). Eine Aufteilung in mehr Reiter berührt sowohl den
  Styleguide (`docs/styleguide.md`, Navigation bisher nicht dokumentiert) als auch die Frage,
  wo eine künftige Saisonaussicht (Spec 04) und die Trefferbilanz-Erweiterungen (Spec 03) einen
  Platz bekämen. Kein Spec-Interview, kein Termin — festgehalten, damit die Idee nicht verloren
  geht. Ein erstes Stück davon steckt inzwischen in der Landingpage („Landingpage statt Redirect
  auf /bl1" oben): Spieltag/Tabelle als Tabs innerhalb einer Liga-Karte, mit demselben
  CSS-only-Muster, das sich auch für eine seitenweite Reiter-Navigation eignen würde — die
  grundsätzliche Frage bleibt aber offen.
