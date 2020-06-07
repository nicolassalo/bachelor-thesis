package com.SentimentAnalysis;

import opennlp.tools.doccat.DoccatFactory;
import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;
import opennlp.tools.doccat.DocumentSampleStream;
import opennlp.tools.util.MarkableFileInputStreamFactory;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.TrainingParameters;

import java.io.*;
import java.util.List;


public class SentimentAnalysis {

    private static SentimentAnalysis instance = null;

    public static SentimentAnalysis getInstance() {
        if (instance == null) {
            instance = new SentimentAnalysis();
        }
        return instance;
    }

    private SentimentAnalysis() {}

    DoccatModel model;


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
}
