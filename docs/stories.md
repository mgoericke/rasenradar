<!-- Copied from the UX lab hand-off (handoff/STORIES.md, 31 Oct 2026). Links to the lab's CHANGELOG and
     snapshots point outside this repository and do not resolve here. Which stories are already
     built is not tracked in this file; check the code and the git history ("rNNN" in commit titles). -->

# User stories: lab features for the real rasen-radar repo

61 stories: 59 for the features the [triage](TRIAGE.md) marked keep or adapt (r083 is part of r009; r064 has a new story because its season wall exists but must open the match) plus 2 new ones from [Mark's review](FEEDBACK.md) (N01, N02). Every lab story links its CHANGELOG entry and snapshot. The tickets are in English and the quoted UI text is in German.

No Definition of Ready or Done exists in the reference repo, so the stories follow the style of the specs' Abnahmekriterien. They describe what and why, not how. Implementation follows the real repo's CLAUDE.md: BCE packages, Qute + htmx, and its glossary terms.

## Product principles (Mark, 31 Oct 2026, see [FEEDBACK.md](FEEDBACK.md))

1. **Current data first, AI in the background.** Forecasts are one feature among many: marked as forecast wherever a match appears, always one click away, never a large block on the start page.
2. **Not a prediction game, not even a small one.** No own calls, no tally against the AI, no challenges, no group chat text.
3. **Newest data on top** in every list and page.
4. **Clicking a match opens the match detail page** (search, lists, club season wall, calendar), never the AI view or the opponent's club page.
5. **Browser-only personalisation is fine** (Mein Verein, stars, page customisation): no login, no server-side user data.
6. **No realtime push.** No goal alerts.

## Rules for every story

- **Wording:** the real UI is plain German without direct address ("Vorschau erstellen"). Where a story quotes lab wording with "du"/"dein" ("Gut für deinen Verein", "Seit deinem letzten Besuch"), use neutral wording when porting ("Gut für den eigenen Verein", "Seit dem letzten Besuch"). "Tipp" is never used, and no betting language.
- **Colour:** green/red only as meaning, brass only for the AI preview. Lab highlights in amber become ink or hairline styling.
- **Personal features** (Mein Verein, starred matches, last visit, hidden sections) live only in the browser. There are no accounts and no server-side user data.
- **Provisional results** may be shown but are always marked, and never count toward review or hit rate.
- **AI terms:** follow Spec 03: "Tendenz getroffen" vs. "genaues Ergebnis"; thresholds for "too few cases" reuse `minimumSampleSize`.

## Decisions (31 Oct 2026)

All decided in [FEEDBACK.md](FEEDBACK.md); the stories are written accordingly.

1. **Own call before the AI (r001):** dropped, together with everything built on it (r023, r040, r079, r089 and "Du : KI"). The AI preview is visible and marked everywhere.
2. **Mein Verein (r003):** yes, browser-only.
3. **Weather and stadium coordinates (r034, r060):** yes. Weather from Bright Sky (DWD open data, no key), one request per match, cached. Coordinates in [data/stadiums.jsonl](data/stadiums.jsonl).
4. **Printed matchday sheet (r028):** the plain sheet stays, the guessing columns are dropped.
5. **Also decided:** group chat text (r074), scoreboard view (r010) and the ticker band (r049) are dropped; "Erste Schritte" (r017) is deferred; goal alerts are dropped (r057, r096).

**Thresholds:** decided; the table below overrides any threshold marked as an assumption in a story.

| Story | Value | Decided |
|---|---|---|
| r004 Anpfiff-Uhr | countdown starts | 24 h before kickoff |
| r060 Derby | distance between grounds | at most 60 km straight line |
| r070 Es geht um | Spitzenspiel / Kellerduell | both on places 1–4 / both on places 15–18 |
| r073 Spannendste zuerst | "close in points" | at most 3 points apart |
| r068 Serien | a streak counts from | 3 wins, 4 unbeaten, 3 defeats |
| r071 Schluss nach 80 | cut-off | goals up to and including minute 80 |
| r091 Aufholjagden | "after trailing" | behind at any time in the match |
| r078 Gut für den eigenen Verein | table neighbours | up to 3 places away |
| r016 Torminuten | text finding shown from | 5 goals |
| r014 Torjäger | list length | top 10, all players tied on place 10 included |
| r098 Heimvorteil | list length | 5 clubs |
| r009 Offline | pages kept | the last 10 visited |
| r018, N01 | "too few cases" | the repo's `minimumSampleSize` |

**Assessor tally (r077):** option A – Spec 05 stays; the three viewpoints are explained on the match detail page, without any tally or right/wrong marks.

Nothing is open any more.

## New from Mark's review

---

### N01 – Spieldetail: everything about one match
Lab reference: none (new in [Mark's review](FEEDBACK.md#new-match-detail-page)) · Extends `MatchdayPages/match.html` · Triage: new

**Title:** Match detail page with head-to-head, form, forecast and the AI record per club
**Story:** As a fan, I want one page per match that shows the history of both clubs and the AI forecast, so that I can understand the match and see how reliable the AI has been for these two clubs.
**Acceptance Criteria:**
- Given any match in any list, gallery, search result, calendar entry or club season wall, When I click or tap it, Then its detail page opens (an extension of the existing `match.html`; no new page is created).
- Given the page, When I read it, Then it shows the head-to-head history (already present), the table position over the season and the table before the matchday (already present) and, per club, the recent matches with the newest first.
- Given a Forecast exists, When the page is shown, Then the AI forecast is shown and marked as a forecast (KI-Vorschau with brass marking); Given none exists, Then the existing wording "Noch keine KI-Vorschau" stays.
- Given a FINAL match with a Forecast, When I open the page, Then the result is shown against the forecast: final score, forecast Richtwert, "Tendenz getroffen" or "Tendenz daneben", and "genaues Ergebnis" or "kein genaues Ergebnis" (see r012).
- Given each of the two clubs, When enough settled forecasts exist, Then a line states "Bei {Verein} lag die KI in xx % der Spiele richtig, in yy % tendenziell richtig", where xx is the share of exact results and yy the share of correct tendencies (Spec 03).
- Given a club with fewer settled forecasts than `minimumSampleSize`, When the page is shown, Then its line is hidden, not shown with a weak number.
- Given the AI record, When it is computed, Then only FINAL results of matches with a Forecast count; provisional results and Backtest forecasts do not.
- Given a PROVISIONAL or running match, When the page is shown, Then the provisional marking is shown as today, and the comparison with the forecast follows once the result is final.
**QA Notes:** Check the page for upcoming, running, provisional and final matches, with and without a Forecast, and clubs just above and below `minimumSampleSize`. Verify xx and yy against the Trefferbilanz counts for the same club (yy is never lower than xx), rounding of percentages, both leagues and cup matches (display only, no AI record if no Forecasts). Check the page at 375 px, and that arriving from search, a club's season wall, the gallery and the calendar always lands here. The AI view must stay one click away from this page, not the other way round.
**Open Questions / Assumptions:**
- Assumption: the percentage yy counts every correct tendency, including exact results, so xx ≤ yy; the lab wording of the review leaves this open.
- Assumption: "bei Verein X" counts home and away matches of the club in the current season and league; whether earlier seasons count is open.
- Assumption: the "recent matches" are the last five per club (as in the existing page).
- Does the AI record also appear on the TeamPage? Not part of this story.

---

### N02 – Start page order
Lab reference: none (new in [Mark's review](FEEDBACK.md#per-area)) · Triage: new

**Title:** Start page: gallery first, matchday list below, forecasts as marked lines
**Story:** As a visitor, I want the start page to show the matches first, so that I get current data right away and find AI forecasts as a marked extra.
**Acceptance Criteria:**
- Given the start page, When it loads, Then the first block is the gallery (r054) with the top match, or the "Mein Verein" match if a club is chosen, first.
- Given the gallery, When I scroll, Then the matchday list follows directly below it, with nothing in between.
- Given the start page, When I look through it, Then there is no "Abgerechnet" block (r012) and no AI explanation block; both live on the match detail page (N01) and the area "KI-Bilanz" (r020).
- Given a match with a Forecast, When it is shown on the start page, Then the forecast appears per match as a line marked as KI-Vorschau (brass marking, "Richtwert"), and the line links to that match's detail page.
- Given a match without a Forecast, When it is shown, Then no empty line or placeholder appears.
- Given the start page, When the page is read, Then no band, ticker or running text sits above the gallery.
- Given a phone viewport, When the page loads, Then the first match is visible without scrolling past other blocks.
**QA Notes:** Check with and without a chosen club, a matchday without a Forecast anywhere, midweek rounds, 375 px, and keyboard order (gallery before list). Verify the page is useful with all AI-related blocks absent; the number of requests must not grow compared with today.
**Open Questions / Assumptions:**
- Mark likes the presentation of "Abgerechnet" and the AI explanation; they move to the match detail page and KI-Bilanz unchanged (see r012).
- The running ticker band above the top match (r049) is dropped by Mark.
- Whether the matchday list stays grouped by day or follows the "Spannendste zuerst" sort (r073) stays with those stories.

---

## Batch 1 (r002–r027)

Stories for features from the UX lab, adapted to the real Rasen-Radar product rules (server-rendered pages, light design with one dark stage, no accounts, no betting language, real OpenLigaDB data only). Assumption: No Definition of Ready/Done exists in the reference repo; stories follow the specs' Abnahmekriterien style. UI texts quoted in criteria are German; everything else is English.

---

### r002 – Sofort da: self-hosted logos + no layout jumps
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r002--sofort-da-self-hosted-logos--no-layout-jumps) · [Snapshot](../snapshots/r002/) · Triage: keep

**Title:** Club crests load from our own host with reserved space
**Story:** As a fan on a phone, I want club crests to appear immediately without the page jumping, so that I can read scores without content shifting under my thumb.
**Acceptance Criteria:**
- Given any page that shows crests (Standings, match list, scoreboard, TeamPage), When I inspect the page's network requests, Then every crest is served from the Rasen-Radar host and no crest is requested from a third-party host.
- Given a cold cache and a throttled mobile connection, When a page with crests loads, Then the cumulative layout shift caused by crests is 0 (no element moves when a crest arrives).
- Given the crest sizes from the styleguide (1,1 rem, 1,4 rem, 2,4 rem, 5,5 rem), When a crest is rendered, Then its box has the fixed size of its context before the image is loaded and the image keeps its proportions.
- Given a Team whose crest file is missing or fails to load, When the page renders, Then the team name is still shown and the reserved space stays unchanged (no broken-image icon).
- Given a crest is requested a second time within a session, When the browser cache is warm, Then no new transfer of that file occurs.
**QA Notes:** Compare total transfer for the start page before/after (lab saw ~1.9 MB of crest traffic per visit before). Test the scoreboard's white round disc behind crests on the dark stage. Check teams newly promoted/relegated in a new season (crest present for all 36 clubs of both Bundesligen plus competition participants shown in CL/DFB-Pokal/Nations League views). Check high-DPI screens for sharpness at 5,5 rem. Verify no horizontal overflow at 375 px.
**Open Questions / Assumptions:**
- Crests are club trademarks; is hosting copies acceptable (licence/permission), and where do updates come from when a club changes its crest? (Lab fetched once from Wikimedia.)
- Assumption: the real app may already host some crests; this story covers all of them uniformly.
- "No layout jump" is measured as CLS 0 for crest-related shifts; overall page CLS target is assumed to be ≤ 0.1.

---

### r003 – Mein Verein: personalised club mode
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r003--mein-verein-personalised-club-mode) · [Snapshot](../snapshots/r003/) · Triage: adapt

**Title:** Choose "Mein Verein" and see it highlighted (browser-only, no club colours)
**Story:** As a fan of one club, I want to mark my club once, so that it is easy to find in matchday lists and Standings on every visit.
**Acceptance Criteria:**
- Given no club is chosen, When I use "Mein Verein wählen", Then I see all clubs of the 1. and 2. Bundesliga (name and crest) and can choose exactly one.
- Given I chose a club, When I reload or return on a later day in the same browser, Then the choice is still active without any login.
- Given a club is chosen, When I view the Spieltag stream, the Standings or a Matchday page, Then that club's rows are highlighted with the neutral highlight style (`--zone`) and no club-specific colour is used anywhere.
- Given a club is chosen, When I open the site, Then a link to that club's TeamPage is reachable from the main navigation area in one tap/click.
- Given a club is chosen, When I choose "Auswahl entfernen" or pick another club, Then the previous highlight disappears everywhere and the new state is applied after the next page load.
- Given I open the site in another browser, When I look at any page, Then no club is highlighted (no account, no server-side user data).
**QA Notes:** Highlight must not use green/red (reserved for meaning) and must keep ≥ 4,5:1 contrast of text. Test a club in a league that has no match this Matchday, a club relegated/promoted to the other league in a new Season, and a stored club that no longer exists in the data (should fall back to "no club chosen" without error). Check that blocking storage (private mode) leaves the site fully usable. Check that the first paint does not flash un-highlighted rows (see open question on the server hint). Verify that Qute-rendered pages stay cacheable.
**Open Questions / Assumptions:**
- Triage asks for a "cookie-free server hint" so that the first render already shows the highlight. Which mechanism is acceptable without storing user data on the server (e.g. a URL parameter on the user's own links)? Needs a product decision; criteria above only require the highlight to appear after load.
- Lab replaced the hero by the club's match and tinted the masthead with the club colour; both are dropped. Is a "Mein Verein" shortcut on the start page wanted at all? Assumption: only highlighting and one link.
- Is a "first visit" prompt desired? Assumption: no, the choice is always user-initiated.

---

### r004 – Anpfiff-Uhr + live mode
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r004--anpfiff-uhr--live-mode) · [Snapshot](../snapshots/r004/) · Triage: keep

**Title:** Countdown to kickoff and self-refreshing live score on the scoreboard
**Story:** As a fan, I want the scoreboard of a match to count down to kickoff and update itself during the match, so that I can keep the page open without reloading.
**Acceptance Criteria:**
- Given an upcoming Match that starts within the next 24 hours, When I open its match page, Then the dark scoreboard shows the remaining time until kickoff (e.g. "Anstoß in 2 Std. 14 Min.") and the countdown decreases at least once per minute.
- Given the countdown reaches zero, When the page remains open, Then the countdown is replaced by the running state ("Läuft") without a manual reload.
- Given a Match is running or begins within 30 minutes, When the page stays open, Then the result fields are re-requested every 60 seconds and only they are replaced, silently and without animation.
- Given a Match is running, When a score is shown, Then it is rendered as `liveScore` in the live style (red outline, no fill) and labelled as provisional; it is never styled like a final result.
- Given no Match on the page is running or about to start, When the page is open, Then no further refresh requests are made.
- Given the page is open in a background tab, When it returns to the foreground, Then the data is refreshed once immediately.
**QA Notes:** Test across the kickoff boundary (±1 min), a delayed kickoff (source moves kickoff later), a postponed Match, extra time / DFB-Pokal, Matches ending (PROVISIONAL then FINAL after 24 h), device clock skewed from the server clock (countdown should rely on server-provided kickoff times and tolerate skew), and a failing OpenLigaDB response (show last known value and the existing stale-data marking, no error flash). Check screen readers are not interrupted every minute (no live-region announcement for each tick). Check battery/CPU: no timers keep running once the Match is over. Check mobile vs desktop scoreboard layout.
**Open Questions / Assumptions:**
- The styleguide already documents a one-minute `#live-poller` for running Matches. Assumption: this story adds the countdown and ensures the behaviour on the scoreboard; if the poller already satisfies the live criteria, those criteria become regression tests only.
- The 24-hour countdown threshold is an assumption; lab showed the countdown for the next hero match only.
- Lab extras dropped: amber split-flap digits, goal flash, "TOR!" in the tab title, pulsing live badge (brass is reserved for the AI preview; no decoration). Is a goal notice in the tab title wanted later?

---

### r005 – Teilbare Spielkarte + Deep Links
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r005--teilbare-spielkarte--deep-links) · [Snapshot](../snapshots/r005/) · Triage: adapt

**Title:** Every match has a stable, shareable link
**Story:** As a fan, I want to share a link that opens exactly one match, so that friends land directly on that match page.
**Acceptance Criteria:**
- Given any Match (past, running, upcoming) in the displayed competitions, When I open its match page, Then its address is a stable URL that contains no session or personal data and opens the same Match on every later visit.
- Given the match page, When I choose "Link teilen" on a device with a native share sheet, Then the share sheet opens with the match URL and a text of the form "Bremen – Köln, 3. Spieltag".
- Given a device or browser without a share sheet, When I choose "Link teilen", Then the URL is copied and the page confirms it with "Link kopiert".
- Given a message app that previews links, When the URL is posted, Then the preview title names both Teams and the Matchday and contains no Forecast pick (the preview text for an upcoming Match must not reveal the AI preview).
- Given a URL for a Match that does not exist, When I open it, Then I see a page that says the Match was not found and links to the current Matchday.
- Given a Match is PROVISIONAL, When it is opened through a shared link, Then the provisional marking is shown exactly as on the normal page.
**QA Notes:** Verify old links keep working after a Season change and after kickoff times move. Test links to matches of CL, DFB-Pokal and Nations League (display only). No images are generated; confirm no canvas/download fallback exists. Check link preview rendering for upcoming vs final Matches in at least two messengers.
**Open Questions / Assumptions:**
- Assumption: match pages already exist at stable addresses in the real app; if not, defining the address scheme is part of this story.
- The canvas share image (1080×1080 card) is dropped per triage (decoration). A text-only link preview is assumed sufficient.
- Should the link preview of a finished Match include the final score? Assumption: yes, only if FINAL.

---

### r007 – "Was wäre, wenn …": table projection
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r007--was-wäre-wenn--table-projection) · [Snapshot](../snapshots/r007/) · Triage: adapt

**Title:** "Was wäre, wenn": Standings projected from the AI tendencies
**Story:** As a fan, I want to see how the Standings would look if the open matches of a Matchday end as the AI expects, so that I can understand what is at stake before kickoff.
**Acceptance Criteria:**
- Given a Matchday with at least one open Match, When I open the Standings, Then I can switch between "Jetzt" and "Nach KI-Vorschau"; there is no further option.
- Given "Nach KI-Vorschau" is active, When the table is shown, Then every open Match is counted with its Forecast Richtwert score, the table is re-sorted by points, goal difference and goals, and a "Veränderung" column shows each Team's position change as a signed number.
- Given a projection is shown, When I look at the page, Then the table carries the heading "Angenommen: …" with the basis, and it is clearly distinguished from the real table, so that it can't be read as a result.
- Given an open Match has no Forecast (insufficient data), When "Nach KI-Vorschau" is active, Then that Match is not counted and a line names it ("Ohne KI-Vorschau: Bremen – Köln").
- Given a Match in the Matchday is PROVISIONAL or running, When a projection is shown, Then it is computed from the provisional score and the note "enthält vorläufige Ergebnisse" is displayed.
- Given I switch back to "Jetzt", When the table is shown, Then it equals the real Standings of the page.
- Given the projection is shown, When I look at the table, Then rows appear without animation (no row movement effect).
**QA Notes:** Test ties (equal points/goal difference), a Matchday where all Matches are final (switch hidden or disabled with explanation), and a Matchday where only some Matches have a Forecast (the others stay as real/unprojected). Check Tabellenzonen markers follow the projected order. Check mobile layout of the extra column. Check that no colour other than the existing zone markers is used for movement. The projection must never be labelled as a result or recommendation.
**Open Questions / Assumptions:**
- Decision (Mark, 31 Oct 2026): the projection uses the AI tendencies only; own calls do not exist (r001 dropped).
- Should movement use green/red (as meaning of up/down) or stay neutral? Assumption: neutral ink.
- Should the projection be a toggle on the Standings or a separate page "Nach Spieltag N"?
- Wording must avoid anything that sounds like betting ("Tipp", "Wette"); the projection is "kein Ergebnis".

---

### r009 – Rasen-Radar als App: installable + offline (incl. install hint, lab r083)
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r009--rasen-radar-als-app-installable--offline) · [Snapshot](../snapshots/r009/) · Triage: adapt
Also: [r083 CHANGELOG entry (install hint)](../monitoring/CHANGELOG.md#r083--aufs-handy-holen) · [r083 Snapshot](../snapshots/r083/)

**Title:** Installable app with offline fallback and a one-time install hint
**Story:** As a returning fan, I want to put Rasen-Radar on my home screen and still see the last pages I visited without a connection, so that I can check results even with bad stadium reception.
**Acceptance Criteria:**
- Given a supporting browser, When I open the site over HTTPS, Then it is installable under the name "Rasen-Radar" with an icon and opens without browser address bar when launched from the home screen.
- Given I visited a page and the connection is lost, When I open that page again, Then the last version is displayed with the banner "Offline – Stand von 14:32 Uhr" and the stale state is marked with the stale-data style.
- Given I never visited a page and I am offline, When I open it, Then I see a page that says that the page is not available offline and offers the pages that are (e.g. the last visited Matchday).
- Given I visited the site on a second, different calendar day (counted in this browser only), When I open any page, Then a hint card offers installation with the text "Rasen-Radar auf den Startbildschirm – schnell erreichbar, auch offline mit den zuletzt besuchten Seiten."
- Given the hint is displayed and the browser offers installation, When I choose "Installieren", Then the browser's install prompt opens; on iPhone Safari the hint instead says "Im Teilen-Menü „Zum Home-Bildschirm“ wählen".
- Given I choose "Nicht jetzt", When I visit again, Then the hint is never shown again; and Given the app is running installed, Then the hint is never shown.
**QA Notes:** Verify the offline page never shows PROVISIONAL results as if current, and that Forecast content from the cache is identical to what was served (Forecasts never change). Test cache growth limits (number of cached pages), updating cached pages after reconnect, a new app version replacing the cached shell without trapping users on an old version, and removal of site data. Check the hint does not overlap the phone thumb bar (r011) and does not use animation. Test Android Chrome, iOS Safari, desktop Chrome/Edge, Firefox (no install prompt). Check the day counter cannot be triggered by one visit across midnight only. Lighthouse installability check passes.
**Open Questions / Assumptions:**
- Which pages count as "last visited" and how many are kept (assumption: the last 10)?
- The lab hint copy mentions a streak and cheering that do not exist here; the copy above is an assumption and needs approval.
- Analytics: self-hosted Umami counts visits; is an install event to be counted? Assumption: no.
- Lab displayed the hint with a slide-up animation; the real app uses no entrance motion.

---

### r011 – Daumenzone: app-style thumb bar on phones
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r011--daumenzone-app-style-thumb-bar-on-phones--quality-check-after-r010) · [Snapshot](../snapshots/r011/) · Triage: keep

