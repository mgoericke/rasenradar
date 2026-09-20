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
