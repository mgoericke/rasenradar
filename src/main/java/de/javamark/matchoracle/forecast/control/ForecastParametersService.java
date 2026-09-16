package de.javamark.matchoracle.forecast.control;

import de.javamark.matchoracle.forecast.entity.ForecastParameters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/** Spec 02, rules: the assessment scales are changeable at runtime and take effect on the next forecast. */
@ApplicationScoped
public class ForecastParametersService {

    @Transactional
    public ForecastParameters current() {
        return ForecastParameters.current();
    }

    @Transactional
    public ForecastParameters update(double homeAdvantage, int formMatches, double promotedTeamMalus) {
        if (homeAdvantage < 0 || homeAdvantage > 0.5 || promotedTeamMalus < 0 || promotedTeamMalus > 0.5 || formMatches < 1 || formMatches > 34) {
            throw new IllegalArgumentException("Heimvorteil und Aufsteiger-Malus zwischen 0 und 0.5, Formzeitraum zwischen 1 und 34");
        }
        ForecastParameters p = ForecastParameters.current();
        p.homeAdvantage = homeAdvantage;
        p.formMatches = formMatches;
        p.promotedTeamMalus = promotedTeamMalus;
        return p;
    }
}
