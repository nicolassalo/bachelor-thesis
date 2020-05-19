package com.reviewerAnalysis;

import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;
import opennlp.tools.doccat.DocumentSampleStream;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;

import java.io.*;
import java.util.List;


public class NaturalLanguageProcessor {

    private static NaturalLanguageProcessor instance = null;

    public static NaturalLanguageProcessor getInstance() {
        if (instance == null) {
            instance = new NaturalLanguageProcessor();
        }
        return instance;
    }

    private NaturalLanguageProcessor() {}

    DoccatModel model;

    public void trainModel(String lang, List<String> trainingData) {
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

    public int classifyNewReview(String text) {
        DocumentCategorizerME myCategorizer = new DocumentCategorizerME(model);
        double[] outcomes = myCategorizer.categorize(text);
        String category = myCategorizer.getBestCategory(outcomes);

        return Integer.parseInt(category);
    }
}
