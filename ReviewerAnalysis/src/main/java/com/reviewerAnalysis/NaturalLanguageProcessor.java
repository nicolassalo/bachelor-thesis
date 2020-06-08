package com.reviewerAnalysis;

import com.reviewerAnalysis.controller.ReviewController;
import com.reviewerAnalysis.data.Persona;
import com.reviewerAnalysis.data.PersonaRepository;
import com.reviewerAnalysis.data.Review;
import com.reviewerAnalysis.data.ReviewRepository;
import opennlp.tools.doccat.*;
import opennlp.tools.ml.maxent.GISTrainer;
import opennlp.tools.ml.maxent.quasinewton.QNTrainer;
import opennlp.tools.ml.naivebayes.NaiveBayesEvalParameters;
import opennlp.tools.ml.naivebayes.NaiveBayesTrainer;
import opennlp.tools.ml.perceptron.PerceptronTrainer;
import opennlp.tools.stemmer.snowball.SnowballStemmer;
import opennlp.tools.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Service
public class NaturalLanguageProcessor {

    @Autowired
    ReviewRepository reviewRepository;

    @Autowired
    PersonaRepository personaRepository;

    DoccatModel model;

    DoccatModel accuracyTestModel;

    private boolean isCalculating;

    private NaturalLanguageProcessor() {}

    public boolean isCalculating() {
        return isCalculating;
    }

    public ReviewController.Result calcAccuracy(String lang) {
        isCalculating = true;
        long start = System.currentTimeMillis();
        int correct = 0;
        Map<String, Integer> correctCounter = new HashMap<>();
        Map<String, Integer> wrongCounter = new HashMap<>();
        for (Persona persona : personaRepository.findAllByOrderByIdAsc()) {
            correctCounter.put(persona.getName(), 0);
            wrongCounter.put(persona.getName(), 0);
        }
        List<Review> reviews = reviewRepository.findByLangAndIsForTraining(lang, true);
        System.out.println("calculating accuracy for nlp");
        for (int i = 0; i < reviews.size(); i++) {
            List<String> trainingData = new LinkedList<>();
            int counter = 0;
            for (Review review : reviews) {
                if (review.getPersona() != null && counter != i) {
                    trainingData.add(review.getPersona() + "\t" + editReviewText(review.getReviewText()) + "\n");
                }
                counter++;
            }

            MarkableFileInputStreamFactory dataIn = null;
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter("nlp-accuracy-testing-" + lang + ".txt"));
                for (String string : trainingData) {
                    writer.write(string);
                }
                writer.close();

                dataIn = new MarkableFileInputStreamFactory(new File("nlp-accuracy-testing-" + lang + ".txt"));
                ObjectStream lineStream = new PlainTextByLineStream(dataIn, "UTF-8");
                ObjectStream sampleStream = new DocumentSampleStream(lineStream);
                TrainingParameters params = TrainingParameters.defaultParams();
                params.put(TrainingParameters.ITERATIONS_PARAM, Integer.toString(100));
                params.put(TrainingParameters.CUTOFF_PARAM, Integer.toString(1));
                params.put("PrintMessages", false);
                if (i % 10 == 0) {
                    System.out.println(Math.round(((double) i/reviews.size()) * 100) + " %");
                }
                accuracyTestModel = DocumentCategorizerME.train(lang, sampleStream, params, new DoccatFactory());

            } catch (IOException e) {
                isCalculating = false;
                e.printStackTrace();
            }

            DocumentCategorizerME myCategorizer = new DocumentCategorizerME(accuracyTestModel);
            double[] outcomes = myCategorizer.categorize(editReviewText(reviews.get(i).getReviewText()).split(" "));
            if (myCategorizer.getBestCategory(outcomes).equals(reviews.get(i).getPersona())) {
                correct++;
                correctCounter.put(reviews.get(i).getPersona(), correctCounter.get(reviews.get(i).getPersona()) + 1);
            } else {
                wrongCounter.put(reviews.get(i).getPersona(), wrongCounter.get(myCategorizer.getBestCategory(outcomes)) + 1);
            }
        }
        double accuracy = (double) correct / reviews.size();

        Map<String, Double> personaAccuracies = new HashMap<>();
        for (Persona persona : personaRepository.findAllByOrderByIdAsc()) {
            int rights = correctCounter.get(persona.getName());
            int wrongs = wrongCounter.get(persona.getName());
            personaAccuracies.put(persona.getName(), (double) rights / (rights + wrongs));
        }


        System.out.println("Time: " + (System.currentTimeMillis() - start) + " ms");
        System.out.println("Accuracy: " + accuracy);
        isCalculating = false;
        return new ReviewController.Result(personaAccuracies, accuracy, System.currentTimeMillis() - start);
    }

    public void train(String lang) {
        List<Review> reviews = reviewRepository.findByLangAndIsForTraining(lang, true);
        List<String> trainingData = new LinkedList<>();
        for (Review review : reviews) {
            if (review.getPersona() != null) {
                trainingData.add(review.getPersona() + "\t" + editReviewText(review.getReviewText()) + "\n");
            }
        }

        MarkableFileInputStreamFactory dataIn = null;
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("persona-training-" + lang + ".txt"));
            for (String string : trainingData) {
                writer.write(string);
            }
            writer.close();

            dataIn = new MarkableFileInputStreamFactory(new File("persona-training-" + lang + ".txt"));
            ObjectStream lineStream = new PlainTextByLineStream(dataIn, "UTF-8");
            ObjectStream sampleStream = new DocumentSampleStream(lineStream);

            TrainingParameters params = TrainingParameters.defaultParams();
            System.out.println("default algorithm: " + params.algorithm());
            params.put(TrainingParameters.ITERATIONS_PARAM, 100+"");
            params.put(TrainingParameters.CUTOFF_PARAM, 1+"");
            params.put("PrintMessages", false);
            model = DocumentCategorizerME.train(lang, sampleStream, params, new DoccatFactory());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String classifyNewReview(String text) {
        DocumentCategorizerME myCategorizer = new DocumentCategorizerME(model);
        double[] outcomes = myCategorizer.categorize(editReviewText(text).split(" "));
        return myCategorizer.getBestCategory(outcomes);
    }

    private String editReviewText(String text) {
        return text
                .replaceAll("\\„", " „ ")
                .replaceAll("\\“", " “ ")
                .replaceAll("\\.", " . ")
                .replaceAll("\\!", " ! ")
                .replaceAll("\\,", " , ")
                .replaceAll("\\:", " : ")
                .replaceAll("\\;", " ; ");
    }
}