**Title:** Bottom navigation bar for the main areas on phones
**Story:** As a fan using one hand on a phone, I want the main areas within thumb reach, so that I can switch between them without scrolling to the top.
**Acceptance Criteria:**
- Given a viewport narrower than 560 px, When any page is displayed, Then a bar fixed at the bottom shows text labels for the main areas "Spieltag", "Tabelle", "KI-Bilanz" and "Vereine".
- Given the bar is displayed, When I tap an entry, Then I land on that area's page and the entry is marked as current (with an `aria-current` state and a visible, non-colour-only mark).
- Given a viewport of 560 px or wider, When any page is displayed, Then no bottom bar is rendered and the top navigation is unchanged.
- Given the bar is displayed, When I scroll to the end of any page, Then the last content line is fully visible above the bar and no content is covered.
- Given the bar is displayed, When I measure each entry, Then its touch target is at least 44 × 44 px.
- Given the bar is displayed, When I look at its design, Then it is on a light surface with a hairline, without shadow, gradient or icon decoration (the dark stage remains only the top navigation and the scoreboard).
**QA Notes:** Test iPhone with home indicator (safe area) and Android gesture navigation, landscape orientation, on-screen keyboard open (bar must not jump above the keyboard), pages with their own bottom elements (install hint r009, filter bar), page zoom 200 %, and text size increase. Check that the current-area marking works on deeper pages (a match page belongs to "Spieltag", a TeamPage to "Vereine"). Check that scrolling the active entry into view in the top navigation still works. Regression: the top navigation's single swipeable row stays intact.
**Open Questions / Assumptions:**
- Which areas are "main areas" in the real navigation? The labels above are an assumption (lab used Spiele, Tabelle, Duell, Verein; r020 later used Spieltag / Saison / KI-Bilanz, which Mark likes). Align the bar with the tabs of r020.
- Does the bar replace or duplicate the competition selection (1./2. Bundesliga, CL, DFB, NL) in the top navigation? Assumption: top navigation keeps the competitions.
- Brass for the active entry "KI-Bilanz" is not used; brass is reserved for AI preview actions.

---

### r012 – Abgerechnet: settled forecasts up front
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r012--abgerechnet-settled-forecasts-up-front) · [Snapshot](../snapshots/r012/) · Triage: adapt

**Title:** Settled forecast (hit or miss) on the match detail page and as a marker in lists
**Story:** As a fan, I want to see for every finished match whether the AI forecast was right, so that I can judge the AI by its hits and misses without a separate block on the start page.
**Acceptance Criteria:**
- Given a FINAL Match that has a Forecast, When I open its match detail page (N01), Then a section "Abgerechnet" shows the real final score, the Forecast Richtwert, and the verdict "Tendenz getroffen" or "Tendenz daneben" together with "genaues Ergebnis" or "kein genaues Ergebnis" (terms of Spec 03).
- Given the same page, When I read the section, Then it names which of the three assessors (Formbewerter, Duellbewerter, Umfeldbewerter) leaned the right way, e.g. "Recht hatte: Direkte Duelle, Umfeld" or "Recht hatte: keiner".
- Given a miss, When the section is displayed, Then it has the same size and prominence as a hit and uses the existing hit/miss style (filled `--win` dot vs. empty ring).
- Given a list that shows a FINAL Match with a Forecast (Matchday list, club page, search result), When the row is shown, Then a small marker shows hit or miss, is labelled for screen readers ("Tendenz getroffen" / "Tendenz daneben") and does not rely on colour alone.
- Given the start page, When it is shown, Then it carries no "Abgerechnet" block (see N02).
- Given a Match is only PROVISIONAL or running, When the section or marker would be built, Then neither is shown; only FINAL Matches are settled.
- Given a Match without Forecast, When its page or list row is shown, Then no section and no marker appear and no empty heading is rendered.
**QA Notes:** Test FINAL, PROVISIONAL and upcoming Matches (the section appears only when the 24-hour finality passes), a Forecast with Outcome DRAW, the wordings "none of the assessors" and "all three". Check 375 px (no horizontal scroll). Verify the numbers equal the Trefferbilanz counts for the same Match. The marker in the list must be understandable without opening the page and must stay small.
**Open Questions / Assumptions:**
- Moved off the start page by Mark (31 Oct 2026); he likes the presentation itself, so the lab's look of the block is kept inside the match detail page.
- Assumption: the assessor line follows Spec 05 vocabulary; the per-assessor tally across matches (r077) remains a separate, open decision.
- Which lists carry the marker: assumption all lists showing single Matches of a finished Matchday.

---

### r013 – Form-Streifen in der Tabelle
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r013--form-streifen-in-der-tabelle--cls-fix) · [Snapshot](../snapshots/r013/) · Triage: keep

**Title:** Form of the last five results in the Standings
**Story:** As a fan, I want a form indicator per Team in the table, so that I see where each Team is heading and not only where it stands.
**Acceptance Criteria:**
- Given the Standings of a table competition, When I look at a row, Then it shows the last five FINAL results of that Team in order, oldest to newest, as stripes for TeamResult WIN (green), DRAW (grey) and LOSS (red).
- Given a stripe, When I hover, focus or long-press it, Then a description appears, e.g. "Sieg 3:1 gegen Bremen".
- Given a screen reader or a colour-blind visitor, When the stripes are read, Then each stripe also carries the text "S", "U" or "N" (visually or as accessible text) so that colour is not the only carrier of meaning.
- Given a Team with fewer than five FINAL Matches, When its row is shown, Then only the existing results are drawn and no placeholders are shown.
- Given a Team has a PROVISIONAL result, When the form is calculated, Then that result is excluded from the stripes.
- Given the page loads, When the Standings render, Then the form column is present in the first render (no content jump) and the column fits at 375 px without horizontal scroll.
**QA Notes:** Test a team with postponed or rescheduled Matches (order by played date, not Matchday number), the first Matchday (no stripes), a Matchday played in midweek, and the 2. Bundesliga table. Compare against the existing Form data used for assessors for the same Team and Matchday. Check the table "vor Spieltag N" variant: form must be as of that point. Check the compact mobile layout (short names) and tooltip availability on touch devices.
**Open Questions / Assumptions:**
- Is the form shown in the compact stream sidebar table too, or only on the full Standings page? Assumption: both if width allows, else hidden on the sidebar.
- Assumption: grey uses the existing `--draw` token and red/green are meaning here (result), which the styleguide allows.

---

### r014 – Torjäger-Trikotwand
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r014--torjäger-trikotwand) · [Snapshot](../snapshots/r014/) · Triage: adapt

**Title:** League top-scorer list
**Story:** As a fan, I want a ranked list of the league's top scorers, so that I can see who leads the goal charts without opening each TeamPage.
**Acceptance Criteria:**
- Given a league (1. or 2. Bundesliga), When I open the top-scorer list, Then I see the 10 players with the most goals in the current Season, ranked, with goals, club (name and crest) and Scorer name.
- Given a player scored own goals, When goals are counted, Then own goals are not counted for that player.
- Given a player scored penalties, When the list is displayed, Then the number of penalties is shown next to the goal count (e.g. "14 Tore, davon 4 Elfmeter").
- Given players with equal goals, When the list is displayed, Then they share the rank and are ordered alphabetically by surname.
- Given I switch between 1. Bundesliga and 2. Bundesliga, When the other league is shown, Then the list updates and the switch state is reflected in the address.
- Given no goal has been scored yet in the Season, When I open the list, Then I see "Noch kein Tor in dieser Saison." and no empty table.
**QA Notes:** The list is a plain table (no shirts, no illustrations, no sampled club colours, no glow). Check totals against the TeamPage Scorer lists of the same clubs, players who transferred mid-season (goals for two clubs: assumption which club to show), goals from provisional Matches (should be marked or excluded), identical surnames, long names at 375 px. Check numeric columns use tabular numbers. Regression: result of the source scorer calculation unchanged.
**Open Questions / Assumptions:**
- Where does the list live (Saison area, Standings sidebar, own page)? Triage says "matchday boundary"; assumption: own section reachable from the league page.
- Provisional goals: included with a mark, or excluded until FINAL? Assumption: included and marked "vorläufig".
- Is 10 the right length, or "top 10 plus ties"? Assumption: all players tied at rank 10 are shown.

---

### r015 – Suchen: command palette
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r015--suchen-command-palette) · [Snapshot](../snapshots/r015/) · Triage: keep

**Title:** Search for clubs and matches with a keyboard shortcut
**Story:** As a returning fan, I want to type a club name and jump straight to its page or match, so that I reach my answer in a few keystrokes.
**Acceptance Criteria:**
- Given any page on a device with a keyboard, When I press `/` outside a text field, Then a search field opens focused; `Esc` closes it and returns focus to where I was.
- Given the search field is open, When I type "koln" or "köln", Then clubs and matches with "Köln" are listed, matching ignores accents and capitalisation, and multi-word input (e.g. "bremen koln") narrows the result.
- Given results are listed, When I use the arrow keys and Enter (or click/tap), Then I land on the selected TeamPage or, for a match result, on the match detail page (N01), never on the AI view and never on the opponent's club page.
- Given results of both types, When they are listed, Then clubs and matches are shown in two labelled groups ("Vereine", "Begegnungen") and matches comprise the current and next Matchday of the displayed competitions.
- Given nothing matches, When I have typed at least two characters, Then I see "Nichts gefunden. Anderen Vereinsnamen oder Spieltag versuchen."
- Given a phone, When I use the search entry in the navigation, Then the same search opens without needing a keyboard shortcut.
**QA Notes:** Test input method editors and `/` while in a form field (must not trigger), umlauts and "ß", club short names and aliases ("Gladbach", "Bayern"), 1-character input, very fast typing, and large result lists (cap displayed results; assumption 8 per group). Check focus trap and screen reader announcement of the result count. Without JavaScript the entry should still lead to a usable page (assumption: a plain link to the club list). The search must not log the typed text on the server.
**Open Questions / Assumptions:**
- Should the shortcut ⌘K / Ctrl+K also open the search? Assumed yes in addition to `/`.
- Lab also searched top scorers and offered "Mein Verein" actions; both are out of scope (top-scorer list is r014; Mein Verein is r003).
- Is the search server-rendered per request or based on a preloaded list? Left to the repo; the story only requires the results to appear within 200 ms perceived after typing.

