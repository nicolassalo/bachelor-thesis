package com.SentimentAnalysis;

import com.SentimentAnalysis.data.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SentimentTrainingCollectionApplication {

	@Autowired
	ReviewRepository reviewRepository;

	public static void main(String[] args) {
		SpringApplication.run(SentimentTrainingCollectionApplication.class, args);

	}
}
