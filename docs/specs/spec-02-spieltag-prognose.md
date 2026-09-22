# Spec 2 — Spieltag-Prognose erstellen

> Rein fachliche Spec. Keine Technologie-, Schnittstellen- oder Datentyp-Festlegungen.

## Ziel

Für jede noch nicht gespielte Begegnung eines Spieltags erstellt das System eine
nachvollziehbare Prognose über den Ausgang — begründet, mit ausgewiesener
Sicherheit und sichtbarem Zustandekommen.

## Akteure

- **Betrachter** — sieht die Prognose, kann einen Rücktest anstoßen
- **Terminplanung** (systemintern) — löst die Prognose aus, ohne dass der
  Betrachter das anstoßen muss
- **Formbewerter** (systeminterner Analyseagent)
- **Duellbewerter** (systeminterner Analyseagent)
- **Umfeldbewerter** (systeminterner Analyseagent)
- **Prognostiker** (systeminterner Agent)
- **Prüfer** (systeminterner Agent)

## Ablauf

1. Die Terminplanung erkennt eine Begegnung, für die eine Prognose fällig ist,
   und stößt sie an — der Betrachter muss dafür nichts tun (Ausnahme: der
   Rücktest, den der Betrachter gezielt selbst anstößt).
2. Je Begegnung stellt das System die bekannten Fakten zusammen: Form beider
   Mannschaften, Tabellenlage, Heim- und Auswärtsbilanz, bisherige direkte Duelle.
3. Drei Bewertungen laufen **gleichzeitig**:
   - **Form**: In welcher Verfassung sind beide Mannschaften?
   - **Direktes Duell**: Was sagt die gemeinsame Historie?
   - **Umfeld**: Gibt es außerhalb der Zahlen Relevantes — Ausfälle, Trainerwechsel,
     besondere Belastung?
4. Der Prognostiker verdichtet die drei Bewertungen zu einer Prognose. Er erhält
   dabei zusätzlich die Rückschau aus Spec 3 (vergleichbare frühere Fälle und wie
   das System damals lag).
5. Der Prüfer kontrolliert die Prognose auf Widersprüche zu den Bewertungen und
   auf unangemessen hohe Sicherheit. Er urteilt **angenommen** oder
   **überarbeiten** mit Begründung.
6. Bei „überarbeiten" geht die Prognose einmalig zurück an den Prognostiker.
   Danach wird sie in jedem Fall ausgegeben — bei anhaltendem Einwand mit
   entsprechendem Vermerk.
7. Die fertige Prognose wird festgeschrieben und ist danach unveränderlich.

## Ergebnis einer Prognose

- Tendenz mit Wahrscheinlichkeit für Heimsieg, Unentschieden, Auswärtssieg
- Erwarteter Torverlauf als Richtwert
- Sicherheitsgrad
- Kurzbegründung in verständlicher Sprache
- Die drei Einzelbewertungen, einzeln einsehbar
- Der Ausgang der Prüfung

## Regeln

- Die drei Wahrscheinlichkeiten ergeben zusammen die volle Gewissheit.
- Eine Prognose entsteht nur für Begegnungen, die noch nicht angepfiffen sind.
  Ausnahme ist der **Rücktest**: eine nachträgliche Prognose zu einer bereits
  gespielten Begegnung der laufenden Saison, erstellt allein aus dem Wissensstand
  von vor dem Anstoß. Rücktests sind als solche gekennzeichnet und dienen dem
  Vergleich von Modellen und Bewertungsmaßstäben; für frühere Saisons sind sie
  unzulässig, weil die Ergebnisse den Modellen bekannt sein könnten.
- Eine einmal festgeschriebene Prognose wird nie nachträglich geändert. Eine
  erneute Prognose zur selben Begegnung entsteht als eigener Eintrag.
- Für eine bevorstehende Begegnung kann die Terminplanung mehrfach neu
  prognostizieren, solange sich die Lage noch ändern kann (Form, Ausfälle,
  Trainerwechsel). Maßgeblich für Anzeige und Rückschau ist jeweils die zuletzt
  vor dem Anstoß entstandene Prognose; frühere bleiben erhalten und sind
  nachvollziehbar einsehbar, zählen aber nicht als „die" Prognose der Begegnung.
- Begegnungen ohne Prognose, die sich angesammelt haben (z. B. nach einer
  Pause der Terminplanung), werden nachgeholt, aber je Lauf nur bis zu einer
  Obergrenze — der Rest folgt beim nächsten Lauf. Das hält die Zahl gleichzeitig
  ausgelöster Prognosen kalkulierbar.
- Schlägt ein Prognoselauf fehl oder entsteht eine Prognose, bei der keine
  einzige Bewertung gelungen ist, holt die Terminplanung sie deutlich früher
  nach als sie eine gelungene Prognose auffrischen würde — aber nicht in einer
  engen Schleife, damit ein Ausfall des Modells keine Kosten verursacht. Die
  misslungene Prognose bleibt als eigener Eintrag erhalten.
- Die fachlichen Bewertungsmaßstäbe (Gewicht des Heimvorteils, Länge des
  betrachteten Formzeitraums, Umgang mit Aufsteigern) sind ohne Eingriff in die
  Anwendung änderbar und wirken sofort auf die nächste Prognose.
- Der Betrachter sieht während des Laufs, welcher Schritt gerade arbeitet.
- Fällt eine der drei Bewertungen aus, wird die Prognose trotzdem erstellt, der
  Ausfall aber ausgewiesen und die Sicherheit entsprechend niedriger angesetzt.

## Abgrenzung

- Keine Quoten, keine Wettempfehlungen, kein Bezug zu Geldeinsätzen.
- Keine Aussagen zu einzelnen Spielern.

## Im Spec-Interview geklärt

- **Form:** Die letzten fünf Spiele der laufenden Saison (siehe Spec 1); die Zahl
  ist ein Bewertungsmaßstab.
- **Bewertungsmaßstäbe:** Der Betrachter kann sie einsehen und selbst ändern;
  eine Änderung wirkt auf die nächste Prognose.
- **Granularität:** Eine Prognose entsteht je Begegnung. Die Terminplanung
  stößt für einen fälligen Spieltag alle noch offenen Begegnungen gesammelt an;
  der Fortschritt ist je Begegnung sichtbar.
- **Umfeld:** Vorerst nur aus dem, was die Spieldaten hergeben (Belastung durch
  dichte Spielfolge, Aufsteiger, Anstoßzeit, Saisonphase). Externe Nachrichten
  sind eine spätere Ausbaustufe.
- **Auslöser:** Die Terminplanung, nicht der Betrachter, löst die Prognose aus.
  Bevorstehende Begegnungen können bis zum Anstoß mehrfach neu prognostiziert
  werden; nachzuholende Lücken in bereits vergangenen, aber noch offenen
  Begegnungen der laufenden Saison folgen einem eigenen, wiederkehrenden Takt
  mit fester Obergrenze je Lauf. Der Rücktest bleibt die einzige vom Betrachter
  gezielt angestoßene Prognoseart.

## Abnahmekriterien

- Für eine bevorstehende Begegnung entsteht eine vollständige Prognose mit allen
  oben genannten Bestandteilen.
- Die drei Einzelbewertungen sind im Ergebnis einzeln nachlesbar.
- Ein Widerspruch zwischen Bewertung und Prognose führt nachweislich zu einer
  Überarbeitungsrunde.
- Eine Änderung der Bewertungsmaßstäbe verändert bei sonst gleicher Datenlage die
  Prognose.
