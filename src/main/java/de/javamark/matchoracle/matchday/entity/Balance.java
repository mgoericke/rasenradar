package de.javamark.matchoracle.matchday.entity;

import java.util.List;

/** Wins, draws, losses and goals of a team over a set of matches. */
public record Balance(int wins, int draws, int losses, int goalsFor, int goalsAgainst) {

    public static final Balance EMPTY = new Balance(0, 0, 0, 0, 0);

    public static Balance of(Team team, List<Match> playedMatches) {
        Balance balance = EMPTY;
        for (Match match : playedMatches) {
            balance = balance.plus(match.viewedBy(team));
        }
        return balance;
    }

    public Balance plus(TeamMatchView view) {
        return new Balance(
                wins + (view.result() == TeamResult.WIN ? 1 : 0),
                draws + (view.result() == TeamResult.DRAW ? 1 : 0),
                losses + (view.result() == TeamResult.LOSS ? 1 : 0),
                goalsFor + view.goalsFor(),
                goalsAgainst + view.goalsAgainst());
    }

    public int played() {
        return wins + draws + losses;
    }

    public int points() {
        return wins * 3 + draws;
    }

    public int goalDifference() {
        return goalsFor - goalsAgainst;
    }
}
