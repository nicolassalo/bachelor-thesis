package com.sentiment.training.collection.SentimentTrainingCollection;

import com.sentiment.training.collection.SentimentTrainingCollection.data.Review;
import com.sentiment.training.collection.SentimentTrainingCollection.data.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.util.List;

@SpringBootApplication
public class SentimentTrainingCollectionApplication {

	@Autowired
	ReviewRepository reviewRepository;

	public static void main(String[] args) {
		SpringApplication.run(SentimentTrainingCollectionApplication.class, args);

	}

	@EventListener(ApplicationReadyEvent.class)
	public void initialize() {
		List<Review> reviews = reviewRepository.findAll();
		for (Review review : reviews) {
			System.out.println(review.getRating() + "\t" + review.getReviewText());
		}
	}


}
