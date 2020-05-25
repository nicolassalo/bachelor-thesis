package com.reviewerAnalysis.controller;

import com.reviewerAnalysis.NaturalLanguageProcessor;
import com.reviewerAnalysis.PersonaDetection;
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
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    @Autowired
    PersonaDetection personaDetection;

    @Autowired
    NaturalLanguageProcessor naturalLanguageProcessor;

    @GetMapping("/personas")
    public List<Persona> getPersonas() {
        return personaRepository.findAllByOrderByIdAsc();
    }

    @GetMapping("/trainModels/{lang}")
    public ResponseEntity<?> trainModels(@PathVariable String lang) {
        long start = System.currentTimeMillis();
        int size = reviewRepository.findByLang(lang).size();

        personaDetection.train(lang);
        naturalLanguageProcessor.train(lang);

        long finish = System.currentTimeMillis();
        return new ResponseEntity<>(new ResponseMessage("Trained models with " + size + " reviews in " + (finish - start) + " ms."), HttpStatus.OK);
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
            reviewRepository.save(new Review(review.getTimestamp(), review.getTimeSincePreviousReview(), review.getRating(), review.getReviewText().length(), review.isHasPicture(), review.isHasVideo(), review.isPurchaseVerified(), getSentiment(review.getReviewText()), review.getReviewText(), language.getLang(), password, review.getPersona(), true));
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
        List<Review> reviews = new LinkedList<>();
        reviews.add(new Review(review.getTimestamp(), review.getTimeSincePreviousReview(), review.getRating(), review.getReviewText().length(), review.isHasPicture(), review.isHasVideo(), review.isPurchaseVerified(), getSentiment(review.getReviewText()), review.getReviewText(), language.getLang(), null, review.getPersona(), true));
        List<String> wekaResult = personaDetection.detectPersona(reviews);
        List<String> nlpResult = new LinkedList<>();
        nlpResult.add(naturalLanguageProcessor.classifyNewReview(review.getReviewText()));
        List<PersonaResponse.Item> nlpItems = getItems(nlpResult);
        List<PersonaResponse.Item> wekaItems = getItems(wekaResult);
        PersonaResponse response = new PersonaResponse(0, nlpItems, wekaItems, calculateResult(nlpItems, wekaItems), calculateActiveness(reviews), calculateElaborateness(reviews));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Detects the persona, that is most suitable for the passed list of reviews
     *
     * @param reviewWrapper A List of reviews of type @{@link ReviewModel} to be analyzed
     * @return a @{@link PersonaResponse} stating the detected persona
     */
    @PostMapping("/")
    public ResponseEntity<?> analyzeMultipleReviews(@Valid @RequestBody ReviewModelListWrapper reviewWrapper) {
        List<Review> reviews = new LinkedList<>();
        for (ReviewModel review : reviewWrapper.getReviews()) {
            if (review.getReviewText().length() < 10000) {
                Language language = getLanguage(review.getReviewText());
                if (language.getConfidence() > 0.95) {
                    reviews.add(new Review(review.getTimestamp(), review.getTimeSincePreviousReview(), review.getRating(), review.getReviewText().length(), review.isHasPicture(), review.isHasVideo(), review.isPurchaseVerified(), getSentiment(review.getReviewText()), review.getReviewText(), language.getLang(), null, review.getPersona(), true));
                }
            }
        }

        List<String> wekaResults = personaDetection.detectPersona(reviews);

        List<String> nlpResults = new LinkedList<>();
        for (Review review : reviews) {
            nlpResults.add(naturalLanguageProcessor.classifyNewReview(review.getReviewText()));
        }

        List<PersonaResponse.Item> nlpItems = getItems(nlpResults);
        List<PersonaResponse.Item> wekaItems = getItems(wekaResults);
        PersonaResponse response = new PersonaResponse(reviewWrapper.getReviews().size() - reviews.size(), nlpItems, wekaItems, calculateResult(nlpItems, wekaItems), calculateActiveness(reviews),calculateElaborateness(reviews));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private PersonaResponse.Item calculateResult(List<PersonaResponse.Item> nlpResults, List<PersonaResponse.Item> wekaResults) {
        double wekaRelevanceFactor = 2; // how much more weight is on the wekaResults
        double nlpRelevanecFactor = 1; // how much more weight is on the nlpResults
        double[] probabilities = new double[personaRepository.findAll().size()];
        for (PersonaResponse.Item item : wekaResults) {
            int position = personaRepository.findByName(item.getPersona()).get().getId().intValue() - 1;
            probabilities[position] += item.getConfidence() * wekaRelevanceFactor;
        }

        for (PersonaResponse.Item item : nlpResults) {
            int position = personaRepository.findByName(item.getPersona()).get().getId().intValue() - 1;
            probabilities[position] += item.getConfidence() * nlpRelevanecFactor;
        }


        int index = 0;
        double sum = 0;
        double highestValue = 0;
        for (int i = 0; i < probabilities.length; i++) {
            if (probabilities[i] > highestValue) {
                index = i;
                highestValue = probabilities[i];
            }
            sum += probabilities[i];
        }

        return new PersonaResponse.Item(personaRepository.findById((long) index + 1).get().getName(), highestValue / sum);
    }

    /*
        Every average review interval higher than the highest is ranked 10
        Every average review interval lower than the lowest is ranked 0
     */
    private int calculateActiveness(List<Review> reviews) {
        int highest = 3600 * 24; // one day
        int lowest = 3600 * 4 * 365 * 5; // 5 years
        int diff = lowest - highest;

        int sum = 0;
        int counter = 0;
        for (Review review : reviews) {
            if (review.getTimeSincePreviousReview() != null) {
                counter += 1;
                sum += review.getTimeSincePreviousReview();
            }
        }

        if (sum > 0) {
            sum /= counter; // average
            double deduction = (double) sum / diff;
            System.out.println(10 - (deduction * 10));
            int result = (int) Math.ceil(10 - (deduction * 10));
            if (result > 10) {
                result = 10;
            } else if (result < 0) {
                result = 0;
            }
            return result;
        } else {
            return 0;
        }
    }

    private int calculateElaborateness(List<Review> reviews) {
        List<Review> allReviews = reviewRepository.findByOrderByLengthAsc();
        int sum = 0;
        for (Review review : reviews) {
            sum += review.getLength();
        }

        sum /= reviews.size(); // average

        int counter = 1;
        for (Review review : allReviews) {
            if (review.getLength() < sum) {
                counter++;
            } else {
                break;
            }
        }
        System.out.println("Counter " + counter);

        double ratio = (double) counter / allReviews.size();
        int result = (int) Math.ceil((ratio * 10));
        if (result > 10) {
            result = 10;
        }
        return result;
    }

    private List<PersonaResponse.Item> getItems(List<String> results) {
        Map<String, Long> result =
                results.stream().collect(
                        Collectors.groupingBy(
                                Function.identity(), Collectors.counting()
                        )
                );

        Map<String, Long> finalMap = new LinkedHashMap<>();

        //Sort a map and add to finalMap
        result.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue()
                        .reversed()).forEachOrdered(e -> finalMap.put(e.getKey(), e.getValue()));

        List<PersonaResponse.Item> items = new LinkedList<>();
        for (Map.Entry<String, Long> entry : finalMap.entrySet()) {
            items.add(new PersonaResponse.Item(entry.getKey(), (double) entry.getValue() / results.size()));
        }

        return items;
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

    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        // do not train model before having at least 2 examples per persona (throws exception)
        naturalLanguageProcessor.train("de");
        personaDetection.train("de");
    }
}
