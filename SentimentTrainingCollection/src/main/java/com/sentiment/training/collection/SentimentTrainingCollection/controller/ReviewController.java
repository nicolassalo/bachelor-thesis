package com.sentiment.training.collection.SentimentTrainingCollection.controller;

import com.sentiment.training.collection.SentimentTrainingCollection.data.Review;
import com.sentiment.training.collection.SentimentTrainingCollection.data.ReviewRepository;
import com.sentiment.training.collection.SentimentTrainingCollection.model.ResponseMessage;
import com.sentiment.training.collection.SentimentTrainingCollection.model.ReviewModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@CrossOrigin(maxAge = 3600)
@RestController
@RequestMapping("/api")
public class ReviewController {

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
