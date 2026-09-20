# Design: Saisonaussicht (Spec 4) — `season`

Grundlage: `docs/specs/spec-04-saisonaussicht.md`. Dieses Dokument trifft die technischen
Entscheidungen, die die Spec bewusst offen lässt.

## Ziel

Für jede Mannschaft einer Liga(-Saison) eine Wahrscheinlichkeit je Platzierungsziel der
Restsaison anzeigen (Vereinsseite), neu berechnet sobald ein Spieltag dieser Liga
vollständig beendet ist. Läuft unabhängig von `forecast`/`review`, ohne KI-Kosten.

## Wo der Betrachter es sieht

Auf der bestehenden `TeamPage` (Vereinsseite), als neuer Abschnitt neben Spielplan,
Saisonverlauf, Bilanz und Torschützen.

## Platzierungsziele je Liga

Neues Konzept `PlacementGoal` in `matchday.entity` — bisher existierten dieselben
Schwellenwerte nur verstreut als CSS-Klassen in `MatchdayPageModels.zone()`
(`de.javamark.matchoracle.matchday.boundary`). Diese werden jetzt einmal fachlich
definiert und von beiden Stellen (Tabellenfarbe *und* Saisonaussicht) genutzt, statt
doppelt gepflegt zu werden.

| Liga | Platzierungsziele (Positionsbereich) |
|------|----------------------------------|
| 1. Bundesliga | Meisterschaft (1), Europapokal (1–4), Play-off (16), Abstieg (17–18) |
| 2. Bundesliga | Aufstieg (1–2), Play-off (3), Play-off Abstieg (16), Abstieg (17–18) |
| Champions-League-Ligaphase | K.-o.-Runde direkt (1–8), Play-off (9–24), Ausscheiden (25–36) |

Ziele können sich überlappen (Meisterschaft ⊂ Europapokal) — das sind unabhängige
Wahrscheinlichkeiten, keine Verteilung, die sich zu 1 aufsummiert. Positionen ohne
Ziel (z. B. Bundesliga-Plätze 5–15) tauchen in keinem Ziel auf.

`matchday` stellt für andere Features nur eine Boundary-Auskunft bereit (nie das
Entity direkt) — Signatur ist Sache des Implementierungsplans, nicht dieses Designs.

## Simulationsmethode

Monte-Carlo-Simulation der Restsaison, 10.000 Durchläufe:

