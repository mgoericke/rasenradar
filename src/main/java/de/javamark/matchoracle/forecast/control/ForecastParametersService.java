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
}
