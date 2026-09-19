# Spec 4 — Saisonaussicht

> Rein fachliche Spec. Keine Technologie-, Schnittstellen- oder Datentyp-Festlegungen.
> Diese trifft der Scaffold-/Implementierungsschritt.

## Ziel

Neben der Prognose einzelner Begegnungen (Spec 2) schätzt das System die Aussichten
einer Mannschaft für die **restliche Saison** ein — wie wahrscheinlich sind
Meisterschaft, ein Platz für einen europäischen Wettbewerb, der Aufstieg oder der
Abstieg, je nachdem, welche Platzierungsziele in ihrer Liga zählen.

## Akteure

- **Betrachter** — sieht die aktuelle Saisonaussicht je Mannschaft
- **Saisonschätzung (systemintern)** — berechnet die Saisonaussicht neu, sobald ein
  Spieltag beendet ist

## Ablauf

1. Sobald ein Spieltag einer Liga beendet ist, ermittelt das System für jede
   Mannschaft dieser Liga die verbleibenden Begegnungen der Saison.
2. Es schätzt jede verbleibende Begegnung auf Basis der bisherigen Punkt- und
   Torausbeute beider beteiligten Mannschaften — unabhängig von einer etwaigen
   KI-Vorschau zu dieser Begegnung aus Spec 2.
3. Aus den möglichen Verläufen der Restsaison leitet das System für jede
   Mannschaft eine Wahrscheinlichkeit je Platzierungsziel ihrer Liga ab
   (Meisterschaft, europäischer Wettbewerb, Auf- bzw. Abstieg, Play-off).
4. Die neu berechnete Saisonaussicht ersetzt den bisherigen Stand vollständig.
5. Der Betrachter kann für jede Mannschaft die aktuelle Saisonaussicht einsehen.

## Regeln

- Die Platzierungsziele je Mannschaft ergeben sich aus den Tabellenzonen ihrer
  Liga (Spec 1): für die Bundesligen Meisterschaft, europäischer Wettbewerb,
  Auf-/Abstieg und die jeweiligen Play-off-Plätze; für die
  Champions-League-Ligaphase direkter Einzug in die K.-o.-Runde, Play-off und
  Ausscheiden.
- Die Saisonaussicht wird unabhängig von einer etwaigen KI-Vorschau einzelner
  Restbegegnungen berechnet. Spec 2 bleibt davon unberührt, beide Features
  laufen nebeneinander her.
- Eine Neuberechnung ersetzt den vorherigen Stand vollständig. Es gibt keine
  Historie vorangegangener Saisonaussichten und keinen Rückschau-Bezug —
  Spec 3 bleibt unberührt, eine Saisonaussicht wird nicht im Nachhinein bewertet.
- Ergebnis ist eine Wahrscheinlichkeit, keine Behauptung über den tatsächlichen
  Ausgang der Saison.
- Keine Quoten, keine Wettempfehlungen.

## Abgrenzung

- Keine Aussage zu einzelnen Spielern.
- Keine Einbindung in die Trefferbilanz (Spec 3).
- Keine Abhängigkeit von Einzelspiel-Prognosen (Spec 2).
- Kein Pokal (siehe Spec 1, Abgrenzung: kein Spieltagszähler, keine Tabelle).

## Im Spec-Interview geklärt

- **Grundlage:** Eigenständige Statistik-Simulation auf Basis von Tabellenlage
  und bisheriger Punkt-/Torausbeute je Mannschaft, unabhängig von den
  Einzelspiel-Prognosen aus Spec 2. Läuft ohne KI-Kosten und unabhängig davon,
  für wie viele Restbegegnungen bereits eine KI-Vorschau vorliegt.
- **Ausdrucksform:** Eine einzelne Wahrscheinlichkeit je Platzierungsziel
  (z. B. „62 % Champions League"), nicht als Bandbreite — passt zum
  Wahrscheinlichkeits-Stil der Einzelspiel-Prognose.
- **Aktualisierungsanlass:** Neuberechnung nach jedem beendeten Spieltag der
  jeweiligen Liga — ein fachlich klarer Auslöser, analog zur Terminplanung aus
  Spec 2.
- **Unveränderlichkeit:** Momentaufnahme ohne Historie. Die Saisonaussicht kann
  sich mit jedem Spieltag stark verschieben; eine unveränderliche Historie wie
  bei Prognosen wäre hier ohne zusätzlichen Nutzen.
- **Rückschau-Bezug:** Getrennt von der Trefferbilanz. Eine Momentaufnahme ohne
  Historie hat keinen Stand, an dem sich im Nachhinein ein „Treffer" festmachen
  ließe.
- **Champions-League-Ligaphase:** Erhält ebenfalls eine Saisonaussicht, auf
  derselben statistischen Grundlage wie die Bundesligen — ohne KI-Bezug, den es
  für die Champions League ohnehin nicht gibt (Spec 1, Abgrenzung).

## Abnahmekriterien

- Nach jedem beendeten Spieltag liegt für jede Mannschaft der betroffenen Liga
  eine aktualisierte Saisonaussicht vor, ohne manuelles Zutun.
- Die Saisonaussicht weist für jedes Platzierungsziel der jeweiligen Liga eine
  Wahrscheinlichkeit aus.
- Die Champions-League-Ligaphase zeigt ebenfalls eine Saisonaussicht.
- Eine Neuberechnung ersetzt den vorherigen Stand vollständig; eine Historie
  ist nicht einsehbar.
- Die Saisonaussicht taucht an keiner Stelle der Trefferbilanz auf.
