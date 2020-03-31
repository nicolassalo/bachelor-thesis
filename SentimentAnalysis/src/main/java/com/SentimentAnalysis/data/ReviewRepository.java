package com.SentimentAnalysis.data;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByLang(String lang);
    long deleteByReviewTextAndRatingAndPassword(String text, int rating, String password);
}

