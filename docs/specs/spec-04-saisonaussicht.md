# Spec 4 — Saisonaussicht (Entwurf, noch nicht terminiert)

> Rein fachliche Spec. Keine Technologie-, Schnittstellen- oder Datentyp-Festlegungen.
> Im Unterschied zu Spec 1–3 ist dies noch keine zur Umsetzung freigegebene Spec,
> sondern ein Diskussionsstand für ein künftiges Spec-Interview. Abschnitte, die
> bei Spec 1–3 „geklärt" heißen, sind hier bewusst offen gelassen.

## Ziel (Entwurf)

Neben der Prognose einzelner Begegnungen (Spec 2) schätzt das System die
Aussichten einer Mannschaft für die **restliche Saison** ein — wie wahrscheinlich
sind Meisterschaft, ein Platz für einen europäischen Wettbewerb oder der Abstieg.

## Warum das eine eigene Spec ist, keine Erweiterung von Spec 2

Eine Einzelspiel-Prognose und eine Saisonaussicht beantworten unterschiedliche
Fragen mit unterschiedlicher Reichweite: die eine „wie geht dieses eine Spiel
aus", die andere „wo steht diese Mannschaft am Ende von noch ausstehenden
Spieltagen". Die zweite braucht zwingend alle Restspiele aller Mannschaften auf
einmal (weil Tabellenplätze sich gegenseitig bedingen), nicht nur die einer
einzelnen Begegnung.

## Offene Fragen für das Spec-Interview

- **Grundlage:** Setzt die Saisonaussicht auf den Einzelspiel-Prognosen aus
  Spec 2 auf (jede Restbegegnung liefert ihre Tendenz zu), oder rechnet sie
  eigenständig auf Basis von Tabellenlage und Formstärke, unabhängig von der
  KI-Vorschau einzelner Spiele? Das entscheidet, ob Spec 4 von Spec 2 abhängt
  oder eigenständig neben ihr steht.
- **Ausdrucksform:** Eine einzelne Kennzahl je Platzierungsziel (z. B. „62 %
  Champions League") oder eine Bandbreite (z. B. „meist zwischen Platz 3 und 7")?
- **Aktualisierungsanlass:** Nach jedem gespielten Spieltag neu, oder in
  einem eigenen Takt? Wie bei Spec 2 stellt sich die Kostenfrage, falls dafür
  KI eingesetzt wird — reine Statistik (Simulation auf Basis von Tabellen und
  Punktschnitten) käme ohne KI-Kosten aus.
- **Unveränderlichkeit:** Gilt dieselbe Regel wie bei Prognosen (Spec 2) — eine
  einmal ausgewiesene Saisonaussicht bleibt stehen, eine neue ist ein eigener
  Eintrag —, oder ist sie als „Momentaufnahme, die sich laufend aktualisiert"
  konzipiert, ohne Historie?
- **Rückschau-Bezug:** Fließt eine vergangene Saisonaussicht in die Trefferbilanz
  (Spec 3) ein — also: wie gut lag die Einschätzung am Saisonende? — oder bleibt
  sie davon getrennt, weil sie sich laufend ändert und ein „Treffer" schwerer zu
  definieren ist als bei einer Einzelspiel-Tendenz?
- **Champions-League-Ligaphase:** Spec 1 grenzt die Champions League bislang auf
  reine Anzeige ohne KI-Vorschau ein. Gilt das auch für die Saisonaussicht, oder
  wäre eine reine Tabellen-Simulation (ohne KI) hier vertretbar?

## Abgrenzung (vorläufig)

- Keine Quoten, keine Wettempfehlungen — wie in Spec 2.
- Keine Aussage zu einzelnen Spielern.
- Ergebnis ist eine Wahrscheinlichkeit, keine Behauptung über den tatsächlichen
  Ausgang der Saison.
