package com.reviewerAnalysis.controller;

import com.reviewerAnalysis.NaturalLanguageProcessor;
import com.reviewerAnalysis.WekaPersonaDetection;
import com.reviewerAnalysis.data.*;
import com.reviewerAnalysis.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.functions.*;
import weka.classifiers.meta.*;
import weka.classifiers.rules.*;
import weka.classifiers.trees.*;
import weka.classifiers.trees.lmt.LogisticBase;

import javax.servlet.http.HttpServletRequest;
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
    ReviewerRepository reviewerRepository;

    @Autowired
    ReviewRepository reviewRepository;

    @Autowired
    PasswordRepository passwordRepository;

    @Autowired
    PersonaRepository personaRepository;

    @Autowired
    StatsRepository statsRepository;

    @Autowired
    WekaPersonaDetection wekaPersonaDetection;

    @Autowired
    NaturalLanguageProcessor naturalLanguageProcessor;

    @GetMapping("/personas")
    public List<Persona> getPersonas() {
        return personaRepository.findAllByOrderByIdAsc();
    }

    /*
        Waiting for calculation results in a timeout.
        Therefore returns latest calculations.
     */
    @GetMapping("/accuracy/{lang}")
    public ResponseEntity<?> getAccuracy(@PathVariable String lang) {
        if (!wekaPersonaDetection.isCalculating() && !naturalLanguageProcessor.isCalculating()) {
            new Thread(() -> {
                Result nlpResult = naturalLanguageProcessor.calcAccuracy(lang);
                Result wekaResult = wekaPersonaDetection.calcAccuracy(lang, null, nlpResult.getTotalPersonaAnalysis());
                statsRepository.deleteAll();
                statsRepository.save(new Stats(lang, wekaResult.getAccuracy(), nlpResult.getAccuracy(), wekaResult.getPersonaAccuracies(), nlpResult.getPersonaAccuracies()));
            }).start();
        }
        List<Stats> stats = statsRepository.findAll();
        if (stats.size() > 0) {
            return new ResponseEntity<>(new ResponseMessage(stats.get(0).getWekaAccuracy() + " for personaDetection and " + stats.get(0).getNlpAccuracy() + " for nlpDetection"), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(new ResponseMessage("Accuracies are being calculated. Please try again in a few minutes"), HttpStatus.OK);
        }
    }

    @GetMapping("/countReviews/{password}")
    public ResponseEntity<?> countReviews(@PathVariable String password) {
        return new ResponseEntity<>(new ResponseMessage("You already saved " + reviewRepository.findByPassword(password).size() + " reviews. Keep going!"), HttpStatus.OK);
    }

    /*
    @GetMapping("/trainModels/{lang}")
    public ResponseEntity<?> trainModels(@PathVariable String lang) {
        long start = System.currentTimeMillis();
        int size = reviewRepository.findByLangAndIsForTraining(lang, true).size();

        wekaPersonaDetection.train(lang);
        naturalLanguageProcessor.train(lang);

        long finish = System.currentTimeMillis();
        return new ResponseEntity<>(new ResponseMessage("Trained models with " + size + " reviews in " + (finish - start) + " ms."), HttpStatus.OK);
    }
     */

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
     * Detects the persona, that is most suitable for the passed list of reviews
     *
     * @param reviewWrapper A List of reviews of type @{@link ReviewModel} to be analyzed
     * @return a @{@link PersonaResponse} stating the detected persona
     */
    @PostMapping("/")
    public ResponseEntity<?> analyzeReviews(@Valid @RequestBody ReviewModelListWrapper reviewWrapper) {
        List<Review> reviews = new LinkedList<>();
        boolean saveReviews = true;
        int sumReviewLength = 0;
        long counter = -1;
        for (ReviewModel review : reviewWrapper.getReviews()) {
            if (review.getReviewText().length() < 10000) {
                Language language = getLanguage(review.getReviewText());
                if (language.getLang().equals("de") && language.getConfidence() > 0.95) {
                    Review r = new Review(review.getTimestamp(), review.getTimeSincePreviousReview(), review.getRating(), review.getReviewText().length(), review.isHasPicture(), review.isHasVideo(), review.isPurchaseVerified(), getSentiment(review.getReviewText()), review.getReviewText(), language.getLang(), null, review.getPersona(), false);
                    if (!reviewRepository.existsByReviewTextAndTimestampAndRatingAndIsForTraining(review.getReviewText(), review.getTimestamp(), review.getRating(), false)) {
                        r = reviewRepository.save(r);
                    } else {
                        saveReviews = false;
                        r.setId(counter);
                        counter--;
                    }
                    System.out.println("new review id: " + r.getId());
                    reviews.add(r);
                    sumReviewLength += review.getReviewText().length();
                }
            }
        }

        if (reviews.size() >= 10 && saveReviews) {
            reviewerRepository.save(new Reviewer(reviews));
            System.out.println("saved new reviewer");
        }

        Map<Long, Map<String, Double>> totalNlpResults = new HashMap<>();
        for (Review review : reviews) {
            totalNlpResults.put(review.getId(), naturalLanguageProcessor.analyzeText(review.getReviewText()));
        }


        // TODO: Länge verglichen zur Durchschnittslänge des Reviewers
        int averageReviewLength = sumReviewLength / reviews.size();
        Map<String, Double> wekaResults = wekaPersonaDetection.detectPersona(reviews, totalNlpResults);
        wekaResults = wekaResults.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));


        PersonaResponse response = new PersonaResponse(reviewWrapper.getReviews().size() - reviews.size(), calculateActiveness(reviews),calculateElaborateness(reviews), wekaResults);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /*
        Every average review interval higher than the highest is ranked 10
        Every average review interval lower than the lowest is ranked 0
     */
    private int calculateActiveness(List<Review> reviews) {
        System.out.println("calculating activeness");
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
            System.out.println("done calculating activeness");
            return result;
        } else {
            System.out.println("done calculating activeness");
            return 0;
        }
    }

    private int calculateElaborateness(List<Review> reviews) {
        System.out.println("calculating elaborateness");
        List<Review> allReviews = reviewRepository.findByOrderByLengthAsc();
        int sum = 0;
        for (Review review : reviews) {
            sum += review.getLength();
        }

        sum /= reviews.size(); // average length of reviewer's reviews

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
        return (int) Math.round((ratio * 10));
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

    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        // do not train model before having at least 2 examples per persona (throws exception)
        Result nlpResult = naturalLanguageProcessor.calcAccuracy("de");
        naturalLanguageProcessor.train("de");
        //compareAlgorithms(nlpResult.getTotalPersonaAnalysis());

        //updateReviewSentiment();


        // TODO: Add average product rating to fields
        wekaPersonaDetection.train("de", nlpResult.getTotalPersonaAnalysis());
        Result wekaResult = wekaPersonaDetection.calcAccuracy("de", null, nlpResult.getTotalPersonaAnalysis());
        statsRepository.deleteByLang("de");
        statsRepository.save(new Stats("de", wekaResult.getAccuracy(), nlpResult.getAccuracy(), wekaResult.getPersonaAccuracies(), nlpResult.getPersonaAccuracies()));
    }

    private void compareAlgorithms(Map<Long, Map<String, Double>> totalPersonaAnalysis) {
        List<Classifier> classifiers = new LinkedList<>();
        classifiers.add(new ClassificationViaRegression()); // new first place
        classifiers.add(new DecisionTable());
        classifiers.add(new LMT());
        classifiers.add(new SimpleLogistic()); // shared first place but slower (maybe because memory was getting full)
        classifiers.add(new LogisticBase()); // shared first place but faster (maybe because memory was getting full)
        classifiers.add(new RandomForest());
        classifiers.add(new BayesNet());
        /*
        classifiers.add(new NaiveBayesMultinomial()); // cannot deal with negative numbers
        classifiers.add(new NaiveBayesMultinomialUpdateable()); // cannot deal with negative numbers
        classifiers.add(new AttributeSelectedClassifier());
        classifiers.add(new FilteredClassifier());
        classifiers.add(new IBk());
        classifiers.add(new J48());
        classifiers.add(new JRip());
        classifiers.add(new KStar());
        classifiers.add(new Logistic());
        classifiers.add(new MultiClassClassifier());
        classifiers.add(new MultiClassClassifierUpdateable());
        classifiers.add(new MultilayerPerceptron());
        classifiers.add(new PART());
        classifiers.add(new RandomTree());
        classifiers.add(new REPTree());
        classifiers.add(new SMO());
        classifiers.add(new Bagging());
        classifiers.add(new BayesNetGenerator());
        classifiers.add(new EditableBayesNet());
        classifiers.add(new BIFReader());
        classifiers.add(new LWL());
        classifiers.add(new RandomCommittee());
        classifiers.add(new RandomSubSpace());
        */

        /* worst
        classifiers.add(new Stacking());
        classifiers.add(new Vote());
        classifiers.add(new NaiveBayesMultinomialText());
        /classifiers.add(new CVParameterSelection());
        classifiers.add(new AdaBoostM1());
        classifiers.add(new ZeroR());
        classifiers.add(new RandomizableFilteredClassifier());
        classifiers.add(new OneR());
        classifiers.add(new MultiScheme());
        classifiers.add(new DecisionStump());
         */

        //classifiers.add(new AdditiveRegression()); //Cannot handle multi-valued nominal class!
        //classifiers.add(new LinearRegression()); //Cannot handle multi-valued nominal class!
        //classifiers.add(new M5P()); //Cannot handle multi-valued nominal class!
        //classifiers.add(new M5Rules()); //Cannot handle multi-valued nominal class!
        //classifiers.add(new SimpleLinearRegression()); //Cannot handle multi-valued nominal class!
        //classifiers.add(new SMOreg()); //Cannot handle multi-valued nominal class!
        //classifiers.add(new VotedPerceptron()); //Cannot handle multi-valued nominal class!
        //classifiers.add(new RegressionByDiscretization()); //Cannot handle multi-valued nominal class!
        //classifiers.add(new SGD()); //Cannot handle multi-valued nominal class!
        //classifiers.add(new SGDText()); //Cannot handle multi-valued nominal class!
        //classifiers.add(new CostSensitiveClassifier()); // file-read exception
        //classifiers.add(new SerializedClassifier()); // file-read exception

        List<ClassifierAccuracy> accuracies = new LinkedList<>();

        for (Classifier classifier : classifiers) {
            System.out.println("using " + classifier.getClass().getName());
            Result result = wekaPersonaDetection.calcAccuracy("de", classifier, totalPersonaAnalysis);
            accuracies.add(new ClassifierAccuracy(result.getAccuracy(), result.getTime(), classifier));
        }

        accuracies.sort((c1, c2) -> (int) ((c2.getAccuracy() * 10000) - (c1.getAccuracy() * 10000)));

        for (ClassifierAccuracy classifierAccuracy : accuracies) {
            System.out.println(classifierAccuracy.getAccuracy() + " % accuracy: " + classifierAccuracy.getClassifier().getClass().getName() + " - " + classifierAccuracy.getTime() + " ms");
        }
    }

    private class ClassifierAccuracy {
        private double accuracy;
        private double time;
        private Classifier classifier;

        public ClassifierAccuracy(double accuracy, double time, Classifier classifier) {
            this.accuracy = accuracy;
            this.time = time;
            this.classifier = classifier;
        }

        public Classifier getClassifier() {
            return classifier;
        }

        public double getAccuracy() {
            return accuracy;
        }

        public double getTime() {
            return time;
        }
    }

    public static class Result {
        private Map<Long, Map<String, Double>> totalPersonaAnalysis;
        private Map<String, Double> personaAccuracies;
        private double accuracy;
        private long time;

        public Result(Map<Long, Map<String, Double>> totalPersonaAnalysis, Map<String, Double> personaAccuracies, double  accuracy, long time) {
            this.totalPersonaAnalysis = totalPersonaAnalysis;
            this.personaAccuracies = personaAccuracies;
            this.accuracy = accuracy;
            this.time = time;
        }

        public Map<Long, Map<String, Double>> getTotalPersonaAnalysis() {
            return totalPersonaAnalysis;
        }

        public Map<String, Double> getPersonaAccuracies() {
            return personaAccuracies;
        }

        public double getAccuracy() {
            return accuracy;
        }

        public long getTime() {
            return time;
        }
    }

    private void updateReviewSentiment() {
        List<Review> reviews = reviewRepository.findAll();
        for (Review review : reviews) {
            Review update = review;
            update.setSentimentAnalysis(getSentiment(update.getReviewText()));
            reviewRepository.save(update);
            System.out.println("Updated reviewId " + update.getId());
        }
    }
}