---

### r016 – Wann fallen die Tore? goals by minute
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r016--wann-fallen-die-tore-goals-by-minute) · [Snapshot](../snapshots/r016/) · Triage: keep

**Title:** Goals by minute chart per league and per club
**Story:** As a fan, I want to see in which phases of the match goals are scored, so that I can learn whether a club is strong at the start or the end.
**Acceptance Criteria:**
- Given a league's Standings area, When I open "Wann fallen die Tore?", Then I see a chart of goals in 15-minute blocks (1–15, 16–30, 31–45, 46–60, 61–75, 76–90+) for the whole league in the current Season.
- Given a TeamPage, When I open the same chart, Then it shows goals scored and goals conceded of that Team per block, with direct value labels and a legend.
- Given the chart, When I read the sentence above it, Then it states the strongest block in plain language, e.g. "Bremen trifft am häufigsten zwischen der 76. und der 90. Minute (6 von 15 Toren)."
- Given a keyboard or screen-reader user, When they reach the chart, Then the same numbers are available as an accessible table and every block is focusable with its value announced.
- Given a Team without goals, When its chart is displayed, Then "Noch kein Tor in dieser Saison." is shown instead of an empty chart.
- Given goals of PROVISIONAL Matches, When the chart is computed, Then they are not included.
**QA Notes:** Boundary minutes (15, 16, 45, 45+2, 90, 90+4, extra time in cup Matches), own goals (counted for the scoring side? must be consistent with Torjäger rules), a Team with tie between two blocks (sentence names both), very small totals (sentence avoids over-claiming; e.g. fewer than 5 goals gives "noch zu wenig Tore für eine Aussage"). Chart must be readable at 375 px, must not use a colour scheme beyond the styleguide, must work in print and with reduced motion (no animation). Verify totals equal the sum of the TeamPage Scorers.
**Open Questions / Assumptions:**
- Assumption: stoppage-time goals of the first half count to block 31–45, of the second half to 76–90+; extra time is out of scope.
- Colours: scored vs. conceded must not be green/red unless it reads as meaning; assumption: ink and `--ink-soft`.
- Minimum number of goals before a text finding is shown (assumption: 5) needs confirmation.
- The lab's "Mein Verein" dependency (r003) is replaced by TeamPage context.

---

### r018 – Wie gut ist die KI wirklich? 100-dot honesty grids
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r018--wie-gut-ist-die-ki-wirklich-100-dot-honesty-grids--api-hang-fix) · [Snapshot](../snapshots/r018/) · Triage: adapt

**Title:** Hit rate and Baseline as "x von 100" dot grids on the Trefferbilanz page
**Story:** As a fan, I want the AI's hit rate shown as "44 von 100" next to the statistical Baseline, so that I can hold the AI to a figure I understand without knowing Brier or skill scores.
**Acceptance Criteria:**
- Given the Trefferbilanz page of a league, When I open it, Then two grids of 100 dots appear: "KI-Vorschau" and "Statistik-Basisprognose", each filled to the rounded HitRate (e.g. "44 von 100" vs. "56 von 100") with the same figure written as text.
- Given the AI is behind the Baseline, When the grids are shown, Then the grids and text state this without softening, in the same visual weight as when the AI is ahead.
- Given the confidence bands, When I read the calibration part, Then for each band a grid and a sentence state the hit rate in plain words, e.g. "Sicherheitsgrad unter 50 %: trifft 44 von 100".
- Given a confidence band with fewer settled Forecasts than the existing `minimumSampleSize` (Spec 03), When it is displayed, Then no grid is drawn and the text says "Zu wenige Fälle für eine Aussage (n = 12)".
- Given the grids, When read by a screen reader, Then the text sentence is read and the individual dots are hidden from the accessibility tree.
- Given I switch between 1. and 2. Bundesliga, When the page updates, Then the grids reflect only FINAL Matches of that league, and Backtest results are shown separately and labelled.
**QA Notes:** Rounding: 43,5 % and 0,4 % boundaries, 0 and 100 values, tie between AI and Baseline. Check that numbers match the existing Trefferbilanz table (HitRate, skillScore, `AccuracyReport.Comparison`) on the same page. Dots are a filled/empty pair in ink only (no brass, no green/red unless hit/miss meaning), in a layout of 10 × 10 that holds at 375 px. Check an empty season start ("Noch keine abgerechneten Begegnungen."). Print and zoom checks.
**Open Questions / Assumptions:**
- Page is the existing Trefferbilanz page; grids supplement and don't replace the Brier/skill figures. Assumption: skill score stays visible with brass as per styleguide.
- "Too few cases" reuses the repo's existing `minimumSampleSize` (Spec 03, same threshold as for the hit rate); no new number is introduced. Its value is part of the open "thresholds" decision.
- Lab's league switch and original-source link are replaced by the existing page controls.

---

### r019 – Saisonrennen: bump chart of the whole season
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r019--saisonrennen-bump-chart-of-the-whole-season) · [Snapshot](../snapshots/r019/) · Triage: keep

**Title:** Bump chart of all clubs' table positions across the season
**Story:** As a fan, I want to see how every club's table position developed Matchday by Matchday, so that I understand momentum and not just today's snapshot.
**Acceptance Criteria:**
- Given a table league with at least two played Matchdays, When I open "Saisonrennen", Then one line per Team shows its StandingPosition after each Matchday, with position 1 at the top.
- Given the chart, When it is first shown, Then all lines are neutral (`--ink-mute`) and the end of each line carries the Team's short name.
- Given I hover, tap or focus a Team's line or label, When it is activated, Then that line is brought forward in `--ink` with a thicker stroke and the other lines stay neutral; the Team's name and position per Matchday are shown.
- Given a keyboard or screen-reader user, When they reach the chart, Then the positions are available as a table ("Position nach Spieltag") and every Team can be focused.
- Given only one Matchday has been played, When I open the page, Then the chart is replaced by "Verlauf ab dem 2. Spieltag." and no empty chart is drawn.
- Given a Matchday contains PROVISIONAL results, When the last point is computed, Then the last point is drawn dashed and labelled "vorläufig".
**QA Notes:** Positions recomputed per Matchday from results (points, goal difference, goals); compare with "Tabelle vor Spieltag N" for several Matchdays. Test postponed Matches (positions with unequal games played), ties on all criteria, 18 labels without overlap at 375 px (assumption: horizontal scroll inside the chart is acceptable, page scroll is not), 2. Bundesliga, and a Team's TeamPage Saisonverlauf for consistency. Check without hover (touch only) and reduced motion. Not applicable to knockout competitions.
**Open Questions / Assumptions:**
- Lab highlighted "Mein Verein" in crest colour; the real app uses one ink highlight (no club colours). Should a TeamPage preselect its own Team? Assumption: yes.
- Where does it live: Saison area or Standings page? Assumed Standings area.
- Do existing Saisonverlauf charts on TeamPage already reuse the same data? Assumed yes; the repo decides.

---

### r020 – Three areas: Spieltag / Saison / KI-Bilanz
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r020--three-areas-spieltag--saison--ki-bilanz) · [Snapshot](../snapshots/r020/) · Triage: adapt

**Title:** Main areas as tabs: Spieltag, Saison and KI-Bilanz
**Story:** As a fan, I want the site split into a few clearly named areas, so that the matchday stays first and fast while the deeper material is one click away.
**Acceptance Criteria:**
- Given any page of the main areas, When I look at the navigation, Then three tabs "Spieltag", "Saison" and "KI-Bilanz" are shown and the current one is marked with `aria-current` and a visible mark that does not rely on colour alone.
- Given the tab "Spieltag", When I open it, Then it shows the gallery (r054) and the matchday list directly below, then the table; it carries no AI explanation block and no "Abgerechnet" block (N02).
- Given the tab "Saison", When I open it, Then it holds all season views (e.g. Saisonrennen, Wann fallen die Tore?, Torjäger, Restprogramm), newest data first.
- Given the tab "KI-Bilanz", When I open it, Then it holds the Trefferbilanz (r018) first, then settled forecasts and the explanation of how the forecast comes about.
- Given a tab, When I copy its address or use the browser's back button, Then each tab has its own stable address and back/forward move between tabs.
- Given a link to a match, When I click it in any tab, Then the match detail page opens (N01), not an overlay on the tab.
- Given a phone viewport, When the bottom bar of r011 is shown, Then its entries correspond to these areas.
**QA Notes:** Check deep links and the previous bookmarks after the split, the active-tab marking on deeper pages (a match page belongs to "Spieltag", a TeamPage to its own area), keyboard operation of the tabs, and that the active tab underline uses ink, not brass or amber. No section may be reachable in two places with different content.
**Open Questions / Assumptions:**
- Decision (Mark, 31 Oct 2026): he likes the tabs Spieltag/Saison and KI-Bilanz. Where "Vereine" and "Tabelle" belong (own tab or inside Spieltag) is open; assumption: Tabelle inside Spieltag, clubs reached via search and tables.
- The lab navigation was a client-side hash route; the real site keeps server-rendered pages.

---


### r022 – Punktestrahl: the table as a number line
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r022--punktestrahl-the-table-as-a-number-line) · [Snapshot](../snapshots/r022/) · Triage: keep

**Title:** Standings as a points axis showing the gaps between clubs
**Story:** As a fan, I want the table drawn on a points axis, so that I see how far apart clubs are, not only their order.
**Acceptance Criteria:**
- Given a table league with at least one played Matchday, When I open "Punktestrahl", Then every Team is placed by its crest at its points value on one axis, and Teams with equal points are stacked vertically at the same position.
- Given the axis, When it is displayed, Then lines mark the zone boundaries of the league with labels (1. Bundesliga: Platz 4 "Champions League", Platz 6 "Europa", Platz 15 "Relegation"; 2. Bundesliga: Platz 2, 3 and 15 with their meanings), using the Tabellenzonen styling (solid for direct, muted for play-off).
- Given the axis, When I read the sentence above it, Then it states the number of Teams within three points of the top-zone line, e.g. "Sechs Mannschaften liegen höchstens drei Punkte hinter Platz 4."
- Given I hover, focus or tap a crest, When it is activated, Then name, points and position appear, and the same data is available as a table for screen readers.
- Given all Teams have the same points (start of the Season), When the page is displayed, Then the axis shows one stack and the sentence says "Noch keine Abstände."
- Given a PROVISIONAL result is included, When the axis is shown, Then it is based on FINAL results only and says "Stand ohne vorläufige Ergebnisse".
**QA Notes:** Large stacks (9 Teams equal) must stay readable at 375 px, label collisions between close zone lines (staggered labels), points deductions or equal points with different Matches played, the 2. Bundesliga zones, CL/Nations League not applicable. Check that zone lines are not amber/brass (reserved) and that green/red only appear as zone meaning. Consistency with the Standings page on the same Matchday. Reduced motion and print.
**Open Questions / Assumptions:**
- Zone thresholds should come from the existing Tabellenzonen configuration, not be re-entered here; the numbers above are lab values and must be confirmed per Season (e.g. play-off places).
- Whether provisional results should be excluded (assumed here) or included and marked, as the Standings table does.
- Lab used a ring in the club colour for "Mein Verein"; dropped.

---

### r024 – Vereinsseiten: every club gets its own page
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r024--vereinsseiten-every-club-gets-its-own-page) · [Snapshot](../snapshots/r024/) · Triage: adapt

**Title:** Compact club page with tabs, next match and scorers on top
**Story:** As a fan of a club, I want a club page that is compact and shows the next match and the scorers first, so that I find the most relevant data without scrolling.
**Acceptance Criteria:**
- Given the existing TeamPage of a club, When I open it, Then the head shows crest, league, place, points, goals and form stripes, and the button "Als Mein Verein wählen" (r003) or "Auswahl entfernen".
- Given the page, When it is shown, Then the order is: "Als Nächstes" (next match, or the last one if the season is over), then the scorers, then the rest in tabs.
- Given the tabs, When I switch between them, Then each holds one topic (assumed: "Überblick", "Saison", "Spiele") and the selected tab is marked without relying on colour alone.
- Given a list of matches on the page, When it is shown, Then the newest data is at the top.
- Given a result or the "Als Nächstes" card, When I click it, Then the match detail page opens (N01), never the opponent's club page.
- Given a club chosen as Mein Verein, When its page is shown, Then no club-specific colour is used anywhere on the page.
- Given the club page, When the page title is shown, Then it is the club name.
**QA Notes:** Check a club without matches yet and one at season end ("Keine weiteren Spiele in dieser Saison."), phone width (tabs do not overflow), and that the page keeps the compact height compared with the current one. Verify the season wall (r064), Vereinskalender (r088), Vereinskarte (r090) and the "Was braucht mein Verein?" view (r039) have a tab or place on this page.
**Open Questions / Assumptions:**
- The club pages exist in the real product; this story changes their order and structure only (Mark, 31 Oct 2026: tabs, compact, "Als Nächstes" and scorers further up).
- Tab names and the grouping of the club stories are assumptions for product to confirm.
- The lab's crest-colour page is dropped (green/red only as meaning, brass for AI).

---


### r026 – Vereinsvergleich: two clubs side by side
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r026--vereinsvergleich-two-clubs-side-by-side) · [Snapshot](../snapshots/r026/) · Triage: keep

**Title:** Compare two clubs of the same league
**Story:** As a fan, I want to compare two clubs side by side with real numbers, so that I can settle the "who is really better" argument and send the result to a friend.
**Acceptance Criteria:**
- Given a TeamPage, When I choose "Vergleichen mit" and pick another club of the same league, Then a comparison page opens at a shareable URL that contains both clubs.
- Given the comparison, When it is displayed, Then mirrored bars show Punkte, Siege, Tore, Gegentore and Tordifferenz for both clubs, and the better value of each pair (the lower one for Gegentore) is set in full ink weight.
- Given the comparison, When it is displayed, Then both clubs' StandingPosition curves over the Matchdays are shown in one chart with labels at the line ends.
- Given the comparison, When it is displayed, Then this Season's HeadToHead Matches of the two clubs are listed with date, score and status; if none have been played, "In dieser Saison noch nicht gegeneinander gespielt." is shown.
- Given the picker, When I open it, Then only clubs of the same league (without the club itself) are offered.
- Given I open the shared URL in another browser, When it loads, Then the same comparison appears without login or stored state.
**QA Notes:** Equal values (neither bar emphasised), zero values (no bar, number still shown), clubs in different leagues via a manipulated URL (page shows an explanation and a link to the TeamPage), club order swapped in the URL (mirrors, values unchanged), PROVISIONAL results (excluded from totals or marked). Colours: both clubs are in neutral ink variants distinguished by line style, no crest colours; verify legibility on 375 px with long names. Check consistency with Standings and the existing Balance on the TeamPage.
**Open Questions / Assumptions:**
- Does the comparison also include previous seasons? Assumption: current Season only.
- Is a dashed line vs. solid line enough to tell the clubs apart for colour-blind readers? Assumption: yes, plus end labels.
- Does the comparison offer a link into the all-time HeadToHead page if it exists?

---

### r027 – Kalender: the fixture list with a reminder
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r027--kalender-the-fixture-list-with-a-reminder) · [Snapshot](../snapshots/r027/) · Triage: keep

**Title:** Download a club's fixtures as an .ics calendar file
**Story:** As a fan, I want to add my club's remaining fixtures to my calendar with a reminder, so that I do not miss a kickoff.
**Acceptance Criteria:**
- Given a TeamPage, When I choose "Spielplan in den Kalender", Then a single .ics file with one event per remaining Match of this Season for that Team is downloaded.
- Given an event, When I open it in a calendar app, Then it shows the title "Bremen – Köln", the kickoff in the correct local time of the venue's time zone (Europe/Berlin), a duration of 2 hours, and a link back to the match page.
- Given an event, When it is imported, Then it has an alert one hour before kickoff with the text "In einer Stunde: Bremen – Köln".
- Given an upcoming Match page, When I choose "In den Kalender", Then an .ics file with that single Match is downloaded with the same properties.
- Given a Team without remaining Matches (end of Season), When I open the TeamPage, Then the button is replaced by "Keine weiteren Spiele in dieser Saison."
- Given I use the feature, When I check network traffic and the account situation, Then no sign-in and no permission prompt is needed and nothing about me is stored on the server.
**QA Notes:** Import into Apple Calendar, Google Calendar and Outlook; kickoff times in summer/winter time change; Matches without a fixed kickoff time (to be determined: all-day vs. omitted, see question); postponed Matches; umlauts in titles; line folding for long titles. Re-download after kickoff changes should update, not duplicate events (stable event identifiers). iOS Safari opens the file in the calendar sheet. Check the button is a neutral action style (not brass).
**Open Questions / Assumptions:**
- Kickoff changes after import cannot be pushed to a downloaded file; should a subscription calendar be considered later? Assumption: no, one-time download.
- Whether Matches without a kickoff time are included as all-day events or left out is undecided.
- Lab wording "Schon eingeschätzt?" in the alert is dropped; own calls do not exist (decision, Mark, 31 Oct 2026).
- Which competitions are included for a club (Bundesliga only, or also DFB-Pokal / CL)? Assumption: league Matches only.


