package com.reviewerAnalysis.model;

public class ReviewModel {

    int rating;
    String reviewText;

    public ReviewModel(int rating, String reviewText) {
        this.rating = rating;
        this.reviewText = reviewText;
    }

    public int getRating() {
        return rating;
    }

    public String getReviewText() {
        return reviewText;
    }

}
