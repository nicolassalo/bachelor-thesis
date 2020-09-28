package com.services;

import opennlp.tools.langdetect.*;
import opennlp.tools.ml.AbstractTrainer;
import opennlp.tools.ml.maxent.quasinewton.QNTrainer;
import opennlp.tools.util.*;

import java.io.File;
import java.io.IOException;

public class LanguageDetector {

    private opennlp.tools.langdetect.LanguageDetector languageDetector;

    private static LanguageDetector instance = null;

    private LanguageDetector() {}

    public static LanguageDetector getInstance() {
        if (instance == null) {
            instance = new LanguageDetector();
        }
        return instance;
    }

    public Language[] detectLanguage(String text) {
        Language[] languages = languageDetector.predictLanguages(text);
        return languages;
    }

    public void trainModel() {
        System.out.println("Training");
        try {
            InputStreamFactory dataIn
                    = new MarkableFileInputStreamFactory(
                    new File("src/main/resources/training/Languages.txt"));
            ObjectStream lineStream = new PlainTextByLineStream(dataIn, "UTF-8");
            LanguageDetectorSampleStream sampleStream
                    = new LanguageDetectorSampleStream(lineStream);
            TrainingParameters params = new TrainingParameters();
            params.put(TrainingParameters.ITERATIONS_PARAM, 100);
            params.put(TrainingParameters.CUTOFF_PARAM, 5);
            params.put("DataIndexer", "TwoPass");
            params.put(AbstractTrainer.ALGORITHM_PARAM, QNTrainer.MAXENT_QN_VALUE);

            LanguageDetectorModel model = LanguageDetectorME
                    .train(sampleStream, params, new LanguageDetectorFactory());

            languageDetector = new LanguageDetectorME(model);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}