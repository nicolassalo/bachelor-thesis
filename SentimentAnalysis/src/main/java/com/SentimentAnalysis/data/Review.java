package com.SentimentAnalysis.data;

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

    private String lang;

    public Review(int rating, String reviewText, String lang) {
        this.rating = rating;
        this.reviewText = reviewText;
        this.lang = lang;
    }

    public Review() {}

    public int getRating() {
        return rating;
    }

    public String getReviewText() {
        return reviewText;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }
}

