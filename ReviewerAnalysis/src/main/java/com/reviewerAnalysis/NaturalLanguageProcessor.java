package com.reviewerAnalysis;

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
import java.util.LinkedList;
import java.util.List;

@Service
public class NaturalLanguageProcessor {

    @Autowired
    ReviewRepository reviewRepository;

    DoccatModel model;

    DoccatModel accuracyTestModel;

    private NaturalLanguageProcessor() {}

    public double getAccuracy(String lang) {
        long start = System.currentTimeMillis();
        int correct = 0;
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
                int trainingIterations = 70;
                accuracyTestModel = DocumentCategorizerME.train(lang, sampleStream, cutoff, trainingIterations);
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

            DocumentCategorizerME myCategorizer = new DocumentCategorizerME(accuracyTestModel);
            double[] outcomes = myCategorizer.categorize(editReviewText(reviews.get(i).getReviewText()));
            if (myCategorizer.getBestCategory(outcomes).equals(reviews.get(i).getPersona())) {
                correct++;
            }
        }
        System.out.println("Time: " + (System.currentTimeMillis() - start) + " ms");
        System.out.println("Accuracy: " + ((double) correct / reviews.size()));
        return (double) correct / reviews.size();
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
            int trainingIterations = 30;
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