## Batch 2 (r028–r072)

### r028 – Spieltagsbogen: the matchday on paper
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r028--spieltagsbogen-the-matchday-on-paper) · [Snapshot](../snapshots/r028/) · Triage: adapt

**Title:** Printable matchday sheet (plain, without guessing columns)
**Story:** As a Bundesliga fan, I want to print a clean A4 sheet of the current matchday, so that I can follow the games on paper at the table or pin it up, without screen or app.
**Acceptance Criteria:**
- Given a matchday page with fixtures, When I choose "Bogen drucken" (or use the browser's print function), Then the print preview shows only the matchday sheet (no top navigation, filters or buttons) with the league, matchday number and date range as heading.
- Given the print preview of a matchday with N fixtures, When I read the sheet, Then each fixture appears exactly once with kickoff date/time, home team, away team and, for finished matches, the final score.
- Given a matchday containing at least one match with a provisional result (`ResultStatus.PROVISIONAL`), When I print, Then that score is shown but marked "vorläufig" in print as well (marking must not rely on colour).
- Given a fixture for which a Forecast exists, When I print, Then the sheet may show its tendency in one column labelled "KI-Vorschau" together with the line "eine Einschätzung, kein Rat"; fixtures without a Forecast show "–".
- Given the printed sheet, When I look for blank columns, name fields, points or scoring rules, Then none are present (no guessing columns).
- Given a matchday of 9 fixtures (1. Bundesliga), When I print on A4 portrait, Then the sheet fits one page in black and white without cut-off text or table columns.
- Given the printed sheet, When I search it for odds or the word "Tipp", Then none are present.
**QA Notes:** Test 1. and 2. Bundesliga (9 vs 9 fixtures), matchdays with rescheduled/postponed games (no kickoff time) and with no results yet. Print in Chrome, Firefox, Safari; also "Save as PDF". Check grayscale printing: the KI-Vorschau column and the provisional marker must still be distinguishable. Regression: print CSS must not change the screen layout; the dark stage (nav/scoreboard) must not print as black blocks.
**Open Questions / Assumptions:** (1) Decision (Mark, 31 Oct 2026): "Bogen drucken" is a good idea, the guessing columns and point system of the lab are dropped. Confirmed by Mark: the plain sheet stays. (2) Should the sheet include other competitions (CL, DFB-Pokal) or only the two Bundesligen? Assumption: only the league currently shown. (3) Visible button and print stylesheet for Ctrl+P both? Assumption: both.

---

### r034 – Wetter am Spielort
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r034--wetter-am-spielort) · [Snapshot](../snapshots/r034/) · Triage: adapt

**Title:** Weather forecast at the ground
**Story:** As a fan planning to attend or follow a match, I want to see the weather forecast at the home ground, so that I know whether to expect rain, snow or strong wind.
**Acceptance Criteria:**
- Given an upcoming match within the forecast range of the source (about 10 days ahead) whose home ground is known, When I open the match detail page (N01), Then a line shows temperature, sky condition and, if rain probability is at least 30 %, the rain probability, and, if wind is at least 30 km/h, the wind speed, all for the time of half-time (kickoff + 45 min).
- Given the matchday page with upcoming matches in range, When it loads, Then each such fixture shows a short weather value (temperature and condition in text) without shifting the layout when it appears.
- Given the weather is requested, When a match is displayed repeatedly, Then at most one request per match is made to the source and the answer is cached and reused for all visitors.
- Given a home team, When the ground is looked up, Then the coordinates come from the stadium reference file `handoff/data/stadiums.jsonl`, keyed by OpenLigaDB `teamId`; a team without an entry shows no weather and no error.
- Given a match beyond the range of the source, finished, or currently live, When I open its page, Then no weather is shown.
- Given the weather source is unreachable or slower than 6 seconds, When I open a match page, Then the page renders completely without the weather line and shows no error message.
- Given the weather is displayed, When I look at the weather line or the page footer, Then the source is named as "Wetterdaten: Deutscher Wetterdienst (DWD) via Bright Sky".
- Given a match page, When I look at the weather, Then no colour other than the neutral text colours and no icon is used.
**QA Notes:** Test the edge of the forecast range, rain 29 % vs 30 %, wind 29 vs 30 km/h, temperatures below 0. Test with slow/blocked network and with a team missing in the stadium file. Verify the request is made server-side, once per match, and the visitor's IP never reaches the weather source. Check a relegated/promoted team (stadiums.jsonl lists the 36 teams of season 2026; later seasons need an update). Mobile: the kickoff row must not wrap awkwardly. Weather must not block the page render.
**Open Questions / Assumptions:** (1) Decision (Mark, 31 Oct 2026): weather is wanted. Proposed free source: Bright Sky (DWD open data, no API key, about 10 days ahead). This is a second data source beside OpenLigaDB (PRODUCT.md says real data only) and needs sign-off for that source. (2) Who maintains `stadiums.jsonl` for new seasons (shared with r060)? (3) Attribution wording of the DWD licence needs checking. (4) Rate limits of Bright Sky: caching per match is assumed to keep requests far below them.

---

### r035 – Bisherige Duelle in the match sheet
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r035--bisherige-duelle-in-the-match-sheet) · [Snapshot](../snapshots/r035/) · Triage: keep

**Title:** All-time head-to-head on the match page
**Story:** As a fan reading a match page, I want to see the complete shared history of both teams, so that I understand the rivalry beyond the last few meetings.
**Acceptance Criteria:**
- Given a match page of two teams that have met before in the data source, When the page loads, Then a "Bisherige Duelle" block shows the total number of meetings, wins for the home team, draws and wins for the away team, as counts and as a proportional bar.
- Given the same block, When I read it, Then the last five meetings are listed with month/year, which team was at home, and the final score, newest first.
- Given the same block, When I read it, Then the biggest win in the shared history is shown with score and year.
- Given the block, When I read its heading, Then it states the earliest season covered ("in OpenLigaDB seit …") so it is not mistaken for the complete record.
- Given two teams that have never met in the data, When I open their match page, Then the block says what is missing ("Noch keine gemeinsamen Spiele in den Daten.") instead of empty counts.
- Given the head-to-head contains a meeting with a provisional result, When the block is shown, Then that meeting is not counted in the totals.
**QA Notes:** Test Bayern – Dortmund (large history), a newly promoted team (few/zero meetings), and a pairing meeting in DFB-Pokal as well as league. Verify duplicates on the same day are counted once and that test/non-competitive leagues in the data source are excluded. Check that 0 wins/draws render a bar without division errors. Mobile: list stays readable without horizontal scroll. Cross-check with the existing HeadToHead block on the match page: numbers for overlapping meetings must not contradict. Source slowness: page must still render without the block.
**Open Questions / Assumptions:** (1) Which competitions count (league only vs. league + DFB-Pokal)? Assumption as in lab: Bundesliga 1/2/3 and DFB-Pokal. (2) Is this an extension of the existing HeadToHead block or a separate block? Assumption: extension. (3) "Last 5 meetings" – only finished meetings, in any competition.

---

### r038 – Sofa-Modus: the matchday as a TV board
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r038--sofa-modus-the-matchday-as-a-tv-board) · [Snapshot](../snapshots/r038/) · Triage: adapt

**Title:** Sofa-Modus: full-screen matchday board
**Story:** As a fan watching the matchday on a tablet or TV, I want a full-screen board of all matches that updates itself, so that I can keep the site open for two hours without touching it.
**Acceptance Criteria:**
- Given a matchday page, When I choose "Sofa-Modus", Then a full-screen board opens (fall back to a full-window layer if full screen is not available) with the brand, a summary such as "2 Spiele laufen, 12 Tore bisher" and the button "Beenden".
- Given the board, When I read it, Then there is one large tile per match with crests, names, score, status, and for finished matches the result against the KI-Vorschau (marked as forecast).
- Given running and finished matches, When the board is shown, Then running matches come first and are marked "Läuft"; finished matches follow with equal legibility.
- Given the board is open, When the live data refreshes and a score changes, Then the tile updates without reload and the change is marked on that tile with a static marker (no pulsing or flashing).
- Given the board is open, When the device would go to sleep, Then the screen is kept awake (wake lock); where this is unsupported, the board says so.
- Given the board, When I press Escape, leave full screen or choose "Beenden", Then it closes and the matchday page is as I left it.
- Given a league filter on the matchday page, When the board opens, Then it shows only that league.
- Given a provisional result, When the board shows it, Then it is marked "vorläufig".
**QA Notes:** Test on tablet and TV browser, with reduced motion, with no live matches (board states "Keine Spiele laufen"), 9 vs 18 matches (tiles fit without scrolling where possible). The board is the existing single dark stage in full size; verify it uses only the stage tokens and no amber or additional dark theme. No sound.
**Open Questions / Assumptions:**
- Decision (Mark, 31 Oct 2026): yes to Sofa-Modus; it is the full-screen form of the dark stage (scoreboard).
- Share the board layer with "Konferenz nachspielen" (r052).
- Does clicking a tile leave the board and open the match detail page? Assumption: yes, after leaving full screen.

---


### r039 – Was braucht mein Verein? Targets from 10,000 seasons
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r039--was-braucht-mein-verein-targets-from-10000-seasons) · [Snapshot](../snapshots/r039/) · Triage: adapt

**Title:** Points needed per placement goal on the team page
**Story:** As a fan of a club, I want to see how many points my club typically needs for each placement goal and how many it still lacks, so that I can judge what is realistic this season.
**Acceptance Criteria:**
- Given a team page of a club in a running league season, When it loads, Then a "Was braucht {Verein}?" block lists the placement goals of that league (1. Liga: Meisterschaft, Champions League, Europa, Klassenerhalt; 2. Liga: Aufstieg, Relegation, Klassenerhalt) each with the typical points total needed.
- Given a goal in the block, When I read it, Then it shows points still missing, the number of remaining matches, and the required points per match next to the current points per match.
- Given a goal in the block, When I read it, Then it shows how often the club reached it in the Saisonaussicht simulation as a percentage, and the text states it is a simulation.
- Given a goal that is already secured or mathematically out of reach, When I read the block, Then the goal says so in words ("bereits sicher" / "rechnerisch nicht mehr erreichbar") instead of a required rate.
- Given a goal the club has reached or currently holds, When the block is shown, Then green is used only for "current pace is enough"; red only for "out of reach"; no brass.
- Given a league with too little data for the Saisonaussicht (e.g. before the first matchday), When I open the page, Then the block is replaced by a statement that there is not enough data yet.
**QA Notes:** Test a leader, a mid-table club, a relegation candidate, and a club already relegated/champion late in the season. Boundaries: points missing = 0, negative, more than remaining × 3. Check the numbers are consistent with the existing Saisonaussicht probabilities on the same page. Mobile: cards should stack, not shrink text below 0.75 rem. Performance: simulation values must come from the existing run, not be recomputed per page view. Provisional results must not enter the simulation inputs.
**Open Questions / Assumptions:** (1) Verdict wording such as "im aktuellen Tempo drin" must be reviewed for "no betting language"; probabilities are allowed only as the existing Saisonaussicht. (2) "Typical points" = median of the simulated threshold – confirm the simulator can output the boundary points per zone. (3) Is the 1. Liga relegation playoff place a separate goal? Assumption: yes, as in zones of the table. (4) Does the Saisonaussicht page state a disclaimer ("eine Einschätzung, kein Rat") that should repeat here? Assumption: yes.

---

### r041 – Tabelle: Heim, Auswärts, Letzte 5
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r041--tabelle-heim-auswärts-letzte-5) · [Snapshot](../snapshots/r041/) · Triage: keep

**Title:** Standings split by home, away and last five matches
**Story:** As a fan, I want to switch the table between all matches, home matches, away matches and the last five matches, so that I can see who is strong at home, on the road or in form.
**Acceptance Criteria:**
- Given the table of a league with at least one finished matchday, When I look above it, Then I can choose between "Gesamt", "Heim", "Auswärts" and "Letzte 5", with "Gesamt" preselected.
- Given I choose "Heim", When the table updates, Then each team shows only points, goals and record from its home matches, ranked by points, then goal difference, then goals scored.
- Given I choose "Auswärts", When the table updates, Then the same applies to away matches only.
- Given I choose "Letzte 5", When the table updates, Then each team is counted with its five most recent finished matches (fewer if it has played fewer), and a note under the table states this.
- Given any split view, When I read the note below the table, Then it states what is counted, and that provisional results are not included.
- Given any split view, When the page is opened without JavaScript, Then "Gesamt" is still shown correctly.
**QA Notes:** Boundaries: matchday 1 (many teams with zero home or away games), fewer than five played matches, equal points/GD/goals tie-breaks (order stability). Check zone markers (Aufstieg, Abstieg) are not wrongly shown in split views if they only apply to the overall table – decide with Mark. Test the "vor Spieltag N" selection if present: the split must respect the selected matchday. Mobile: tab row must fit 360 px width. Regression: existing table view, live table (r059) and "Nach KI" views if present.
**Open Questions / Assumptions:** (1) Should zone markers (CL, relegation) be shown in split tables? Assumption: no, since zones refer to the overall table. (2) Is the choice remembered in the browser? Assumption: only for the session/not at all; no server-side storage. (3) Cup and Nations League formats have no such table; assumption: tabs only for `CompetitionFormat.TABLE` leagues.

---

### r044 – Seit deinem letzten Besuch
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r044--seit-deinem-letzten-besuch) · [Snapshot](../snapshots/r044/) · Triage: adapt

**Title:** "Seit deinem letzten Besuch" summary for returning visitors
**Story:** As a returning visitor, I want a short summary of what happened since my last visit, so that I do not have to search for new results and table changes.
**Acceptance Criteria:**
- Given I visit for the first time in this browser, When the page loads, Then no summary is shown and my visit time is remembered only in this browser.
- Given my last visit was more than 2 hours ago and at least one match has been finished since, When the page loads, Then a card "Seit dem letzten Besuch" lists these new final results with crests and the scores.
- Given I have chosen a favourite club ("Mein Verein") and its table position changed, When the card is shown, Then it states the move in words (e.g. "Bremen: von Platz 9 auf Platz 7") with no colour other than neutral.
- Given I have no favourite club, When the card is shown, Then it shows the largest table move in the league instead.
- Given a result is still provisional, When the card is built, Then it is not listed (or is clearly marked "vorläufig" and not used to compute the table comparison).
- Given I dismiss the card or my last visit was less than 2 hours ago, When I load a page, Then no card is shown, and the stored time is updated.
- Given I clear the site data in my browser, When I load the page, Then the page behaves as a first visit and no data was ever sent to the server.
**QA Notes:** Test the 2-hour boundary (1:59 vs 2:01), no new results, many new results (list cap), a different league selected, blocked localStorage/private mode (no error, no card), and clock changes (time zone/DST). Check the card does not shift the layout, does not cover the bottom navigation on mobile, and is reachable with keyboard and screen reader. Comparison data must come from final data only.
**Open Questions / Assumptions:** (1) Lab card also showed the visitor's own calls and the score against the AI. Dropped (decision, Mark, 31 Oct 2026: not a prediction game). (2) "Mein Verein" selection exists or is introduced by a separate story – assumption: it exists in the browser only. (3) "Rückblick ansehen" link target: Review of the matchday if finalised. (4) Lab used the informal "deinem"; the UI must use neutral wording ("Seit dem letzten Besuch").

---

### r045 – Restprogramm: Wer hat es leichter?
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r045--restprogramm-wer-hat-es-leichter) · [Snapshot](../snapshots/r045/) · Triage: keep

