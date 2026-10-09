# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

Bundesliga-Fans, öffentlich. Der Betreiber ist selbst einer davon und hat das Projekt aus
Fußballinteresse begonnen; die Seite (rasen-radar.de) ist aber bewusst für alle Fans
gebaut, nicht als persönliches Werkzeug. Typische Situationen: vor dem Spieltag
einschätzen, was ansteht; während/nach dem Spieltag Ergebnisse, Tabelle und Form
nachsehen; die KI-Vorschau einer Begegnung lesen und prüfen, ob sie etwas taugt.

## Product Purpose

Aktuelle Spieltage, Tabellen und Vereinsdaten der 1./2. Bundesliga (dazu Champions-League-
Ligaphase, DFB-Pokal und Nations League zur Ansicht) plus eine KI-Vorschau je Begegnung,
die sich an ihrer eigenen Trefferbilanz messen lässt.

Erfolg heißt:

- **Schnelle Spieltags-Übersicht** — Ergebnisse, Tabelle, Form auf einen Blick.
- **Die KI-Vorschau wird verstanden** — Leser sehen, wie eine Prognose zustande kommt und
  wie gut sie wirklich ist (Trefferbilanz gegen eine statistische Basisprognose).
- **Wiederkehrende Besucher** — Fans kommen Spieltag für Spieltag wieder.

## Positioning

Keine Wettseite und kein reines Ergebnisportal: Die KI-Vorschau legt offen, wie sie
entsteht (drei Bewerter, Prognostiker, Prüfer mit Einwand), wird nie nachträglich
geändert, und wird öffentlich gegen endgültige Ergebnisse und eine Statistik-
Basisprognose abgerechnet. Bei zu dünner Datenlage sagt sie ausdrücklich nichts statt
einer Zahl.

## Operating Context

- Rhythmus des Spieltags: Vorschau vor dem Anstoß, Ergebnisse live (vorläufig), endgültig
  24 Stunden nach Anstoß, danach Rückschau und neue Saisonaussicht.
- Datenquelle OpenLigaDB, alle 15 Minuten abgeglichen.
- Desktop und Mobil sind gleichrangig; kein Gerät hat Vorrang.
- Server-gerenderte Seiten (Qute + htmx + Chart.js), kein SPA.
- Besucherzahlen über selbst gehostetes Umami; keine Statistikseite in der App.

## Capabilities and Constraints

- Spieltage, Tabellen („vor Spieltag N"), Form, direkte Duelle, Vereinsseiten mit
  Saisonverlauf, Spielplan, Bilanz, Torschützen; Saisonaussicht (Monte-Carlo) je Mannschaft.
- KI-Vorschau (agentischer Workflow), Zustandekommen, Verlauf, Trefferbilanz, Rangliste,
  Kalibrierung, Rücktest-Modus (getrennt ausgewiesen).
- Vorläufige Ergebnisse werden angezeigt und klar markiert, aber nie für Rückschau genutzt.
- Prognosen werden nie geändert oder gelöscht.
- **Keine Quoten, keine Wettempfehlungen, keine Wettsprache.**
- Fachbegriffe und ihre Code-Namen: Glossar in `CLAUDE.md`.

## Brand Commitments

- Name **Rasen-Radar**, Zusatz „mit KI-Vorschau".
- UI-Sprache Deutsch, sachlich und Sie-frei („Vorschau erstellen", nicht „Jetzt starten!");
  derselbe Begriff über den ganzen Weg; leere Zustände sagen, was fehlt und was als
  Nächstes geht.
- Privates, nicht kommerzielles Hobbyprojekt; die KI-Vorschau ist eine Einschätzung, kein Rat.
- Die visuelle Sprache ist in `docs/styleguide.md` festgelegt und verbindlich.

## Evidence on Hand

- Echte Live-Daten und echte Prognosen samt Trefferbilanz in der laufenden App.
- Screenshots: `docs/screenshots/` (Startseite, Spieltag, Begegnung, KI-Vorschau,
  Vereinsseite, Trefferbilanz).
- Fachliche Specs: `docs/specs/`; Architektur: `docs/architecture/`.
- Keine Testimonials, keine Pressestimmen, keine Nutzerzahlen zur Veröffentlichung —
  nichts davon erfinden.

## Product Principles

1. **Der Spieltag zuerst.** Was gerade ansteht oder passiert ist, muss ohne Suchen
   erreichbar und auf einen Blick lesbar sein.
2. **Die KI muss sich erklären und messen lassen.** Jede Vorschau zeigt, wie sie
   entstanden ist, und ihre Bilanz ist öffentlich — auch wenn sie schlecht aussieht.
3. **Lieber nichts als eine dünne Zahl.** Unsicherheit wird benannt, nicht übertüncht.
4. **Fan-Werkzeug, kein Wettangebot.** Nichts, was nach Quote, Tipp oder Einsatz aussieht.
5. **Ein Grund zum Wiederkommen je Spieltag.** Neue Ergebnisse, neue Vorschau, neue
   Abrechnung — der Rhythmus der Saison trägt die Seite.
