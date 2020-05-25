package com.reviewerAnalysis.data;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByLang(String lang);
    List<Review> findByLangAndIsForTraining(String lang, boolean forTraining);
    List<Review> findByOrderByLengthAsc();
    long deleteByReviewTextAndRatingAndPassword(String text, int rating, String password);
}