**Title:** Remaining fixtures difficulty grid
**Story:** As a fan following a title, promotion or relegation race, I want to compare the difficulty of the remaining fixtures of all clubs, so that I can see who has the easier run-in.
**Acceptance Criteria:**
- Given a league season with remaining matchdays, When I open the season view, Then a "Restprogramm" grid shows one row per club in table order and one column per each of the next six matchdays.
- Given a cell, When I read it, Then it shows the opponent (crest and short name), whether the match is at home or away, and the opponent's current table position.
- Given a cell, When I compare it with others, Then stronger opponents (better table position) are shown with a darker shade, and the shade is not the only carrier of information (position number is printed).
- Given a row, When I read next to the club, Then the average opponent position over all remaining matches and its rank ("3. schwerstes von 18") are shown.
- Given a club that has no match on a matchday (or already played it), When I read the cell, Then it says so ("spielfrei" / "gespielt").
- Given the grid on a phone, When I scroll horizontally, Then the club column stays visible and the page itself does not scroll sideways.
**QA Notes:** Test late season (fewer than six matchdays left → fewer columns), rescheduled matches (Nachholspiele) falling on another matchday, 1. vs 2. Liga (18 teams each), and ties in average difficulty (rank tie handling). Contrast of text on darkest shade must meet WCAG AA. Check that the table position used is the current one with final results only. Performance: 18×6 cells with crests should not cause layout shift.
**Open Questions / Assumptions:** (1) Lab used a green ramp; since green is reserved for meaning, the shades must be a neutral ink/grey ramp (assumption). (2) "Difficulty" = current table position of the opponent, nothing more; no forecast or odds. (3) Highlight of the favourite club is allowed if "Mein Verein" exists.

---

### r052 – Konferenz nachspielen
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r052--konferenz-nachspielen) · [Snapshot](../snapshots/r052/) · Triage: keep

**Title:** Replay the last matchday's goals in clock order
**Story:** As a fan, I want to replay the last complete matchday as a conference in which the goals fall in the order they happened, so that I can relive the late turnarounds instead of reading nine final scores.
**Acceptance Criteria:**
- Given a league with at least one complete, FINAL matchday, When I choose "Konferenz nachspielen", Then a full-screen board (the dark stage, as in r038) opens for the last complete matchday of the league shown.
- Given the replay runs, When the clock advances, Then it runs from 0' to 90'+ in about one minute with a short pause at half-time, and each real goal from the match data appears at its minute: the tile's score increases and the line below names scorer and minute (e.g. "94. Romulo", "Elfmeter", "Eigentor").
- Given a goal appears, When the ticker line is updated, Then it calls it in plain words, e.g. "Tor für Leipzig! Leipzig 3:0 Elversberg – C. Baumgartner (84.)".
- Given the replay reaches the end, When the final whistle is shown, Then it states "Abpfiff. 25 Tore an diesem Spieltag." with the real number and the final scores equal the real results.
- Given the replay, When I use "Pause", "Weiter" or "Nochmal", Then it pauses, continues or restarts from 0'.
- Given a user with reduced motion, When the replay is started, Then the clock does not run by itself; each goal is stepped with an explicit control.
- Given the replay, When I look at it, Then it contains no AI forecast, no sound and only data of FINAL matches.
**QA Notes:** Test matchdays with goals in stoppage time, own goals, penalties, matches without goals, rescheduled matches (the "last complete matchday" excludes incomplete ones), both Bundesligen, and "Beenden"/Escape. Check the goal list matches the match pages. Flashing is replaced by a static marker for the tile with a new goal.
**Open Questions / Assumptions:**
- Decision (Mark, 31 Oct 2026): keep, "sehr gut".
- Replay speed (650 ms per match minute in the lab) is assumed.
- Whether it appears on the Matchday page only or also on the start page: assumption Matchday page.
- Sound of the lab is dropped.

---


### r053 – Spielverlauf als Führungslinie
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r053--spielverlauf-als-führungslinie) · [Snapshot](../snapshots/r053/) · Triage: keep

**Title:** Lead line showing the course of each match
**Story:** As a fan, I want a small lead line under each match result, so that I can see how a match unfolded without opening it.
**Acceptance Criteria:**
- Given a finished match with goals in the data, When I view the fixture list, Then a lead line from minute 0 to 90 (+ stoppage time) with a half-time marker is drawn under the score.
- Given the line, When a goal is scored, Then the line steps up for each home goal and down for each away goal, and an own goal counts for the team that benefits.
- Given I hover or focus a goal marker, When the tooltip appears, Then it names the scorer (or "Eigentor"/"Elfmeter") and the minute; the same information is available for touch and keyboard users.
- Given a running match, When the list refreshes, Then the line is drawn only up to the current minute and the match is marked live per the existing live marker.
- Given a match without goals (0:0), When the list is shown, Then a flat line is shown; given a match without goal data, no line is shown and no error.
- Given a legend sentence above the list, When I read it, Then it explains how to read the line in one sentence.
**QA Notes:** Test lead beyond two goals (line is clamped; verify scorer tooltip still correct), stoppage-time goals (90+), goals in extra time (cup), a late reversal (3:1 → 3:3), own goals by either team, and goals missing minute data. Verify consistency: final line height must equal the final score difference (within clamp). Mobile: line width must not compress rows; goals tooltips must work by tap. Provisional matches: line shown but the score keeps the provisional marking. Accessibility: a text alternative per line.
**Open Questions / Assumptions:** (1) Lab used amber dots; brass is reserved for the AI, so use neutral ink. (2) Does the goal data (OpenLigaDB) give minute for every goal? Assumption yes. (3) Scope: league fixtures only, not Nations League/cup lists.

---

### r054 – Stadion-Karussell
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r054--stadion-karussell) · [Snapshot](../snapshots/r054/) · Triage: adapt

**Title:** Gallery on top of the start page
**Story:** As a visitor, I want the start page to open with a gallery that begins with the most important match, so that I see the top match or my club's match first and can page through the rest.
**Acceptance Criteria:**
- Given the start page, When it loads, Then a gallery of the matches of the current matchday is shown at the top, in kickoff order, starting with the "Mein Verein" match if a club is chosen, otherwise with the top match.
- Given the gallery, When I read a slide, Then it shows crests, names, status or countdown, the score if started, and the KI-Vorschau as a marked line that links to the match detail page.
- Given the gallery, When I use the ‹ › controls, the arrow keys while it is focused, or a swipe on a touch screen, Then it moves to the previous or next match and shows the position ("Spiel 9 von 18").
- Given a slide, When I click it, Then the match detail page opens (N01).
- Given the gallery, When it is shown, Then the matchday list follows directly below it.
- Given live matches, When the live data refreshes, Then the slides update their score without reload and without moving the gallery position.
- Given a user with reduced motion, When the gallery changes slide, Then there is no sliding animation.
- Given the gallery, When I look at it, Then it carries no question to answer, no own call and no club colours.
**QA Notes:** Test a matchday with one match, 9 and 18 matches, a "Mein Verein" without a match this matchday (falls back to the top match), a window of midweek rounds, keyboard operation and screen reader announcement of the position. Check no layout shift when slides change and that the list below starts without a large gap on a 375 px phone.
**Open Questions / Assumptions:**
- Decision (Mark, 31 Oct 2026): keep the gallery on top with the top match or the Mein-Verein match first; the lab's pitch drawing is replaced by plain match cards (styleguide: no illustrations).
- Definition of "top match": assumption the open match with the highest Spannung (r073), otherwise the first by kickoff.
- Which gallery position the visitor sees after a reload: assumption always the first.

---


### r057 – Live im Tab und Tor-Alarm
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r057--live-im-tab-und-tor-alarm) · [Snapshot](../snapshots/r057/) · Triage: adapt

**Title:** Live score in the tab title
**Story:** As a fan with the page open in a background tab, I want the tab to show the live score, so that I can follow the match while doing something else.
**Acceptance Criteria:**
- Given a match is running (`liveScore` available), When the page is open, Then the tab title shows the Zwischenstand and teams, e.g. "1:0 Bremen – Hoffenheim · Rasen-Radar", with the match of "Mein Verein" first if one is chosen, otherwise the first live match.
- Given a match is running without a liveScore yet, When the tab title is shown, Then it says "läuft" instead of a score.
- Given no match is running, When the page is open, Then the normal page title is shown.
- Given the title shows a score, When I read it, Then it is marked as live and provisional ("vorläufig"), never as a final result.
- Given the title changes after a goal, When it updates, Then it does so without flashing or blinking.
- Given the feature, When I look for notifications, permission prompts or a setting for goal alerts, Then none exist.
**QA Notes:** Test a goal cancelled by VAR (title corrects), several clubs live, a tab in the background for hours (polling interval, battery), and switching "Mein Verein" while live. Check the favicon stays unchanged. Verify no permission request is ever made.
**Open Questions / Assumptions:** (1) Decision (Mark, 31 Oct 2026): no goal notifications; they would need realtime data plus push, which is not feasible. (2) The title follows the existing live refresh (about one minute); the delay is acceptable. (3) Depends on a "Mein Verein" browser setting only for the order, not for the feature itself.

---

### r058 – Wer kann noch?
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r058--wer-kann-noch) · [Snapshot](../snapshots/r058/) · Triage: keep

**Title:** "Wer kann noch?" – points today, maximum possible and pace
**Story:** As a fan, I want to see for every club how many points it has, how many it can still reach at most, and where it stands at its current pace, so that I can tell what is still open in the season.
**Acceptance Criteria:**
- Given a league season in progress, When I open the season view, Then every club has one row with its current points, the maximum reachable points (current + 3 × open matches), and a value "bei gleichem Tempo" (points per match × all matches).
- Given the view, When I read the markers, Then reference lines show the points of the leader and of the relevant thresholds (1. Liga: Champions League, Europa, Klassenerhalt; 2. Liga: Aufstieg, Relegation, Klassenerhalt).
- Given a club whose maximum is below the current points of the leader, When I read the view, Then it is marked as "kann Platz 1 nicht mehr erreichen" in text, not only through lighter styling.
- Given the intro sentence, When I read it, Then it is computed from the data (remaining matchdays, maximum points, leader) and says "eine Rechnung, keine Vorhersage".
- Given a phone, When I open the view, Then rows stack the numbers below the club name and nothing is cut off horizontally.
- Given a run with provisional results, When the view is built, Then only final results are counted.
**QA Notes:** Test matchday 1 (pace for 1 match), leader ahead of the field mathematically (title decided), clubs with fewer matches played (postponed games), points deductions if the data source contains them, 1. vs 2. Liga thresholds. Check every number against the table by hand for two clubs. Verify there is no overlap or confusion with the Saisonaussicht probabilities (the pace value is not a forecast). Accessibility: a data table alternative to the bar graphic.
**Open Questions / Assumptions:** (1) Green/red must not be used decoratively on the bars; use ink tones (assumption). (2) The "Hochrechnung" diamond was amber in the lab; brass is reserved for the AI preview – use a neutral marker. (3) Include the point thresholds as the current table's zones or as simulated ones (r039)? Assumption: from the current table.

---

### r059 – Live-Tabelle
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r059--live-tabelle) · [Snapshot](../snapshots/r059/) · Triage: keep

**Title:** Live table "wenn es so bleibt"
**Story:** As a fan watching a matchday, I want a table that counts running matches at their current score, so that I can see where clubs would stand if the scores stayed as they are.
**Acceptance Criteria:**
- Given at least one match of the league is running with a `liveScore`, When I open the table, Then a "Live" view is offered and preselected, headed "Wenn es so bleibt" and clearly labelled "vorläufig – nicht die offizielle Tabelle".
- Given the Live view, When I read a row of a club that is playing, Then it shows the live score and opponent in text, with green only if winning, red only if losing, and neutral if level.
- Given the Live view, When I compare each row with the table before kickoff, Then the movement of each club is shown as a number of places up or down.
- Given a running match without a `liveScore`, When the Live view is shown, Then that match counts as 0:0 and is marked as "läuft, Stand unbekannt".
- Given no match of the league is running, When I open the table, Then the Live view is not offered and the normal view is shown.
- Given a match whose result is already in the official table of the data source, When the Live view is built, Then it is not counted twice.
- Given the Live view, When I leave the page, Then no live (provisional) values enter Review, HitRate or any stored statistic.
**QA Notes:** Test several matches at once, a goal taking the lead and equalising within one refresh (table updates silently), a match ending while the view is open (falls back to the normal view), hour with matches in both leagues, and Saturday conference with nine matches. Check ties (points, goal difference, goals) are sorted as in the normal table. Verify the polling stops when nothing runs. Mobile: row chips do not truncate the club name. Respect `prefers-reduced-motion` for row movement.
**Open Questions / Assumptions:** (1) Use the existing live marker (red outline) for the chips; do not add new colours. (2) Is the view remembered (browser only)? Assumption: only per session. (3) Whether the Live view also appears in the sidebar table of the landing page: assumption yes.

---

### r060 – Auswärtsfahrten und Derbys
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r060--auswärtsfahrten-und-derbys) · [Snapshot](../snapshots/r060/) · Triage: adapt

**Title:** Away trip distances and derbies
**Story:** As a fan, I want to see how far clubs travel to their away matches and which matches are derbies, so that I can understand the burden of a season and spot the local rivalries.
**Acceptance Criteria:**
- Given a league season, When I open the season view, Then an "Auswärtsfahrten" block ranks all clubs by the total straight-line distance (in km, rounded) to all their away grounds this season, split into "schon gefahren" and "noch zu fahren".
- Given the block, When I read it, Then the longest single trip per club is named with destination and km.
- Given the league, When I read "Derbys", Then every pairing whose grounds are at most 60 km apart (assumed threshold) is listed with the distance, both teams and both meetings (result or date).
- Given a match detail page (N01) of a trip longer than the threshold or of a derby, When I read the page, Then one line states "Anreise für {Gast}: {km} km Luftlinie nach {Ort}" or "Derby: {km} km zwischen den Stadien."
- Given the coordinates, When distances are computed, Then they come from `handoff/data/stadiums.jsonl` (keyed by OpenLigaDB `teamId`, with stadium, city, lat, lon).
- Given a club that has no entry in the file, When the block is built, Then the club is listed as "Entfernung unbekannt" and does not distort the ranking or lead sentence.
- Given a phone, When I open the view, Then the layout stays within the page width.
**QA Notes:** Validate distances for three known pairs against an independent source (e.g. Köln–Leverkusen about 15 km), the 60 km boundary (59/60/61), clubs that changed grounds, relegation/promotion between seasons (the file covers the 36 teams of season 2026), Nachholspiele. Distances are straight-line – the wording must say "Luftlinie". Check both leagues. Totals count only planned or played away matches of the shown season.
**Open Questions / Assumptions:** (1) Decision (Mark, 31 Oct 2026): stadium coordinates are wanted and delivered in `stadiums.jsonl`; who maintains it per season is open (shared with r034). (2) The 60 km derby rule is a lab choice and part of the open "thresholds" decision; real "Derby" naming may be disputed – confirm or call it "Nahduell". (3) Is the long-distance ranking worth the page space on mobile?

---

### r063 – Torjäger-Steckbrief
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r063--torjäger-steckbrief) · [Snapshot](../snapshots/r063/) · Triage: keep

**Title:** Scorer profile from goals only
**Story:** As a fan, I want to open a scorer's profile from the scorer list, so that I can see when and how often a player scores for his club.
**Acceptance Criteria:**
- Given a scorer list entry (`Scorer`) on a team page, When I select the name, Then a profile opens with goals, matches in which he scored, and penalties ("8 Tore in 6 Spielen, davon 3 Elfmeter").
- Given the profile, When I read it, Then it shows his share of the club's goals ("53 % der Tore von Bremen, 8 von 15") and the split home:away and before:after half-time.
- Given the profile, When I read the goal list, Then every goal is a row with matchday, opponent (gegen/bei), minute, resulting score from the club's view and the final score.
- Given the profile, When I look for other statistics (assists, minutes played, cards, ratings), Then none are shown (Spec 01: no player statistics beyond goal scorers).
- Given own goals in the data, When the profile is built, Then own goals are not counted for the scorer.
- Given I close the profile via button, Escape key or backdrop, When it closes, Then focus returns to the name I selected.
**QA Notes:** Test a scorer with a single goal, one with penalties only, a player who moved clubs mid-season (goals counted per club or total – assumption below), identical names, stoppage-time goals. Check the minute strip handles goals in the same minute. Without JavaScript the link should still lead to a readable page/anchor. Provisional goals (running match) are shown but marked, and not counted in the share until final. Mobile: dialog scrolls and fits.
**Open Questions / Assumptions:** (1) Is "Spiele" (games in which he scored) the right wording? We do not know appearances. (2) Mid-season transfers: assume profile per club. (3) The "goals from the 80th minute" fact: keep or drop? Assumption: keep.

---

