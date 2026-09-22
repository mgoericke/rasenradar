package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.entity.CupFunnel;
import de.javamark.matchoracle.matchday.entity.Decision;
import de.javamark.matchoracle.matchday.entity.League;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.Matchday;
import de.javamark.matchoracle.matchday.entity.Team;
import de.javamark.matchoracle.matchday.entity.TeamTier;
import de.javamark.matchoracle.matchday.entity.Tier;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Spec 06: builds the funnel of a knockout edition — the field per round broken down by
 * division, plus the edition's figures. Never stored, always derived from the matches.
 */
@ApplicationScoped
public class CupFunnelCalculator {

    /** One round with its matches, as loaded by the caller. */
    public record RoundInput(String name, List<Match> matches) {
    }

    public CupFunnel funnelFor(League league, int season) {
        List<Matchday> rounds = Matchday.<Matchday>list("league = ?1 and season = ?2", league, season);
        rounds.sort(Comparator.comparingInt(r -> r.number));
        List<RoundInput> inputs = rounds.stream()
                .map(r -> new RoundInput(r.displayName(), Match.findByMatchday(r)))
                .filter(r -> !r.matches().isEmpty())
                .toList();
        Map<Team, Tier> tiers = new HashMap<>();
        for (RoundInput round : inputs) {
            for (Match m : round.matches()) {
                tiers.computeIfAbsent(m.homeTeam, t -> TeamTier.of(t, season));
                tiers.computeIfAbsent(m.awayTeam, t -> TeamTier.of(t, season));
            }
        }
        return of(inputs, tiers);
    }

    /** Pure calculation over already-loaded rounds; a club missing from {@code tiers} is an amateur. */
    static CupFunnel of(List<RoundInput> rounds, Map<Team, Tier> tiers) {
        List<CupFunnel.Round> result = new ArrayList<>();
        int matches = 0, goals = 0, shootouts = 0, extraTime = 0, upsets = 0;
        for (RoundInput round : rounds) {
            Set<Team> clubs = new HashSet<>();
            Map<Tier, Integer> byTier = new EnumMap<>(Tier.class);
            for (Tier tier : Tier.values()) {
                byTier.put(tier, 0);
            }
            for (Match m : round.matches()) {
                for (Team team : List.of(m.homeTeam, m.awayTeam)) {
                    if (clubs.add(team)) {
                        byTier.merge(tierOf(tiers, team), 1, Integer::sum);
                    }
                }
                if (!m.isPlayed()) {
                    continue;
                }
                matches++;
                goals += m.fullTimeScore.home + m.fullTimeScore.away;
                if (m.decision == Decision.PENALTIES) shootouts++;
                if (m.decision == Decision.EXTRA_TIME) extraTime++;
                if (isUpset(m, tiers)) upsets++;
            }
            result.add(new CupFunnel.Round(round.name(), clubs.size(), Map.copyOf(byTier)));
        }
        return new CupFunnel(List.copyOf(result), new CupFunnel.Figures(matches, goals, shootouts, extraTime, upsets));
    }

    private static Tier tierOf(Map<Team, Tier> tiers, Team team) {
        return tiers.getOrDefault(team, Tier.LOWER);
    }

    private static boolean isUpset(Match match, Map<Team, Tier> tiers) {
        return match.winner()
                .map(won -> {
                    Team lost = won.equals(match.homeTeam) ? match.awayTeam : match.homeTeam;
                    return TeamTier.isUpset(tierOf(tiers, won), tierOf(tiers, lost));
                })
                .orElse(false);
    }
}
