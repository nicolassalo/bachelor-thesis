package com.reviewerAnalysis.data;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByLang(String lang);
    List<Review> findByLangAndIsForTraining(String lang, boolean forTraining);
    List<Review> findByLangAndPersonaAndIsForTraining(String lang, String persona, boolean forTraining);
    List<Review> findByOrderByLengthAsc();
    List<Review> findByPassword(String password);
    long deleteByReviewTextAndRatingAndPassword(String text, int rating, String password);
    boolean existsByReviewTextAndTimestampAndRatingAndIsForTraining(String reviewText, long timestamp, int rating, boolean isForTraining);
}

