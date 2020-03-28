package com.sentiment.training.collection.SentimentTrainingCollection.model;

public class Language {

    private String lang;
    private double confidence;

    public Language(String lang, double confidence) {
        this.lang = lang;
        this.confidence = confidence;
    }

    public Language() {}

    public String getLang() {
        return lang;
    }

    public double getConfidence() {
        return confidence;
    }
}
