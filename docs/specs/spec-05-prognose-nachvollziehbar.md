# Spec 5 — Prognose nachvollziehbar machen

> Rein fachliche Spec. Keine Technologie-, Schnittstellen- oder Datentyp-Festlegungen.
> Ergänzt Spec 2 (Prognose) und Spec 3 (Rückschau). Fast alles, was hier
> beschrieben wird, entsteht aus Daten, die das System bereits festhält; neu
> sind wenige Regeln und die Art, wie es dem Betrachter gezeigt wird.

## Ziel

Der Betrachter soll einer Prognose beim Zustandekommen zusehen können, ohne
etwas über KI wissen zu müssen — und er soll sehen, wo das System danebenlag.
Das Besondere der Seite ist nicht die Prognose selbst, sondern dass sie sich
erklärt, sich selbst prüft und ihre Fehler nicht versteckt. Alles in
allgemeinverständlicher Sprache, ohne Fachvokabular aus der KI-Welt.

## Akteure

- **Betrachter** — liest Prognosen, Begründungen und Bilanzen
- **Rückschau (Spec 3)** — liefert Trefferbilanz, Vergleichsfälle und
  Kalibrierung; wird um zwei Auswertungen ergänzt (siehe Regeln)
- Die Agenten aus Spec 2 (Form-, Duell-, Umfeldbewerter, Prognostiker, Prüfer)
  — werden hier nur *gezeigt*, nicht verändert

## Ablauf

### A. Zu jeder einzelnen Prognose

1. **Das Zustandekommen** ist als Abfolge lesbar: Was der Formbewerter sagt, was
   der Duellbewerter sagt, was der Umfeldbewerter sagt, wie der Prognostiker das
   verdichtet, wie der Prüfer urteilt. Jeder Schritt in wenigen Sätzen, in der
   Reihenfolge, in der er gelaufen ist, als Absätze mit dem Rollennamen als
   Marginalie (keine Sprechblasen). Standardmäßig eingeklappt; die Prognose
   selbst (Tendenz, Wahrscheinlichkeiten, Sicherheit, Kurzbegründung) bleibt
   das Erste, was der Betrachter sieht.
