package de.javamark.matchoracle.matchday.entity;

import java.util.List;
import java.util.Optional;

/** League table of a season before a given matchday. */
public record Standings(League league, int season, int beforeMatchday, List<StandingPosition> positions, boolean includesProvisional) {

    public Optional<StandingPosition> of(Team team) {
        return positions.stream().filter(p -> p.team().equals(team)).findFirst();
    }
}
