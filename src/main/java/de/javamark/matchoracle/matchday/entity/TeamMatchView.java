package de.javamark.matchoracle.matchday.entity;

/** A played match as seen by one of the two teams. */
public record TeamMatchView(Match match, boolean home, int goalsFor, int goalsAgainst) {

    public TeamResult result() {
        return TeamResult.of(goalsFor, goalsAgainst);
    }

    public Team opponent() {
        return home ? match.awayTeam : match.homeTeam;
    }
}
