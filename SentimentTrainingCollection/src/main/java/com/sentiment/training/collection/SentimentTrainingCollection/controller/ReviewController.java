package com.sentiment.training.collection.SentimentTrainingCollection.controller;

import com.sentiment.training.collection.SentimentTrainingCollection.SentimentAnalysis;
import com.sentiment.training.collection.SentimentTrainingCollection.data.Review;
import com.sentiment.training.collection.SentimentTrainingCollection.data.ReviewRepository;
import com.sentiment.training.collection.SentimentTrainingCollection.model.Language;
import com.sentiment.training.collection.SentimentTrainingCollection.model.ResponseMessage;
import com.sentiment.training.collection.SentimentTrainingCollection.model.ReviewModel;
import com.sentiment.training.collection.SentimentTrainingCollection.model.TextRating;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

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
        Language language = getLanguage(review.getReviewText());
        if (language.getConfidence() < 0.95) {
            return new ResponseEntity<>(new ResponseMessage("Language might be " + language.getLang() + ", but only " + Math.round(language.getConfidence() * 100) + " % confident!"), HttpStatus.BAD_REQUEST);
        }
        reviewRepository.save(new Review(review.getRating(), editReviewText(review), language.getLang()));
        trainSentimentModel();
        return new ResponseEntity<>(new ResponseMessage("Review saved!"), HttpStatus.OK);
    }

    @PostMapping("/reviews/calcRating")
    public ResponseEntity<?> calcRating(@RequestParam String text) {
        Language language = getLanguage(text);
        if (language.getLang().equals("de")) {
            int rating = SentimentAnalysis.getInstance().classifyNewTweet(text);
            return new ResponseEntity<>(new TextRating(rating, language.getConfidence()), HttpStatus.OK);
        }
        return new ResponseEntity<>(new ResponseMessage("Language currently not supported. Language found: " + language.getLang() + " with a confidence of " + language.getConfidence() * 100 + " %."), HttpStatus.BAD_REQUEST);
    }

    private Language getLanguage(String text) {
        final String uri = "http://localhost:8081/api/language?text=" + text;

        RestTemplate restTemplate = new RestTemplate();
        Language[] result = restTemplate.getForObject(uri, Language[].class);
        return result[0];
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

    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        trainSentimentModel();
    }

    private void trainSentimentModel() {
        List<Review> reviews = reviewRepository.findByLang("de");
        List<String> trainingData = new LinkedList<>();
        for (Review review : reviews) {
            trainingData.add(review.getRating() + "\t" + review.getReviewText() + "\n");
        }

        SentimentAnalysis.getInstance().trainModel("de", trainingData);
    }
}
