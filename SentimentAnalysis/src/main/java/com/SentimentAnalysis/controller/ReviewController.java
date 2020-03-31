package com.SentimentAnalysis.controller;

import com.SentimentAnalysis.SentimentAnalysis;
import com.SentimentAnalysis.data.PasswordRepository;
import com.SentimentAnalysis.data.Review;
import com.SentimentAnalysis.data.ReviewRepository;
import com.SentimentAnalysis.model.Language;
import com.SentimentAnalysis.model.ResponseMessage;
import com.SentimentAnalysis.model.ReviewModel;
import com.SentimentAnalysis.model.TextRating;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.transaction.Transactional;
import javax.validation.Valid;
import java.util.LinkedList;
import java.util.List;

@CrossOrigin(maxAge = 3600)
@RestController
@RequestMapping("/api")
public class ReviewController {

    @Autowired
    ReviewRepository reviewRepository;

    @Autowired
    PasswordRepository passwordRepository;

    /**
     * Saves the sent review in the collection
     *
     * @param review A review of type @{@link ReviewModel} to be saved in the collection
     * @return HttpStatus.OK if the review was saved
     */
    @PostMapping("/reviews/{password}")
    public ResponseEntity<?> saveReview(@Valid @RequestBody ReviewModel review, @PathVariable String password) {
        if (passwordRepository.existsByPassword(password)) {
            if (review.getReviewText().length() > 10000) {

                return new ResponseEntity<>(new ResponseMessage("Text is too long!"), HttpStatus.BAD_REQUEST);
            }
            Language language = getLanguage(review.getReviewText());
            if (language.getConfidence() < 0.95) {
                return new ResponseEntity<>(new ResponseMessage("Language might be " + language.getLang() + ", but only " + Math.round(language.getConfidence() * 100) + " % confident!"), HttpStatus.BAD_REQUEST);
            }
            reviewRepository.save(new Review(review.getRating(), editReviewText(review), language.getLang(), password));
            trainSentimentModel();
            return new ResponseEntity<>(new ResponseMessage("Review saved!"), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(new ResponseMessage("Permission denied!"), HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Deletes the sent review from the collection
     *
     * Should be of type @DeleteMapping but simplifies frontend code this way
     *
     * @param review A review of type @{@link ReviewModel} to be saved in the collection
     * @return HttpStatus.OK if the review was deleted
     */
    @Transactional
    @PostMapping("/delete/reviews/{password}")
    public ResponseEntity<?> deleteReview(@Valid @RequestBody ReviewModel review, @PathVariable String password) {
        if (passwordRepository.existsByPassword(password)) {
            long count = reviewRepository.deleteByReviewTextAndRatingAndPassword(editReviewText(review), review.getRating(), password);
            if (count > 0) {
                trainSentimentModel();
                return new ResponseEntity<>(new ResponseMessage("Deleted " + count + " reviews!"), HttpStatus.OK);
            }
            return new ResponseEntity<>(new ResponseMessage("Review not found!"), HttpStatus.NOT_FOUND);
        } else {
            return new ResponseEntity<>(new ResponseMessage("Permission denied!"), HttpStatus.FORBIDDEN);
        }
    }

    // TODO: Use @RequestBody instead of @RequestParam. Throws error if URL is too long
    @PostMapping("/reviews/calcRating")
    public ResponseEntity<?> calcRating(@RequestParam String text) {
        Language language = getLanguage(text);
        if (language.getLang().equals("de")) {
            if (language.getConfidence() < 0.95) {
                return new ResponseEntity<>(new ResponseMessage("Not sure that this is really german. Only" + " with a confidence of " + Math.round(language.getConfidence() * 100) + " %."), HttpStatus.BAD_REQUEST);
            }
            int rating = SentimentAnalysis.getInstance().classifyNewTweet(text);
            return new ResponseEntity<>(new TextRating(rating, language.getConfidence()), HttpStatus.OK);
        }
        return new ResponseEntity<>(new ResponseMessage("Language currently not supported. Language found: " + language.getLang() + " with a confidence of " + Math.round(language.getConfidence() * 100) + " %."), HttpStatus.BAD_REQUEST);
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
