package com.SentimentAnalysis;

import com.SentimentAnalysis.controller.ReviewController;
import com.SentimentAnalysis.data.Review;
import com.SentimentAnalysis.data.ReviewRepository;
import opennlp.tools.cmdline.doccat.DoccatFineGrainedReportListener;
import opennlp.tools.doccat.*;
import opennlp.tools.util.MarkableFileInputStreamFactory;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.TrainingParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Service
public class SentimentAnalysis {

    private static SentimentAnalysis instance = null;

    @Autowired
    ReviewRepository reviewRepository;

    public static SentimentAnalysis getInstance() {
        if (instance == null) {
            instance = new SentimentAnalysis();
        }
        return instance;
    }

    private SentimentAnalysis() {}

    DoccatModel model;

    DoccatModel accuracyTestModel;

    public void trainModel(String lang, List<String> trainingData) {
        MarkableFileInputStreamFactory dataIn = null;
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("sentiment-training-" + lang + ".txt"));
            for (String string : trainingData) {
                writer.write(string);
            }
            writer.close();

            dataIn = new MarkableFileInputStreamFactory(new File("sentiment-training-" + lang + ".txt"));
            ObjectStream lineStream = new PlainTextByLineStream(dataIn, "UTF-8");
            ObjectStream sampleStream = new DocumentSampleStream(lineStream);
            TrainingParameters params = TrainingParameters.defaultParams();
            params.put(TrainingParameters.ITERATIONS_PARAM, Integer.toString(100));
            params.put(TrainingParameters.CUTOFF_PARAM, Integer.toString(1));
            model = DocumentCategorizerME.train(lang, sampleStream, params, new DoccatFactory());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int classifyNewReview(String text) {
        DocumentCategorizerME myCategorizer = new DocumentCategorizerME(model);
        double[] outcomes = myCategorizer.categorize(text.split(" "));
        String category = myCategorizer.getBestCategory(outcomes);

        return Integer.parseInt(category);
    }

    public void calcAccuracy(String lang) {
        long start = System.currentTimeMillis();
        int correct = 0;
        int wrong = 0;
        Map<Integer, Integer> correctCounter = new HashMap<>();
        Map<Integer, Integer> wrongCounter = new HashMap<>();
        for (int i = 1; i < 6; i++) {
            correctCounter.put(i, 0);
            wrongCounter.put(i, 0);
        }
        List<Review> reviews = reviewRepository.findByLang(lang);
        System.out.println("calculating accuracy for nlp");
        int split = reviews.size() / 5;
        int counter = 0;
        for (int i = 0; i < reviews.size(); i = i + split) {
            List<String> trainingData = new LinkedList<>();

            for (int j = 0; j < reviews.size(); j++) {
                    if (j < i || j >= i + split) {
                        trainingData.add(reviews.get(j).getRating() + "\t" + reviews.get(j).getReviewText() + "\n");
                    } else {
                        counter++;
                    }
            }

            MarkableFileInputStreamFactory dataIn;
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
                accuracyTestModel = DocumentCategorizerME.train(lang, sampleStream, params, new DoccatFactory());

            } catch (IOException e) {
                e.printStackTrace();
            }

            DocumentCategorizerME myCategorizer = new DocumentCategorizerME(accuracyTestModel);
            for (int j = 0; j < split && j < reviews.size() - i; j++) {
                double[] outcomes = myCategorizer.categorize(reviews.get(j + i).getReviewText().split(" "));
                if (myCategorizer.getBestCategory(outcomes).equals(reviews.get(i + j).getRating() + "")) {
                    correct++;
                    int counterBefore = correctCounter.get(reviews.get(i + j).getRating());
                    correctCounter.put(reviews.get(i + j).getRating(), correctCounter.get(reviews.get(i + j).getRating()) + 1);
                    System.out.println("Correct: " + counterBefore + ", " + correctCounter.get(reviews.get(i + j).getRating()));
                } else {
                    wrong++;
                    int counterBefore = wrongCounter.get(reviews.get(i + j).getRating());
                    wrongCounter.put(reviews.get(i + j).getRating(), wrongCounter.get(reviews.get(i + j).getRating()) + 1);
                    System.out.println(counterBefore + ", " + wrongCounter.get(reviews.get(i + j).getRating()));
                }
            }
        }
        double accuracy = (double) correct / reviews.size();

        for (int i = 1; i < 6; i++) {
            int rights = correctCounter.get(i);
            int wrongs = wrongCounter.get(i);
            System.out.println("Rights and Wrongs for " + i + ": " + rights + " / " + wrongs);
            System.out.println("Accuracy for " + i + ": " + (double) rights / (rights + wrongs));
        }

        System.out.println("Read reviews: " + counter);
        System.out.println("Correct reviews: " + correct);
        System.out.println("Wrong reviews: " + wrong);

        System.out.println("Time: " + (System.currentTimeMillis() - start) + " ms");
        System.out.println("Accuracy: " + accuracy);
    }
}
