package com.reviewerAnalysis.model;

import java.util.LinkedList;
import java.util.List;

public class PersonaResponse {

    private int ignored;
    private List<Item> personasByReviewVariables;
    private List<Item> personasByLanguageProcessing;

    private Item result;

    private int activeness;
    private int elaborateness;

    public PersonaResponse(int ignored, List<Item> personasByLanguageProcessing, List<Item> personasByReviewVariables, Item result, int activeness, int elaborateness) {
        this.ignored = ignored;
        this.personasByLanguageProcessing = personasByLanguageProcessing;
        this.personasByReviewVariables = personasByReviewVariables;
        this.result = result;
        this.activeness = activeness;
        this.elaborateness = elaborateness;
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

    public List<Item> getPersonasByReviewVariables() {
        return personasByReviewVariables;
    }

    public List<Item> getPersonasByLanguageProcessing() {
        return personasByLanguageProcessing;
    }

    public static class Item {

        private String persona;
        private double confidence;

        public Item(String persona, double confidence) {
            this.persona = persona;
            this.confidence = confidence;
        }

        public String getPersona() {
            return persona;
        }

        public double getConfidence() {
            return confidence;
        }
    }
}