### r064 – Saison-Wand auf der Vereinsseite
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r064--saison-wand-auf-der-vereinsseite) · [Snapshot](../snapshots/r064/) · Triage: adapt

**Title:** "Die ganze Saison": each tile opens the match
**Story:** As a fan, I want to see the whole season of a club as one wall of matches and open any of them, so that I can look at a match I remember or one that is still to come.
**Acceptance Criteria:**
- Given a club page, When I open the season section "Die ganze Saison", Then every matchday of the season is a tile, played ones show opponent crest, score from the club's view, home/away and a win/draw/loss mark; open ones show crest and date.
- Given a tile, When I click or tap it, Then the match detail page (N01) opens; it never opens the opponent's club page.
- Given the wall, When I read the key, Then it counts the results ("3 Siege, 3 Remis, 1 Niederlage, 27 offen").
- Given the wall, When it is shown, Then the newest data is on top (latest matchday first) and the next match is marked with a hairline frame, not amber.
- Given a provisional result, When its tile is shown, Then it is marked "vorläufig" and is not counted in the key.
- Given a phone, When the wall is shown, Then the tiles reflow within the page width and every tile has a touch target of at least 44 px.
- Given a screen reader, When a tile is read, Then it says opponent, venue, score or date and the result in words, not only by colour.
**QA Notes:** Test 34 and 38 tiles (both Bundesligen differ), postponed matches, a club at season start, the opponent's name staying visible on the tile as the only way to know it. Green/red only as meaning (win/loss), sand/neutral for draw if used in the styleguide. The opponent's page remains reachable from the match detail page.
**Open Questions / Assumptions:**
- The wall already exists in the real product; this story only fixes the click target (Mark, 31 Oct 2026).
- "Newest on top" for a wall that is also a calendar: assumption latest played matchday first, then the remaining ones in date order.

---


### r066 – Tor-Ticker
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r066--tor-ticker) · [Snapshot](../snapshots/r066/) · Triage: keep

**Title:** Goal ticker across all grounds
**Story:** As a fan following a matchday, I want a feed of all goals of the matchday in time order, so that I can follow what happens everywhere at once.
**Acceptance Criteria:**
- Given a matchday with goals, When I open the matchday page, Then a "Tor-Ticker" lists every goal with the newest on top, grouped by day.
- Given an entry, When I read it, Then it shows an approximate clock time (labelled "ca."), the scoring team, scorer (or "Eigentor"/"Elfmeter"), the minute and the match with the new score.
- Given an entry, When I select it, Then the match page opens.
- Given a match is running, When the ticker refreshes, Then the new goal appears on top and the heading shows the live marker; goals from running matches are marked "vorläufig".
- Given the league filter or a selected matchday, When I change it, Then the ticker shows only goals for that selection.
- Given a matchday without goals, When I open the page, Then the ticker says "Noch kein Tor an diesem Spieltag."
**QA Notes:** Check clock approximation (kickoff + minute, plus break gap) for a late kickoff and stoppage time, simultaneous goals (order by exact time then match), goals later removed by VAR (entry disappears), matchdays with 50+ goals (internal scroll, performance), and the page's placement on phones (below the table, no layout shift). Verify scores in entries match the running score at the time of the goal. Accessibility: live updates should not interrupt screen readers aggressively.
**Open Questions / Assumptions:** (1) "Mein Verein" highlighting depends on the browser-only favourite club. (2) The clock time is an estimate – the UI must say so. (3) How far back may the ticker go (past matchdays)? Assumption: any matchday.

---

### r068 – Serien
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r068--serien) · [Snapshot](../snapshots/r068/) · Triage: keep

**Title:** Running streaks per club
**Story:** As a fan, I want to see which clubs are on a streak and which are in a slump, so that I know the current storylines of the league.
**Acceptance Criteria:**
- Given a league season, When I open the season view, Then a "Serien" block shows two groups, "Läuft" and "Hängt".
- Given the "Läuft" group, When I read it, Then it lists clubs with at least 3 wins in a row, 4 matches unbeaten, 3 matches without conceding, or 6 matches with a goal.
- Given the "Hängt" group, When I read it, Then it lists clubs with at least 3 losses in a row, 4 without a win, or 3 without a goal.
- Given a streak, When I read its card, Then it states the number and the sentence ("in allen 7 Saisonspielen ungeschlagen" when it covers the whole season) and shows the individual results.
- Given a club that has no streak above the thresholds, When I read the block, Then the club does not appear; if no club qualifies, the block says so.
- Given only final results count, When a result is provisional, Then the streak is computed without it.
**QA Notes:** Boundaries at each threshold (2/3 wins, 3/4 unbeaten, etc.), clubs with several qualifying streaks (only the strongest is shown), streak ordering ties, postponed matches (order by played date vs. matchday), early season (matchday 3), streak spanning the season start (do streaks cross seasons? assumption: no). Both leagues. Cross-check three clubs manually from the results list. Streak sentences also appear elsewhere (scoreboard text/match page) – these must agree with the block.
**Open Questions / Assumptions:** (1) The lab also placed streaks in a ticker and under the match title; here only the season block – confirm if match-page line is wanted. (2) Colour: use green/red only for "Läuft"/"Hängt" as meaning; no brass.

---

### r070 – Es geht um
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r070--es-geht-um) · [Snapshot](../snapshots/r070/) · Triage: keep

**Title:** "Es geht um" stakes line per open match
**Story:** As a fan looking at the matchday, I want one line under each open match telling what is at stake in the table, so that I know which matches matter even if my club does not play.
**Acceptance Criteria:**
- Given an open match between the first and second in the table, When I view the matchday, Then the line reads "Spitzenspiel: Erster gegen Zweiter".
- Given an open match between teams in the table's last positions, When I view the matchday, Then the line reads "Kellerduell: 15. gegen 16." with their positions.
- Given any other open match, When I view the matchday, Then the line states what a win would mean today for each side, e.g. "Ein Sieg brächte Bremen auf Platz 3", based on points and goal difference with the rest of the table unchanged.
- Given a side that would not move up with a win, When I read the line, Then that side is left out; given neither side would move, the line is not shown.
- Given a match that is running, finished, or on a past matchday, When I view it, Then no stakes line is shown.
- Given any line, When I read it, Then it contains no probabilities, forecasts or odds language.
**QA Notes:** Define thresholds in tests (what counts as "near the top": positions 1–3? bottom: 15–18?). Test equal points and goal difference ties, matchday 1 (table empty/all equal), the leader playing against a mid-table side, and both sides in the same match could climb. Check wording grammar for different positions (Platz 1, Platz 18) and the 2. Liga. Table used is the final-only table; a live match elsewhere does not change the lines. Mobile: line wraps cleanly under the fixture.
**Open Questions / Assumptions:** (1) Thresholds for "Spitzenspiel" (top 3?) and "Kellerduell" (bottom 4?) are not defined in the lab; assumption: positions 1–3 and the bottom four – confirm. (2) Win consequences for matchdays with postponed games, assumption: based on the table as it stands.

---

### r071 – Schluss nach 80
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r071--schluss-nach-80) · [Snapshot](../snapshots/r071/) · Triage: keep

**Title:** Table at half-time and after 80 minutes
**Story:** As a fan, I want to see the table as it would be if all matches had ended at half-time or after 80 minutes, so that I can see which clubs win or lose points late.
**Acceptance Criteria:**
- Given the table view options, When I look above the table, Then "Halbzeit" and "Schluss nach 80" are offered in addition to the existing views.
- Given I choose "Halbzeit", When the table updates, Then it is rebuilt from every finished match of the season with the score at half-time (`halfTimeScore`).
- Given I choose "Schluss nach 80", When the table updates, Then it is rebuilt from the score after 80 minutes computed from the goal list.
- Given either view, When I read the table, Then each row shows the places gained or lost against the real table.
- Given "Schluss nach 80", When I read the note under the table, Then it names the clubs that gained and lost the most points after minute 80, with the number of points.
- Given a match without goal minutes in the data, When the table is built, Then the match is excluded and the note says how many matches were left out.
**QA Notes:** Test goals exactly in minute 80 and 81 (boundary definition), stoppage-time goals in the first half (45+), own goals, matches with missing minutes, the first matchday, ties. Compare two clubs' totals by hand. Provisional matches are excluded. Both leagues. Mobile: tab row with 6+ options must scroll or wrap without hiding choices. Cross-check with Heim/Auswärts split (r041) for a consistent tab pattern.
**Open Questions / Assumptions:** (1) Define whether a goal in minute 80 counts (assumption: at 80:00 included, the 81st minute onwards excluded). (2) "Verspielt/geholt" wording must not read as blame on clubs; neutral phrasing to be confirmed.

---

### r072 – Kreuztabelle
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r072--kreuztabelle) · [Snapshot](../snapshots/r072/) · Triage: keep

**Title:** Cross table of all pairings
**Story:** As a fan, I want a grid of all home and away pairings of the season, so that I can see at a glance how any match between two clubs ended.
**Acceptance Criteria:**
- Given a league season, When I open the season view, Then a "Kreuztabelle" shows home teams down, away teams across, both in current table order.
- Given a played pairing, When I read its cell, Then it shows the final score, and the cell indicates home win, draw or away win by more than colour alone (e.g. text or pattern).
- Given a pairing not yet played, When I read its cell, Then it is visibly different (hatched or empty) and has an accessible label "noch offen".
- Given the diagonal (team against itself), When I read it, Then the cell is blank.
- Given the lead sentence, When I read it, Then it counts the share of home wins, draws and away wins across all played matches ("Bisher 64 Spiele: 52 % Heimsiege, 20 % Remis, 28 % Auswärtssiege").
- Given a cell, When I select it, Then the match page opens.
- Given a phone, When I scroll the grid horizontally, Then team names stay visible and the page does not scroll sideways.
**QA Notes:** Early season (mostly empty), full season (306 cells for 18 teams, 34 matchdays), rounding of percentages (sum to 100 %), provisional results (shown with "vorläufig" marker and not counted in the lead sentence), postponed games. Check column headers are readable (abbreviations with full names accessible). Keyboard navigation through cells; performance on mobile (rendering 306 cells). Use of green/red for home/away win is allowed only if read as win/loss for the home side – see question.
**Open Questions / Assumptions:** (1) Green = home win, red = away win in the lab; "red" means loss/error elsewhere. Decide whether to keep or use neutral tones (ink for home win, outlined for away win). (2) Own club row/column highlight in brass is not allowed (brass is for the AI); use an ink outline. (3) Cup and Nations League formats: no grid.


## Batch 3 (r073–r100)

### r073 – Spannendste zuerst
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r073--spannendste-zuerst) · [Snapshot](../snapshots/r073/) · Triage: keep

**Title:** Tension per open match and "Spannendste zuerst" sort on the matchday page
**Story:** As a Bundesliga fan with little time on a busy matchday, I want to see which open matches are likely to be the most exciting and to sort the matchday by that, so that I know which two or three matches to follow without any AI forecast or odds.
**Acceptance Criteria:**
- Given a matchday page with at least one open (not yet started) `Match`, When the page loads, Then every open match shows a five-step "Spannung" indicator under the teams, with the number of filled steps also available as text (e.g. "Spannung 4 von 5") for screen readers.
- Given two open matches where the first pairs two teams that are close in points, both in the top six, and each with 9+ points from their last five matches (`Form`), and the second pairs teams far apart in points, When the Spannung is shown, Then the first shows more filled steps than the second.
- Given a matchday page, When the visitor selects the sort "Spannendste zuerst", Then all matches of the matchday appear in one list ordered by descending Spannung (ties by kickoff time), each row shows weekday and time, and a sentence explains in plain German how the value is derived (table position, point gap, form; no AI).
- Given the sort "Spannendste zuerst" is active, When the visitor reloads the page or returns later in the same browser, Then the sort is still active; When the visitor selects the default sort, Then the day-grouped list returns.
- Given a `Forecast` exists for a match, When the Spannung of that match is computed, Then the value is identical to the value for the same match without a `Forecast` (the AI preview has no influence).
- Given a match that has started or is finished, When the page is shown, Then it shows no Spannung indicator and is placed after all open matches in the "Spannendste zuerst" list.
**QA Notes:**
- Boundaries: exactly 9 points from five matches (counts) vs 8 (does not); team on rank 6 vs 7 and rank 15 vs 14 in an 18-team table; matchday 1-5 where fewer than five matches exist for `Form`.
- Tension must be computed from `Standings` "before matchday N", so check it matches the table shown for that matchday, including 2. Bundesliga.
- Provisional results: a live match must no longer count as "open"; check the transition at kickoff.
- Test that no color other than green/red-as-meaning or neutral ink is used for the indicator; the lab used an amber edge for high values, which conflicts with brass being reserved for the AI preview.
- Mobile 375 px: sort control must not push the head row wider than the viewport.
**Open Questions / Assumptions:**
- The weights of the three factors (point gap, both top six / both bottom four, both on form) are taken from the lab; the real repo/owner should confirm them. "Close in points" needs a number (assumption: lab threshold, to be confirmed).
- The lab's amber edge for 4-5 bars is not allowed by the styleguide (brass = AI only); assumption: emphasise with ink weight instead. Decision needed.
- Does the sort apply to both Bundesligen on one page (stream) or per league? Assumption: per matchday page of one league.
- Is the stored sort choice acceptable as browser-only storage? Assumption: yes, no server-side data.

---

### r076 – Häufigste Ergebnisse
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r076--häufigste-ergebnisse) · [Snapshot](../snapshots/r076/) · Triage: keep

**Title:** Final-score frequency grid for a league season
**Story:** As a fan who wonders how often a result like 1:1 really happens, I want a grid of all final scores of the season, so that I can see the real distribution of results in the league.
**Acceptance Criteria:**
- Given a `LeagueSeason` with finished matches, When the visitor opens the section "Häufigste Ergebnisse", Then a grid shows home goals (rows) against away goals (columns) from 0 to 4+ and each cell shows how many matches ended with that final score.
- Given the grid, When it is rendered, Then home-win cells use `--win`-based, away-win cells `--loss`-based and draw cells grey shades whose intensity rises with frequency, and every cell's count is readable as text (not by colour alone).
- Given the season has a single most frequent score or several tied ones, When the lead sentence is shown, Then it names all tied scores and the count, e.g. "Am häufigsten in 64 Spielen: 0:0, 2:1, 2:2 und 1:0 (je 5-mal).", and a second sentence states how many matches had a team scoring four or more goals.
- Given a cell, When the visitor hovers, focuses, or taps it, Then the matches with that score are listed (home - away, matchday).
- Given a match with a provisional result, When the grid is built, Then it is not counted; the section states the number of matches it is based on.
- Given a league with no finished match yet, When the section is opened, Then it shows "Noch kein Spiel beendet." instead of an empty grid.
**QA Notes:**
- Grouping: 4:0, 5:1, 7:2 all land in the "4+" row/column; check that 4:4 and 6:5 land in the 4+/4+ cell and the list on tap still shows the real score.
- Singular/plural ("1 Spiel" vs "2 Spiele", "je 5-mal" vs a single winner).
- Both leagues: 1. and 2. Bundesliga separately; switching leagues must not keep stale counts.
- Mobile: the 5x5 grid must fit 375 px without horizontal scroll and with text >= 0.75 rem; touch targets for the match list.
- Colour contrast of counts on the darkest cell (WCAG AA).
**Open Questions / Assumptions:**
- The lab highlighted the visitor's club in amber; amber/brass is reserved for the AI preview. Assumption: highlighting of "Mein Verein" results uses ink outline or is dropped. Needs the "Mein Verein" decision (see r078).
- Placement: assumed on the season/league page. Cup and Nations League not included (not requested).
- Is "grey for draw" consistent with `--draw`? Assumption yes.

---

### r077 – Die drei Blickwinkel
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r077--die-drei-blickwinkel) · [Snapshot](../snapshots/r077/) · Triage: adapt

