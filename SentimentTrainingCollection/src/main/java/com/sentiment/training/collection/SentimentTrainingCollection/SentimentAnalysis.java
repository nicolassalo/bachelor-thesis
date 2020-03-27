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


// TODO: make singleton
public class SentimentAnalysis {

    private static String[] str = {
            "3	Watching a nice movie\n",
            "0	The painting is ugly, will return it tomorrow...\n",
            "5	One of the best soccer games , worth seeing it\n",
            "5	Very tasty, not only for vegetarians\n",
            "5	Super party!\n",
            "2	Too early to travel..need a coffee\n",
            "1	Damn ..the train is late again...\n",
            "1	Bad news, my flight just got cancelled.\n",
            "3	Happy birthday mr. president\n",
            "4	Just watch it. Respect.\n",
            "5	Wonderful sunset.\n",
            "4	Bravo, first title in 2014!\n",
            "0	Had a bad evening, need urgently a beer.\n",
            "1	I put on weight again\n",
            "5	On today's show we met Angela, a woman with an amazing story\n",
            "5	I fell in love again\n",
            "1	I lost my keys\n",
            "3	On a trip to Iceland\n",
            "5	Happy in Berlin\n",
            "0	I hate Mondays\n",
            "5	Love the new book I reveived for Christmas\n",
            "0	He killed our good mood\n",
            "4	I am in good spirits again\n",
            "5	This guy creates the most awesome pics ever\n",
            "1	The dark side of a selfie.\n",
            "5	Cool! John is back!\n",
            "4	Many rooms and many hopes for new residents\n",
            "1	False hopes for the people attending the meeting\n",
            "4	I set my new year's resolution\n",
            "0	The ugliest car ever!\n",
            "1	Feeling bored\n",
            "1	Need urgently a pause\n",
            "4	Nice to see Ana made it\n",
            "5	My dream came true\n",
            "2	I didn't see that one coming\n",
            "2	Sorry mate, there is no more room for you\n",
            "2	Who could have possibly done this?\n",
            "4	I won the challenge\n",
            "1	I feel bad for what I did\n",
            "5	I had a great time tonight\n",
            "5	It was a lot of fun\n",
            "5	Thank you Molly making this possible\n",
            "1	I just did a big mistake\n",
            "5	I love it!!\n",
            "3	I never loved so hard in my life\n",
            "0	I hate you Mike!!\n",
            "1	I hate to say goodbye\n",
            "5	Lovely!\n"
    };

    //TODO: make static
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

    public int classifyNewTweet(String tweet) {
        DocumentCategorizerME myCategorizer = new DocumentCategorizerME(model);
        double[] outcomes = myCategorizer.categorize(tweet);
        String category = myCategorizer.getBestCategory(outcomes);

        return Integer.parseInt(category);
    }
}
