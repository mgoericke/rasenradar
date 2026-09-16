# Spec 2 — Spieltag-Prognose erstellen

> Rein fachliche Spec. Keine Technologie-, Schnittstellen- oder Datentyp-Festlegungen.

## Ziel

Für jede noch nicht gespielte Begegnung eines Spieltags erstellt das System eine
nachvollziehbare Prognose über den Ausgang — begründet, mit ausgewiesener
Sicherheit und sichtbarem Zustandekommen.

## Akteure

- **Betrachter** — stößt eine Prognose an und sieht das Ergebnis
- **Formbewerter** (systeminterner Analyseagent)
- **Duellbewerter** (systeminterner Analyseagent)
- **Umfeldbewerter** (systeminterner Analyseagent)
- **Prognostiker** (systeminterner Agent)
- **Prüfer** (systeminterner Agent)

## Ablauf

1. Der Betrachter wählt eine Liga und einen kommenden Spieltag.
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
- **Granularität:** Eine Prognose entsteht je Begegnung. Der Betrachter kann für
  einen Spieltag alle noch offenen Begegnungen gesammelt anstoßen; der
  Fortschritt ist je Begegnung sichtbar.
- **Umfeld:** Vorerst nur aus dem, was die Spieldaten hergeben (Belastung durch
  dichte Spielfolge, Aufsteiger, Anstoßzeit, Saisonphase). Externe Nachrichten
  sind eine spätere Ausbaustufe.

## Abnahmekriterien

- Für eine bevorstehende Begegnung entsteht eine vollständige Prognose mit allen
  oben genannten Bestandteilen.
- Die drei Einzelbewertungen sind im Ergebnis einzeln nachlesbar.
- Ein Widerspruch zwischen Bewertung und Prognose führt nachweislich zu einer
  Überarbeitungsrunde.
- Eine Änderung der Bewertungsmaßstäbe verändert bei sonst gleicher Datenlage die
  Prognose.
