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

    private Instances train;

    private PersonaDetection() {}

    public void train(String lang) {
        try {
            String fileName = "train-" + lang + ".arff";
            writeFile(fileName, lang, reviewRepository.findByLang(lang));

            ConverterUtils.DataSource source1 = new ConverterUtils.DataSource("train-" + lang + ".arff");
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


    public String detectPersona(List<Review> reviews) {
        try {
            Iterator<Review> iterator = reviews.iterator();
            while(iterator.hasNext()) {
                iterator.next().setPersona(null);
            }
            String lang = reviews.get(0).getLang();
            String fileName = "predict-" + lang + ".arff";
            writeFile(fileName, lang, reviews);
            ConverterUtils.DataSource source2 = new ConverterUtils.DataSource(fileName);
            Instances test = source2.getDataSet();
            // setting class attribute if the data format does not provide this information
            // For example, the XRFF format saves the class attribute information as well
            if (test.classIndex() == -1)
                test.setClassIndex(train.numAttributes() - 1);
            double label = naiveBayes.classifyInstance(test.instance(0));
            test.instance(0).setClassValue(label);

            System.out.println(test.instance(0).stringValue(4));
            return null;
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

            writer.write("@DATA\n");
            for (Review review : reviews) {
                Stats stats = calculateTextStats(review.getReviewText());
                String string = "";
                string += (review.isHasPicture() ? 1 : 0) + ",";
                string += (review.isHasVideo() ? 1 : 0) + ",";
                string += (review.isPurchaseVerified() ? 1 : 0) + ",";
                string += review.getLength() + ",";
                string += review.getRating() + ",";
                string += review.getSentimentAnalysis() + ",";
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
        writer.write("@ATTRIBUTE consecutiveCaps        NUMERIC\n");
        writer.write("@ATTRIBUTE distinctWordRatio      NUMERIC\n");
        writer.write("@ATTRIBUTE averageWordLength      NUMERIC\n");
        writer.write("@ATTRIBUTE numberOfLineBreaks     NUMERIC\n");
        writer.write("@ATTRIBUTE numberOfQuestionMarks  NUMERIC\n");
        writer.write("@ATTRIBUTE numberOfExclMarks      NUMERIC\n");
        writer.write("@ATTRIBUTE persona                " + personas + "\n\n");
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
