package de.javamark.matchoracle.review.entity;

import java.util.Optional;

/** Spec 03, step 4: a completed match with a similar situation, and how the system did back then (if it forecast it). */
public record SimilarCase(Situation situation, double distance, boolean sameLeague, boolean sameSeason,
                          Optional<ForecastEvaluation> evaluation) {
}