**Title:** Explain the three viewpoints behind a forecast on the match detail page (no tally)
**Story:** As a reader of a forecast, I want to know which three viewpoints the AI weighs and what each of them said for this match, so that I can follow how the forecast came about.
**Acceptance Criteria:**
- Given a match with a Forecast, When I open its match detail page (N01), Then the forecast section names the three viewpoints "Form" (letzte Spiele, Heim- und Auswärtsbilanz), "Direkte Duelle" (frühere Begegnungen) and "Umfeld" (Spielplan, Pause, Umstände), each with one plain-German sentence on what it looks at.
- Given the stored `Assessment` of each viewpoint for this match, When the section is shown, Then each viewpoint shows its lean in words (e.g. "spricht für Heimsieg") taken from the stored data, with no new AI step.
- Given a viewpoint produced no usable `Assessment` (e.g. insufficient data), When the section is shown, Then it says so in words instead of a lean.
- Given a FINAL match, When the section is shown, Then no viewpoint is marked "richtig"/"falsch", and no count, bar, ranking or "best viewpoint" appears on this page or any other (Spec 05: "Keine Bewertung einzelner Agenten").
- Given the start page, When I view it, Then this explanation does not appear there (it lives on the match detail page only).
**QA Notes:**
- Check the German descriptions against what the assessors actually evaluate; review the wording for anything that reads like a recommendation or betting language.
- Test a match where all three viewpoints agree and one where they lean three different ways.
- Mobile: the section stays compact at 375 px; screen readers hear one sentence per viewpoint.
- Regression: Spec 05's existing "Zustandekommen" content must not be duplicated; this section extends it.
**Open Questions / Assumptions:**
- Decision (Mark, 31 Oct 2026): option A – Spec 05 stays as it is; the lab's per-assessor tally and right/wrong chips are dropped.
- Assumption: if Spec 05's "Zustandekommen" already shows the three leans, this story reduces to the plain descriptions and the placement on the match detail page.

---

### r078 – Gut für deinen Verein
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r078--gut-für-deinen-verein) · [Snapshot](../snapshots/r078/) · Triage: keep

**Title:** "Gut für deinen Verein" hints in rivals' matches
**Story:** As a fan with a favourite club ("Mein Verein"), I want open matches of my club's table neighbours to say what result would help my club, so that I have a reason to follow the whole matchday.
**Acceptance Criteria:**
- Given the visitor has chosen a club as "Mein Verein" (stored in the browser only) and an open `Match` involves exactly one team within three places of that club in `Standings`, When the matchday page is shown, Then that match shows a hint "Gut für <Verein>: Sieg <Gegner des Nachbarn>" naming the team whose win helps.
- Given an open match in which both teams are table neighbours of the club, When the page is shown, Then the hint reads "Gut für <Verein>: Remis".
- Given a match involving the club itself or no team within three places, When the page is shown, Then no hint appears.
- Given no club has been chosen, When the page is shown, Then no hint and no prompt interrupts the list; the "Mein Verein" selection remains reachable where it was before.
- Given the visitor views another matchday than the current one, or the match is started or finished, When the page is shown, Then no hint appears.
- Given a hint is shown, Then it uses neutral ink styling (no brass, no green/red as decoration), and is derived only from the table, never from a `Forecast`, and contains no betting wording.
**QA Notes:**
- Boundaries: neighbour at distance exactly 3 vs 4; club at rank 1-2 or 17-18 (fewer neighbours); neighbours above and below on points equality.
- Defining "helps": a team above the club should lose, a team below should also lose? Test data for both directions, and the table "before matchday N" vs current table.
- Clearing "Mein Verein" in the browser or private mode must hide all hints without errors.
- Two matches with the same neighbour (e.g. same team in both leagues is not possible, but different-league clubs for 2. Bundesliga club chosen): check league scoping.
**Open Questions / Assumptions:**
- "Mein Verein" does not exist in the real app yet (no accounts; browser-only). Assumption: this story depends on a Mein-Verein story being ported or created, so it cannot be tested alone.
- Lab logic: neighbours up to three places; the product owner should confirm whether "helps" means the neighbour losing for both above and below.
- Club colour of the lab chip is dropped (colour tokens only); confirm hint styling.

---

### r080 – Vorbericht
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r080--vorbericht) · [Snapshot](../snapshots/r080/) · Triage: keep

**Title:** Rule-based "Vorbericht" above the fixtures
**Story:** As a visitor who opens the matchday page for the first time, I want a short preview text above the fixtures, so that I immediately see why this matchday matters.
**Acceptance Criteria:**
- Given the current matchday has open matches, When the page loads, Then a section "Vorbericht" with at most three sentences appears above the fixture list and is part of the server-rendered page (no layout shift after load).
- Given the matchday, When the first sentence is built, Then it names the match with the highest Spannung (see r073) with kickoff day and time and a table-based reason, e.g. "Spiel des Spieltags: Bayern – Dortmund (Sa 18:30) – Erster gegen Zweiter.".
- Given the longest unbeaten or winning streak among the teams of open matches, When the third sentence is built, Then it names the team, the streak length and the opponent as a question, e.g. "Serie auf dem Spiel: Hertha, in allen 9 Saisonspielen ungeschlagen. Hält das gegen Kaiserslautern?"
- Given no sentence can be formed from real data, When the page loads, Then that sentence is left out; Given none can be formed, Then the Vorbericht section is not shown at all.
- Given all matches are played or the visitor views another matchday, When the page loads, Then the Vorbericht is not shown.
- Given any matchday, Then the text contains no `Forecast` data, no odds, no "Tipp", and each statement can be verified against `Standings` and `Form` on the same page.
**QA Notes:**
- Check text correctness against the table: ranks, "Erster gegen Zweiter", point gaps; wording for ties on points.
- Streak rules: unbeaten vs winning vs losing; two teams with the same streak length; streak at 0.
- Season start (matchday 1, no history): section must disappear or say nothing false.
- Long team names and mobile line wrapping; plural/singular ("1 Saisonspiel").
- Lab included a sentence about the club of the visitor ("Bremen: ... mit einem Sieg ginge es auf Platz 3") - covered only if Mein Verein exists (see open questions).
**Open Questions / Assumptions:**
- Sentence 2 (own club's match and what a win would bring) needs "Mein Verein" (browser-only). Assumption: shown only when a club is chosen, otherwise the Vorbericht has up to two sentences.
- "Three rule-based sentences": template wording is subject to German copy review; assumed fixed templates, no AI-generated text.
- Provisional results: assumption that the table used is built from final results only.

---

### r081 – Besser als vor einem Jahr?
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r081--besser-als-vor-einem-jahr) · [Snapshot](../snapshots/r081/) · Triage: keep

**Title:** Points compared with the same stage last season
**Story:** As a fan, I want to see for every club whether it has more or fewer points than after the same number of games last season, so that I can answer "Sind wir besser als letztes Jahr?".
**Acceptance Criteria:**
- Given a `LeagueSeason` with at least one finished matchday and the previous `Season` available, When the visitor opens the section "Besser als vor einem Jahr?", Then every club has one row with a bar showing the point difference: more points to the right in `--win`, fewer to the left in `--loss`, zero as a neutral marker, sorted from biggest gain to biggest loss.
- Given a row, When it is shown, Then it states both numbers as text, e.g. "12 statt 8" and the difference, comparing the points after the same number of games the club has played now.
- Given a club that played in the other Bundesliga last season, When it is listed, Then its row carries "(2. Liga)" or "(1. Liga)" for the division of last season.
- Given a club that played in neither Bundesliga last season, When the section is shown, Then it is listed separately as "Ohne Vorjahresvergleich" and not in the bar list.
- Given the lead sentence, When it is shown, Then it states how many clubs improved and who gained most, e.g. "Mainz (+6)".
- Given no finished match in the current season, When the section is opened, Then it shows "Noch kein Spiel gespielt." instead of bars.
**QA Notes:**
- Compare number of games: clubs with a postponed match have fewer games; the comparison must use "after the same number of games", not same matchday.
- Provisional results must not count for the current season.
- Verify with a promoted and a relegated club; with ties (equal points → zero difference); long lists (18 clubs) at 375 px.
- Previous-season data must come from final results only; test the first season in the database (no previous season).
- Bars on a diverging axis: check scaling when the maximum difference is small.
**Open Questions / Assumptions:**
- Previous-season data source: assumption that OpenLigaDB history is used (lab built a one-off file); the real repo decides storage.
- Highlighting of "Mein Verein" sentence ("Bremen: 4 Punkte mehr als nach 7 Spielen im Vorjahr") only when a club is chosen.
- Goal/points rule for 2. Bundesliga with three points for a win throughout: assumed identical for both seasons.

---

### r084 – Tore der besonderen Art
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r084--tore-der-besonderen-art) · [Snapshot](../snapshots/r084/) · Triage: keep

**Title:** Special goals: late, early, penalties, own goals
**Story:** As a fan, I want to see how many late goals, very early goals, converted penalties and own goals the season has produced and which clubs are behind them, so that I can retell the stories of the season with real numbers.
**Acceptance Criteria:**
- Given a `LeagueSeason` with `Goal` data, When the visitor opens "Tore der besonderen Art", Then four cards appear: "Ab der 90. Minute", "In den ersten fünf Minuten", "Vom Punkt" and "Geschenkt", each with the count of matching goals in that league.
- Given a card, When it is shown, Then it lists the top three clubs with crest and count; clubs tied for third place are all shown; Given there are no goals in a category, Then the card says "Noch kein Tor dieser Art."
- Given a card, When it is shown, Then it shows the most recent example in the form "Zuletzt: <Torschütze>, <Minute>. Minute, <Heim> – <Gast>" and the line links to the match page.
- Given "Vom Punkt" and "Geschenkt", When goals are counted, Then converted penalties count for the shooting team, own goals count for the team that benefits, and penalty-shootout kicks never count.
- Given matches with a provisional result, When counting, Then their goals are excluded and the section says on how many final matches it is based.
- Given the visitor's club is not in the top three, When "Mein Verein" is set, Then its own count is added below the list.
**QA Notes:**
- Boundary: minute 90 (counts) vs 89; stoppage-time "90+3" must count as 90+; minute 5 (counts?) vs 6.
- Own goal attribution (credit to the opponent) is easy to get wrong; test a match with an own goal and check the card total and team.
- Both leagues separate; cup/Nations League excluded.
- Data gaps: goals without minute or without scorer; matches with missing goal lists.
- Mobile: four cards stack without horizontal scroll.
**Open Questions / Assumptions:**
- "In den ersten fünf Minuten" boundary (minute 1-5) and whether minute 0/1 numbering from OpenLigaDB equals the lab; to confirm with real data.
- Penalty marking requires the data source to flag penalty goals; assumption that OpenLigaDB provides it.
- Whether the section is part of the league season page or the team pages is open; assumption season page.

---

### r085 – Wegweiser
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r085--wegweiser) · [Snapshot](../snapshots/r085/) · Triage: adapt

**Title:** Section navigation for a long season page
**Story:** As a visitor on a long season page, I want a row of links to its sections that shows where I am, so that I can find sections further down without endless scrolling.
**Acceptance Criteria:**
- Given a season page with more sections than the agreed threshold, When the page loads, Then a row of links, one per section in page order, appears directly below the page head and every link jumps to its section.
- Given the visitor scrolls, When a section is in view, Then its link is marked as current by a visible ink style (not brass, not green/red) and the link row scrolls sideways to keep it visible.
- Given a link is used, When the section appears, Then the address shows the section anchor, and the browser's back button returns to the earlier position; without script, the links still jump to the anchors.
- Given a phone viewport of 375 px, When the row is shown, Then it stays on one line, scrolls sideways without a visible scrollbar and the page itself has no horizontal scroll.
- Given a page that has the threshold number of sections or fewer, When it loads, Then the row is not shown.
- Given keyboard use, When the visitor tabs through the row, Then each link has a visible focus state and a section name that matches its heading.
**QA Notes:**
- Sticky behaviour with the top navigation (dark stage): the row must not cover the target heading after jumping (anchor offset).
- Sections that appear/disappear based on data (empty states) must not leave dead links.
- Check long labels in the row and a language-level check: link text equals section heading.
- Verify no layout shift when the row appears and that it does not conflict with the sticky filter bar of the stream page.
- Reduced motion: jumping must not animate if `prefers-reduced-motion` is set.
**Open Questions / Assumptions:**
- Only needed if the real season page becomes long (lab had 14 sections). Decision: product owner defines the threshold (assumption: 6+ sections) or defers the story.
- Whether the row is sticky or static; lab used sticky at 50 px height. Needs a styleguide entry if sticky.
- The amber highlight of the lab conflicts with the styleguide; ink is assumed.

---

### r086 – Kompakt
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r086--kompakt) · [Snapshot](../snapshots/r086/) · Triage: keep

**Title:** Compact one-line view of the matchday
**Story:** As a regular visitor who only wants scores, I want a compact view in which every match is a single line, so that I can see a whole matchday of both leagues with little scrolling.
**Acceptance Criteria:**
- Given a matchday page, When the visitor selects the view "Kompakt", Then every `Match` is shown as one line with kickoff time, crests and short names, and the score (or the time for open matches).
- Given "Kompakt" is active and a `Forecast` exists, When a line is shown, Then it also contains the KI-Vorschau Richtwert (e.g. "2:1"), marked as a forecast; Given none exists, Then no placeholder text or "KI ?" is shown.
- Given "Kompakt" is active, When a line is shown, Then secondary elements (goal flow, hints, tension meter, weather, Vorbericht) are not displayed, and a tap or Enter on the line opens the match page.
- Given "Kompakt" was selected, When the visitor reloads or returns in the same browser, Then the compact view is still active; selecting the list view restores the previous view fully.
- Given a provisional result, When it is shown in the compact line, Then it is still marked as provisional (dashed, unfilled pill) and a running match shows "Läuft" or the interim score in red outline.
- Given a phone viewport of 375 px, When the view is active, Then 18 matches fit on at most two screen heights, and no line wraps or causes horizontal scroll.
**QA Notes:**
- Longest short team names and 2-digit score cases; postponed matches without time.
- Auto-update of live matches must keep working in the compact view (polling, no flicker).
- Touch target height for one-line rows (>= 44 px?) vs density: check both.
- The view switch itself must not overflow on phones (lab hit 400 px with five view buttons).
- Remembered choice in browser storage: blocked storage must fall back to the list view without error.
**Open Questions / Assumptions:**
- Which elements exist in the real matchday page differs from the lab; the list of hidden items is assumed to be "everything below the pairing".
- The lab's KI cell ("KI ?" until an own call was made) is dropped; own calls do not exist. A line shows the Richtwert only if a Forecast exists, marked as KI-Vorschau.
- Name "Kompakt" for the toggle next to "Liste" is kept.

---

### r088 – Vereinskalender
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r088--vereinskalender) · [Snapshot](../snapshots/r088/) · Triage: keep

**Title:** Club season as a month calendar on the TeamPage
**Story:** As a fan who plans trips to the ground, I want to see a club's whole season as month grids, so that I can see at a glance when it plays at home and when away.
**Acceptance Criteria:**
- Given a `TeamPage` with `ScheduleRow` entries, When the visitor opens the calendar, Then the season is shown as month grids (first to last month with a match), Monday first, each match day marked with the opponent's crest.
- Given a calendar day, When it is shown, Then home games and away games are distinguishable without colour (filled cell vs frame) and a played game also shows its outcome (`TeamResult.WIN` green, `DRAW` grey, `LOSS` red) with the result as text.
- Given a day cell, When the visitor hovers, focuses or taps it, Then a label appears such as "7. November: gegen Leipzig, 18:30" or the result, and the cell links to the match page.
- Given a match without a fixed kickoff time, When it is shown, Then the cell states "Uhrzeit offen" and a note under the calendar explains that late kickoff times are often fixed only a few weeks ahead.
- Given today falls in the season, When the calendar is shown, Then today is outlined in ink (not brass).
- Given a club in `League.DFB_POKAL` or other competitions, When the calendar is shown, Then only matches of the competition of the TeamPage appear, labelled accordingly, and statistics are not merged across competitions.
**QA Notes:**
- Two matches in one week and two on the same day (cup), midweek rounds, winter break, months with 5 weeks.
- Postponed or rescheduled matches: the cell must move with the new date.
- Provisional results in the grid must be marked provisional.
- Mobile 375 px: month grids stack or scroll without overflow; touch targets for day cells.
- Time zones around midnight (late kickoff); first month with only a few days.
- Existing calendar export (.ics) must remain unchanged.
**Open Questions / Assumptions:**
- Season span: lab used August to May; assumption derived from actual match dates.
- Lab also had the .ics export; assumed already available in the real app, to confirm.
- Cup competition entries on the same TeamPage: show or hide is open.

---

### r090 – Vereinskarte teilen
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r090--vereinskarte-teilen) · [Snapshot](../snapshots/r090/) · Triage: adapt

