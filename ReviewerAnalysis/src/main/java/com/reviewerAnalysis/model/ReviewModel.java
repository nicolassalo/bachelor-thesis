package com.reviewerAnalysis.model;

public class ReviewModel {

    private long timestamp;
    private int rating;
    private String reviewText;

    // null if no previous review
    // cannot be calculated after all reviews were saved because model was already trained
    private Long timeSincePreviousReview;

    private boolean hasPicture;

    private boolean hasVideo;

    private boolean isPurchaseVerified;

    private String persona;

    public ReviewModel(long timestamp, int rating, String reviewText, Long timeSincePreviousReview, boolean hasPicture, boolean hasVideo, boolean isPurchaseVerified, String persona) {
        this.timestamp = timestamp;
        this.rating = rating;
        this.reviewText = reviewText;
        this.timeSincePreviousReview = timeSincePreviousReview;
        this.hasPicture = hasPicture;
        this.hasVideo = hasVideo;
        this.isPurchaseVerified = isPurchaseVerified;
        this.persona = persona;
    }

    public int getRating() {
        return rating;
    }

    public String getReviewText() {
        return reviewText;
    }

    public boolean isPurchaseVerified() {
        return isPurchaseVerified;
    }

    public String getPersona() {
        return persona;
    }

    public void setPersona(String persona) {
        this.persona = persona;
    }

    public Long getTimeSincePreviousReview() {
        return timeSincePreviousReview;
    }

    public void setTimeSincePreviousReview(Long timeSincePreviousReview) {
        this.timeSincePreviousReview = timeSincePreviousReview;
    }

    public boolean isHasPicture() {
        return hasPicture;
    }

    public void setHasPicture(boolean hasPicture) {
        this.hasPicture = hasPicture;
    }

    public boolean isHasVideo() {
        return hasVideo;
    }

    public void setHasVideo(boolean hasVideo) {
        this.hasVideo = hasVideo;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
