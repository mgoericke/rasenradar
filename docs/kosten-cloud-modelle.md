# Kosten eines Cloud-Modells für die KI-Vorschau

Stand: 15. September 2026. Listenpreise (USD) der offiziellen Preisseiten, ohne Rabatte.

## Messbasis

Eine Vorschau (Spec 2) besteht aus fünf Modellaufrufen: drei Bewerter parallel, dann
Prognostiker und Prüfer. Sagt der Prüfer „überarbeiten", laufen Prognostiker und Prüfer ein
zweites Mal (sieben Aufrufe). Gemessen mit dem lokalen Modell (`gemma4:12b`, Faktenblatt auf
Deutsch, JSON-Antworten):

| Lauf | Input-Tokens | Output-Tokens |
|---|---|---|
| ohne Überarbeitung (5 Aufrufe) | ≈ 4.500 | ≈ 580 |
| mit Überarbeitung (7 Aufrufe, angenommen) | ≈ 6.300 | ≈ 800 |

Annahmen für die Hochrechnung: 18 Begegnungen pro Spieltag (9 je Liga), 34 Spieltage → 612
Vorschauen pro Saison; 20 % der Läufe mit Überarbeitung.

```
Kosten pro Vorschau     = Input/1e6 · Preis_in + Output/1e6 · Preis_out
Erwartungswert          = 0,8 · Kosten(5 Aufrufe) + 0,2 · Kosten(7 Aufrufe)
Kosten pro Spieltag     = 18 · Erwartungswert
Kosten pro Saison       = 612 · Erwartungswert
```

## Ergebnis

| Modell | $/1M Input | $/1M Output | pro Vorschau | pro Spieltag | pro Saison |
|---|---|---|---|---|---|
| Claude Haiku 4.5 | 1,00 | 5,00 | 0,007 $ | 0,14 $ | 4,88 $ |
| Claude Sonnet 5 | 2,00 | 10,00 | 0,015 $ | 0,29 $ | 9,77 $ |
| Claude Opus 5 | 5,00 | 25,00 | 0,037 $ | 0,72 $ | 24,42 $ |
| GPT-5-nano | 0,05 | 0,40 | 0,0005 $ | 0,009 $ | 0,30 $ |
| GPT-5-mini | 0,25 | 2,00 | 0,002 $ | 0,04 $ | 1,51 $ |
| GPT-5 | 1,25 | 10,00 | 0,011 $ | 0,22 $ | 7,54 $ |
| Gemini 3.8 Flash | 0,75 | 3,75 | 0,006 $ | 0,11 $ | 3,66 $ |
| Gemini 3.1 Pro | 2,00 | 12,00 | 0,016 $ | 0,31 $ | 10,53 $ |
| DeepSeek Flash (Off-Peak) | 0,15 | 0,60 | 0,001 $ | 0,02 $ | 0,68 $ |

Zum Vergleich: Ollama lokal (`gemma4:12b` auf einem M1 Max) kostet nichts, braucht aber rund
eine Minute pro Vorschau — etwa zehn Stunden Rechenzeit pro Saison.

## Einordnung

- Der Tokenverbrauch ist klein, weil die Agenten vorverdaute Faktenblätter statt Rohdaten
  bekommen. Selbst das teuerste Modell bleibt unter 25 $ pro Saison.
- Nicht eingerechnet: Batch-APIs (üblicherweise ~50 % Rabatt, für „Spieltag durchrechnen"
  naheliegend) und Prompt-Caching (die Systemprompts wiederholen sich; Cache-Reads kosten bei
  Anthropic und OpenAI etwa 10 % des Input-Preises).
- Preise können sich ändern und nach Region oder Tarif abweichen; DeepSeek staffelt nach
  Tageszeit.

## Empfehlung

Claude Sonnet 5 als Cloud-Modell (≈ 10 $ pro Saison): Die Aufgabe ist Schlussfolgern über
vorbereitete Fakten mit strukturierter Antwort auf Deutsch; der Prüfer profitiert von
Urteilskraft. Haiku 4.5 wäre die Sparvariante, Opus 5 lohnt sich erst, wenn die Trefferbilanz
(Spec 3) zeigt, dass Sonnet Fehler macht. Das lokale Modell bleibt als Fallback, wenn das
Cloud-Modell nicht erreichbar ist oder das Guthaben aufgebraucht ist.

## Quellen

- Anthropic: https://claude.com/pricing
- OpenAI: https://developers.openai.com/api/docs/pricing.md
- Google: https://ai.google.dev/gemini-api/docs/pricing
- DeepSeek: https://api-docs.deepseek.com/quick_start/pricing
