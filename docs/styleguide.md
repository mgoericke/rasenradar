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
| `--ink-mute` | `#66706B` | Beschriftungen, Zeiten, Platzziffern — ≥ 4,5:1 auf Grund und Karte (WCAG AA); vorher `#8C9691` mit 2,8:1 |
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
- **Die Schriftgröße sitzt an der Wurzel** (`html`), nicht am `body` — sonst rechnet jedes
  `rem` gegen die 16 px des Browsers, während der Fließtext auf einem anderen Wert steht.
  Sie ist fluide mit `rem`-Anteil (`clamp(16px, 0.9rem + 0.35vw, 17.5px)`): 16 px auf dem
  Telefon, 17,5 px auf großen Schirmen, und Zoom bleibt benutzbar.
- **Nichts unter 0,75 rem.** Kleinstschrift in `--ink-mute` ist die schwächste Stelle jeder
  Seite; darunter wird sie auf dem Telefon unlesbar.
- **Auf dem Telefon wird Schrift nicht verkleinert.** Das Telefon ist der Ort, an dem man
  Größe braucht — Platz kommt aus der Breite (Spalten, die zusammenfallen, wenn sie leer
  sind), nicht aus der Schriftgröße.
- **`clamp()` muss auf Telefonbreite auch wirklich arbeiten.** Liegt der `vw`-Anteil bei
  430 px unter dem Minimum, ist die Größe dort eingefroren und der ganze Ausdruck nutzlos.
  Faustregel: das Minimum so wählen, dass es bei ~400 px greift, nicht erst bei 600.

## Bausteine

- **Karten** (`.standings`, `.situation`, `.chart-block`, `.forecast` …): weiße Fläche,
  Haarlinie `1px solid var(--line)`, Radius 12 px, Innenabstand ≥ 1,1 rem. Kein Schatten.
- **Pillen** (`.pill`, `.score`): Radius 6 px, Display-Schrift 700. Endgültige Ergebnisse
  gefüllt (`--ink`, auf der Bühne weiß), vorläufige gestrichelt und ungefüllt, offene
  Spiele leise (`--ink-mute`, nur Rahmen). Läuft eine Begegnung gerade (`.score.is-live`,
  Spec 1), steht „Läuft" in Text-Schrift im `--loss`-Rahmen ohne Füllung, solange die
  Quelle noch keinen Zwischenstand liefert; danach der Zwischenstand selbst (`.has-score`)
  in Display-Schrift, weiter nur gerahmt und rot — auffällig, aber erkennbar kein Ergebnis.
- **Navigation auf dem Telefon** (`.nav-links` unter 560 px): Marke oben, darunter alle
  Wettbewerbe in **einer** wischbaren Zeile mit vollem Namen, ohne sichtbaren Scrollbalken —
  kein Umbruch in mehrere Zeilen, die die mitlaufende Navigation dauerhaft stehen ließe.
  `nav.js` scrollt den aktiven Eintrag in die Mitte.
