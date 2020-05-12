package com.reviewerAnalysis.data;

import javax.persistence.*;

/*
    This table holds very detailed information about each review.
    Information that can be extracted from other variables are not
    saved separately to avoid having to add, change or remove fields.
 */
@Entity
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    // in ms
    private Long timestamp;

    // in ms
    private Long timeSincePreviousReview;

    private int rating;

    private boolean hasPicture;

    private boolean hasVideo;

    private boolean isPurchaseVerified;

    // how many reviews the product has
    private int numberProductReviews;

    // calculated ideal rating
    private int sentimentAnlysis;

    /*
    data extractable from reviewText:
        length
        average length of words and sentences
        number of question marks, exclamation marks, consecutive capital letters
        distinct words / all words
        
     */
    @Column( length = 10000 ) // limit might vary between platforms, Amazon's limit is 5000
    private String reviewText;

    private String lang;

    private String password;

    public Review(int rating, boolean isPurchaseVerified, String reviewText, String lang, String password) {
        this.rating = rating;
        this.isPurchaseVerified = isPurchaseVerified;
        this.reviewText = reviewText;
        this.lang = lang;
        this.password = password;
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

    public String getPassword() {
        return password;
    }

    public boolean isPurchaseVerified() {
        return isPurchaseVerified;
    }

    public void setPurchaseVerified(boolean purchaseVerified) {
        isPurchaseVerified = purchaseVerified;
    }
}

