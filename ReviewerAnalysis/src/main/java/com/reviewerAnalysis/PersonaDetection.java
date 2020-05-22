package com.reviewerAnalysis;

import com.reviewerAnalysis.data.PersonaRepository;
import com.reviewerAnalysis.data.Review;
import com.reviewerAnalysis.data.ReviewRepository;
import opennlp.tools.doccat.DocumentCategorizerME;
import opennlp.tools.doccat.DocumentSampleStream;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import org.springframework.beans.factory.annotation.Autowired;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.bayes.NaiveBayesUpdateable;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;

import java.io.*;
import java.util.List;

public class PersonaDetection {

    private static PersonaDetection instance = null;

    @Autowired
    PersonaRepository personaRepository;

    @Autowired
    ReviewRepository reviewRepository;

    private NaiveBayes naiveBayes;

    private Instances train;

    public static PersonaDetection getInstance() {
        if (instance == null) {
            instance = new PersonaDetection();
        }
        return instance;
    }

    private PersonaDetection() {}

    public void train() {
        try {
            ConverterUtils.DataSource source1 = new ConverterUtils.DataSource("train.arff");
            train = source1.getDataSet();
            // setting class attribute if the data format does not provide this information
            // For example, the XRFF format saves the class attribute information as well
            if (train.classIndex() == -1)
                train.setClassIndex(train.numAttributes() - 1);



            naiveBayes = new NaiveBayes();
            naiveBayes.buildClassifier(train);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public String detectPersona() {
        try {
            ConverterUtils.DataSource source2 = new ConverterUtils.DataSource("test.arff");
            Instances test = source2.getDataSet();
            // setting class attribute if the data format does not provide this information
            // For example, the XRFF format saves the class attribute information as well
            if (test.classIndex() == -1)
                test.setClassIndex(train.numAttributes() - 1);
            double label = naiveBayes.classifyInstance(test.instance(0));
            test.instance(0).setClassValue(label);

            System.out.println(test.instance(0).stringValue(4));
            return null;//personaRepository.findById((long) Math.ceil(Math.random() * personaRepository.findAll().size())).get().getName();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void writeFile(String lang) {
        InputStream dataIn = null;
        try {
            List<Review> reviews = reviewRepository.findByLang(lang);
            BufferedWriter writer = new BufferedWriter(new FileWriter("train-" + lang + ".arff"));
            writer.write("@RELATION reviews");
            for (Review review : reviews) {
                writer.write("");
            }
            writer.close();
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
}
