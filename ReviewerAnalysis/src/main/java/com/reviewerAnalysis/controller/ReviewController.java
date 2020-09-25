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
import weka.classifiers.bayes.NaiveBayesMultinomial;
import weka.classifiers.bayes.NaiveBayesMultinomialUpdateable;
import weka.classifiers.bayes.net.BIFReader;
import weka.classifiers.bayes.net.BayesNetGenerator;
import weka.classifiers.bayes.net.EditableBayesNet;
import weka.classifiers.functions.*;
import weka.classifiers.lazy.IBk;
import weka.classifiers.lazy.KStar;
import weka.classifiers.lazy.LWL;
import weka.classifiers.meta.*;
import weka.classifiers.rules.*;
import weka.classifiers.trees.*;
import weka.classifiers.trees.lmt.LogisticBase;

import javax.transaction.Transactional;
import javax.validation.Valid;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLOutput;
import java.util.*;
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
    AnalysisRepository analysisRepository;

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
            reviewRepository.save(new Review(review.getTimestamp(), review.getTimeSincePreviousReview(), review.getRating(), review.getAverageProductRating(), review.getReviewText().length(), review.isHasPicture(), review.isHasVideo(), review.isPurchaseVerified(), getSentiment(review.getReviewText()), review.getReviewText(), language.getLang(), password, review.getPersona(), true));
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
        if (reviewWrapper.getReviews().size() == 0) {
            return new ResponseEntity<>(new ResponseMessage("Review list is empty!"), HttpStatus.BAD_REQUEST);
        }
        List<Review> reviews = new LinkedList<>();
        boolean saveReviews = true;
        int sumReviewLength = 0;
        long counter = -1;
        Language previousDetectedLanguage = null;
        for (ReviewModel review : reviewWrapper.getReviews()) {
            if (review.getReviewText().length() < 10000) {
                Language language = getLanguage(review.getReviewText());
                previousDetectedLanguage = language;
                if (language.getLang().equals("de") && language.getConfidence() > 0.95) {
                    Review r = new Review(review.getTimestamp(), review.getTimeSincePreviousReview(), review.getRating(), review.getAverageProductRating(), review.getReviewText().length(), review.isHasPicture(), review.isHasVideo(), review.isPurchaseVerified(), getSentiment(review.getReviewText()), review.getReviewText(), language.getLang(), null, review.getPersona(), false);
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
                } else if (reviewWrapper.getReviews().size() == 1) {
                    return new ResponseEntity<>(new ResponseMessage("Language might be " + language.getLang() + ", but only " + Math.round(language.getConfidence() * 100) + " % confident!"), HttpStatus.BAD_REQUEST);
                }
            } else if (reviewWrapper.getReviews().size() == 1) {
                // Should never happen. Amazon reviews are limited to 5000 characters
                return new ResponseEntity<>(new ResponseMessage("Text is too long!"), HttpStatus.BAD_REQUEST);
            }
        }

        if (reviews.size() == 0) {
            if (previousDetectedLanguage == null) {
                return new ResponseEntity<>(new ResponseMessage("Text is too long!"), HttpStatus.BAD_REQUEST);
            }
            return new ResponseEntity<>(new ResponseMessage("Language might be " + previousDetectedLanguage.getLang() + ", but only " + Math.round(previousDetectedLanguage.getConfidence() * 100) + " % confident!"), HttpStatus.BAD_REQUEST);
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
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
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

        naturalLanguageProcessor.train("de");
        Result nlpResult = naturalLanguageProcessor.calcAccuracy("de");

        //compareAlgorithms(nlpResult.getTotalPersonaAnalysis());
        //updateReviewSentiment();

        wekaPersonaDetection.train("de", nlpResult.getTotalPersonaAnalysis());


        Result wekaResult = wekaPersonaDetection.calcAccuracy("de", null, nlpResult.getTotalPersonaAnalysis());
        System.out.println(wekaResult.getAccuracy() + ", " + wekaResult.getTime());
        statsRepository.deleteByLang("de");
        statsRepository.save(new Stats("de", wekaResult.getAccuracy(), nlpResult.getAccuracy(), wekaResult.getPersonaAccuracies(), nlpResult.getPersonaAccuracies()));

        //analyzeReviewers();

    }

    private void analyzeReviewers() {
        List<Persona> personas = personaRepository.findAll();
        List<Reviewer> reviewers = reviewerRepository.findAll();
        Map<String, Integer> personaIndexMap = new HashMap<>();
        List<String> output = new LinkedList<>();
        String line = "ID";
        int index = 0;
        for (Persona persona : personas) {
            personaIndexMap.put(persona.getName(), index);
            index++;
            line += "," + persona.getName();
        }
        double[] likelihoods = new double[personas.size()];
        output.add(line + "\n");

        int[] personaWinCount = new int[personas.size()];

        for (Reviewer reviewer: reviewers) {
            Map<Long, Map<String, Double>> totalNlpResults = new HashMap<>();
            for (Review review : reviewer.getReviews()) {
                totalNlpResults.put(review.getId(), naturalLanguageProcessor.analyzeText(review.getReviewText()));
            }

            Map<String, Double> wekaResults = wekaPersonaDetection.detectPersona(reviewer.getReviews(), totalNlpResults);
            wekaResults = wekaResults.entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

            line = reviewer.getId() + "";
            for (Persona persona : personas) {
                Integer personaIndex = personaIndexMap.get(persona.getName());
                Double result = wekaResults.get(persona.getName());
                likelihoods[personaIndex] += result;
                line += "," + result;
            }
            output.add(line + "\n");


            String[] keys = (String[]) wekaResults.keySet().toArray(new String[0]);
            Analysis analysis = new Analysis(keys[0], keys[1], keys[2], keys[3], keys[4], keys[5], keys[keys.length - 1], wekaResults.get(keys[0]), wekaResults.get(keys[1]), wekaResults.get(keys[2]), wekaResults.get(keys[3]), wekaResults.get(keys[4]), wekaResults.get(keys[5]), wekaResults.get(keys[keys.length - 1]), calculateActiveness(reviewer.getReviews()), calculateElaborateness(reviewer.getReviews()));
            analysisRepository.save(analysis);

            String winner = keys[0];
            personaWinCount[personaIndexMap.get(winner)]++;

        }

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("reviewerAnalysis.csv"));
            for (String string : output) {
                writer.write(string);
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("ReviewerAnalysis:");
        index = 0;
        System.out.println("Rare numbers");
        for (Persona persona : personas) {
            System.out.println(persona.getName() + ": " + likelihoods[index] / reviewers.size());
            index++;
        }

        System.out.println("Win count");
        for (Persona persona : personas) {
            System.out.println(persona.getName() + ": " + personaWinCount[personaIndexMap.get(persona.getName())]);
        }
    }

    private void compareAlgorithms(Map<Long, Map<String, Double>> totalPersonaAnalysis) {
        List<Classifier> classifiers = new LinkedList<>();
        classifiers.add(new ClassificationViaRegression());
        classifiers.add(new DecisionTable());
        classifiers.add(new LMT()); // very slow
        classifiers.add(new SimpleLogistic());
        //classifiers.add(new LogisticBase()); // cannot deal with null values
        classifiers.add(new RandomForest());
        classifiers.add(new BayesNet());


        //classifiers.add(new NaiveBayesMultinomial()); // cannot deal with negative numbers or missing values
        //classifiers.add(new NaiveBayesMultinomialUpdateable()); // cannot deal with negative numbers or missing numbers
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