1. Ausgangspunkt: aktuelle Tabelle (Punkte, Tore) nach dem gerade beendeten Spieltag.
2. Für jede verbleibende Begegnung: ein konkretes Toreverhältnis aus genau dem
   Poisson-Raster ziehen, das `PoissonScoreModel` (matchday.control) auch für die
   KI-Vorschau nutzt — Basis sind Heim-/Auswärts-Bilanz beider Mannschaften zum
   aktuellen Zeitpunkt, unverändert über den ganzen simulierten Rest der Saison
   (kein Nachziehen der Stärke innerhalb eines Simulationslaufs — deckt sich mit der
   Spec: „auf Basis der bisherigen Punkt- und Torausbeute").
3. Punkte/Tore aus dem gezogenen Ergebnis in die simulierte Tabelle einrechnen.
4. Nach allen verbleibenden Begegnungen: Tabelle sortieren (Punkte, Tordifferenz,
   Tore — wie im echten Ligabetrieb), jeder Mannschaft ihre Endplatzierung zuordnen.
5. Über alle 10.000 Läufe: Anteil, in dem eine Mannschaft in einem Platzierungsziel
   landet, ergibt dessen Wahrscheinlichkeit.

Exakte Toreverhältnisse statt nur Sieg/Remis/Niederlage, weil die echte Tabelle über
Tordifferenz entscheidet — eine reine Tendenz-Simulation könnte das nicht abbilden.

**Randfall Saisonende:** keine verbleibenden Begegnungen mehr → keine Simulation
nötig, die aktuelle Endtabelle bestimmt die Platzierung direkt (Wahrscheinlichkeit
1.0 für das erreichte Ziel, 0.0 für alle anderen).

Eine rein analytische Berechnung (ohne Zufallsziehung) wurde verworfen: Tordifferenz-
Tiebreaks lassen sich kaum geschlossen berechnen, ohne nennenswerten Genauigkeitsgewinn
gegenüber der Simulation.

## Auslöser

`season` beobachtet das bestehende CDI-Event `ResultFinalized`
(`matchday.boundary`, aktuell nur von `review` beobachtet). Zusätzlich braucht
`MatchdayFacade` eine neue Auskunft, ob mit diesem Ergebnis der komplette Spieltag
fertig ist (alle Begegnungen dieses Spieltags `FINAL`). Nur dann wird für die
betroffene Liga(-Saison) neu simuliert — nicht nach jedem Einzelergebnis.

Kein neuer Event-Typ, kein Scheduler/Polling (anders als `ForecastScheduler`):
der fachliche Auslöser ist ein klarer Zeitpunkt („Spieltag beendet"), kein Intervall.

Gilt für alle drei Ligen inklusive Champions-League-Ligaphase (dort ebenfalls über
denselben Spieltag-Mechanismus abgebildet, siehe `League.CHAMPIONS_LEAGUE`).

## Speicherung

Neue Entity `SeasonOutlook` (PanacheEntity, `season.entity`), analog zum
Schnappschuss-Stil von `Forecast`: `league` (Kürzel als String, kein Entity-Verweis
auf `matchday`), `season`, `teamId`, `computedAt`, dazu eine `@ElementCollection`
aus (`PlacementGoal`, Wahrscheinlichkeit)-Paaren — kein eigenes Kind-Entity nötig,
da diese Werte keine eigene Identität oder eigenständige Abfragen brauchen (anders
als z. B. `Assessment` bei `Forecast`).

Eine Neuberechnung löscht die bisherigen Zeilen der betroffenen Liga-Saison und
schreibt die neuen in einer Transaktion — passt zu „ersetzt vollständig, keine
Historie" aus der Spec.

## Architektur

Neues Package `season` mit den drei üblichen Schichten:

```
season
├── boundary   – ResultFinalized-Observer, TeamPage-Erweiterung (Anzeige)
├── control    – SeasonSimulator (Monte-Carlo-Lauf), SeasonOutlookService (Neuberechnung/Ersetzen)
└── entity     – SeasonOutlook
```

`season.control` holt sich Ausgangstabelle, verbleibende Begegnungen und
Heim-/Auswärts-Bilanzen ausschließlich über `MatchdayFacade` (nie über
`matchday`-Entities direkt) — exakte neue Facade-Methoden sind Sache des
Implementierungsplans.

## Testen (fachlich relevant, kein Framework-Verhalten)

- Platzierungsziele je Liga stimmen mit der Tabelle oben überein (Grenzfälle: Platz 4
  vs. 5, Platz 16 vs. 17 usw.)
- Eine krasse Tabellenführung mit wenigen Restspielen ergibt eine hohe (nicht
  zwingend exakte) Meisterschafts-Wahrscheinlichkeit — Toleranzprüfung, kein exakter
  Wert, da Monte Carlo
- Randfall „keine Restspiele mehr": Wahrscheinlichkeit 1.0/0.0 ohne Zufallsziehung
- Auslöser reagiert erst, wenn *alle* Begegnungen eines Spieltags final sind, nicht
  nach dem ersten
- Eine zweite Neuberechnung ersetzt die erste vollständig (keine alten Zeilen bleiben
  übrig)
- Champions-League-Ligaphase bekommt ebenfalls eine Saisonaussicht

Für deterministische Tests: der Zufallszahlengenerator der Simulation wird von außen
injiziert (fester Seed in Tests), nicht global.
