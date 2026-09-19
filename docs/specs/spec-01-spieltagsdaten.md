# Spec 1 — Spieltagsdaten bereitstellen

> Rein fachliche Spec. Keine Technologie-, Schnittstellen- oder Datentyp-Festlegungen.
> Diese trifft der Scaffold-/Implementierungsschritt.

## Ziel

Das System kennt jederzeit die Begegnungen und Ergebnisse der 1. und 2. Bundesliga
der laufenden Saison sowie eine ausreichende Historie, um Formverläufe und
frühere Begegnungen bewerten zu können.

## Akteure

- **Betrachter** — sieht Begegnungen und Ergebnisse
- **Datenpflege (systemintern)** — hält den Bestand aktuell

## Ablauf

1. Das System bezieht die Begegnungen beider Ligen für die laufende Saison aus
   einer externen Spieldatenquelle.
2. Es prüft in regelmäßigen Abständen, ob sich an einem Spieltag etwas geändert
   hat, und lädt nur bei Änderung nach.
3. Zu jeder Begegnung werden festgehalten: beteiligte Mannschaften, Anstoßzeit,
   Spieltag, Liga, Saison, Halbzeit- und Endstand sowie die gefallenen Tore mit
   Minute und Schütze.
4. Ein Ergebnis gilt zunächst als **vorläufig**. Nach einer festgelegten Frist
   ohne weitere Änderung gilt es als **endgültig**.
5. Aus dem Bestand leitet das System je Mannschaft ab: aktuelle Tabellenposition,
   Ergebnisse der letzten Spiele, Heim- bzw. Auswärtsbilanz sowie die bisherigen
   direkten Begegnungen gegen einen bestimmten Gegner.

## Regeln

- Jede Liga führt ihren eigenen Spieltagszähler. Ein gemeinsamer Zähler über
  beide Ligen ist unzulässig — die Ligen können unterschiedlich weit sein.
- Solange ein Ergebnis vorläufig ist, darf es für Rückschau und Lernen nicht
  verwendet werden. Für die Anzeige darf es verwendet werden, ist aber als
  vorläufig kenntlich zu machen.
- Fällt die externe Quelle aus, arbeitet das System mit dem letzten bekannten
  Stand weiter und weist dessen Alter aus.
- Die Herkunft der Daten wird gegenüber dem Betrachter ausgewiesen.

## Abgrenzung

- Keine Live-Ergebnisse während laufender Spiele.
- Keine Spielerstatistiken über die Torschützen hinaus.
- Keine weiteren Wettbewerbe (Pokal, europäische Wettbewerbe) in dieser Ausbaustufe.

## Im Spec-Interview geklärt

- **Historie:** Vorgehalten werden die laufende Saison und die zwei
  vorangegangenen Saisonen beider Ligen.
- **Reichweite der Quelle:** Wie weit sich die Historie über die aktuell
  vorgehaltenen Saisonen hinaus ausbauen ließe, hängt an der externen Quelle:
  vollständige Saisondaten liegen für die 1. Bundesliga ab 2003/04, für die
  2. Bundesliga ab 2006/07 vor (Stand: Prüfung 2026). Eine tiefere Historie
  (mehr Saisonen auf der Vereinsseite, in Direktduellen, in einer möglichen
  Torschützen-Ewigkeitsliste) ist damit möglich, aber eine eigene spätere
  Ausbaustufe — noch nicht entschieden, ob und wie weit.
- **Frist für endgültig:** Ein Ergebnis gilt als endgültig, wenn seit dem Anstoß
  24 Stunden vergangen sind und es sich in dieser Zeit nicht mehr geändert hat.
  Die Frist ist ein Bewertungsmaßstab und ohne Neuauslieferung änderbar.
- **Prüftakt:** Das System prüft alle 15 Minuten, ob sich an einem Spieltag
  etwas geändert hat. Der Takt ist ohne Neuauslieferung änderbar.
- **Kennzeichnung laufender Spiele:** Ein Spiel gilt als „läuft" innerhalb eines
  großzügigen Zeitfensters nach dem Anstoß, solange noch kein Endstand vorliegt
  — rein aus der Anstoßzeit abgeleitet, ohne Live-Ergebnis. Das Zeitfenster ist
  ein Bewertungsmaßstab und ohne Neuauslieferung änderbar. Bleibt innerhalb der
  Abgrenzung „Keine Live-Ergebnisse während laufender Spiele" — es wird
  angezeigt, *dass* ein Spiel läuft, nicht *wie* es gerade steht.
- **Identität von Mannschaften:** Eine Mannschaft wird über eine stabile
  Kennung der Spieldatenquelle identifiziert, die Saisonwechsel sowie Auf- und
  Abstieg überdauert. Name und Kürzel sind änderbare Eigenschaften, keine
  Identitätsmerkmale.

## Abnahmekriterien

- Für beide Ligen ist der jeweils aktuelle Spieltag mit allen Begegnungen abrufbar.
- Ein abgeschlossenes Spiel weist Halbzeit- und Endstand sowie die Torfolge aus.
- Eine nachträgliche Ergebniskorrektur an der Quelle ist im System sichtbar,
  ohne dass jemand manuell eingreift.
- Für eine beliebige Begegnung lassen sich Form beider Mannschaften und die
  bisherigen direkten Duelle abrufen.
