# Season Outlook (Spec 4) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** For every team, show on its club page (`TeamPage`) a Monte-Carlo-simulated probability per placement goal (championship, Europe, promotion, relegation, play-offs) for the rest of the current season, recomputed automatically once a matchday of its league is fully final.

**Architecture:** New feature package `season` (boundary/control/entity, BCE like `matchday`/`forecast`/`review`). `season.control` reads everything it needs from `matchday` exclusively through `MatchdayFacade` (new methods added there). The trigger is the existing `ResultFinalized` CDI event, already fired by `matchday` and already observed by `review` — `season` adds its own observer. Display is a self-contained htmx fragment `season` serves at its own URL, embedded from `team.html` by a plain link string — mirroring exactly how `forecast`'s summary fragment is embedded in `match.html`. `matchday` never imports anything from `season`.

**Tech Stack:** Quarkus, Hibernate ORM with Panache (active record), Qute templates + htmx fragments, Flyway, JUnit 5 (plain, no `@QuarkusTest` — this codebase tests business logic as pure functions on in-memory entities, never against a real database; see `StandingsCalculatorTest`/`ReviewServiceTest` for the established style).

**Spec:** `docs/specs/spec-04-saisonaussicht.md`; technical design: `docs/superpowers/specs/2026-09-20-season-outlook-design.md`

## Global Constraints

- Code in English; German only in Qute templates and UI copy (CLAUDE.md).
- `entity` must not depend on `control` or `boundary`; `control` must not depend on `boundary` — both within `season` (ArchUnit `bce_layers`, checked per-feature).
- Cross-feature access only through a feature's `boundary` — `season.control` may depend on `matchday.boundary.MatchdayFacade`, never on `matchday.control` or `matchday.entity` (ArchUnit `features_collaborate_only_via_boundary`).
- `ArchitectureTest.FEATURES` must list `season`, or the `bce_layers` check wrongly flags `season`'s (legitimate) calls into `matchday.boundary`.
- Placement-goal position ranges (Task 1) are the single source of truth — `MatchdayPageModels.zone()` must be refactored to use them, not keep its own copy.
- A recomputation replaces a league-season's outlook rows wholesale (delete then insert in one transaction) — no history, no partial update (spec 04 rule).
- Tests only for business logic (per CLAUDE.md: no tests for trivial persistence wiring, getters, or framework behaviour) — pure calculators get direct unit tests with in-memory objects; thin facade/persistence/event-wiring methods do not, matching every existing example in this codebase (`MatchdayFacade`, `ResultFinalizedObserver`, `ForecastScheduler.forecastDueMatches` are untested; only their pure cores are).

---

## Task 1: `PlacementGoal` — the shared placement-goal definitions

**Files:**
- Create: `src/main/java/de/javamark/matchoracle/matchday/entity/PlacementGoal.java`
- Test: `src/test/java/de/javamark/matchoracle/matchday/entity/PlacementGoalTest.java`

**Interfaces:**
- Consumes: `de.javamark.matchoracle.matchday.entity.League` (existing enum, values `BUNDESLIGA_1`, `BUNDESLIGA_2`, `CHAMPIONS_LEAGUE`)
- Produces: `PlacementGoal` enum with instance method `label()` (German display label) and static methods `forPosition(League, int)` and `all(League)` — used by Task 2 (refactor) and Task 4 (`MatchdayFacade`)

- [ ] **Step 1: Write the failing tests**

```java
package de.javamark.matchoracle.matchday.entity;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Spec 04: the same position ranges MatchdayPageModels.zone() colors the table by. */
class PlacementGoalTest {

    @Test
    void bundesliga1ChampionQualifiesForBothChampionshipAndEurope() {
        assertEquals(List.of(PlacementGoal.CHAMPIONSHIP, PlacementGoal.EUROPE), PlacementGoal.forPosition(League.BUNDESLIGA_1, 1));
    }

    @Test
    void bundesliga1FourthPlaceIsEuropeOnly() {
        assertEquals(List.of(PlacementGoal.EUROPE), PlacementGoal.forPosition(League.BUNDESLIGA_1, 4));
    }

    @Test
    void bundesliga1FifthPlaceHasNoGoal() {
        assertTrue(PlacementGoal.forPosition(League.BUNDESLIGA_1, 5).isEmpty());
    }

    @Test
    void bundesliga1SixteenthIsTheRelegationPlayoff() {
        assertEquals(List.of(PlacementGoal.RELEGATION_PLAYOFF), PlacementGoal.forPosition(League.BUNDESLIGA_1, 16));
    }

    @Test
    void bundesliga1SeventeenthIsRelegation() {
        assertEquals(List.of(PlacementGoal.RELEGATION), PlacementGoal.forPosition(League.BUNDESLIGA_1, 17));
    }

    @Test
    void bundesliga2SecondIsDirectPromotion() {
        assertEquals(List.of(PlacementGoal.PROMOTION), PlacementGoal.forPosition(League.BUNDESLIGA_2, 2));
    }

    @Test
    void bundesliga2ThirdIsThePromotionPlayoff() {
        assertEquals(List.of(PlacementGoal.PROMOTION_PLAYOFF), PlacementGoal.forPosition(League.BUNDESLIGA_2, 3));
    }

    @Test
    void bundesliga2SixteenthIsTheRelegationPlayoffToo() {
        assertEquals(List.of(PlacementGoal.RELEGATION_PLAYOFF), PlacementGoal.forPosition(League.BUNDESLIGA_2, 16));
    }

    @Test
    void championsLeagueEighthIsDirectKnockout() {
        assertEquals(List.of(PlacementGoal.KNOCKOUT_DIRECT), PlacementGoal.forPosition(League.CHAMPIONS_LEAGUE, 8));
    }

    @Test
    void championsLeagueNinthIsThePlayoff() {
        assertEquals(List.of(PlacementGoal.KNOCKOUT_PLAYOFF), PlacementGoal.forPosition(League.CHAMPIONS_LEAGUE, 9));
    }

    @Test
    void championsLeagueTwentyFifthIsElimination() {
        assertEquals(List.of(PlacementGoal.ELIMINATION), PlacementGoal.forPosition(League.CHAMPIONS_LEAGUE, 25));
    }

    @Test
    void allListsEveryGoalOfALeagueExactlyOnce() {
        assertEquals(4, PlacementGoal.all(League.BUNDESLIGA_1).size());
        assertEquals(4, PlacementGoal.all(League.BUNDESLIGA_2).size());
        assertEquals(3, PlacementGoal.all(League.CHAMPIONS_LEAGUE).size());
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./mvnw test -Dtest=PlacementGoalTest`
Expected: FAIL — compile error, `PlacementGoal` does not exist yet.