**Title:** Share the club card as an image
**Story:** As a fan, I want to share a picture of my club's season so far, so that I can show it in a group chat.
**Acceptance Criteria:**
- Given a club page, When I choose "Vereinskarte teilen", Then a portrait image (1080 × 1350) is created showing the crest, short name, league and "nach 7 Spielen", three figures (Platz, Punkte, Tore), the last five results, "Bester Torschütze: N. Füllkrug (8)" and the running streak ("Seit 6 Spielen ungeschlagen"), signed "Rasen-Radar".
- Given a phone with a share sheet, When the image is ready, Then the share sheet opens with the image.
- Given a device without a share sheet, When the image is ready, Then it is saved as `rasen-radar-Bremen.png` and the page confirms "Bild gespeichert."
- Given the card, When I look at it, Then it follows the styleguide: white surface, ink and hairlines, no club colours, no gradients, no amber or brass.
- Given the card, When it is created, Then it uses only data of the page (FINAL results; provisional ones are excluded), contains no KI-Vorschau, no personal data and no Mein-Verein marking.
- Given a club without a played match, When I look at the page, Then the button is hidden.
**QA Notes:** Check long club names, clubs with no scorer yet or no streak (line omitted), both leagues, the file size, and that the image text is German and readable on a phone chat preview. No new requests are made for the image.
**Open Questions / Assumptions:**
- Decision (Mark, 31 Oct 2026): keep, he likes it. The lab version used the crest colour, which is dropped.
- The match share image of r005 was dropped as decoration; the club card is an explicit exception requested by Mark.
- Is the image created on the device or on the server? Left to the repo; no account or upload by the visitor.

---


### r091 – Aufholjagden
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r091--aufholjagden) · [Snapshot](../snapshots/r091/) · Triage: keep

**Title:** Comebacks and thrown-away leads per club
**Story:** As a fan, I want to see for each club how many points it won after trailing and how many it dropped after leading, so that I can back up talk about "Moral" with real numbers.
**Acceptance Criteria:**
- Given a `LeagueSeason` with finished matches and `Goal` lists, When the visitor opens "Aufholjagden", Then every club has one row with a diverging bar: points dropped after leading to the left in `--loss`, points won after trailing to the right in `--win`, sorted by the balance (won minus dropped).
- Given a row, When it is shown, Then it states both numbers as text, e.g. "6 geholt, 0 verspielt", and a screen reader hears one sentence per club.
- Given a match that a team won or drew after being behind at any moment, When points are counted, Then it counts 3 or 1 points as "geholt" for the team; Given a match lost or drawn after being ahead at any moment, Then 3 minus actual points are "verspielt" (a win after leading does not count).
- Given the lead sentence, When it is shown, Then it names the biggest turnaround by goal deficit and the match, e.g. "Größte Aufholjagd: Dortmund lag 2 Tore zurück und holte noch den Sieg – Hoffenheim – Dortmund 2:3", and the sequence of goals can be checked on the match page.
- Given matches without a complete goal list or with a provisional result, When counting, Then they are excluded and the section states how many matches it is based on.
- Given a season with no comeback yet, When the section is opened, Then it shows "Noch keine Aufholjagd in dieser Saison."
**QA Notes:**
- Goal order: equalisers at the same minute, own goals (credit to the opponent), a team that leads, falls behind and wins; 0:0 draws count nowhere.
- Penalty-shootout goals must not count (cup not in scope).
- Verify against the lab example: 1:0 45', 2:0 54', 2:1, 2:2, 2:3 87'.
- Tied balance sort order; 18 rows at 375 px, no horizontal scroll.
- Missing goal minutes: ordering of goals must not depend on minute alone.
**Open Questions / Assumptions:**
- Definition "points won after trailing": assumed the team was behind at any time; a stricter definition (e.g. behind at half-time) is possible; owner to confirm.
- "Mein Verein" highlight only when chosen.
- Both leagues separate, no merge.

---

### r092 – Anstoß-Ticket
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r092--anstoß-ticket) · [Snapshot](../snapshots/r092/) · Triage: adapt

**Title:** Next-match bar for "Mein Verein"
**Story:** As a fan with a favourite club, I want a small bar with my club's next match and a countdown on every page, so that my club's match is never more than a glance away.
**Acceptance Criteria:**
- Given a club is chosen as "Mein Verein" and has a next `Match`, When any page is shown, Then a flat bar (hairline border, white surface, no shadow) shows the opponent's crest, "<Verein> gegen <Gegner>" (or "bei <Gegner>" away), the date and kickoff time and a countdown such as "heute 15:30 · in 13 Std 26 Min" or "Sa., 8.11. 15:30 · in 3 Tagen".
- Given the match is running, When the bar is shown, Then it shows "Live" with the interim score in a `--loss` outline; Given the match is finished, Then the bar switches to the next open match; Given a provisional result, Then it is marked "vorläufig".
- Given the bar is shown, When the visitor taps or presses it, Then the match page opens.
- Given no club is chosen, or the club has no further match in the season, When any page is shown, Then no bar appears; for the latter the club page says "Keine Spiele mehr in dieser Saison."
- Given a phone viewport, When the bar is shown, Then it sits above the bottom navigation without covering content or the thumb bar controls, no layout shift happens when it appears, and text stays >= 0.75 rem; on desktop it sits at the corner.
- Given the page is open, When a minute passes, Then the countdown updates without a page reload.
**QA Notes:**
- Time boundaries: exactly at kickoff, 59 minutes before, midnight rollover, "heute"/"morgen"/weekday wording, switching summer/winter time.
- Postponed match (no time) → "Termin offen".
- Overlap with other fixed elements (cookie/consent, install card, keyboard open on phone).
- Printing and accessibility: should not be announced every minute by screen readers (no live region spam).
- Private mode / cleared storage → no bar, no error.
**Open Questions / Assumptions:**
- Browser-only storage of "Mein Verein": same decision as r078/r096; the bar disappears when storage is unavailable.
- Lab slid the bar in after scrolling past the hero and used a shadow; adaptation per triage: flat, no shadow, no slide animation (only action-triggered motion allowed). Visibility rule (always vs after scroll) is open.
- Whether the bar appears on every page including the dark-stage match page.
- Cup matches and Nations League of the club: assumption only league matches for now.

---

### r095 – DFB-Pokal
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r095--dfb-pokal) · [Snapshot](../snapshots/r095/) · Triage: adapt

**Title:** "Pokalschreck" marker and "Noch dabei" row in the DFB-Pokal view
**Story:** As a fan following the cup, I want upsets by lower-division clubs to be marked and the clubs still in the competition shown together, so that I see the cup stories and who is still alive.
**Acceptance Criteria:**
- Given `League.DFB_POKAL` (`CompetitionFormat.KNOCKOUT`) with a finished tie in which the winner plays in a lower division than the loser, When the round is shown, Then the tie carries the marker "Pokalschreck" in text, in ink style (not brass, not green/red), and the winner is set in bold.
- Given a tie decided after extra time or penalties, When the result is shown, Then the score after 90 minutes is shown with "n. V." or "i. E. 5:4" and the winner is determined by shootout, else extra time, else 90 minutes (Spec 06).
- Given at least one finished round, When the view is opened, Then a row "Noch dabei" lists all clubs still in the competition with crest and short name, with a count in the lead, e.g. "Noch 16 Vereine im Wettbewerb".
- Given a club that is out, When the lead sentence is shown, Then it states the number of upsets so far and, with "Mein Verein", where the club was knocked out, e.g. "Bremen ist in der 2. Runde ausgeschieden: 2:3 in Dortmund."
- Given a club without a crest in the data, When it is listed, Then a neutral initials badge is shown, an empty or odd short name falls back to the full name.
- Given the round is open and results are provisional, When the tie is shown, Then it is marked as provisional and does not yet set "Pokalschreck" or remove a club from "Noch dabei" as final.
**QA Notes:**
- Division comes from the 1./2./3. Liga lists of the same season; everyone else counts as lower; a 2. Bundesliga club beating a 1. Bundesliga club is an upset, 3. Liga vs lower is also; same division is not.
- Verify extra-time/penalty cases (e.g. 1:1 i. E. 5:4); shootout kicks are no goals and must not show in scorer lists.
- Rounds fill over time (1. Runde, 2. Runde ...); a round before draw has no ties: "Noch nicht ausgelost".
- Compare the "Noch dabei" count with the winners of the latest finished round; byes or walkovers.
- Mobile: tabs for rounds on one swipeable line.
**Open Questions / Assumptions:**
- Spec 06 already requires class display and highlighting of upsets (Überraschungen) with strength by class distance; this story only ports the visual term "Pokalschreck" and the "Noch dabei" row. Confirm that "Noch dabei" is not already covered by the Spec 06 team chain.
- The lab placed the cup on the Saison page; assumption: on the cup competition page of the real app.
- Brass amber tag in the lab is replaced by an ink style (brass = AI preview only).
- No forecast or bracket is shown (Spec 06 excludes a tournament tree).

---

### r096 – Beobachten
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r096--beobachten) · [Snapshot](../snapshots/r096/) · Triage: adapt

**Title:** Star a match to pin it on top (browser-only, no alerts)
**Story:** As a fan following more than my own club, I want to star single matches, so that they stay on top of the fixtures.
**Acceptance Criteria:**
- Given an open (not started) `Match` in the list, When the visitor selects its star control, Then the match is marked "beobachtet" (control state announced to screen readers) and moves to a block "Beobachtet" at the top of the fixtures.
- Given a finished match, When it is listed, Then it has no star control.
- Given a starred match, When the visitor selects the star again, Then it returns to its day group.
- Given starred matches and a new visit in the same browser, When the page is shown, Then the stars are still set (browser-only, no account, no login) and at most 30 starred matches are kept; the oldest is dropped first.
- Given a starred match, When I look for notifications, permission prompts or goal alerts, Then none exist; the star only pins.
- Given a starred match, When the visitor clicks it in the block, Then the match detail page (N01) opens.
**QA Notes:** Without script, no star appears; blocked storage: stars not remembered, no error. A starred match that starts, ends or gets postponed: pin block and star state must update (a started match leaves the block or stays marked, see open question). The block "Beobachtet" must not use a brass edge (the lab used amber); use ink/hairline. Mobile: the star sits in the time line with a touch target of at least 44 px. Verify no notification permission is ever requested.
**Open Questions / Assumptions:**
- Decision (Mark, 31 Oct 2026): keep the star, without goal alerts (realtime data plus push is not feasible).
- Hidden in a compact view (r086) as in the lab? Assumption yes.
- Does a starred match stay pinned while running or after it ends? Assumption: it stays until the Matchday is over, then returns to its day group.

---

### r098 – Heimvorteil
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r098--heimvorteil) · [Snapshot](../snapshots/r098/) · Triage: keep

**Title:** Home advantage in the league and per club
**Story:** As a fan, I want to see how often the home team wins, draws or loses in the league and which clubs are strong at home or away, so that I know how much the home advantage really is.
**Acceptance Criteria:**
- Given a `LeagueSeason` with finished matches, When the visitor opens "Heimvorteil", Then one bar of three parts shows the shares of home wins, draws and away wins with labels as text, e.g. "52 % Heimsiege · 20 % Remis · 28 % Auswärtssiege", and the number of matches it is based on.
- Given the lead sentence, When it is shown, Then it states the home-win to away-win ratio, e.g. "Wer zu Hause spielt, gewinnt 1,8-mal so oft wie der Gast.", and shows no sentence if there are no away wins (no division by zero).
- Given the club lists, When they are shown, Then "Zu Hause am stärksten" and "Lieber auswärts" list clubs with points per home game and points per away game and a mirrored bar, sorted by the difference between home and away points per game.
- Given a club with no home or no away game yet, When lists are built, Then the club is left out and not shown with zero.
- Given a result is provisional, When shares are computed, Then it is not counted.
- Given an empty league, When the section is opened, Then it shows "Noch kein Spiel beendet."
**QA Notes:**
- Numbers: rounding of percentages to 100 %; decimal comma ("2,3"); equal differences.
- Compare computed points with the `Standings` home/away split.
- Both leagues separate and neutral venues (none in league).
- Colour: the lab used blue for away; the styleguide has no blue, use ink for home/neutral and `--win`/neutral; check legend as text.
- Early season (few matches): consider the minimum sample note.
**Open Questions / Assumptions:**
- Colour coding of the mirrored bar is open (see above).
- How many clubs per list (lab: short lists); assumption five, to be confirmed.
- A minimum number of games before the section is shown (`minimumSampleSize`) is not defined.

---

### r099 – Glossar auf der KI-Seite
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r099--glossar-auf-der-ki-seite) · [Snapshot](../snapshots/r099/) · Triage: keep

**Title:** Plain-language glossary on the KI pages
**Story:** As a reader of the Trefferbilanz and the preview pages, I want to look up key terms where they appear, so that numbers like "Sicherheit 50 bis 70 %" are understandable without prior knowledge.
**Acceptance Criteria:**
- Given a KI page (Trefferbilanz, preview, "So entsteht eine Prognose"), When it is shown, Then the first occurrence per section of Tendenz (`Outcome`), Sicherheit (`confidence`), Trefferquote (`HitRate`), "reine Statistik" (`Baseline`), Direkte Duelle and Umfeld is shown as a button with a dotted underline in ink (not brass).
- Given such a term, When the visitor taps or presses Enter/Space, Then a short box under the word explains it in one plain sentence, e.g. Sicherheit: "Wie wahrscheinlich die KI ihre eigene Tendenz hält, in Prozent. 44 % heißt: etwas wahrscheinlicher als jede der beiden anderen – sicher ist das nicht."
- Given an open explanation, When the visitor taps the term again, taps elsewhere or presses Escape, Then the box closes and focus returns to the term.
- Given a screen reader, When a term is focused, Then it is announced as a button with its expanded or collapsed state.
- Given the page is updated by a partial refresh, When the content is replaced, Then the terms are still marked and working.
- Given the texts, Then they contain no AI jargon (model, agent, token), no odds wording, and match the same wording as the "So entsteht eine Prognose" page; unchanged for every visitor.
**QA Notes:**
- Without script the explanation must still be reachable (e.g. visible on the explainer page); the lab applied marks by script only.
- Terms inside links, headings or table cells; the same word in headings should not become a button; first occurrence per section only.
- Mobile: box width at 375 px and not clipped at screen edges; keyboard order.
- Glossary wording versus Spec 05 "kleiner, fester Sprachschatz": all texts should live in one place.
- Contrast and 0.75 rem minimum.
**Open Questions / Assumptions:**
- Spec 05 fixes the vocabulary but not an inline glossary; this story goes beyond it. Whether the dotted mark is brass (lab: amber) or ink: styleguide allows brass for the AI preview, so a brass mark on the KI pages may be acceptable; owner decides.
- Final wording for each term is subject to copy review; "Direkte Duelle" and "Umfeld" definitions follow the assessor names.
- Applies also to pages other than the Trefferbilanz? Assumption: only KI pages.



---

### r100 – Seite anpassen
Lab reference: [CHANGELOG entry](../monitoring/CHANGELOG.md#r100--seite-anpassen) · [Snapshot](../snapshots/r100/) · Triage: adapt

**Title:** Switch page sections off (browser-only)
**Story:** As a regular visitor, I want to hide sections I never use, so that each page shows only what matters to me.
**Acceptance Criteria:**
- Given any page with several sections, When I scroll to its foot, Then "Diese Seite anpassen" opens a dialog listing the sections of that page with a checkbox each.
- Given the dialog, When I switch a section off, Then it disappears at once and the dialog says: "Was ausgeschaltet wird, bleibt in diesem Browser ausgeblendet. Die Spiele selbst bleiben immer da."
- Given a hidden section, When I reload or return later in the same browser, Then it is still hidden and never appears for a moment before disappearing.
- Given hidden sections, When I choose "Alles zeigen", Then every section is shown again.
- Given a hidden section that is part of an in-page navigation, When the page is shown, Then it is also missing from that navigation.
- Given the match list, matchday list and match detail core data, When the dialog is shown, Then they are not offered as switchable.
- Given blocked storage or a private window, When I open the dialog, Then it says that the choice cannot be remembered and the page still works.
- Given the feature, When I use it, Then no login is needed and nothing is stored on the server.
**QA Notes:** Check keyboard and screen reader use of the dialog (focus trap, return focus, Escape), 375 px, a visitor switching everything off, and that hidden-section state survives a new season or a renamed section (unknown keys are ignored). Verify the first paint shows no hidden section (no flash).
**Open Questions / Assumptions:**
- Decision (Mark, 31 Oct 2026): keep, browser-only and only if it works without login.
- Sections that no longer exist in the product (own calls, friends' duel, bingo) are not listed.
- Which sections per page are switchable is for product to decide; the lab offered all sections except the matches.