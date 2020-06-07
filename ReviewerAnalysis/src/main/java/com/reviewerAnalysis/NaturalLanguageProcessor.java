package com.reviewerAnalysis;

import com.reviewerAnalysis.controller.ReviewController;
import com.reviewerAnalysis.data.Persona;
import com.reviewerAnalysis.data.PersonaRepository;
import com.reviewerAnalysis.data.Review;
import com.reviewerAnalysis.data.ReviewRepository;
import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;
import opennlp.tools.doccat.DocumentSampleStream;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
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

    private Map<String, Double> accuracies;
    private boolean isCalculating;

    private NaturalLanguageProcessor() {
        accuracies = new HashMap<>();
    }

    public boolean isCalculating() {
        return isCalculating;
    }

    public double getAccuracy(String lang) {
        return accuracies.get(lang) == null ? -1.0 : accuracies.get(lang);
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
        for (int i = 0; i < reviews.size(); i++) {
            List<String> trainingData = new LinkedList<>();
            int counter = 0;
            for (Review review : reviews) {
                if (review.getPersona() != null && counter != i) {
                    trainingData.add(review.getPersona() + "\t" + editReviewText(review.getReviewText()) + "\n");
                }
                counter++;
            }

            InputStream dataIn = null;
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter("nlp-accuracy-testing-" + lang + ".txt"));
                for (String string : trainingData) {
                    writer.write(string);
                }
                writer.close();

                dataIn = new FileInputStream("nlp-accuracy-testing-" + lang + ".txt");
                ObjectStream lineStream = new PlainTextByLineStream(dataIn, "UTF-8");
                ObjectStream sampleStream = new DocumentSampleStream(lineStream);
                // Specifies the minimum number of times a feature must be seen
                int cutoff = 1;
                int trainingIterations = 100;
                accuracyTestModel = DocumentCategorizerME.train(lang, sampleStream, cutoff, trainingIterations);
            } catch (IOException e) {
                isCalculating = false;
                e.printStackTrace();
            } finally {
                if (dataIn != null) {
                    try {
                        dataIn.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            DocumentCategorizerME myCategorizer = new DocumentCategorizerME(accuracyTestModel);
            double[] outcomes = myCategorizer.categorize(editReviewText(reviews.get(i).getReviewText()));
            if (myCategorizer.getBestCategory(outcomes).equals(reviews.get(i).getPersona())) {
                correct++;
                correctCounter.put(reviews.get(i).getPersona(), correctCounter.get(reviews.get(i).getPersona()) + 1);
            } else {
                wrongCounter.put(reviews.get(i).getPersona(), wrongCounter.get(myCategorizer.getBestCategory(outcomes)) + 1);
            }
        }
        double accuracy = (double) correct / reviews.size();
        accuracies.put(lang, accuracy);

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

        InputStream dataIn = null;
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("persona-training-" + lang + ".txt"));
            for (String string : trainingData) {
                writer.write(string);
            }
            writer.close();

            dataIn = new FileInputStream("persona-training-" + lang + ".txt");
            ObjectStream lineStream = new PlainTextByLineStream(dataIn, "UTF-8");
            ObjectStream sampleStream = new DocumentSampleStream(lineStream);
            // Specifies the minimum number of times a feature must be seen
            int cutoff = 1;
            int trainingIterations = 100;
            model = DocumentCategorizerME.train(lang, sampleStream, cutoff, trainingIterations);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (dataIn != null) {
                try {
                    dataIn.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public String classifyNewReview(String text) {
        DocumentCategorizerME myCategorizer = new DocumentCategorizerME(model);
        double[] outcomes = myCategorizer.categorize(editReviewText(text));
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
