package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.entity.CupFunnel;
import de.javamark.matchoracle.matchday.entity.Decision;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.Score;
import de.javamark.matchoracle.matchday.entity.Team;
import de.javamark.matchoracle.matchday.entity.Tier;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.javamark.matchoracle.matchday.control.MatchFixtures.played;
import static de.javamark.matchoracle.matchday.control.MatchFixtures.team;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Spec 06: der Trichter zeigt, wie sich das Feld von Runde zu Runde ausdünnt und wer
 * übrig bleibt — die Frage, die eine Tabelle nie beantwortet: War das ein Jahrgang der
 * Außenseiter?
 */
class CupFunnelCalculatorTest {

    private final Team first = team("Erstligist");
    private final Team second = team("Zweitligist");
    private final Team third = team("Drittligist");
    private final Team amateur = team("Amateur");

    private final Map<Team, Tier> tiers = new HashMap<>(Map.of(
            first, Tier.FIRST, second, Tier.SECOND, third, Tier.THIRD, amateur, Tier.LOWER));

    @Test
    void everyRoundCountsTheClubsThatPlayedInIt() {
        CupFunnel funnel = CupFunnelCalculator.of(List.of(
                new CupFunnelCalculator.RoundInput("1. Runde", List.of(
                        played(1, amateur, 1, first, 0),
                        played(1, third, 0, second, 2))),
                new CupFunnelCalculator.RoundInput("Endspiel", List.of(
                        played(2, amateur, 0, second, 3)))), tiers);

        assertEquals(List.of("1. Runde", "Endspiel"), funnel.rounds().stream().map(CupFunnel.Round::name).toList());
        assertEquals(4, funnel.rounds().get(0).clubs());
        assertEquals(2, funnel.rounds().get(1).clubs());
    }

    @Test
    void aRoundBreaksItsFieldDownByDivision() {
        CupFunnel funnel = CupFunnelCalculator.of(List.of(
                new CupFunnelCalculator.RoundInput("1. Runde", List.of(
                        played(1, amateur, 1, first, 0),
                        played(1, third, 0, second, 2)))), tiers);

        CupFunnel.Round round = funnel.rounds().get(0);
        assertEquals(1, round.byTier().get(Tier.FIRST));
        assertEquals(1, round.byTier().get(Tier.SECOND));
        assertEquals(1, round.byTier().get(Tier.THIRD));
        assertEquals(1, round.byTier().get(Tier.LOWER));
    }

    @Test
    void aClubOutsideEveryDivisionListCountsAsAmateur() {
        Team unknown = team("Unbekannt");

        CupFunnel funnel = CupFunnelCalculator.of(List.of(
                new CupFunnelCalculator.RoundInput("1. Runde", List.of(played(1, unknown, 1, first, 0)))), tiers);

        assertEquals(1, funnel.rounds().get(0).byTier().get(Tier.LOWER));
    }

    @Test
    void theEditionsFiguresCountGoalsShootoutsAndExtraTime() {
        Match shootout = played(1, amateur, 1, first, 1);
        shootout.decision = Decision.PENALTIES;
        shootout.penaltyScore = new Score(5, 4);
        Match extraTime = played(1, third, 2, second, 1);
        extraTime.decision = Decision.EXTRA_TIME;

        CupFunnel funnel = CupFunnelCalculator.of(List.of(
                new CupFunnelCalculator.RoundInput("1. Runde", List.of(shootout, extraTime))), tiers);

        assertEquals(2, funnel.figures().matches());
        assertEquals(5, funnel.figures().goals());
        assertEquals(1, funnel.figures().shootouts());
        assertEquals(1, funnel.figures().extraTime());
    }

    @Test
    void theEditionsFiguresCountTheUpsets() {
        // Amateur schlaegt Erstligist, Drittligist verliert gegen Zweitligist
        CupFunnel funnel = CupFunnelCalculator.of(List.of(
                new CupFunnelCalculator.RoundInput("1. Runde", List.of(
                        played(1, amateur, 1, first, 0),
                        played(1, third, 0, second, 2)))), tiers);

        assertEquals(1, funnel.figures().upsets());
    }

    @Test
    void anUnplayedRoundContributesNoFigures() {
        Match open = played(1, amateur, 0, first, 0);
        open.fullTimeScore = null;

        CupFunnel funnel = CupFunnelCalculator.of(List.of(
                new CupFunnelCalculator.RoundInput("1. Runde", List.of(open))), tiers);

        assertEquals(0, funnel.figures().matches());
        assertEquals(2, funnel.rounds().get(0).clubs(), "angesetzt ist die Begegnung trotzdem");
    }
}
