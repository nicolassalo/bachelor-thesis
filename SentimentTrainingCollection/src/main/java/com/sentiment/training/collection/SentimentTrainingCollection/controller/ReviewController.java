package com.sentiment.training.collection.SentimentTrainingCollection.controller;

import com.sentiment.training.collection.SentimentTrainingCollection.SentimentAnalysis;
import com.sentiment.training.collection.SentimentTrainingCollection.data.Review;
import com.sentiment.training.collection.SentimentTrainingCollection.data.ReviewRepository;
import com.sentiment.training.collection.SentimentTrainingCollection.model.ResponseMessage;
import com.sentiment.training.collection.SentimentTrainingCollection.model.ReviewModel;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

@CrossOrigin(maxAge = 3600)
@RestController
@RequestMapping("/api")
public class ReviewController {



    // TODO: Add sentiment analysis to this spring boot app und keep language detection as separate microservice?



    @Autowired
    ReviewRepository reviewRepository;

    /**
     * Saves the sent review in the collection
     *
     * @param review A review of type @{@link ReviewModel} to be saved in the collection
     * @return HttpStatus.OK if the review was saved
     */
    @PostMapping("/reviews")
    public ResponseEntity<?> saveReview(@Valid @RequestBody ReviewModel review) {
        if (review.getReviewText().length() > 10000) {

            return new ResponseEntity<>(new ResponseMessage("Text is too long!"), HttpStatus.BAD_REQUEST);
        }
        reviewRepository.save(new Review(review.getRating(), editReviewText(review)));
        return new ResponseEntity<>(new ResponseMessage("Review saved!"), HttpStatus.OK);
    }

    @PostMapping("/reviews/calcRating")
    public ResponseEntity<?> calcRating(@RequestParam String text) {
        System.out.println(text);

        List<Review> reviews = reviewRepository.findAll();
        List<String> trainingData = new LinkedList<>();
        for (Review review : reviews) {
            trainingData.add(review.getRating() + "\t" + review.getReviewText() + "\n");
        }

        SentimentAnalysis analysis = new SentimentAnalysis();
        analysis.trainModel(trainingData);
        int rating = analysis.classifyNewTweet(text);
        return new ResponseEntity<>(new ResponseMessage("Rating should be " + rating), HttpStatus.OK);
    }

    private String editReviewText(ReviewModel reviewModel) {
        String editedString = reviewModel.getReviewText()
                .replaceAll("\\„", " „ ")
                .replaceAll("\\“", " “" )
                .replaceAll("\\.", " . ")
                .replaceAll("\\!", " ! ")
                .replaceAll("\\,", " , ")
                .replaceAll("\\:", " : ")
                .replaceAll("\\;", " ; ");

        return editedString;
    }
}
