# Styleguide „Rasen-Radar – mit KI-Vorschau"

Gilt für alle Seiten (Qute-Templates unter `src/main/resources/templates/`, Stylesheet
`src/main/resources/META-INF/resources/app.css`). Neue Seiten und Fragmente halten sich an
diese Entscheidungen; Abweichungen werden hier eingetragen, nicht still im CSS.

## Leitidee: der frisch gemähte Platz

Ein Bundesliga-Rasen von der Haupttribüne aus: zwei Grüntöne in breiten Bahnen, darauf Kreidelinien.
Genau das ist die Struktur der Seite, nicht ihre Dekoration („Rasenschnitt", im UX-Lab als Design B
entwickelt und gemessen).

- **Heller Kreidegrund.** Inhalt steht auf hellem Grund. Die Navigation ist ein Rasenband (`--turf-a`)
  über die volle Breite, damit sie mit der Bühne darunter eine Fläche bildet (Entwurf r102).
- **Mittige Spalte.** Der Inhalt steht in einer mittigen Spalte von höchstens 1280 px; nur Navigation,
  Bühne der Startseite und Fußzeile laufen über die volle Breite. Die Saisonseite nutzt die ganze Spalte.
- **Mähstreifen sind der Rhythmus der Daten.** Spiellisten, Tabellen, Torschützen und Spielpläne
  wechseln zeilenweise zwei blasse Grüntöne (`--mow-a`/`--mow-b`). Sie ersetzen Zebra, weiße Karten und
  Haarlinien zwischen Zeilen. Die Streifen tragen Rhythmus, keine Bedeutung — nichts darf davon abhängen,
  sie zu unterscheiden.
