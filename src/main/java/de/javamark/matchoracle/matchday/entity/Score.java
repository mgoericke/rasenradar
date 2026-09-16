package de.javamark.matchoracle.matchday.entity;

import jakarta.persistence.Embeddable;

@Embeddable
public class Score {

    public int home;
    public int away;
    protected Score() {}
    public Score(int home, int away) {
        this.home = home;
        this.away = away;
    }
}
