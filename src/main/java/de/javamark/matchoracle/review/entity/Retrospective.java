package de.javamark.matchoracle.review.entity;

import java.util.List;

/** Spec 03, step 4: what the forecaster gets — the similar cases and a German summary. */
public record Retrospective(List<SimilarCase> cases, String summary) {
}
