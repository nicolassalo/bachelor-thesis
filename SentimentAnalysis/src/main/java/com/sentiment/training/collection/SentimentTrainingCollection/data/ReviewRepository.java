package com.sentiment.training.collection.SentimentTrainingCollection.data;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByLang(String lang);
}

