package com.sentiment.training.collection.SentimentTrainingCollection.data;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Optional<Review> findByReviewText(String text);
    Optional<Review> findByRating(int rating);
}