- [ ] **Step 3: Write the implementation**

```java
package de.javamark.matchoracle.matchday.entity;

import java.util.ArrayList;
import java.util.List;

/**
 * A placement a team's final league position can achieve — championship, a European slot,
 * promotion, relegation, or one of their play-offs (spec 04). The position ranges are the
 * same ones {@code MatchdayPageModels.zone()} colors the table by (spec 01) — the season
 * outlook is the only other consumer, so the ranges live here once instead of twice.
 */
public enum PlacementGoal {

    CHAMPIONSHIP("Meisterschaft"),
    EUROPE("Europapokal"),
    PROMOTION("Aufstieg"),
    PROMOTION_PLAYOFF("Play-off"),
    RELEGATION_PLAYOFF("Play-off"),
    RELEGATION("Abstieg"),
    KNOCKOUT_DIRECT("K.-o.-Runde direkt"),
    KNOCKOUT_PLAYOFF("Play-off"),
    ELIMINATION("Ausscheiden");

    private final String label;

    PlacementGoal(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** The goals a final table position satisfies for a league; empty for a plain mid-table finish. */
    public static List<PlacementGoal> forPosition(League league, int position) {
        List<PlacementGoal> goals = new ArrayList<>();
        if (league == League.CHAMPIONS_LEAGUE) {
            if (position <= 8) {
                goals.add(KNOCKOUT_DIRECT);
            } else if (position <= 24) {
                goals.add(KNOCKOUT_PLAYOFF);
            } else {
                goals.add(ELIMINATION);
            }
            return goals;
        }
        if (league == League.BUNDESLIGA_1) {
            if (position == 1) {
                goals.add(CHAMPIONSHIP);
            }
            if (position <= 4) {
                goals.add(EUROPE);
            }
        }
        if (league == League.BUNDESLIGA_2) {
            if (position <= 2) {
                goals.add(PROMOTION);
            }
            if (position == 3) {
                goals.add(PROMOTION_PLAYOFF);
            }
        }
        if (position == 16) {
            goals.add(RELEGATION_PLAYOFF);
        }
        if (position >= 17) {
            goals.add(RELEGATION);
        }
        return goals;
    }

    /** Every goal a league's table can produce — e.g. to list all of them even before any team has clinched one. */
    public static List<PlacementGoal> all(League league) {
        return switch (league) {
            case CHAMPIONS_LEAGUE -> List.of(KNOCKOUT_DIRECT, KNOCKOUT_PLAYOFF, ELIMINATION);
            case BUNDESLIGA_1 -> List.of(CHAMPIONSHIP, EUROPE, RELEGATION_PLAYOFF, RELEGATION);
            case BUNDESLIGA_2 -> List.of(PROMOTION, PROMOTION_PLAYOFF, RELEGATION_PLAYOFF, RELEGATION);
        };
    }
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./mvnw test -Dtest=PlacementGoalTest`
Expected: PASS (13 tests)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/de/javamark/matchoracle/matchday/entity/PlacementGoal.java src/test/java/de/javamark/matchoracle/matchday/entity/PlacementGoalTest.java
git commit -m "feat(matchday): add PlacementGoal, the shared table-zone definitions (spec 04)"
```

---

## Task 2: Refactor `MatchdayPageModels.zone()` onto `PlacementGoal`

**Files:**
- Modify: `src/main/java/de/javamark/matchoracle/matchday/boundary/MatchdayPageModels.java:144-156`
- Modify: `src/test/java/de/javamark/matchoracle/matchday/boundary/MatchdayPageModelsTest.java` (add a `zone()` section)

**Interfaces:**
- Consumes: `PlacementGoal.forPosition(League, int)` (Task 1)
- Produces: `MatchdayPageModels.zone(League, int)` unchanged signature/behaviour — CSS class strings `"zone-top"`, `"zone-top-playoff"`, `"zone-bottom-playoff"`, `"zone-bottom"`, `""`

- [ ] **Step 1: Write the failing tests**

Add to `MatchdayPageModelsTest.java` (same package, so `zone()` — package-private — is callable directly):

```java
    @Test
    void zoneColorsBundesliga1sChampionsLeagueSpotsAndTheRelegationLadder() {
        assertEquals("zone-top", MatchdayPageModels.zone(League.BUNDESLIGA_1, 1));
        assertEquals("zone-top", MatchdayPageModels.zone(League.BUNDESLIGA_1, 4));
        assertEquals("", MatchdayPageModels.zone(League.BUNDESLIGA_1, 10));
        assertEquals("zone-bottom-playoff", MatchdayPageModels.zone(League.BUNDESLIGA_1, 16));
        assertEquals("zone-bottom", MatchdayPageModels.zone(League.BUNDESLIGA_1, 18));
    }

    @Test
    void zoneColorsBundesliga2sPromotionAndRelegationSpots() {
        assertEquals("zone-top", MatchdayPageModels.zone(League.BUNDESLIGA_2, 2));
        assertEquals("zone-top-playoff", MatchdayPageModels.zone(League.BUNDESLIGA_2, 3));
        assertEquals("zone-bottom-playoff", MatchdayPageModels.zone(League.BUNDESLIGA_2, 16));
        assertEquals("zone-bottom", MatchdayPageModels.zone(League.BUNDESLIGA_2, 17));
    }

    @Test
    void zoneColorsTheChampionsLeaguePhaseInThirds() {
        assertEquals("zone-top", MatchdayPageModels.zone(League.CHAMPIONS_LEAGUE, 8));
        assertEquals("zone-top-playoff", MatchdayPageModels.zone(League.CHAMPIONS_LEAGUE, 24));
        assertEquals("zone-bottom", MatchdayPageModels.zone(League.CHAMPIONS_LEAGUE, 25));
    }
