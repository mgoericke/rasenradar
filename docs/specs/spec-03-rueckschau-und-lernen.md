# Spec 3 — Rückschau und Lernen aus früheren Prognosen

> Rein fachliche Spec. Keine Technologie-, Schnittstellen- oder Datentyp-Festlegungen.

## Ziel

Das System bewertet seine eigenen Prognosen im Nachhinein, macht die eigene
Trefferbilanz sichtbar und stellt bei neuen Prognosen vergleichbare frühere Fälle
samt damaligem Ausgang bereit.

## Akteure

- **Betrachter** — sieht die Trefferbilanz und die herangezogenen Vergleichsfälle
- **Rückschau (systemintern)** — gleicht Prognosen mit Ergebnissen ab
- **Prognostiker** aus Spec 2 — Empfänger der Rückschau

## Ablauf

1. Sobald ein Ergebnis als endgültig gilt, wird jede dazu festgeschriebene
   Prognose mit dem tatsächlichen Ausgang verglichen.
2. Festgehalten werden: ob die Tendenz getroffen wurde, wie gut die
   Wahrscheinlichkeiten zum Ausgang passten, und ob die Sicherheit angemessen war.
3. Zu jeder abgeschlossenen Begegnung hinterlegt das System eine Beschreibung der
   **Ausgangslage** — die Konstellation, wie sie vor dem Spiel aussah.
4. Wird eine neue Prognose erstellt, sucht das System Begegnungen mit ähnlicher
   Ausgangslage und stellt dem Prognostiker eine kurze Rückschau bereit:
   welche Fälle, wie das System damals lag, welcher Fehler sich dabei zeigt.
5. Der Betrachter kann für jede Prognose einsehen, welche Vergleichsfälle
   herangezogen wurden und wie ähnlich sie waren.
6. Über alle Prognosen hinweg weist das System eine Trefferbilanz aus:
   Trefferquote je Liga, Verlauf über die Spieltage und die Frage, ob die
   angegebene Sicherheit zur tatsächlichen Trefferquote passt.
7. Zu jedem abgeschlossenen Spieltag stellt das System einen kurzen Rückblick in
   Textform bereit: wie viele Tendenzen und wie viele genaue Ergebnisse
   getroffen wurden, die durchschnittliche Sicherheit, und welche Begegnung am
   deutlichsten danebenlag.

## Regeln

- Nur endgültige Ergebnisse fließen in die Rückschau ein.
- Prognosen und ihre Bewertung werden nie gelöscht oder überschrieben — auch
  schlechte nicht.
- Solange zu wenige abgeschlossene Fälle vorliegen, liefert die Rückschau
  ausdrücklich nichts, statt eine dünne Grundlage als Erkenntnis auszugeben.
- Die Rückschau beeinflusst die Prognose nur als zusätzliche Information. Sie
  verändert die fachlichen Bewertungsmaßstäbe nicht selbsttätig.
- Eine Vergleichbarkeit über Liga- und Saisongrenzen hinweg ist zulässig und
  wird gekennzeichnet.
- Vergleichsfälle stammen ausschließlich aus Begegnungen, die vor dem Anstoß der
  betrachteten Begegnung stattfanden — auch bei Rücktests kein Blick in die Zukunft.
- Rücktest-Prognosen (Spec 2) werden sofort bewertet und in der Trefferbilanz
  getrennt von den Prognosen vor dem Anstoß ausgewiesen.
- Jede Ausgangslage trägt eine **Basisprognose**: eine reine Statistik aus dem
  Torschnitt beider Mannschaften (Heim gegen Auswärts) und dem Heimvorteil, ohne
  KI, mit dem Wissensstand von vor dem Anstoß. Sie ist der Maßstab, an dem sich
  jede Prognose messen lassen muss: Die Trefferbilanz zeigt Prognose und
  Basisprognose auf denselben Begegnungen nebeneinander
  (Tendenztreffer und Brier-Score) sowie einen Skill-Score (Verbesserung des
  Brier-Scores gegenüber der Basisprognose). Für den Vergleich gilt dieselbe
  Mindestanzahl wie für die Trefferquote.
