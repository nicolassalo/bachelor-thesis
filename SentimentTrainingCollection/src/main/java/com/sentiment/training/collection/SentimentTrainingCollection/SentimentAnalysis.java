package com.sentiment.training.collection.SentimentTrainingCollection;

import com.sentiment.training.collection.SentimentTrainingCollection.data.Review;
import com.sentiment.training.collection.SentimentTrainingCollection.data.ReviewRepository;
import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;
import opennlp.tools.doccat.DocumentSampleStream;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.*;
import java.util.LinkedList;
import java.util.List;


public class SentimentAnalysis {

    private static SentimentAnalysis instance = null;

    public static SentimentAnalysis getInstance() {
        if (instance == null) {
            instance = new SentimentAnalysis();
        }
        return instance;
    }

    DoccatModel model;

    @Autowired
    ReviewRepository reviewRepository;



    public void trainModel(List<String> trainingData) {
        InputStream dataIn = null;
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("sentiment-training.txt"));
            for (String string : trainingData) {
                writer.write(string);
            }
            writer.close();

            dataIn = new FileInputStream("sentiment-training.txt");
            ObjectStream lineStream = new PlainTextByLineStream(dataIn, "UTF-8");
            ObjectStream sampleStream = new DocumentSampleStream(lineStream);
            // Specifies the minimum number of times a feature must be seen
            int cutoff = 1;
            int trainingIterations = 30;
            model = DocumentCategorizerME.train("de", sampleStream, cutoff, trainingIterations);
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

    public int classifyNewTweet(String text) {
        DocumentCategorizerME myCategorizer = new DocumentCategorizerME(model);
        double[] outcomes = myCategorizer.categorize(text);
        String category = myCategorizer.getBestCategory(outcomes);

        return Integer.parseInt(category);
    }
}
