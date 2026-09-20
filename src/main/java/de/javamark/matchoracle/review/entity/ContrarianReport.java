package de.javamark.matchoracle.review.entity;

/**
 * Spec 05, "Mut-Bilanz": how often the system forecast against the statistical baseline's
 * tendency, and how often that paid off — kept apart from the overall hit rate. Same
 * reliability rule as the rest of the accuracy report: null below the required count.
 */
public record ContrarianReport(int evaluated, int hits, Double hitRate) {

    public boolean reliable() {
        return hitRate != null;
    }
}
