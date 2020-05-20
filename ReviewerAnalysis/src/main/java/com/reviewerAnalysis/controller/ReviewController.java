package com.reviewerAnalysis.controller;

import com.reviewerAnalysis.NaturalLanguageProcessor;
import com.reviewerAnalysis.data.*;
import com.reviewerAnalysis.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.transaction.Transactional;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@CrossOrigin(maxAge = 3600)
@RestController
@RequestMapping("/reviewerAnalysis")
public class ReviewController {

    @Autowired
    ReviewRepository reviewRepository;

    @Autowired
    PasswordRepository passwordRepository;

    @Autowired
    PersonaRepository personaRepository;

    @GetMapping("/personas")
    public List<Persona> test() {
        return personaRepository.findAll();
    }

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
            System.out.println("hasPicture " + review.isHasPicture());
            reviewRepository.save(new Review(review.getTimestamp(), review.getTimeSincePreviousReview(), review.getRating(), review.isHasPicture(), review.isHasVideo(), review.isPurchaseVerified(), getSentiment(review.getReviewText()), review.getReviewText(), language.getLang(), password, review.getPersona()));
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
            long count = reviewRepository.deleteByReviewTextAndRatingAndPassword(review.getReviewText(), review.getRating(), password);
            if (count > 0) {
                return new ResponseEntity<>(new ResponseMessage("Deleted " + count + " reviews!"), HttpStatus.OK);
            }
            return new ResponseEntity<>(new ResponseMessage("Review not found!"), HttpStatus.NOT_FOUND);
        } else {
            return new ResponseEntity<>(new ResponseMessage("Permission denied!"), HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Detects the persona, that is most suitable for the passed review
     *
     * @param review A review of type @{@link ReviewModel} to be analyzed
     * @return a @{@link ResponseMessage} stating the detected persona
     */
    @PostMapping("/analyzeReview")
    public ResponseEntity<?> analyzeSingleReview(@Valid @RequestBody ReviewModel review) {
        if (review.getReviewText().length() > 10000) {
            return new ResponseEntity<>(new ResponseMessage("Text is too long!"), HttpStatus.BAD_REQUEST);
        }
        Language language = getLanguage(review.getReviewText());
        if (language.getConfidence() < 0.95) {
            return new ResponseEntity<>(new ResponseMessage("Language might be " + language.getLang() + ", but only " + Math.round(language.getConfidence() * 100) + " % confident!"), HttpStatus.BAD_REQUEST);
        }
        // TODO: Analysis
        return new ResponseEntity<>(new ResponseMessage("Detected Persona: TODO!"), HttpStatus.OK);
    }

    /**
     * Detects the persona, that is most suitable for the passed list of reviews
     *
     * @param reviews A List of reviews of type @{@link ReviewModel} to be analyzed
     * @return a @{@link PersonaResponse} stating the detected persona
     */
    @PostMapping("/")
    public ResponseEntity<?> analyzeMultipleReviews(@Valid @RequestBody ReviewModelListWrapper reviews) {
        List<ReviewModel> ignore = new LinkedList<>();
        for (ReviewModel review : reviews.getReviews()) {
            if (review.getReviewText().length() > 10000) {
                ignore.add(review);
            } else {
                Language language = getLanguage(review.getReviewText());
                if (language.getConfidence() < 0.95) {
                    ignore.add(review);
                }
            }
        }
        // TODO: check if this works
        reviews.getReviews().removeAll(ignore);
        // TODO: Analysis
        List nlp = new LinkedList();
        for (ReviewModel review : reviews.getReviews()) {
            nlp.add(numberToPersona(NaturalLanguageProcessor.getInstance().classifyNewReview(editReviewText(review.getReviewText()))));
        }

        System.out.println(reviews.getReviews().size());

        PersonaResponse response = new PersonaResponse(ignore.size());
        return new ResponseEntity<>(new ResponseMessage("Detected Persona: TODO!"), HttpStatus.OK);
    }

    private int getSentiment(String text) {
        final String uri = "http://localhost:8081/sentimentAnalysis/reviews/calcRating";

        RestTemplate restTemplate = new RestTemplate();
        // request body parameters
        Map<String, String> map = new HashMap<>();
        map.put("text", text);

        ResponseEntity<TextRating> response = restTemplate.postForEntity(uri, map, TextRating.class);
        TextRating rating = response.getBody();
        return rating.getRating();
    }

    private Language getLanguage(String text) {
        final String uri = "http://localhost:8082/languageDetection/detect";

        RestTemplate restTemplate = new RestTemplate();
        // request body parameters
        Map<String, String> map = new HashMap<>();
        map.put("text", text);

        ResponseEntity<Language[]> response = restTemplate.postForEntity(uri, map, Language[].class);
        Language[] languages = response.getBody();
        return languages[0];
    }

    private String editReviewText(String text) {
        return text
                .replaceAll("\\„", " „ ")
                .replaceAll("\\“", " “" )
                .replaceAll("\\.", " . ")
                .replaceAll("\\!", " ! ")
                .replaceAll("\\,", " , ")
                .replaceAll("\\:", " : ")
                .replaceAll("\\;", " ; ");
    }

    private int personaToNumber(String persona) {
        return personaRepository.findByName(persona).get().getId().intValue();
    }

    private String numberToPersona(int number) {
        return personaRepository.findById((long) number).get().getName();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        trainSentimentModel();
    }

    private void trainSentimentModel() {
        List<Review> reviews = reviewRepository.findByLang("de");
        List<String> trainingData = new LinkedList<>();
        for (Review review : reviews) {
            if (review.getPersona() != null) {
                trainingData.add(personaToNumber(review.getPersona()) + "\t" + editReviewText(review.getReviewText()) + "\n");
            }
        }

        NaturalLanguageProcessor.getInstance().trainModel("de", trainingData);
    }
}
