package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.entity.Goal;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.ResultStatus;
import de.javamark.matchoracle.matchday.entity.Team;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Story r063 "Torjäger-Steckbrief": everything the goal list says about one scorer of a club — and
 * nothing else (Spec 01 keeps no player statistics beyond goals). Own goals are never credited to
 * the scorer. Goals of provisional results are listed and marked, but the share of the club's goals
 * counts final results only.
 */
public final class ScorerProfileCalculator {

    /** From this minute on a goal counts as "late" in the profile. */
    public static final int LATE = 80;

    private ScorerProfileCalculator() {
    }

    /** One goal of the scorer: the score after it and at the end, both from the club's side. */
    public record ScorerGoal(Match match, boolean home, Team opponent, Integer minute, int forAfter, int againstAfter,
                             int forFinal, int againstFinal, boolean penalty, boolean provisional) {
    }

    public record Profile(String name, List<ScorerGoal> goals, int finalGoals, int clubFinalGoals) {
        public int count() {
            return goals.size();
        }

        public long matches() {
            return goals.stream().map(ScorerGoal::match).distinct().count();
        }

        public long penalties() {
            return goals.stream().filter(ScorerGoal::penalty).count();
        }

        public long homeGoals() {
            return goals.stream().filter(ScorerGoal::home).count();
        }

        public long firstHalf() {
            return goals.stream().filter(g -> g.minute() != null && g.minute() <= 45).count();
        }

        public long secondHalf() {
            return goals.stream().filter(g -> g.minute() != null && g.minute() > 45).count();
        }

        public long late() {
            return goals.stream().filter(g -> g.minute() != null && g.minute() >= LATE).count();
        }

        /** Share of the club's final goals in whole percent, or null while the club has none. */
        public Integer sharePercent() {
            return clubFinalGoals == 0 ? null : (int) Math.round(100.0 * finalGoals / clubFinalGoals);
        }
    }

    /** One profile per scorer of the club, by name; newest goal first within each profile. */
    public static Map<String, Profile> of(Team team, List<Match> matches) {
        Map<String, List<ScorerGoal>> byName = new LinkedHashMap<>();
        int clubFinal = 0;
        List<Match> played = matches.stream().filter(m -> m.fullTimeScore != null)
                .sorted(Comparator.comparing((Match m) -> m.kickoff).reversed()).toList();
        for (Match m : played) {
            boolean home = m.homeTeam.equals(team);
            boolean provisional = m.resultStatus != ResultStatus.FINAL;
            int forFinal = home ? m.fullTimeScore.home : m.fullTimeScore.away;
            int againstFinal = home ? m.fullTimeScore.away : m.fullTimeScore.home;
            if (!provisional) clubFinal += forFinal;
            int before = 0;
            List<ScorerGoal> ofMatch = new ArrayList<>();
            List<String> names = new ArrayList<>();
            for (Goal g : m.goals) {
                int after = home ? g.scoreAfter.home : g.scoreAfter.away;
                if (after > before && !g.ownGoal && g.scorerName != null) {
                    ofMatch.add(new ScorerGoal(m, home, home ? m.awayTeam : m.homeTeam, g.minute, after,
                            home ? g.scoreAfter.away : g.scoreAfter.home, forFinal, againstFinal, g.penalty, provisional));
                    names.add(g.scorerName);
                }
                before = after;
            }
            // within a match the later goal comes first, so the whole list reads newest first
            for (int i = ofMatch.size() - 1; i >= 0; i--) {
                byName.computeIfAbsent(names.get(i), k -> new ArrayList<>()).add(ofMatch.get(i));
            }
        }
        Map<String, Profile> profiles = new LinkedHashMap<>();
        int club = clubFinal;
        byName.forEach((name, goals) -> profiles.put(name, new Profile(name, List.copyOf(goals),
                (int) goals.stream().filter(g -> !g.provisional()).count(), club)));
        return profiles;
    }
}