- **Wettbewerbsabzeichen** (`.badge`): kleines Zeichen links am Navigationseintrag,
  Display-Schrift 700, Haarrahmen; aktiv in Messing gefüllt. Ein einzelnes Zeichen
  (die Ligazahl „1", „2") ergibt eine runde Scheibe, ein Kürzel („CL", „DFB", „NL")
  eine mitwachsende Plakette — `min-width` statt fester Breite, Radius 999 px. Nur
  Ligen haben eine Zahl; Pokal und Nationenwettbewerb in ein Zahlenabzeichen zu
  zwingen wäre eine falsche Aussage. **Keine Wettbewerbslogos**: die Datenquelle
  liefert keine, und die echten sind Marken.
- **Tabellenzonen**: 2 px-Marker links an der Platzziffer, durchgezogen = direkt,
  gedämpft (60 % Deckkraft) = Relegation/Play-off.
- **Wappen**: `object-fit: contain`, auf der Bühne auf einer weißen runden Scheibe
  (`.crest.large`), sonst ohne Teller. Größen: 1,1 rem (Tabelle), 1,4 rem (Paarungen),
  2,4 rem (Anzeigetafel), 5,5 rem (Vereinsseite).
- **Schaltflächen**: `.button` (Aktion, gefüllt `--ink`), `.link-button` (Navigation,
  Rahmen). Wege zur KI-Vorschau tragen den Messing-Rahmen (`.link-button.ai`).
- **Bewegung** nur als Antwort auf eine Aktion (Hover, Fortschrittspunkt); kein
  Einblenden von Abschnitten. `prefers-reduced-motion` wird respektiert.
- **Laufende Spiele aktualisieren sich selbst** (`#live-poller`): Zeigt eine Seite ein Spiel,
  das läuft oder in den nächsten 30 Minuten beginnt, fragt sie jede Minute die
  Ergebnisfelder ab (Spielseite: auch Halbzeit und Torliste) und tauscht nur diese —
  still, ohne Animation, ohne Hinweis. Das Ergebnisfeld ist dafür ein gemeinsamer
  Baustein (`{#score}`), damit Seite und Nachladen gleich aussehen. Läuft nichts mehr,
  hört die Seite auf zu fragen.
- **Landingpage: Spieltags-Strom** (Spec 1). Kein Kopfbereich mit Schriftzug — die Marke steht
  in der Navigation. H1 „Bundesliga, 23.–25. Oktober", darunter beide Spieltage verlinkt.
  Beide Bundesligen bilden **einen** Strom nach Tag (`.stream-day-label`) und Anstoß-Block
  (`.stream-slot`, weiße Karte, Kopfzeile mit Uhrzeit und „KI x von y getroffen"); jede Zeile
  trägt links das Liga-Abzeichen und rechts **neben** dem Link die KI-Zelle (`.stream-ki`):
  Richtwert in Display-Schrift mit 2 px-Messinglinie, davor die Tendenz mit Wahrscheinlichkeit,
  danach das Urteil — „getroffen" in `--ink` 600 mit gefülltem `--win`-Punkt, „daneben" in
  `--ink-soft` mit leerem Ring, bei vorläufigem Ergebnis „· vorläufig". Darüber eine klebende
  Filterleiste (Segment Alle/Live/Beendet/Anstehend, Liga-Chips; `stream.js`, ohne Skript
  unsichtbar). Rechts eine klebende Spalte (21 rem) mit Tabelle (Tabs je Liga, Kurznamen)
  und KI-Bilanz. Unter 1000 px fällt die Spalte weg und ein Segment Spiele | Tabelle |
  KI-Bilanz schaltet um; unter 720 px rutscht die KI-Zelle zentriert unter die Paarung.
- **Radio-Umschalter ohne Skript** (Ansicht auf dem Telefon, Tabellen-Tabs): versteckte, aber
  fokussierbare Radio-Buttons mit Label, sichtbarer Zustand über `:checked` und den
  `~`-Geschwister-Selektor — native Radiogruppen-Semantik statt eines ARIA-Tablist-Musters.
- **Spielseite: Urteil in der Anzeigetafel** (`.board-ai`, `.board-verdict`): unter dem Ergebnis
  eine KI-Zeile auf der Bühne — Richtwert und Tendenz links, Wahrscheinlichkeitsbalken mit
  Beschriftung in der Mitte, nach Abpfiff rechts das Urteil als Messing-Pille (gefüllt =
  getroffen, Umriss = daneben). Darunter links die Begründung offen (drei Bewerter als Karten,
  Prognostiker und Prüfer in einer Karte), dann Form und Duelle; rechts die Tabelle vor dem
  Spieltag (`.match-rail`), die beiden Mannschaften der Begegnung auf `--zone` hervorgehoben.
  K.-o.-Spiele haben keine Tabelle und damit keine rechte Spalte.

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