- **Kreidelinien trennen.** Tagesüberschriften enden in einer 2-px-Kreidelinie, Abschnitte ebenso.
- **Grüne Bühne nur für ein einzelnes Spiel: der Spielkopf, das Topspiel und die Pokalbühne.** Auf der Spielseite steht
  die Begegnung auf dem gestreiften Rasen (`--turf-a`/`--turf-b`) mit Seitenlinie und Strafräumen als
  Rahmen; keine Linie kreuzt Text. Die Fakten (Wappen, Namen, Ergebnis oder Anstoß, Platz, Punkte,
  Form) sind die große Schrift; die KI-Vorschau ist eine markierte Zeile darunter. Die Startseite zeigt
  darüber hinaus genau ein Spiel auf demselben Rasen, als Link zur Spielseite und über die volle Breite
  direkt unter der Navigation, Wappen über dem Namen, das Ergebnis dazwischen: das Topspiel
  (laufend vor anstehend vor beendet, je die beste Paarung nach Tabellenplatz) oder mit „Mein Verein"
  dessen laufendes bzw. nächstes Spiel. Unter den Namen stehen Platz und Punkte vor dem Spiel und die
  Form als S/U/N-Kacheln (neuestes links; Sieg/Remis/Niederlage in den festen Tagesfarben von `--win`,
  `--draw`, `--loss`, damit die weiße Schrift in beiden Themen trägt). Unter einer Kreidelinie folgt die
  KI-Zeile (Richtwert, drei Wahrscheinlichkeiten unter den Vereinsnamen, Begründung in einer gekürzten
  Zeile), daneben „Alles zum Spiel". Pfeile auf der Seitenlinie blättern durch alle Spiele der
  Startseite („Spiel 11 von 18"). Die Pokalseite hat dieselbe Bühne in eigenem Platz (`#cup-stage`,
  „Mein Verein" tauscht dort nichts): vor und während der Runde das „Spiel der Runde" (die Paarung der
  beiden höchsten Spielklassen), danach die „Überraschung der Runde" (größter Klassenunterschied)
  oder sonst das torreichste Spiel; statt Platz und Punkte die Spielklassen („Bochum (2. Liga) schlägt
  Mainz (1. Liga)"), keine KI-Zeile. Die Pfeile blättern durch die Runde. Mehr Bühnen gibt es nicht;
  Listen, Tabellen und Karten bleiben auf Kreide.
- **Suche** (`.site-search`, Entwurf r102): Feld im Rasenband, findet Vereine, Spiele der Startseite und
  Torjäger beider Bundesligen. Die Treffer stehen in einem Feld wie das Menü „Darstellung", `/`
  springt hinein. Ohne Skript führt das Feld auf die Seite `/suche`. Auf dem Telefon ist es eine Lupe in
  der Größe der anderen Schalter, die sich beim Antippen über die erste Zeile legt.
- **Keine Anzeigetafel-Optik.** Keine LED- oder Klappziffern, kein tickender Sekunden-Countdown, keine
  Laufschrift. Eine Anstoßzeit steht in Worten da („in 5 Std 11 Min").
- **Wertigkeit durch Verzicht.** Keine Schatten, keine weichen Verläufe, keine Icons als Dekoration.

## Farben (Tokens in `app.css`)

Hell („Tagspiel"); jede Textfarbe ist gegen **beide** Streifen gemessen.

| Token | Wert | Verwendung |
|---|---|---|
| `--chalk` | `#F3F5EE` | Seitengrund |
| `--paper` | `#FAFCF7` | Erklärkarten, Formulare, Menüs — keine Datenlisten |
| `--mow-a` / `--mow-b` | `#E3EDDB` / `#D5E4CB` | Mähstreifen: Zeilenrhythmus jeder Datenliste |
| `--turf-a` / `--turf-b` | `#1C5530` / `#24653A` | Die Bühne: Spielkopf, Topspiel; `--turf-a` auch die Navigation |
| `--turf-ink` / `--turf-soft` | `#F3F5EE` / `#D3E3CC` | Text auf der Bühne (8,0:1 / 5,2:1 auf dem helleren Band) |
| `--turf-line` | `rgba(243,245,238,.55)` | Kreidelinien auf der Bühne |
| `--ink` | `#0F2417` | Text, gespielte Ergebnisse, Auswahl („gedrückt", „aktiv") |
| `--ink-soft` | `#3B5444` | Nebentext — ≥ 6,2:1 auf beiden Streifen |
| `--ink-mute` | `#435B4B` | Beschriftungen, Zeiten, Platzziffern — ≥ 5,6:1 auf beiden Streifen |
| `--line` | `#B9CCAE` | Kreidelinien auf hellem Grund |
| `--win` | `#1E7038` | Sieg, Aufstiegs-/CL-Zone |
| `--loss` | `#A5321F` | Niederlage, Abstiegszone, Fehler, veraltete Daten |
| `--draw` | `#66726A` | Unentschieden (weiße Schrift darauf 5,0:1) |
| `--ki` | `#6A3BD4` | Lila: die KI-Vorschau, nichts sonst |
| `--ki-wash` / `--ki-deep` | `#E6DCFF` / `#2A1466` | KI-Plakette (Grund / Schrift) |
| `--ki-on-turf` | `#DCCBFF` | KI auf der Bühne |
| `--live` | `#1D4FD8` | Laufende Spiele, nichts sonst |
| `--zone` | `--paper` | Hervorhebung einer Zeile (Hover, die beiden Mannschaften einer Begegnung) |

Dunkel („Abendspiel", folgt `prefers-color-scheme`, siehe unten):
`--chalk #0A1A10`, `--paper #11261A`, `--mow-a #0F2318`, `--mow-b #16301F`, `--turf-a #0E2F1B`,
`--turf-b #123A22`, `--turf-ink #E8F0E4`, `--turf-soft #B9CCB4`, `--ink #E8F0E4`, `--ink-soft #A8BBA8`, `--ink-mute #9CB09C`, `--line #2C4A35`,
`--win #5BC27E`, `--loss #FF7A63`, `--draw #8A988B`, `--ki #B49CFF`, `--ki-wash #2E2160`,
`--ki-deep #E6DCFF`, `--ki-on-turf #C4B0FF`, `--live #8FB4FF`.

Regeln:

- **Grün als Grund, Farbe als Bedeutung.** Die Rasen- und Mähtöne sind Grund. Sieg/Niederlage und
  Tabellenzonen sind kleine gesättigte Marken (Buchstaben-Pillen, 2-px-Zonenmarker), nie die Füllung
  einer ganzen Zeile.
- **Lila (`--ki`) ist ausschließlich die KI-Vorschau** und steht immer zusammen mit dem Wort „KI"
  (die Helligkeit liegt nahe an den Ergebnisfarben — in Graustufen trägt das Wort die Aussage).
  Messing gibt es nicht mehr.
- **Blau (`--live`) ist ausschließlich „läuft gerade".**
- **Auswahl ist Tinte.** Gedrückte Segmente, aktive Reiter und Abzeichen sind `--ink` auf hellem Grund.
- Heimsieg-Tendenz ist `--ink`, Auswärtssieg `--ink-soft`, Unentschieden `--draw` — so in Balken und
  Pillen. Die knappen KI-Marker in Paarungslisten verzichten auf Farbcodierung; Tendenz und
  Wahrscheinlichkeit stehen im Tooltip.

## Darstellung hell/dunkel

Die Seite folgt der Systemeinstellung (`prefers-color-scheme`). Ein kleines Menü „Darstellung" in
der Navigation erlaubt „Wie System", „Hell" und „Dunkel"; die Wahl liegt nur im Browser
(`localStorage`), es gibt kein Konto. Sie wird im `<head>` vor dem ersten Zeichnen gesetzt, damit
`hx-boost`-Seitenwechsel nicht aufblitzen.

## Typografie

- **Eine Schriftfamilie in zwei Breiten, selbst gehostet** (`/fonts`, WOFF2, Latin-Teilmenge,
  SIL Open Font License). Keine Verbindung zu Google oder einem anderen Schriftdienst.
- **Display: Sofia Sans Extra Condensed** für Namen, Ergebnisse, Anstoßzeiten, Überschriften,
  Tabellenpunkte. Gewicht 700 für Überschriften, 800 für Namen und Ergebnisse — die hohe, schmale
  Form der Trikotnummern.
- **Text: Sofia Sans** für Fließtext, Tabellen, Beschriftungen. Gewichte 400/600/700.
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

- **Datenlisten auf Mähstreifen** (`.fixtures`, `.stream-slot`, `.standings`, `.stream-table`,
  `.schedule`, `.scorers`, `.meetings`, `.group-table`, `.forecast-list`, `.leaderboard`): keine Karte,
  kein Rahmen, keine Linie zwischen Zeilen — die Zeilen wechseln `--mow-a`/`--mow-b`, Radius 8 px am
  Block, Zeilen mindestens 40 px hoch (Tippziel). Hover und hervorgehobene Zeilen (`--zone`) hellen
  auf Kreide auf.
- **Erklärkarten** (`.situation`, `.chart-block`, `.forecast`, `.role-card`, `.accuracy-card`, Bewerter):
  Fläche `--paper`, Kreidelinie `1px solid var(--line)`, Radius 12 px, Innenabstand ≥ 1,1 rem. Kein
  Schatten. Karten tragen Text und Diagramme, keine Datenzeilen.
- **Abschnittsüberschriften** (`.stream-day-label`, `h2` in Datenblöcken): Display-Schrift 700, enden
  in einer 2-px-Kreidelinie, die den Rest der Zeile füllt.
- **Pillen** (`.pill`, `.score`): Radius 6 px, Display-Schrift 700. Endgültige Ergebnisse
  gefüllt (`--ink`, auf der Bühne weiß), vorläufige gestrichelt und ungefüllt, offene
  Spiele leise (`--ink-mute`, nur Rahmen). Läuft eine Begegnung gerade (`.score.is-live`,
  Spec 1), steht „Läuft" in Text-Schrift im `--live`-Rahmen ohne Füllung, solange die
  Quelle noch keinen Zwischenstand liefert; danach der Zwischenstand selbst (`.has-score`)
  in Display-Schrift, weiter nur gerahmt und blau — auffällig, aber erkennbar kein Ergebnis.
- **Navigation auf dem Telefon** (`.nav-links` unter 560 px): Marke oben, darunter alle
  Wettbewerbe in **einer** wischbaren Zeile mit vollem Namen, ohne sichtbaren Scrollbalken —
  kein Umbruch in mehrere Zeilen, die die mitlaufende Navigation dauerhaft stehen ließe.
  `nav.js` scrollt den aktiven Eintrag in die Mitte.
- **Wettbewerbsabzeichen** (`.badge`): kleines Zeichen links am Navigationseintrag,
  Display-Schrift 700, Haarrahmen; aktiv in `--ink` gefüllt. Ein einzelnes Zeichen
  (die Ligazahl „1", „2") ergibt eine runde Scheibe, ein Kürzel („CL", „DFB", „NL")
  eine mitwachsende Plakette — `min-width` statt fester Breite, Radius 999 px. Nur
  Ligen haben eine Zahl; Pokal und Nationenwettbewerb in ein Zahlenabzeichen zu
  zwingen wäre eine falsche Aussage. **Keine Wettbewerbslogos**: die Datenquelle
  liefert keine, und die echten sind Marken.
- **Tabellenzonen**: 2 px-Marker links an der Platzziffer, durchgezogen = direkt,
  gedämpfter Marker (55 % Deckkraft, nur der Marker, nie die Ziffer) = Relegation/Play-off.
- **Wappen**: `object-fit: contain`, auf der Bühne auf einer weißen runden Scheibe
  (`.crest.large`), sonst ohne Teller. Größen: 1,1 rem (Tabelle), 1,4 rem (Paarungen),
  2,4 rem (Spielkopf), 5,5 rem (Vereinsseite).
- **Schaltflächen**: `.button` (Aktion, gefüllt `--ink`), `.link-button` (Navigation,
  Rahmen). Wege zur KI-Vorschau tragen den lila Rahmen (`.link-button.ai`).
- **KI-Plakette** (`.ki-chip`): das Wort „KI" in `--ki-deep` auf `--ki-wash`, Radius 4 px, Text-Schrift
  700. Steht vor jedem KI-Wert, der nicht schon in einem KI-Abschnitt steht.
- **Bewegung** nur als Antwort auf eine Aktion (Hover, Fortschrittspunkt); kein
  Einblenden von Abschnitten. `prefers-reduced-motion` wird respektiert.
- **Laufende Spiele aktualisieren sich selbst** (`#live-poller`): Zeigt eine Seite ein Spiel,
  das läuft oder in den nächsten 30 Minuten beginnt, fragt sie jede Minute die
  Ergebnisfelder ab (Spielseite: auch Halbzeit und Torliste) und tauscht nur diese —
  still, ohne Animation, ohne Hinweis. Das Ergebnisfeld ist dafür ein gemeinsamer
  Baustein (`{#score}`), damit Seite und Nachladen gleich aussehen. Läuft nichts mehr,
  hört die Seite auf zu fragen.
- **Landingpage: Spieltags-Strom** (Spec 1). Kein Kopfbereich mit Schriftzug — die Marke steht
  in der Navigation. **Spieltagskopf** (Entwurf r102) über dem Strom: H1 „Spieltag", in derselben
  Zeile der Ligaschalter Alle | 1. Liga | 2. Liga (`.stream-leagues`, schließt aus, die Tabelle
  daneben folgt) und das Menü „Mehr zum Spieltag" (`details.md-menu`: Status Alle/Live/Beendet/
  Anstehend, je Liga Spieltag und Tabelle, Torschützen, Saison in Zahlen, dazu KI-Bilanz).
  Darunter beide Spieltage verlinkt und der Zeitraum, dann zwischen zwei 2 px-Kreidelinien die
  Werkzeugleiste (`.md-tools`): „‹ Aktueller Spieltag ›" als Links (`/?versatz=-1`, ohne Skript
  bedienbar, am Saisonrand ausgegraut, „Zurück zu jetzt" abseits von heute) und Liste | Kompakt
  (`rr-view`). Abseits von heute fallen „Seit dem letzten Besuch" und der nächste Spieltag weg.
  Darunter der **Vorbericht** (`.brief`): kein Kasten, H2, je Zeile ein 2 px-Strich in `--ink`,
  fetter Anfang, unten eine Kreidelinie. Die Zeile von „Mein Verein" steht für jeden Verein im
  HTML und wird nur über `rrClubCss` sichtbar; in Kompakt fällt der Vorbericht weg.
  **Spielzeilen** ohne Anstoß-Kästen: eine Mähstreifen-Zeile je Spiel, links Anstoß und
  Liga-Abzeichen, das Endergebnis ohne Kasten in Display-Schrift (vorläufig gestrichelt, live
  rot umrandet), unter beendeten Spielen der Torverlauf (Story r053).
  **Pokal im Strom:** Pokalspiele, die in den vier Tagen vor dem gezeigten Bundesliga-Spieltag oder
  währenddessen angepfiffen werden, stehen mit Abzeichen „DFB" im selben Strom; der Ligaschalter
  bekommt „Pokal", das Menü einen Abschnitt DFB-Pokal, die Unterzeile die Runde. Unter 720 px steht
  der Schalter in einer eigenen Zeile. Vorbericht, Tabelle und KI-Vorschau bleiben bei den Bundesligen.
  Beide Bundesligen bilden **einen** Strom nach Tag (`.stream-day-label`) und Anstoß-Block
  (`.stream-slot`, Mähstreifen, Kopfzeile mit Uhrzeit und „KI x von y getroffen"); jede Zeile
  trägt links das Liga-Abzeichen und rechts **neben** dem Link die KI-Zelle (`.stream-ki`):
  Richtwert in Display-Schrift in `--ki` mit 2 px-Linie in `--ki`, davor die Tendenz mit Wahrscheinlichkeit,
  danach das Urteil — „getroffen" in `--ink` 600 mit gefülltem `--win`-Punkt, „daneben" in
  `--ink-soft` mit leerem Ring, bei vorläufigem Ergebnis „· vorläufig". Ligaschalter und Status
  brauchen `stream.js` und bleiben ohne Skript unsichtbar. Rechts eine klebende Spalte (21 rem) mit Tabelle (Tabs je Liga, Kurznamen)
  und KI-Bilanz. Unter 1000 px fällt die Spalte weg und ein Segment Spiele | Tabelle |
  KI-Bilanz schaltet um; unter 720 px rutscht die KI-Zelle zentriert unter die Paarung.
- **Radio-Umschalter ohne Skript** (Ansicht auf dem Telefon, Tabellen-Tabs): versteckte, aber
  fokussierbare Radio-Buttons mit Label, sichtbarer Zustand über `:checked` und den
  `~`-Geschwister-Selektor — native Radiogruppen-Semantik statt eines ARIA-Tablist-Musters.
- **Saisonseite** (`/{liga}/saison`, `.season-page`): je Auswertung ein Abschnitt (`.season-block`)
  mit Display-Überschrift und 2-px-Kreidelinie, darunter ein Satz in Worten, dann die Zahlen auf
  Mähstreifen. Anteile Heim/Remis/Auswärts als ein Balken (`.share-bar`) in den Tendenzfarben
  (`--ink`, `--draw`, `--ink-soft`), die Zahlen immer als Text daneben.
- **Wegweiser** (`.season-toc`, Saisonseite): ab sieben Abschnitten direkt unter dem Seitenkopf eine
  Zeile mit einem Link je Abschnitt, Linktext gleich Überschrift, in Seitenreihenfolge. Einfache
  Anker, also auch ohne Skript nutzbar. Mit Skript klebt die Zeile unter der Navigation (deren Höhe
  wird gemessen, sie wächst mit „Mein Verein" und auf dem Telefon), der Abschnitt im Blick ist wie ein
  aktiver Reiter markiert (`--ink`, 3-px-Unterstrich) und die Zeile wischt ihn ins Bild. Auf dem
  Telefon eine wischbare Zeile ohne sichtbaren Scrollbalken, wie die Wettbewerbe in der Navigation.
  Der Sprung ist nie animiert.
- **Kompakt** (Spieltagsseite, Umschalter „Liste | Kompakt“): eine Zeile je Spiel mit Zeit,
  Kurznamen und Ergebnis, der KI-Marker bleibt; alles unter der Paarung entfällt. Die Wahl liegt nur
  im Browser (`rr-view`) und wird wie die Darstellung im `<head>` vor dem ersten Zeichnen gesetzt.
- **Pokalseite** (DFB-Pokal, K.-o.-Runden): dieselben Werkzeuge wie die Spieltagsseite —
  Torverlauf unter jeder beendeten Begegnung (gemeinsamer Baustein `{#leadLine}`; nach Verlängerung
  reicht die Achse bis 125 Minuten, ein zweiter Strich markiert die 90.), Stern und „Beobachtet"
  für offene Begegnungen, Liste | Kompakt und „Runde drucken" (gedruckt wird nur die offene Runde,
  ohne Bühne, Seitenleiste und Feld). Zwei Spalten wie die Startseite (`.knockout-layout`, unter
  1000 px untereinander): links Kopf, Runden und „Noch im Wettbewerb", rechts eine nicht klebende
  Leiste mit Trichter und Zahlen, den ersten fünf Torjägern und den Überraschungen in Kurzform.
  **Noch im Wettbewerb** (`.cup-field`): Wappenwand der verbliebenen Vereine nach Spielklasse (Schlüssel
  wie im Trichter), darunter aufklappbar „Ausgeschieden" mit der Runde, in der es endete, in kleiner
  Schrift; nach dem Endspiel nur der Pokalsieger.
- **Pokal in Zahlen** (`/dfb/saison`, Vorlage der Saisonseite mit `page.cup`): nur, was im K.-o.
  trägt — häufigste Ergebnisse, Tore der besonderen Art, die größte Aufholjagd (ohne Vereinsbalken,
  Punkte gibt es im Pokal nicht), Torminuten mit eigenem Fenster 91.–120. und „Verlängerung und
  Elfmeterschießen" als gestreifte Liste (`.cup-decisions`). Ohne Tabelle, Serien, Vorjahr,
  Kreuztabelle und Heimvorteil; die Sprungmarken folgen.
- **Spielseite: der Spielkopf** (`.scoreboard`, `.board-ai`, `.board-verdict`): gestreifter Rasen
  mit Seitenlinie und zwei Strafräumen als Rahmen; Wappen auf weißer Scheibe, Namen und Ergebnis in
  Display-Schrift 800. Unter einer Kreidelinie eine KI-Zeile — KI-Plakette, Richtwert und Tendenz
  links, Wahrscheinlichkeitsbalken mit Beschriftung in der Mitte, nach Abpfiff rechts das Urteil als
  lila Pille (gefüllt = getroffen, Umriss = daneben). Darunter links die Begründung offen (drei Bewerter als Karten,
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

- Keine Anzeigetafel-Optik: keine LED- oder Klappziffern, kein Sekunden-Countdown, keine Laufschrift.
- Keine Karten-Kits mit identischen Kacheln und Schatten; Datenlisten stehen auf Mähstreifen.
- Keine Emojis, keine Illustrationen, keine Stock-Fotos.
- Keine weichen Farbverläufe, keine Glaseffekte. Erlaubt sind nur harte Bänder (Mähstreifen), auch
  wenn sie technisch als `repeating-linear-gradient` gezeichnet werden.
- Keine Schriften von fremden Servern.
