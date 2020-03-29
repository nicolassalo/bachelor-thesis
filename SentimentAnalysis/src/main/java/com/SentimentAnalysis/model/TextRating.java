package com.SentimentAnalysis.model;

public class TextRating {

    private int rating;
    private double languageConfidence;

    public TextRating(int rating, double languageConfidence) {
        this.rating = rating;
        this.languageConfidence = languageConfidence;
    }

    public double getLanguageConfidence() {
        return languageConfidence;
    }

    public int getRating() {
        return rating;
    }
}