- Die Trefferbilanz weist zusätzlich aus, ob der ausgewiesene Sicherheitsgrad zur
  tatsächlichen Trefferquote passt (Kalibrierung): Prognosen werden in niedrige,
  mittlere und hohe Sicherheit gruppiert, und für jede Gruppe wird die
  tatsächliche Trefferquote der tatsächlich ausgewiesenen gegenübergestellt. Für
  jede Gruppe gilt dieselbe Mindestanzahl wie für die Trefferquote insgesamt.
- Der Rückblick je Spieltag ist reiner Text aus bereits erfassten Zahlen
  (Treffer, Fehlschläge, Sicherheit, größte Abweichung) — kein KI-Agent, keine
  Einschätzung, die über diese Zahlen hinausgeht. Dieselbe Abgrenzung wie bei
  der Prognose gilt auch hier: keine Quoten, keine Wettsprache.

## Abgrenzung

- Kein automatisches Nachjustieren der Bewertungsmaßstäbe durch das System.
  Erkenntnisse werden sichtbar gemacht, die Änderung entscheidet ein Mensch.
- Keine Bewertung einzelner Analyseagenten in dieser Ausbaustufe.

## Im Spec-Interview geklärt

- **Ähnlichkeit:** Zwei Ausgangslagen sind ähnlich, wenn Tabellenabstand
  (Heim gegenüber Gast), Form beider Mannschaften (Punkte aus den letzten
  Spielen), Heim- bzw. Auswärtsbilanz und Aufsteiger-Status nahe beieinander
  liegen. Fälle aus derselben Liga werden bevorzugt; Fälle aus der anderen Liga
  oder früheren Saisons sind zulässig und werden gekennzeichnet.
- **Belastbarkeit:** Vergleichsfälle werden nur bereitgestellt, wenn mindestens
  drei ähnliche abgeschlossene Begegnungen vorliegen. Eine Trefferquote wird je
  Liga erst ab zehn bewerteten Prognosen ausgewiesen; darunter erscheint der
  Hinweis auf die zu dünne Datenlage.
- **Anzahl:** Der Prognostiker erhält höchstens die fünf ähnlichsten Fälle.
- **Sichtbarkeit:** Die Trefferbilanz ist öffentlich einsehbar.
- **Kalibrierungs-Gruppen:** Drei breite Gruppen (niedrige, mittlere, hohe
  Sicherheit) statt feiner Abstufungen — bei einer Saison mit einigen hundert
  Prognosen sind feinere Gruppen zu dünn, um belastbar zu sein.
- **Rückblick-Erzeugung:** Regelbasiert aus bereits vorhandenen Zahlen
  zusammengesetzt, kein eigener Agent — analog zur bestehenden Rückschau für
  den Prognostiker, die ebenfalls ohne KI reinen Text aus Zahlen bildet.

## Abnahmekriterien

- Nach Endgültigwerden eines Ergebnisses liegt die Bewertung der zugehörigen
  Prognose ohne manuelles Zutun vor.
- Eine neue Prognose weist nachvollziehbar aus, welche früheren Fälle
  herangezogen wurden.
- Die Trefferbilanz ist je Liga und im Verlauf über die Spieltage einsehbar.
- Bei zu dünner Datenlage erscheint ausdrücklich der Hinweis darauf statt einer
  Zahl.
- Die Trefferbilanz zeigt zu jeder der drei Sicherheitsgruppen die tatsächliche
  Trefferquote, sobald genug Fälle vorliegen — sonst den Hinweis auf zu dünne
  Datenlage für diese Gruppe.
- Zu jedem abgeschlossenen Spieltag mit mindestens einer bewerteten Prognose
  liegt ein Rückblick in Textform vor, der ausschließlich Zahlen aus der
  Trefferbilanz wiedergibt.
