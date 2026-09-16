package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.entity.Balance;
import de.javamark.matchoracle.matchday.entity.Form;
import de.javamark.matchoracle.matchday.entity.Match;
import de.javamark.matchoracle.matchday.entity.Matchday;
import de.javamark.matchoracle.matchday.entity.ResultStatus;
import de.javamark.matchoracle.matchday.entity.Team;
import de.javamark.matchoracle.matchday.entity.TeamMatchView;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;

/**
 * Spec 01, step 5: form = the last N played matches of a team in the current
 * season. Previous seasons never count ("new league, new squad").
 */
@ApplicationScoped
public class FormCalculator {

    @ConfigProperty(name = "matchoracle.matchday.form-matches", defaultValue = "5")
    int formMatches;

    /** Form of a team before the given matchday, with the default form window. */
    public Form formBefore(Team team, Matchday matchday) {
        return formBefore(team, matchday, formMatches);
    }

    /** Form of a team before the given matchday, considering the last {@code matches} played matches. */
    public Form formBefore(Team team, Matchday matchday, int matches) {
        List<Match> played = Match.findPlayedByTeamBefore(team, matchday.league, matchday.season, matchday.number);
        return form(team, played, matches);
    }

    /** Pure calculation; {@code playedNewestFirst} are the team's played matches of one season, newest first. */
    Form form(Team team, List<Match> playedNewestFirst) {
        return form(team, playedNewestFirst, formMatches);
    }

    Form form(Team team, List<Match> playedNewestFirst, int matches) {
        List<TeamMatchView> last = playedNewestFirst.stream()
                .limit(matches)
                .map(m -> m.viewedBy(team))
                .toList();
        Balance home = Balance.of(team, playedNewestFirst.stream().filter(m -> m.homeTeam.equals(team)).toList());
        Balance away = Balance.of(team, playedNewestFirst.stream().filter(m -> m.awayTeam.equals(team)).toList());
        boolean provisional = last.stream().anyMatch(v -> v.match().resultStatus == ResultStatus.PROVISIONAL);
        return new Form(team, last, home, away, provisional);
    }
}