2. **Der Einwand des Prüfers** ist sichtbar, wenn es einen gab: eine
   Kennzeichnung an der Prognose („Der Prüfer hat widersprochen") und der
   Einwand im Klartext, dazu ob die Überarbeitung ihn ausgeräumt hat oder die
   Prognose mit bestehendem Einwand ausgegeben wurde.
3. **Der Verlauf vor dem Anstoß**: Wurde eine Begegnung mehrfach prognostiziert,
   sieht der Betrachter die Abfolge — Zeitpunkt, Tendenz und Wahrscheinlichkeiten
   je Prognose —, vollständig und kompakt als Zeile je Prognose, und kann jede
   frühere Prognose mit ihrer Begründung öffnen. Maßgeblich bleibt die letzte
   vor dem Anstoß (Spec 2).
4. **Gegen den Strom**: Weicht die Tendenz der Prognose von der Tendenz der
   Basisprognose (Spec 3) ab, ist das an der Prognose gekennzeichnet. Nach dem
   Spiel zeigt die Kennzeichnung, ob sich das Abweichen gelohnt hat.
5. **Aus der Erfahrung**: Die Vergleichsfälle, die dem Prognostiker vorlagen
   (Spec 3), sind für den Betrachter als kurzer Prosa-Absatz lesbar: wie viele
   ähnliche Konstellationen es gab, wie das System dort lag und welcher Fehler
   sich zeigte. Liegen zu wenige Fälle vor, sagt der Absatz genau das.

### B. Über alle Prognosen hinweg

6. **Wer liegt vorne?** Auf der Startseite steht eine kleine Rangliste aus drei
   Zeilen: die Prognose des Systems, die Basisprognose und die Faustregel
   „immer Heimsieg" — mit Tendenztreffern auf denselben Begegnungen,
   ligenübergreifend (Umschalter je Liga in der Trefferbilanz). Die
   Reihenfolge ergibt sich aus den Zahlen; führt die Basisprognose, steht sie
   oben. Die Rangliste weist aus, auf wie vielen Begegnungen sie beruht (z. B.
   „nach 27 Spielen"), damit eine frühe Zahl nicht wie eine späte gelesen
   wird. Brier-Score und Skill-Score erscheinen nicht hier, sondern nur in der
   Trefferbilanz (Spec 3).
7. **Kalibrierung in Sätzen**: Je Sicherheitsgruppe (niedrig, mittel, hoch) ein
   Satz nach dem Muster „Wenn das System hohe Sicherheit ausweist, trifft die
   Tendenz in X von 100 Fällen." Die Zahlen stammen aus der Kalibrierung in
   Spec 3.
8. **Mut-Bilanz**: Die Trefferbilanz weist zusätzlich aus, wie oft das System
   gegen den Strom prognostiziert hat und wie oft das aufging — getrennt von
   der übrigen Trefferquote.

### C. Erklärseite

9. Eine eigene Seite **„So entsteht eine Prognose"** erklärt die fünf Rollen aus
   Spec 2 — bei ihren Spec-Namen (Formbewerter, Duellbewerter, Umfeldbewerter,
   Prognostiker, Prüfer), keine Maskottchen oder Eigennamen — als Bild und in
   wenigen Absätzen, dazu die Rückschau aus Spec 3. Sie enthält einen Abschnitt
   **„Was das System nicht weiß"**: ein Absatz — keine Aufstellungen, keine
   Verletzten, keine Nachrichten — und warum Prognosen deshalb danebenliegen
   können. Unter jeder einzelnen Prognose steht dazu ein kurzer Satz mit
   Verweis auf diesen Abschnitt.

## Regeln

- **Keine neuen KI-Schritte.** Alle Texte dieser Spec — Verlauf, Erfahrung,
  Kalibrierungssätze, Rangliste, Mut-Bilanz — entstehen aus bereits erfassten
  Zahlen und Texten. Kein Agent formuliert nachträglich etwas Neues über eine
  Prognose.
- **Sprache:** Kein KI-Fachvokabular gegenüber dem Betrachter — keine Modell-,
  Agenten- oder Token-Begriffe. Die Rollen heißen wie in den Specs
  (Formbewerter, Prüfer, …). Es gibt einen kleinen, festen Sprachschatz für
  wiederkehrende Bausteine; er ist ein Bewertungsmaßstab.
- **Prognose ist Tendenz, nicht Behauptung.** Keine Überschrift der Art „Das
  System sagt: X gewinnt". Die Tendenz wird immer zusammen mit der
  Wahrscheinlichkeit gezeigt. Keine Quoten, keine Wettsprache, kein „Tipp" —
  wie in Spec 2 und 3.
- **Ehrlichkeit vor Wirkung.** Die Rangliste zeigt, was die Zahlen zeigen; ein
  Spieltag, an dem die Basisprognose besser lag, wird genauso ausgewiesen wie
  einer, an dem das System vorn lag. Der Prüfer-Einwand wird nie weggelassen.
- **Zu wenig Grundlage heißt: nichts zeigen.** Rangliste, Kalibrierungssätze,
  Mut-Bilanz und „Aus der Erfahrung" unterliegen derselben Mindestanzahl wie
  die Trefferbilanz (Spec 3). Wird sie nicht erreicht, steht an der Stelle ein
  Hinweis („noch zu wenige Spiele"), keine Zahl.
- **„Gegen den Strom"** ist eine Eigenschaft der Prognose zum Zeitpunkt ihrer
  Festschreibung: die Tendenz weicht von der Tendenz der Basisprognose
  derselben Ausgangslage ab. Sie ändert sich nie nachträglich. Ist die
  Basisprognose für eine Begegnung nicht verfügbar, gibt es keine Kennzeichnung.
- Das Zustandekommen und der Einwand des Prüfers stammen unverändert aus der
  festgeschriebenen Prognose (Spec 2). Auch für frühere Prognosen derselben
  Begegnung sind sie einsehbar.
- Die Erklärseite beschreibt das Verfahren, nicht die konkreten
  Bewertungsmaßstäbe (Gewichte, Zeiträume) — die sind laut Spec 2 an eigener
  Stelle einsehbar und änderbar.

## Abgrenzung

- Keine Bewertung einzelner Agenten (wie in Spec 3 ausgegrenzt): Die Rangliste
  vergleicht Prognose, Basisprognose und Faustregel, nicht Form- gegen
  Duellbewerter.
- Keine Erklärung, *warum* sich eine Prognose zwischen zwei Läufen geändert hat,
  über die beiden Begründungen hinaus. Der Betrachter vergleicht selbst.
- Keine Kommentarfunktion, kein Teilen, keine Reaktionen.
- Keine Aussagen zu einzelnen Spielern (Spec 2).
- Keine Zusammenfassungen ganzer Saisons — die Saisonaussicht ist Spec 4.

## Abnahmekriterien

- Zu jeder Prognose lässt sich das Zustandekommen in fünf Schritten lesen, ohne
  dass ein Begriff aus der KI-Technik vorkommt.
- Eine Prognose, die in die Überarbeitung ging, trägt eine sichtbare
  Kennzeichnung und zeigt den Einwand des Prüfers im Klartext.
- Eine mehrfach prognostizierte Begegnung zeigt alle Prognosen in zeitlicher
  Reihenfolge; die maßgebliche ist als solche erkennbar.
- Eine Prognose, deren Tendenz von der Basisprognose abweicht, ist gekennzeichnet;
  nach dem Spiel ist erkennbar, ob die Abweichung traf.
- Auf der Startseite steht die Rangliste aus Prognose, Basisprognose und
  „immer Heimsieg"; führt die Basisprognose, steht sie oben. Sie weist aus,
  auf wie vielen Begegnungen sie beruht.
- Die Kalibrierung ist in drei Sätzen lesbar, je Sicherheitsgruppe einer.
- Bei zu wenigen abgeschlossenen Fällen zeigen Rangliste, Kalibrierungssätze,
  Mut-Bilanz und „Aus der Erfahrung" einen Hinweis statt einer Zahl.
- Die Erklärseite ist erreichbar, beschreibt die fünf Rollen und die Rückschau
  und enthält den Abschnitt „Was das System nicht weiß".
- Keine Seite enthält die Wörter „Tipp", „Quote" oder eine Überschrift, die
  einen Ausgang als sicher behauptet.

## Glossar-Ergänzungen (in `CLAUDE.md` übernehmen)

| Spec                                        | Code                                    |
| ------------------------------------------- | --------------------------------------- |
| Zustandekommen (einer Prognose)             | `ForecastTrace`                         |
| Einwand des Prüfers                         | `Reviewer.objection` / `Objection`      |
| Verlauf vor dem Anstoß                      | `ForecastHistory`                       |
| Gegen den Strom (abweichend von Basisprognose) | `contrarian` / `ContrarianOutcome`   |
| Aus der Erfahrung (Vergleichsfälle in Prosa) | `ExperienceNote`                       |
| Rangliste (Prognose / Basisprognose / immer Heimsieg) | `Leaderboard`                 |
| Kalibrierungssatz                           | `CalibrationStatement`                  |
| Mut-Bilanz                                  | `ContrarianReport`                      |
| Erklärseite „So entsteht eine Prognose"     | `HowItWorksPage`                        |
| „Was das System nicht weiß"                 | `KnownBlindSpots`                       |
| Mindestanzahl (Schwelle für Bilanzen)       | `minimumSampleSize` (bereits Spec 3)    |
