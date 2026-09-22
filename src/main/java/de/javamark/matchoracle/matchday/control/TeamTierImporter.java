package de.javamark.matchoracle.matchday.control;

import de.javamark.matchoracle.matchday.entity.Team;
import de.javamark.matchoracle.matchday.entity.TeamTier;
import de.javamark.matchoracle.matchday.entity.Tier;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Spec 06: records which division each club played in during a season, so a cup result can
 * be read as the surprise it may be. The third division is only a reference list here — it
 * is deliberately not a competition of its own, it just decides who counts as an amateur.
 */
@ApplicationScoped
public class TeamTierImporter {

    private static final Logger LOG = Logger.getLogger(TeamTierImporter.class);

    /** Source shortcut per division; everything outside these lists is {@link Tier#LOWER}. */
    private static final Map<String, Tier> DIVISIONS = new LinkedHashMap<>();
    static {
        DIVISIONS.put("bl1", Tier.FIRST);
        DIVISIONS.put("bl2", Tier.SECOND);
        DIVISIONS.put("bl3", Tier.THIRD);
    }

    @Inject
    @RestClient
    OpenLigaDbClient client;

    /**
     * Reads the three divisions' team lists for a season and records each club's division.
     * Does nothing once a season is recorded — which division a club played in is settled
     * for that season, and it saves three requests on every sync tick.
     */
    @Transactional
    public void importSeason(int season) {
        if (TeamTier.count("season", season) > 0) {
            return; // divisions of a season do not change; one import is enough
        }
        int recorded = 0;
        for (Map.Entry<String, Tier> division : DIVISIONS.entrySet()) {
            try {
                for (var sourceTeam : client.teams(division.getKey(), season)) {
                    Team team = Team.findByExternalId(sourceTeam.teamId()).orElse(null);
                    if (team == null) {
                        continue; // a club of that division that never appears in a competition we load
                    }
                    record(team, season, division.getValue());
                    recorded++;
                }
            } catch (RuntimeException e) {
                // spec 01, rules: source unavailable means keeping what we know, not failing the sync
                LOG.warnf(e, "%s %d: team list unavailable, tiers stay as they are", division.getKey(), season);
            }
        }
        LOG.infof("Tiers for season %d: recorded %d clubs", season, recorded);
    }

    private static void record(Team team, int season, Tier tier) {
        TeamTier existing = TeamTier.find("team = ?1 and season = ?2", team, season).firstResult();
        if (existing != null) {
            existing.tier = tier;
            return;
        }
        TeamTier entry = new TeamTier();
        entry.team = team;
        entry.season = season;
        entry.tier = tier;
        entry.persist();
    }
}
