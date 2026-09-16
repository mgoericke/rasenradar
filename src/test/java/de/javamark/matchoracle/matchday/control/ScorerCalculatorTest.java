package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.entity.LeagueScorer;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.Scorer;
import de.javamark.matchoracle.matchday.entity.Team;
import org.junit.jupiter.api.Test;

import java.util.List;

import static de.javamark.matchoracle.matchday.control.MatchFixtures.goal;
import static de.javamark.matchoracle.matchday.control.MatchFixtures.played;
import static de.javamark.matchoracle.matchday.control.MatchFixtures.team;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Spec 01: goals with scorer are kept per match; the club page lists who scored for the club this season. */
class ScorerCalculatorTest {

    private final Team mainz = team("Mainz");
    private final Team leipzig = team("Leipzig");

    private final ScorerCalculator calculator = new ScorerCalculator();

    @Test
    void countsGoalsForTheTeamOnlyAndRanksByGoals() {
        Match home = played(1, mainz, 2, leipzig, 1);
        goal(home, "Burkardt", 1, 0);
        goal(home, "Openda", 1, 1);   // for Leipzig
        goal(home, "Amiri", 2, 1);
        Match away = played(2, leipzig, 0, mainz, 1);
        goal(away, "Amiri", 0, 1);

        List<Scorer> scorers = calculator.scorers(mainz, List.of(home, away));

        assertEquals(List.of(new Scorer("Amiri", 2, 0), new Scorer("Burkardt", 1, 0)), scorers);
    }

    @Test
    void ownGoalsOfTheOpponentCountForTheTeamButNotForTheScorer() {
        Match match = played(1, mainz, 1, leipzig, 0);
        goal(match, "Orban", 1, 0).ownGoal = true;

        assertEquals(List.of(), calculator.scorers(mainz, List.of(match)));
    }

    @Test
    void penaltiesAreCountedSeparately() {
        Match match = played(1, mainz, 2, leipzig, 0);
        goal(match, "Amiri", 1, 0).penalty = true;
        goal(match, "Amiri", 2, 0);

        assertEquals(List.of(new Scorer("Amiri", 2, 1)), calculator.scorers(mainz, List.of(match)));
    }

    @Test
    void leagueScorersRankAcrossClubsByGoals() {
        Match home = played(1, mainz, 2, leipzig, 1);
        goal(home, "Burkardt", 1, 0);
        goal(home, "Amiri", 2, 0);
        goal(home, "Openda", 2, 1);
        Match away = played(2, leipzig, 3, mainz, 1);
        goal(away, "Openda", 1, 0);
        goal(away, "Openda", 2, 0);
        goal(away, "Nkunku", 3, 0);
        goal(away, "Amiri", 3, 1);

        List<LeagueScorer> scorers = calculator.leagueScorers(List.of(home, away));

        assertEquals(List.of(
                new LeagueScorer("Openda", "Leipzig", 3, 0),
                new LeagueScorer("Amiri", "Mainz", 2, 0),
                new LeagueScorer("Burkardt", "Mainz", 1, 0),
                new LeagueScorer("Nkunku", "Leipzig", 1, 0)), scorers);
    }

    @Test
    void leagueScorersKeepScorersOfTheSameNameAtDifferentClubsSeparate() {
        Team koeln = team("Köln");
        Match match = played(1, mainz, 1, leipzig, 0);
        goal(match, "Schmidt", 1, 0);
        Match other = played(1, koeln, 1, mainz, 0);
        goal(other, "Schmidt", 1, 0);

        List<LeagueScorer> scorers = calculator.leagueScorers(List.of(match, other));

        assertEquals(List.of(new LeagueScorer("Schmidt", "Köln", 1, 0), new LeagueScorer("Schmidt", "Mainz", 1, 0)), scorers);
    }

    @Test
    void leagueScorersCreditOwnGoalsToTheBenefitingClubNotTheScorersClub() {
        Match match = played(1, mainz, 1, leipzig, 0);
        goal(match, "Orban", 1, 0).ownGoal = true;

        List<LeagueScorer> scorers = calculator.leagueScorers(List.of(match));

        assertEquals(List.of(), scorers);
    }
}
