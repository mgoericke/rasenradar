# Styleguide „Rasen-Radar – mit KI-Vorschau"

Gilt für alle Seiten (Qute-Templates unter `src/main/resources/templates/`, Stylesheet
`src/main/resources/META-INF/resources/app.css`). Neue Seiten und Fragmente halten sich an
diese Entscheidungen; Abweichungen werden hier eingetragen, nicht still im CSS.

## Leitidee: hell und wertig, eine dunkle Bühne

Wettseiten sind dunkel, weil Dunkel Spannung und Casino signalisiert. Professionalität
kommt bei ihnen aus etwas anderem: strenger Typografie, sauberen Zahlenkolonnen, wenig
Farbe, viel Luft. Genau das setzen wir **hell** um.

- **Heller Grund, weiße Flächen.** Inhalt steht auf Papier, nicht auf Nacht.
- **Genau eine dunkle Bühne.** Die Top-Navigation und die Anzeigetafel der Spielseite
  (Wappen – Ergebnis – Wappen) sind tiefes Grünschwarz („Flutlicht"). Sonst nichts.
  Keine weiteren dunklen Panels, kein Dark Mode.
- **Wertigkeit durch Verzicht.** Keine Schatten, keine Verläufe, keine Icons als
  Dekoration. Haarlinien, Innenabstand und Typografie tragen die Hierarchie.

## Farben (Tokens in `app.css`)

| Token | Wert | Verwendung |
|---|---|---|
| `--ground` | `#F4F4F1` | Seitengrund (Papier) |
| `--surface` | `#FFFFFF` | Karten, Tabellen, Listen |
| `--stage` | `#101B16` | Die dunkle Bühne: Top-Navigation, Anzeigetafel |
| `--stage-ink` | `#EEF3EF` | Text auf der Bühne |
| `--stage-soft` | `#9DB0A5` | Nebentext auf der Bühne |
| `--stage-line` | `rgba(255,255,255,.14)` | Linien und Rahmen auf der Bühne |
| `--ink` | `#14201B` | Text, gespielte Ergebnisse |
| `--ink-soft` | `#5A655F` | Nebentext |
| `--ink-mute` | `#8C9691` | Beschriftungen, Zeiten, Platzziffern |
| `--line` | `#E2E5E2` | Haarlinien |
| `--win` | `#1A8A50` | Sieg, Aufstiegs-/CL-Zone, Auswärtssieg-Tendenz |
| `--loss` | `#C63C3C` | Niederlage, Abstiegszone, Fehler, veraltete Daten |
| `--draw` | `#A3ABA6` | Unentschieden |
| `--accent` | `#B99A5B` | Messing: der eine Akzent für die KI-Vorschau |

Regeln:

- **Grün und Rot sind Bedeutung, keine Dekoration**: Sieg/Niederlage, Tabellenzonen,
  Fehlerzustände. Nirgends sonst.
- **Messing (`--accent`) ist der einzige Schmuckton** und markiert ausschließlich die
  KI-Vorschau: Schaltflächen zur Vorschau, aktive Markierung in der dunklen Navigation,
  der Skill-Score. Nicht für Überschriften, nicht für Rahmen um Inhalte.
- Heimsieg-Tendenz ist `--ink`, Auswärtssieg `--win`, Unentschieden `--ink-soft` —
  so in Balken und Pillen. Die knappen KI-Vorschau-Marker in Paarungslisten (Richtwert
  wie „2:1" statt Toto-Symbol) verzichten bewusst auf die Farbcodierung — bei einer
  einzelnen Zahl in Fließtextgröße wäre sie kaum lesbar; Tendenz und Wahrscheinlichkeit
  stehen stattdessen im Tooltip.

## Typografie

- **Display: Bricolage Grotesque** (variabel, `opsz`/`wght`) für Überschriften, Spieltagsziffer,
  Ergebnisse, große Zahlen. Gewicht 700, enge Laufweite (−0,03 em bis −0,04 em bei
  Großgrößen), optische Größe 96 für Hero-Ziffern.
- **Text: IBM Plex Sans** für Fließtext, Tabellen, Beschriftungen. Gewichte 400/500/600.
- **Zahlen immer tabular** (`font-variant-numeric: tabular-nums`), damit Kolonnen stehen.
- Keine Versalien als Beschriftung, keine Eyebrow-Labels, keine hervorgehobenen
  Einzelwörter in Überschriften.
- Zeilenlänge unter 80 Zeichen (`max-width: 70ch` für Begründungen und Erklärtexte).

## Bausteine

- **Karten** (`.standings`, `.situation`, `.chart-block`, `.forecast` …): weiße Fläche,
  Haarlinie `1px solid var(--line)`, Radius 12 px, Innenabstand ≥ 1,1 rem. Kein Schatten.
- **Pillen** (`.pill`, `.score`): Radius 6 px, Display-Schrift 700. Endgültige Ergebnisse
  gefüllt (`--ink`, auf der Bühne weiß), vorläufige gestrichelt und ungefüllt, offene
  Spiele leise (`--ink-mute`, nur Rahmen). Läuft eine Begegnung gerade (`.score.is-live`,
  rein aus der Anstoßzeit abgeleitet, siehe Spec 1), ersetzt „Läuft" den Platzhalter —
  Text-Schrift statt Display, `--loss`-Rahmen ohne Füllung: auffällig, aber erkennbar kein
  Ergebnis.
- **Tabellenzonen**: 2 px-Marker links an der Platzziffer, durchgezogen = direkt,
  gedämpft (60 % Deckkraft) = Relegation/Play-off.
- **Wappen**: `object-fit: contain`, auf der Bühne auf einer weißen runden Scheibe
  (`.crest.large`), sonst ohne Teller. Größen: 1,1 rem (Tabelle), 1,4 rem (Paarungen),
  2,4 rem (Anzeigetafel), 5,5 rem (Vereinsseite).
- **Schaltflächen**: `.button` (Aktion, gefüllt `--ink`), `.link-button` (Navigation,
  Rahmen). Wege zur KI-Vorschau tragen den Messing-Rahmen (`.link-button.ai`).
- **Bewegung** nur als Antwort auf eine Aktion (Hover, Fortschrittspunkt); kein
  Einblenden von Abschnitten. `prefers-reduced-motion` wird respektiert.
- **Zentrierter Einstieg über vollbreitem Karten-Grid** (Landingpage): Titel/Einleitung
  und die Spotlight-Begegnung sind auf 700px zentriert (`.masthead`, `.spotlight`) — ein
  ruhiger Fokuspunkt. Darunter geht `.landing` wieder auf volle Breite, die Liga-Karten
  (`.landing-grid`, `repeat(auto-fit, minmax(320px, 1fr))`) stehen nebeneinander, nicht
  in derselben schmalen Spalte — sie sollen die Seitenbreite nutzen.
- **Spotlight** (`.spotlight`): eine einzelne hervorgehobene Begegnung über der
  Liga-Übersicht — die zeitlich nächste über beide Ligen hinweg, oder eine gerade laufende
  mit Vorrang. Helle Karte wie jede andere, kein eigener Bühnen-Auftritt; die KI-Vorschau
  darin (Balken, Richtwert, Sicherheit) ist dieselbe Fragment-Ansicht wie auf der Spielseite.
- **CSS-only Tabs** (`.landing-tabs`): versteckte, aber fokussierbare Radio-Buttons mit
  Label als Umschalter zwischen zwei Ansichten (Landingpage: Spieltag/Tabelle je
  Liga-Abschnitt); sichtbarer Zustand über `:checked` und den `~`-Geschwister-Selektor, kein
  JavaScript. Native Radiogruppen-Semantik statt eines ARIA-Tablist-Musters, das eigene
  Tastatursteuerung (Pfeiltasten, Roving Tabindex) per Skript bräuchte.

## Sprache im UI

Deutsch, Sie-freie Sachsprache („Vorschau erstellen", nicht „Jetzt starten!"). Aktive
Verben auf Schaltflächen, derselbe Begriff über den ganzen Weg (Schaltfläche „Vorschau
erstellen" → Fortschritt „Vorschau wird erstellt" → Karte „KI-Vorschau"). Leere Zustände
sagen, was fehlt und was als Nächstes geht („Noch kein Tor in dieser Saison."). Keine
Quoten, keine Wettsprache.

## Was wir bewusst nicht tun

- Kein Dark Mode, keine Theme-Umschaltung.
- Keine Karten-Kits mit identischen Kacheln und Schatten.
- Keine Emojis, keine Illustrationen, keine Stock-Fotos.
- Keine Farbverläufe, keine Glaseffekte.
