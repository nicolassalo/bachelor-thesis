package com.sentiment.training.collection.SentimentTrainingCollection.data;

import javax.persistence.*;

@Entity
public class Review {
    private static final int EXPIRATION_DEFAULT = 1000 * 60 * 60 * 24;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private int rating;

    @Column( length = 10000 )
    private String reviewText;

    public Review(int rating, String reviewText) {
        this.rating = rating;
        this.reviewText = reviewText;
    }

    public Review() {}

    public int getRating() {
        return rating;
    }

    public String getReviewText() {
        return reviewText;
    }
}