```

Add the matching imports if not already present (`League`, `assertEquals`).

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./mvnw test -Dtest=MatchdayPageModelsTest`
Expected: PASS already (old implementation) — these are characterization tests, so this step just confirms they pass *before* the refactor. Skip to Step 3, then re-run in Step 4 to prove the refactor didn't change behaviour.

- [ ] **Step 3: Refactor the implementation**

Replace lines 144-156 of `MatchdayPageModels.java`:

```java
    static String zone(League league, int position) {
        List<PlacementGoal> goals = PlacementGoal.forPosition(league, position);
        if (goals.contains(PlacementGoal.KNOCKOUT_DIRECT) || goals.contains(PlacementGoal.EUROPE) || goals.contains(PlacementGoal.PROMOTION)) {
            return "zone-top";
        }
        if (goals.contains(PlacementGoal.KNOCKOUT_PLAYOFF) || goals.contains(PlacementGoal.PROMOTION_PLAYOFF)) {
            return "zone-top-playoff";
        }
        if (goals.contains(PlacementGoal.RELEGATION_PLAYOFF)) {
            return "zone-bottom-playoff";
        }
        if (goals.contains(PlacementGoal.RELEGATION) || goals.contains(PlacementGoal.ELIMINATION)) {
            return "zone-bottom";
        }
        return "";
    }
```

Add `import de.javamark.matchoracle.matchday.entity.PlacementGoal;` (and confirm `java.util.List` is already imported — it is, `MatchdayPageModels` uses `List` extensively).

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./mvnw test -Dtest=MatchdayPageModelsTest`
Expected: PASS — same assertions, now exercising the refactored code.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/de/javamark/matchoracle/matchday/boundary/MatchdayPageModels.java src/test/java/de/javamark/matchoracle/matchday/boundary/MatchdayPageModelsTest.java
git commit -m "refactor(matchday): derive zone() from PlacementGoal instead of its own copy"
```

---

## Task 3: `Match.findUnplayed` — the remaining-fixtures finder

**Files:**
- Modify: `src/main/java/de/javamark/matchoracle/matchday/entity/Match.java`

**Interfaces:**
- Consumes: nothing new
- Produces: `static List<Match> findUnplayed(League league, int season)` — used by Task 4

- [ ] **Step 1: Add the finder**

Add next to the other `find*` static methods in `Match.java` (near `findPlayedBefore`):

```java
    /** Unplayed matches of a season — the season outlook's remaining-fixtures input (spec 04). */
    public static List<Match> findUnplayed(League league, int season) {
        return list("matchday.league = ?1 and matchday.season = ?2 and fullTimeScore is null", league, season);
    }
```

