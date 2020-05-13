package com.reviewerAnalysis.model;

import java.util.List;

public class ReviewModelListWrapper {

    private List<ReviewModel> reviews;

    public ReviewModelListWrapper(List<ReviewModel> reviews) {
        this.reviews = reviews;
    }

    public List<ReviewModel> getReviews() {
        return reviews;
    }

    public void setReviews(List<ReviewModel> reviews) {
        this.reviews = reviews;
    }
}
