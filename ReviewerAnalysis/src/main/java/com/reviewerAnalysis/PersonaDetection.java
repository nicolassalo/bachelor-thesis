package com.reviewerAnalysis;

import com.reviewerAnalysis.data.Persona;
import com.reviewerAnalysis.data.PersonaRepository;
import com.reviewerAnalysis.data.Review;
import com.reviewerAnalysis.data.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
public class PersonaDetection {

    @Autowired
    PersonaRepository personaRepository;

    @Autowired
    ReviewRepository reviewRepository;

    private NaiveBayes naiveBayes;

    private NaiveBayes accuracyNaiveBayes;

    private Instances train;

    private Instances accuracyTrain;

    private PersonaDetection() {}

    public double getAccuracy(String lang) {
        long start = System.currentTimeMillis();
        int correct = 0;
        List<Review> reviews = reviewRepository.findByLangAndIsForTraining(lang, true);
        for (int i = 0; i < reviews.size(); i++) {
            List<String> trainingData = new LinkedList<>();
            int counter = 0;
            try {
                String fileName = "accuracy-test-" + lang + ".arff";
                List<Review> list = reviewRepository.findByLangAndIsForTraining(lang, true);
                list.remove(reviews.get(i));
                writeFile(fileName, lang, list);

                ConverterUtils.DataSource source1 = new ConverterUtils.DataSource("accuracy-test-" + lang + ".arff");
                accuracyTrain = source1.getDataSet();
                if (accuracyTrain.classIndex() == -1) {
                    accuracyTrain.setClassIndex(accuracyTrain.numAttributes() - 1);
                }

                accuracyNaiveBayes = new NaiveBayes();
                accuracyNaiveBayes.buildClassifier(accuracyTrain);
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {

                String fileName = "predict-accuracy-" + lang + ".arff";
                List<Review> predictAccuracy = new LinkedList<>();
                predictAccuracy.add(reviews.get(i));
                writeFile(fileName, lang, predictAccuracy);

                ConverterUtils.DataSource source2 = new ConverterUtils.DataSource(fileName);
                Instances prediction = source2.getDataSet();

                if (prediction.classIndex() == -1) {
                    prediction.setClassIndex(accuracyTrain.numAttributes() - 1);
                }

                List<String> personas = new LinkedList<>();
                for (int j = 0; j < prediction.numInstances(); j++) {
                    double label = accuracyNaiveBayes.classifyInstance(prediction.instance(j));
                    prediction.instance(j).setClassValue(label);
                    String persona = prediction.instance(j).stringValue(prediction.numAttributes() - 1);
                    System.out.println(persona);
                    personas.add(persona);
                }

                if (personas.get(0).equals(reviews.get(i).getPersona())) {
                    correct++;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return 0;
            }
        }

        System.out.println("Time: " + (System.currentTimeMillis() - start) + " ms");
        System.out.println("Accuracy: " + ((double) correct / reviews.size()));
        return (double) correct / reviews.size();
    }

    public void train(String lang) {
        try {
            String fileName = "train-" + lang + ".arff";
            writeFile(fileName, lang, reviewRepository.findByLangAndIsForTraining(lang, true));

            ConverterUtils.DataSource source1 = new ConverterUtils.DataSource("train-" + lang + ".arff");
            train = source1.getDataSet();
            if (train.classIndex() == -1) {
                train.setClassIndex(train.numAttributes() - 1);
            }

            naiveBayes = new NaiveBayes();
            naiveBayes.buildClassifier(train);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public List<String> detectPersona(List<Review> reviews) {
        try {
            Iterator<Review> iterator = reviews.iterator();
            while(iterator.hasNext()) {
                iterator.next().setPersona(null);
            }
            String lang = reviews.get(0).getLang();
            String fileName = "predict-" + lang + ".arff";
            writeFile(fileName, lang, reviews);

            ConverterUtils.DataSource source2 = new ConverterUtils.DataSource(fileName);
            Instances prediction = source2.getDataSet();

            if (prediction.classIndex() == -1) {
                prediction.setClassIndex(train.numAttributes() - 1);
            }

            List<String> personas = new LinkedList<>();
            for (int i = 0; i < prediction.numInstances(); i++) {
                double label = naiveBayes.classifyInstance(prediction.instance(i));
                prediction.instance(i).setClassValue(label);
                String persona = prediction.instance(i).stringValue(prediction.numAttributes() - 1);
                System.out.println(persona);
                personas.add(persona);
            }

            return personas;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void writeFile(String fileName, String lang, List<Review> reviews) {
        InputStream dataIn = null;
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));

            writeBasis(writer, lang);

            for (Review review : reviews) {
                Stats stats = calculateTextStats(review.getReviewText());
                int sentimentRatingOffset = review.getSentimentAnalysis() - review.getRating();
                String string = "";
                string += (review.isHasPicture() ? 1 : 0) + ",";
                string += (review.isHasVideo() ? 1 : 0) + ",";
                string += (review.isPurchaseVerified() ? 1 : 0) + ",";
                string += review.getLength() + ",";
                string += review.getRating() + ",";
                string += review.getSentimentAnalysis() + ",";
                string += sentimentRatingOffset + ",";
                string += stats.getConsecutiveCaps() + ",";
                string += stats.getDistinctWordRatio() + ",";
                string += stats.getAverageWordLength() + ",";
                string += stats.getLineBreaks() + ",";
                string += stats.getQuestionMarks() + ",";
                string += stats.getExclMarks() + ",";
                string += review.getPersona() == null ? "?" : review.getPersona();
                System.out.println(string);
                writer.write(string + "\n");
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

    private void writeBasis(BufferedWriter writer, String lang) throws IOException {
        List<String> personaNames = new LinkedList<>();
        for (Persona persona : personaRepository.findAll()) {
            personaNames.add(persona.getName());
        }
        String personas = "{";
        personas += String.join(",", personaNames) + "}";

        writer.write("@RELATION reviews-" + lang + "\n\n");
        writer.write("@ATTRIBUTE hasPicture             NUMERIC\n");
        writer.write("@ATTRIBUTE hasVideo               NUMERIC\n");
        writer.write("@ATTRIBUTE isPurchaseVerified     NUMERIC\n");
        writer.write("@ATTRIBUTE length                 NUMERIC\n");
        writer.write("@ATTRIBUTE rating                 NUMERIC\n");
        writer.write("@ATTRIBUTE sentimentAnalysis      NUMERIC\n");
        writer.write("@ATTRIBUTE sentimentRatingOffset  NUMERIC\n");
        writer.write("@ATTRIBUTE consecutiveCaps        NUMERIC\n");
        writer.write("@ATTRIBUTE distinctWordRatio      NUMERIC\n");
        writer.write("@ATTRIBUTE averageWordLength      NUMERIC\n");
        writer.write("@ATTRIBUTE numberOfLineBreaks     NUMERIC\n");
        writer.write("@ATTRIBUTE numberOfQuestionMarks  NUMERIC\n");
        writer.write("@ATTRIBUTE numberOfExclMarks      NUMERIC\n");
        writer.write("@ATTRIBUTE persona                " + personas + "\n\n");
        writer.write("@DATA\n");
    }

    private Stats calculateTextStats(String text) {
        int questionMarks = StringUtils.countOccurrencesOf(text, "?");
        int exclMarks = StringUtils.countOccurrencesOf(text, "!");
        int lineBreaks = StringUtils.countOccurrencesOf(text, "<br>");


        text = StringUtils.replace(text, "<br>", " ");
        Scanner scan = new Scanner(text);
        ArrayList<String> words = new ArrayList<String>();
        ArrayList<String> uniqueWords = new ArrayList<String>();
        while (scan.hasNext()) {
            String word = scan.next();
            words.add(word);
            if (!uniqueWords.contains(word)) {
                uniqueWords.add(word);
            }
        }

        int consecutiveCaps = 0;
        for (int i = 0; i < text.length() - 1; i++) {
            if (Character.isUpperCase(text.charAt(i)) && Character.isUpperCase(text.charAt(i + 1))) {
                consecutiveCaps++;
            }
        }

        double averageWordLength = (double) text.length() / words.size();
        double distinctWordRatio = (double) uniqueWords.size() / words.size();
        return new Stats(consecutiveCaps, exclMarks, questionMarks, lineBreaks, distinctWordRatio, averageWordLength);
    }

    private class Stats {
        private int consecutiveCaps;
        private int exclMarks;
        private int questionMarks;
        private int lineBreaks;
        private double distinctWordRatio;
        private double averageWordLength;

        public Stats(int consecutiveCaps, int exclMarks, int questionMarks, int lineBreaks, double distinctWordRatio, double averageWordLength) {
            this.consecutiveCaps = consecutiveCaps;
            this.exclMarks = exclMarks;
            this.questionMarks = questionMarks;
            this.lineBreaks = lineBreaks;
            this.distinctWordRatio = distinctWordRatio;
            this.averageWordLength = averageWordLength;
        }

        public int getConsecutiveCaps() {
            return consecutiveCaps;
        }

        public int getExclMarks() {
            return exclMarks;
        }

        public int getQuestionMarks() {
            return questionMarks;
        }

        public double getDistinctWordRatio() {
            return distinctWordRatio;
        }

        public double getAverageWordLength() {
            return averageWordLength;
        }

        public int getLineBreaks() {
            return lineBreaks;
        }
    }
}