No dedicated test: this is a plain Panache query, the same kind as `findPlayedBefore`/`findByTeam` right above and below it, none of which have direct tests in this codebase (they need a database; the project's convention — see `StandingsCalculatorTest`, `ReviewServiceTest` — is to unit-test the pure calculation that consumes the query result, not the query itself).

- [ ] **Step 2: Compile**

Run: `./mvnw compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/de/javamark/matchoracle/matchday/entity/Match.java
git commit -m "feat(matchday): add Match.findUnplayed for the season outlook's remaining fixtures"
```

---

## Task 4: `MatchdayFacade` additions for `season`

**Files:**
- Modify: `src/main/java/de/javamark/matchoracle/matchday/boundary/MatchdayFacade.java`
- Modify: `src/test/java/de/javamark/matchoracle/ArchitectureTest.java:25`

**Interfaces:**
- Consumes: `PlacementGoal` (Task 1), `Match.findUnplayed` (Task 3), existing `standingsCalculator`, `formCalculator`, `poissonScoreModel` fields already injected in `MatchdayFacade`
- Produces (all `public`, on `MatchdayFacade`, used by Task 7's `SeasonOutlookService`):
  - `record LeagueSeasonRef(String league, int season)`
  - `record Fixture(long homeTeamId, long awayTeamId)`
  - `record TeamState(long teamId, int points, int goalDifference, int goalsFor, MatchSituation.Balance homeRecord, MatchSituation.Balance awayRecord)`
  - `Optional<LeagueSeasonRef> matchdayJustCompleted(long matchId)`
  - `List<TeamState> currentSeasonState(String league)`
  - `List<Fixture> remainingFixtures(String league)`
  - `ScorelineForecast scorelineForecast(MatchSituation.Balance home, MatchSituation.Balance away)` (new overload alongside the two existing `scorelineForecast` overloads)
  - `List<String> placementGoals(String league)`
  - `List<String> placementGoalsAt(String league, int position)`

This task has no isolated unit test of its own: like every other `MatchdayFacade` method (`currentSeason`, `leagueShortcut`, `finalMatchIds`, …), these are thin, database-backed delegations with no `@QuarkusTest` in this codebase (see the Global Constraints note on testing). They are exercised indirectly once Task 7's `SeasonOutlookService` is wired up and run against the running application.

- [ ] **Step 1: Register `season` with ArchUnit**

In `ArchitectureTest.java:25`, change:

```java
    static final List<String> FEATURES = List.of("matchday", "forecast", "review");
```

to:

```java
    static final List<String> FEATURES = List.of("matchday", "forecast", "review", "season");
```

Without this, `season.control` depending on `matchday.boundary.MatchdayFacade` (added by this task's consumers in Task 7) fails `bce_layers`, because that dependency isn't in the ignore-list yet.

- [ ] **Step 2: Add the new records and methods to `MatchdayFacade`**

Add near the top of the class, alongside the existing inner usage of `MatchSituation.*` types (the file already has `import de.javamark.matchoracle.matchday.boundary.MatchSituation.Balance;` — reuse that `Balance` alias):

```java
    public record LeagueSeasonRef(String league, int season) {
    }

    public record Fixture(long homeTeamId, long awayTeamId) {
    }

    public record TeamState(long teamId, int points, int goalDifference, int goalsFor, Balance homeRecord, Balance awayRecord) {
    }
```

Add these methods (anywhere among the other `@Transactional(SUPPORTS)` read methods):

```java
    /** Present with the league/season if this result was its matchday's last unplayed match (spec 04's "Spieltag beendet" trigger); empty otherwise. */
    @Transactional(Transactional.TxType.SUPPORTS)
    public Optional<LeagueSeasonRef> matchdayJustCompleted(long matchId) {
        return Match.<Match>findByIdOptional(matchId)
                .filter(m -> Match.findByMatchday(m.matchday).stream().allMatch(mm -> mm.resultStatus == ResultStatus.FINAL))
                .map(m -> new LeagueSeasonRef(m.matchday.league.sourceShortcut(), m.matchday.season));
    }

    /** Every team of a league's current season with its table state and home/away scoring record — the season outlook's starting point (spec 04). */
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<TeamState> currentSeasonState(String league) {
        Optional<League> l = League.bySourceShortcut(league);
        if (l.isEmpty()) {
            return List.of();
        }
        Optional<Matchday> current = Matchday.findCurrent(l.get());
        if (current.isEmpty()) {
            return List.of();
        }
        Standings standings = standingsCalculator.standingsBefore(current.get());
        List<TeamState> states = new java.util.ArrayList<>();
        for (StandingPosition p : standings.positions()) {
            Form form = formCalculator.formBefore(p.team(), current.get());
            states.add(new TeamState(p.team().id, p.balance().points(), p.balance().goalDifference(), p.balance().goalsFor(),
                    balance(form.home()), balance(form.away())));
        }
        return states;
    }

    /** Unplayed fixtures of a league's current season — the season outlook's remaining-season input (spec 04). */
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<Fixture> remainingFixtures(String league) {
        Optional<League> l = League.bySourceShortcut(league);
        if (l.isEmpty()) {
            return List.of();
        }
        Optional<Integer> season = Matchday.latestSeason(l.get());
        if (season.isEmpty()) {
            return List.of();
        }
        return Match.findUnplayed(l.get(), season.get()).stream().map(m -> new Fixture(m.homeTeam.id, m.awayTeam.id)).toList();
    }

    /** Same Poisson estimate as the other overloads, from two teams' current home/away records (spec 04, independent of any KI-Vorschau). */
    public ScorelineForecast scorelineForecast(Balance home, Balance away) {
        return scorelineForecast(poissonScoreModel.forecast(entityBalance(home), entityBalance(away)));
    }

    /** Every placement-goal label a league's table can produce (spec 04). */
    public List<String> placementGoals(String league) {
        return League.bySourceShortcut(league)
                .map(l -> PlacementGoal.all(l).stream().map(PlacementGoal::label).toList())
                .orElse(List.of());
    }

    /** The placement-goal labels a final table position satisfies for a league (spec 04). */
    public List<String> placementGoalsAt(String league, int position) {
        return League.bySourceShortcut(league)
                .map(l -> PlacementGoal.forPosition(l, position).stream().map(PlacementGoal::label).toList())
                .orElse(List.of());
    }
```

Add two new imports: `de.javamark.matchoracle.matchday.entity.PlacementGoal` and `de.javamark.matchoracle.matchday.entity.StandingPosition` (neither is currently imported in this file — check with `grep -n "^import" MatchdayFacade.java` first; `Form`, `Matchday`, `Standings` already are).

- [ ] **Step 3: Compile and run the existing architecture and matchday test suites**

Run: `./mvnw test -Dtest=ArchitectureTest,MatchdayPageModelsTest,PlacementGoalTest`
Expected: PASS (the `season` addition to `FEATURES` doesn't affect these tests yet, but confirms nothing broke)

- [ ] **Step 4: Commit**

```bash
git add src/main/java/de/javamark/matchoracle/matchday/boundary/MatchdayFacade.java src/test/java/de/javamark/matchoracle/ArchitectureTest.java
git commit -m "feat(matchday): expose season-state, remaining fixtures and placement goals for spec 04"
```

---

## Task 5: `SeasonSimulator` — the Monte Carlo core

**Files:**
- Create: `src/main/java/de/javamark/matchoracle/season/control/SeasonSimulator.java`
- Test: `src/test/java/de/javamark/matchoracle/season/control/SeasonSimulatorTest.java`

**Interfaces:**
- Consumes: nothing outside `java.util.*` — deliberately has no dependency on `matchday` or CDI beans, so it is plain-JUnit-testable like `StandingsCalculator`'s pure core
- Produces: `SeasonSimulator` (CDI `@ApplicationScoped`, but constructible with `new SeasonSimulator()` for tests) with:
  - `record TeamState(long teamId, int points, int goalDifference, int goalsFor)`
  - `record Fixture(long homeTeamId, long awayTeamId, double expectedHomeGoals, double expectedAwayGoals)`
  - `record Outcome(long teamId, Map<String, Double> probabilities)`
  - `List<Outcome> simulate(List<TeamState> teams, List<Fixture> fixtures, List<String> allGoals, IntFunction<List<String>> goalsAtPosition, Random random)`
  - `static int poisson(double lambda, Random random)` (package-visible, used directly by one test)
  - Used by Task 7's `SeasonOutlookService`

- [ ] **Step 1: Write the failing tests**

```java
package de.javamark.matchoracle.season.control;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Spec 04: remaining-season simulation, one Poisson-drawn scoreline per fixture per run. */
class SeasonSimulatorTest {

    private final SeasonSimulator simulator = new SeasonSimulator();

    @Test
    void withNoRemainingFixturesTheCurrentTableDecidesWithCertainty() {
        List<SeasonSimulator.TeamState> teams = List.of(
                new SeasonSimulator.TeamState(1L, 30, 20, 40),
                new SeasonSimulator.TeamState(2L, 20, 0, 25));

        List<SeasonSimulator.Outcome> outcomes = simulator.simulate(teams, List.of(), List.of("Meisterschaft"),
                position -> position == 1 ? List.of("Meisterschaft") : List.of(), new Random(1));

        Map<Long, Double> byTeam = outcomes.stream()
                .collect(Collectors.toMap(SeasonSimulator.Outcome::teamId, o -> o.probabilities().get("Meisterschaft")));
        assertEquals(1.0, byTeam.get(1L), 1e-9);
        assertEquals(0.0, byTeam.get(2L), 1e-9);
    }

    @Test
    void aBigLeadWithFewFixturesLeftGivesAHighTitleProbability() {
        List<SeasonSimulator.TeamState> teams = List.of(
                new SeasonSimulator.TeamState(1L, 60, 40, 70),
                new SeasonSimulator.TeamState(2L, 30, 0, 30));
        List<SeasonSimulator.Fixture> fixtures = List.of(new SeasonSimulator.Fixture(1L, 2L, 1.6, 1.0));

        List<SeasonSimulator.Outcome> outcomes = simulator.simulate(teams, fixtures, List.of("Meisterschaft"),
                position -> position == 1 ? List.of("Meisterschaft") : List.of(), new Random(42));

        double leaderTitleChance = outcomes.stream().filter(o -> o.teamId() == 1L).findFirst().orElseThrow()
                .probabilities().get("Meisterschaft");
        assertTrue(leaderTitleChance > 0.99, "title chance " + leaderTitleChance);
    }

    @Test
    void probabilitiesForEveryDeclaredGoalArePresentEvenAtZero() {
        List<SeasonSimulator.TeamState> teams = List.of(new SeasonSimulator.TeamState(1L, 0, 0, 0));

        List<SeasonSimulator.Outcome> outcomes = simulator.simulate(teams, List.of(), List.of("Meisterschaft", "Abstieg"),
                position -> List.of("Meisterschaft"), new Random(1));

        Map<String, Double> probabilities = outcomes.get(0).probabilities();
        assertEquals(1.0, probabilities.get("Meisterschaft"), 1e-9);
        assertEquals(0.0, probabilities.get("Abstieg"), 1e-9);
    }

    @Test
    void poissonSamplesAverageCloseToTheirExpectedValue() {
        Random random = new Random(7);
        long sum = 0;
        int samples = 50_000;
        for (int i = 0; i < samples; i++) {
            sum += SeasonSimulator.poisson(2.3, random);
        }
        double average = sum / (double) samples;
        assertTrue(Math.abs(average - 2.3) < 0.05, "average " + average);
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./mvnw test -Dtest=SeasonSimulatorTest`
Expected: FAIL — compile error, `SeasonSimulator` does not exist yet.

- [ ] **Step 3: Write the implementation**

```java
package de.javamark.matchoracle.season.control;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.IntFunction;

/**
 * Spec 04: Monte Carlo simulation of the remaining season. Each run draws one scoreline per
 * remaining fixture — independent home/away goal counts, same Poisson assumption the KI-Vorschau
 * uses — applies it to a running table, and records which placement goal each team's final
 * position satisfies. Averaged over many runs, that gives each goal's probability.
 */
@ApplicationScoped
public class SeasonSimulator {

    public static final int RUNS = 10_000;

    public record TeamState(long teamId, int points, int goalDifference, int goalsFor) {
    }

    public record Fixture(long homeTeamId, long awayTeamId, double expectedHomeGoals, double expectedAwayGoals) {
    }

    public record Outcome(long teamId, Map<String, Double> probabilities) {
    }

    public List<Outcome> simulate(List<TeamState> teams, List<Fixture> fixtures, List<String> allGoals,
                                   IntFunction<List<String>> goalsAtPosition, Random random) {
        Map<Long, Map<String, Integer>> hits = new HashMap<>();
        for (TeamState team : teams) {
            Map<String, Integer> zeros = new HashMap<>();
            for (String goal : allGoals) {
                zeros.put(goal, 0);
            }
            hits.put(team.teamId(), zeros);
        }

        // With no remaining fixtures every run produces the same table — one run is exact, not an approximation.
        int runs = fixtures.isEmpty() ? 1 : RUNS;
        for (int run = 0; run < runs; run++) {
            Map<Long, long[]> table = new HashMap<>();
            for (TeamState team : teams) {
                table.put(team.teamId(), new long[]{team.teamId(), team.points(), team.goalDifference(), team.goalsFor()});
            }
            for (Fixture fixture : fixtures) {
                int homeGoals = poisson(fixture.expectedHomeGoals(), random);
                int awayGoals = poisson(fixture.expectedAwayGoals(), random);
                credit(table.get(fixture.homeTeamId()), homeGoals, awayGoals);
                credit(table.get(fixture.awayTeamId()), awayGoals, homeGoals);
            }
            List<long[]> ranked = new ArrayList<>(table.values());
            ranked.sort(Comparator.<long[]>comparingLong(row -> row[1])
                    .thenComparingLong(row -> row[2])
                    .thenComparingLong(row -> row[3])
                    .reversed());
            for (int i = 0; i < ranked.size(); i++) {
                long teamId = ranked.get(i)[0];
                for (String goal : goalsAtPosition.apply(i + 1)) {
                    hits.get(teamId).merge(goal, 1, Integer::sum);
                }
            }
        }

        List<Outcome> outcomes = new ArrayList<>();
        for (TeamState team : teams) {
            Map<String, Double> probabilities = new HashMap<>();
            for (String goal : allGoals) {
                probabilities.put(goal, hits.get(team.teamId()).get(goal) / (double) runs);
            }
            outcomes.add(new Outcome(team.teamId(), probabilities));
        }
        return outcomes;
    }

    private static void credit(long[] row, int goalsFor, int goalsAgainst) {
        row[1] += goalsFor > goalsAgainst ? 3 : goalsFor == goalsAgainst ? 1 : 0;
        row[2] += goalsFor - goalsAgainst;
        row[3] += goalsFor;
    }

    /** Knuth's algorithm: draws a Poisson(lambda)-distributed goal count. */
    static int poisson(double lambda, Random random) {
        double threshold = Math.exp(-lambda);
        int k = 0;
        double product = 1.0;
        do {
            k++;
            product *= random.nextDouble();
        } while (product > threshold);
        return k - 1;
    }
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./mvnw test -Dtest=SeasonSimulatorTest`
Expected: PASS (4 tests)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/de/javamark/matchoracle/season/control/SeasonSimulator.java src/test/java/de/javamark/matchoracle/season/control/SeasonSimulatorTest.java
git commit -m "feat(season): add SeasonSimulator, the Monte Carlo remaining-season engine"
```

---

## Task 6: `SeasonOutlook` entity + migration

**Files:**
- Create: `src/main/java/de/javamark/matchoracle/season/entity/SeasonOutlook.java`
- Create: `src/main/resources/db/migration/V10__season_outlook_schema.sql`

**Interfaces:**
- Consumes: nothing
- Produces: `SeasonOutlook` Panache entity with `league`, `season`, `teamId`, `computedAt`, `probabilities` (`Map<String, Double>`), and static finders `findByLeagueSeason`, `findByTeam`, `deleteByLeagueSeason` — used by Task 7 and Task 9

- [ ] **Step 1: Write the migration**

```sql
-- Spec 04: season outlook. A snapshot per team, replaced wholesale after each completed
-- matchday of its league — no history, see spec 04's rules.

create sequence season_outlook_seq start with 1 increment by 50;

create table season_outlook (
    id          bigint      not null primary key,
    league      varchar(20) not null,
    season      integer     not null,
    team_id     bigint      not null,
    computed_at timestamptz not null
);
create unique index season_outlook_team_idx on season_outlook (league, season, team_id);

create table season_outlook_probability (
    season_outlook_id bigint           not null references season_outlook(id),
    placement_goal     varchar(50)      not null,
    probability         double precision not null,
    primary key (season_outlook_id, placement_goal)
);
```

- [ ] **Step 2: Write the entity**

```java
package de.javamark.matchoracle.season.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A team's current season outlook (spec 04) — replaced wholesale on every recomputation,
 * never updated in place, no history kept.
 */
@Entity
public class SeasonOutlook extends PanacheEntity {

    @Column(nullable = false, length = 20)
    public String league;

    @Column(nullable = false)
    public int season;

    @Column(nullable = false)
    public long teamId;

    @Column(nullable = false)
    public Instant computedAt;

    /** Placement-goal label (e.g. "Meisterschaft") to its probability, 0..1. */
    @ElementCollection
    @CollectionTable(name = "season_outlook_probability", joinColumns = @JoinColumn(name = "season_outlook_id"))
    @MapKeyColumn(name = "placement_goal")
    @Column(name = "probability", nullable = false)
    public Map<String, Double> probabilities = new HashMap<>();

    public static List<SeasonOutlook> findByLeagueSeason(String league, int season) {
        return list("league = ?1 and season = ?2", league, season);
    }

    public static Optional<SeasonOutlook> findByTeam(String league, int season, long teamId) {
        return find("league = ?1 and season = ?2 and teamId = ?3", league, season, teamId).firstResultOptional();
    }

    public static void deleteByLeagueSeason(String league, int season) {
        delete("league = ?1 and season = ?2", league, season);
    }
}
```

No dedicated test: a plain Panache entity with simple queries, same category as `Team`/`Match`'s finders (untested database wiring, per the Global Constraints note).

- [ ] **Step 3: Compile**

Run: `./mvnw compile`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/de/javamark/matchoracle/season/entity/SeasonOutlook.java src/main/resources/db/migration/V10__season_outlook_schema.sql
git commit -m "feat(season): add SeasonOutlook entity and schema migration"
```

---

## Task 7: `SeasonOutlookService` — recompute and replace

**Files:**
- Create: `src/main/java/de/javamark/matchoracle/season/control/SeasonOutlookService.java`

**Interfaces:**
- Consumes: `MatchdayFacade.currentSeasonState`, `.remainingFixtures`, `.placementGoals`, `.placementGoalsAt`, `.scorelineForecast(Balance, Balance)` (Task 4); `SeasonSimulator.simulate` (Task 5); `SeasonOutlook.deleteByLeagueSeason` (Task 6)
- Produces: `SeasonOutlookService` (CDI `@ApplicationScoped`) with `public void recompute(String league, int season)` — used by Task 8's `SeasonOutlookObserver`

No dedicated test: this is orchestration (facade calls, delegate to the already-tested `SeasonSimulator`, persist) — the same category as `ForecastService`'s scheduling glue, which also has no direct test in this codebase; its only non-trivial logic (the simulation itself) is already covered by `SeasonSimulatorTest`.

- [ ] **Step 1: Write the implementation**

```java
package de.javamark.matchoracle.season.control;

import de.javamark.matchoracle.matchday.boundary.MatchSituation;
import de.javamark.matchoracle.matchday.boundary.MatchdayFacade;
import de.javamark.matchoracle.matchday.boundary.ScorelineForecast;
import de.javamark.matchoracle.season.entity.SeasonOutlook;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Spec 04, step 4: recomputes and fully replaces a league-season's outlook once its matchday is done. */
@ApplicationScoped
public class SeasonOutlookService {

    @Inject
    MatchdayFacade matchday;

    @Inject
    SeasonSimulator simulator;

    public void recompute(String league, int season) {
        recompute(league, season, new Random());
    }

    @Transactional
    void recompute(String league, int season, Random random) {
        List<MatchdayFacade.TeamState> teams = matchday.currentSeasonState(league);
        List<String> allGoals = matchday.placementGoals(league);
        if (teams.isEmpty() || allGoals.isEmpty()) {
            return;
        }
        Map<Long, MatchdayFacade.TeamState> byId = teams.stream()
                .collect(java.util.stream.Collectors.toMap(MatchdayFacade.TeamState::teamId, t -> t));

        List<SeasonSimulator.Fixture> fixtures = matchday.remainingFixtures(league).stream()
                .map(f -> toSimFixture(f, byId))
                .toList();
        List<SeasonSimulator.TeamState> simTeams = teams.stream()
                .map(t -> new SeasonSimulator.TeamState(t.teamId(), t.points(), t.goalDifference(), t.goalsFor()))
                .toList();

        List<SeasonSimulator.Outcome> outcomes = simulator.simulate(simTeams, fixtures, allGoals,
                position -> matchday.placementGoalsAt(league, position), random);

        SeasonOutlook.deleteByLeagueSeason(league, season);
        Instant now = Instant.now();
        for (SeasonSimulator.Outcome outcome : outcomes) {
            SeasonOutlook row = new SeasonOutlook();
            row.league = league;
            row.season = season;
            row.teamId = outcome.teamId();
            row.computedAt = now;
            row.probabilities.putAll(outcome.probabilities());
            row.persist();
        }
    }

    private SeasonSimulator.Fixture toSimFixture(MatchdayFacade.Fixture fixture, Map<Long, MatchdayFacade.TeamState> byId) {
        MatchSituation.Balance homeRecord = byId.get(fixture.homeTeamId()).homeRecord();
        MatchSituation.Balance awayRecord = byId.get(fixture.awayTeamId()).awayRecord();
        ScorelineForecast forecast = matchday.scorelineForecast(homeRecord, awayRecord);
        return new SeasonSimulator.Fixture(fixture.homeTeamId(), fixture.awayTeamId(),
                forecast.expectedHomeGoals(), forecast.expectedAwayGoals());
    }
}
```

- [ ] **Step 2: Compile**

Run: `./mvnw compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/de/javamark/matchoracle/season/control/SeasonOutlookService.java
git commit -m "feat(season): add SeasonOutlookService, recompute-and-replace orchestration"
```

---

## Task 8: `SeasonOutlookObserver` — the trigger

**Files:**
- Create: `src/main/java/de/javamark/matchoracle/season/boundary/SeasonOutlookObserver.java`

**Interfaces:**
- Consumes: `de.javamark.matchoracle.matchday.boundary.ResultFinalized` (existing CDI event), `MatchdayFacade.matchdayJustCompleted` (Task 4), `SeasonOutlookService.recompute` (Task 7)
- Produces: nothing consumed elsewhere — this is the feature's entry point from `matchday`'s event

No dedicated test: mirrors `review.boundary.ResultFinalizedObserver`, which also has none — a two-line CDI wiring method with no branching logic of its own (the actual decision, "is the matchday complete", already lives in and is exercised through `MatchdayFacade`).

- [ ] **Step 1: Write the implementation**

```java
package de.javamark.matchoracle.season.boundary;

import de.javamark.matchoracle.matchday.boundary.MatchdayFacade;
import de.javamark.matchoracle.matchday.boundary.ResultFinalized;
import de.javamark.matchoracle.season.control.SeasonOutlookService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/** Spec 04, step 1: once a whole matchday is done, the affected league's outlook is recomputed. */
@ApplicationScoped
public class SeasonOutlookObserver {

    @Inject
    MatchdayFacade matchday;

    @Inject
    SeasonOutlookService outlook;

    void onResultFinalized(@Observes ResultFinalized event) {
        matchday.matchdayJustCompleted(event.matchId()).ifPresent(ref -> outlook.recompute(ref.league(), ref.season()));
    }
}
```

- [ ] **Step 2: Compile and run the architecture tests**

Run: `./mvnw test -Dtest=ArchitectureTest`
Expected: PASS — the `bce_layers` layered-architecture rule flags a dependency into a `..boundary..` package from any other layer unless the two features are on the `ignoreDependency` list, and that applies to every layer combination (boundary-to-boundary included), not just control/entity. `season`'s membership in `FEATURES` (added in Task 4) already covers this dependency in both directions.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/de/javamark/matchoracle/season/boundary/SeasonOutlookObserver.java
git commit -m "feat(season): observe ResultFinalized and recompute once a matchday is complete"
```

---

## Task 9: `SeasonOutlookPages` — the display fragment

**Files:**
- Create: `src/main/java/de/javamark/matchoracle/season/boundary/SeasonOutlookPages.java`
- Create: `src/main/resources/templates/SeasonOutlookPages/outlook.html`

**Interfaces:**
- Consumes: `SeasonOutlook.findByTeam` (Task 6), `MatchdayFacade.placementGoals` (Task 4)
- Produces: `GET /{league}/{season}/teams/{id}/outlook` → HTML fragment — consumed by Task 10's `team.html` via htmx

- [ ] **Step 1: Write the resource + template**

```java
package de.javamark.matchoracle.season.boundary;

import de.javamark.matchoracle.matchday.boundary.MatchdayFacade;
import de.javamark.matchoracle.season.entity.SeasonOutlook;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.Optional;

/** Spec 04, step 5: the fragment the club page (matchday) loads via htmx — matchday never touches this feature's entities. */
@Path("/")
public class SeasonOutlookPages {

    @Inject
    MatchdayFacade matchday;

    @CheckedTemplate
    static class Templates {
        static native TemplateInstance outlook(OutlookFragment fragment);
    }

    public record GoalRow(String label, int percent) {
    }

    public record OutlookFragment(boolean available, List<GoalRow> goals) {
    }

    @GET
    @Path("/{league}/{season}/teams/{id}/outlook")
    @Produces(MediaType.TEXT_HTML)
    @Transactional(Transactional.TxType.SUPPORTS)
    public TemplateInstance outlook(@PathParam("league") String league, @PathParam("season") int season, @PathParam("id") long id) {
        Optional<SeasonOutlook> outlook = SeasonOutlook.findByTeam(league, season, id);
        List<GoalRow> rows = outlook.map(o -> matchday.placementGoals(league).stream()
                .map(label -> new GoalRow(label, (int) Math.round(o.probabilities.getOrDefault(label, 0.0) * 100)))
                .toList()).orElse(List.of());
        return Templates.outlook(new OutlookFragment(outlook.isPresent(), rows));
    }
}
```

```html
{#if fragment.available}
<ul class="outlook-goals">
  {#for g in fragment.goals}
  <li><span class="label">{g.label}</span><span class="percent">{g.percent} %</span></li>
  {/for}
</ul>
{#else}
<p class="muted">Noch keine Saisonaussicht — erscheint nach dem nächsten beendeten Spieltag.</p>
{/if}
```

No dedicated test: like `ForecastPages`' fragment endpoints, this is a thin rendering wrapper with no branching business logic of its own (the "available or not" decision is just "did a row exist", already covered indirectly through `SeasonOutlookService`/`SeasonSimulator`).

- [ ] **Step 2: Compile**

Run: `./mvnw compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/de/javamark/matchoracle/season/boundary/SeasonOutlookPages.java src/main/resources/templates/SeasonOutlookPages/outlook.html
git commit -m "feat(season): serve the season-outlook fragment for the club page"
```

---

## Task 10: Wire the fragment into the club page

**Files:**
- Modify: `src/main/java/de/javamark/matchoracle/matchday/boundary/MatchdayPageModels.java:277-280` (`TeamPage` record)
- Modify: `src/main/java/de/javamark/matchoracle/matchday/boundary/MatchdayPages.java:288` (`teamPage()` return statement)
- Modify: `src/main/resources/templates/MatchdayPages/team.html`

**Interfaces:**
- Consumes: nothing new in Java — `outlookLink` is a plain `String` built from values already in scope (`shortcut`, `season`, `team.id`), exactly like the existing `forecastLink`/`markersPath` fields elsewhere in this file. `matchday` still has zero Java dependency on `season`.
- Produces: `TeamPage.outlookLink` — read only by `team.html`

- [ ] **Step 1: Add the field to `TeamPage`**

In `MatchdayPageModels.java`, change:

```java
    record TeamPage(Nav nav, String season, int seasonYear, boolean currentSeason, String name, String icon,
                    Integer position, String zone, BalanceRow total, boolean standingsProvisional,
                    List<SeasonChip> otherSeasons, List<ScheduleRow> schedule, BalanceRow home, BalanceRow away,
                    List<ScorerRow> scorers, String goalTimingJson, String chartJson, DataInfo data) {
```

to:

```java
    record TeamPage(Nav nav, String season, int seasonYear, boolean currentSeason, String name, String icon,
                    Integer position, String zone, BalanceRow total, boolean standingsProvisional,
                    List<SeasonChip> otherSeasons, List<ScheduleRow> schedule, BalanceRow home, BalanceRow away,
                    List<ScorerRow> scorers, String goalTimingJson, String chartJson, String outlookLink, DataInfo data) {
```

- [ ] **Step 2: Pass the link from `teamPage()`**

In `MatchdayPages.java`, the return statement currently ends:

```java
                scorerCalculator.scorersFor(team, league, season).stream().map(ScorerRow::of).toList(),
                MatchdayPageModels.goalTimingJson(goalTimingCalculator.timingFor(team, league, season)),
                positionChartJson,
                DataInfo.of(reference, Instant.now()));
```

Change to:

```java
                scorerCalculator.scorersFor(team, league, season).stream().map(ScorerRow::of).toList(),
                MatchdayPageModels.goalTimingJson(goalTimingCalculator.timingFor(team, league, season)),
                positionChartJson,
                "/" + shortcut + "/" + season + "/teams/" + team.id + "/outlook",
                DataInfo.of(reference, Instant.now()));
```

- [ ] **Step 3: Add the section to `team.html`**

In `team.html`, inside `<aside class="club-side">`, add a new `.facts-block` (after the existing "Bilanz" block, before "Torschützen" — or wherever reads best next to it):

```html
      <div class="facts-block" hx-get="{page.outlookLink}" hx-trigger="load" hx-swap="innerHTML">
        <h2>Saisonaussicht</h2>
        <p class="muted">Wird geladen …</p>
      </div>
```

- [ ] **Step 4: Compile and run the matchday test suite**

Run: `./mvnw test -Dtest=MatchdayPageModelsTest,MatchdayPagesTest`
Expected: PASS — check whether `MatchdayPagesTest` constructs a `TeamPage` directly; if so, add the new `outlookLink` argument there too so it still compiles.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/de/javamark/matchoracle/matchday/boundary/MatchdayPageModels.java src/main/java/de/javamark/matchoracle/matchday/boundary/MatchdayPages.java src/main/resources/templates/MatchdayPages/team.html
git commit -m "feat(matchday): show the season outlook on the club page"
```

---

## Task 11: Full verification pass

**Files:** none (verification only)

- [ ] **Step 1: Run the full test suite**

Run: `./mvnw test`
Expected: BUILD SUCCESS, all tests pass — including `ArchitectureTest` (season now fully wired both directions: `season → matchday.boundary`, `matchday.boundary → season` via link string only, no illegal entity/control leakage).

**Caveat for whoever runs this:** the user typically has `./mvnw quarkus:dev` running against a Docker Compose–managed Postgres. `./mvnw test` does not touch that database (this codebase has no `@QuarkusTest`s), but confirm before running any Quarkus-dev-services-backed command that it won't collide with an already-running dev-mode instance.

- [ ] **Step 2: Manually smoke-test in dev mode**

Ask the user to check (or check yourself if you already have dev mode access): open a club page (`/bl1/{season}/teams/{id}` for a team of the current season) and confirm the "Saisonaussicht" block loads, either showing goal percentages or the "Noch keine Saisonaussicht" note. A recomputation only happens after a matchday finishes, so on a fresh dev database the note is the expected state until then.

---

## Self-Review Notes

- **Spec coverage:** every rule in `spec-04-saisonaussicht.md` maps to a task — per-league placement goals (Task 1), simulation independent of KI-Vorschau (Task 5, uses `PoissonScoreModel` only, never `forecast`), recompute after every finished matchday including Champions League (Task 4's `matchdayJustCompleted` works for any `League`), full replacement with no history (Task 6/7's delete-then-insert), viewer sees it per team (Task 9/10), no accuracy-report integration (nothing in this plan touches `review`).
- **Placeholder scan:** none — every step has real, complete code.
- **Type consistency:** `MatchdayFacade.TeamState`/`Fixture`/`LeagueSeasonRef` (Task 4) are consumed with matching field names in Task 7; `SeasonSimulator.TeamState`/`Fixture`/`Outcome` (Task 5) match their construction in Task 7; `SeasonOutlook.probabilities` (Task 6) matches the `Map<String, Double>` produced by `SeasonSimulator.Outcome` and read by Task 9.
