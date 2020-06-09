package com.reviewerAnalysis.model;

import java.util.Map;

public class PersonaResponse {

    private int ignored;
    private int activeness;
    private int elaborateness;

    private Map<String, Double> results;

    public PersonaResponse(int ignored, int activeness, int elaborateness, Map<String, Double> results) {
        this.ignored = ignored;
        this.activeness = activeness;
        this.elaborateness = elaborateness;
        this.results = results;
    }

    public int getActiveness() {
        return activeness;
    }

    public int getElaborateness() {
        return elaborateness;
    }

    public int getIgnored() {
        return ignored;
    }

    public Map<String, Double> getResults() {
        return results;
    }
}
