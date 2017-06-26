package topicmodel;

import datastructure.*;
import util.Randoms;
import util.TimeClock;
import util.TopicPrintUtil;

import java.io.File;
import java.util.ArrayList;

import static java.lang.Math.exp;

/**
 * Created by huangwaleking on 6/26/17.
 */
@SuppressWarnings("Duplicates")
public class TP {
    protected ArrayList<TopicAssignment> data;//include instance and topic assignments

    private int numTopics;
    private double alpha;
    private double beta;
    protected double betaSum;
    private int numWordTypes;//size of word vocabulary
    private int numPhraseTypes;//size of phrase vocabulary

    private Randoms random;
    protected StatisticsOfWords statisticsOfWords;//nKV, nK_
    protected StatisticsOfPhrases statisticsOfPhrases;

    public TP(int numTopics, double alpha, double beta) {
        this(numTopics, alpha, beta, new Randoms());
    }

    public TP(int numTopics, double alpha, double beta, Randoms random) {
        this.numTopics = numTopics;
        this.alpha = alpha;
        this.beta = beta;

        this.random = random;
    }

    public void addInstances(InstanceList training) {
        this.data = new ArrayList<TopicAssignment>();

        this.numWordTypes = training.getAlphabet().size();
        this.numPhraseTypes = training.getPhraseAlphabet().size();
        this.betaSum = this.numWordTypes * this.beta;
        this.statisticsOfWords = new StatisticsOfWords(numTopics, numWordTypes);
        this.statisticsOfPhrases = new StatisticsOfPhrases(numTopics, training.getPhraseAlphabet().size());

        for (Instance instance : training) {
            int[][] phrases = instance.getPhrases();
            int[][] phraseTopicSequence = new int[phrases.length][];

            for (int position = 0; position < instance.getPhraseNum(); position++) {
                int[] phrase = instance.getPhrases()[position];
                phraseTopicSequence[position] = new int[phrase.length];
                if(phrase.length==1){//word
                    int topic = random.nextInt(numTopics);
                    phraseTopicSequence[position][0] = topic;
                    statisticsOfWords.increase(topic, phrase[0]);
                }else{//phrase
                    int numWords=phrase.length-1;
                    int phraseTermPosition=phrase.length-1;
                    for (int i = 0; i < numWords; i++) {
                        int wordTopic = random.nextInt(numTopics);
                        phraseTopicSequence[position][i] = wordTopic;
                        statisticsOfWords.increase(wordTopic, phrase[i]);
                    }
                    int phraseTopic=random.nextInt(numTopics);
                    phraseTopicSequence[position][phraseTermPosition]=phraseTopic;
                    statisticsOfPhrases.increase(phraseTopic,phrase[phraseTermPosition]);
                }
            }

            TopicAssignment t = new TopicAssignment(instance, phraseTopicSequence);
            data.add(t);
        }
    }

    public void sample() {
        for (int doc = 0; doc < data.size(); doc++) {
            TopicAssignment t = data.get(doc);
            int[][] phrases = t.instance.getPhrases();
            int[][] topicSequence = t.topicSequence;
            if (phrases.length > 0) {
                sampleForSingleDoc(phrases, topicSequence);
            }
        }
    }

    public void sampleForSingleDoc(int[][] phrases, int[][] topicSequence) {
        int[] ndk = new int[this.numTopics];
        //update ndk
        for (int i = 0; i < phrases.length; i++) {
            int[] phrase = phrases[i];
            if(phrase.length==1){
                int k=topicSequence[i][0];
                ndk[k]++;
            }else{//phrase
                for (int j = 0; j < phrase.length; j++) {//ndk includes the topics of phrase and words
                    int k = topicSequence[i][j];
                    ndk[k]++;
                }
            }
        }
        //update topicSequence
        for (int position = 0; position < phrases.length; position++) {
            if (phrases[position].length == 1) {
                sampleForWord(phrases[position], topicSequence[position], ndk);
            } else {
                sampleForPhrase(phrases[position], topicSequence[position], ndk);
            }
        }
    }

    /**
     * compute the topic distribution for a phrase
     */
    public void sampleForPhrase(int[] phrase, int[] topicSequence, int[] ndk) {//TODO, to check if it's right
        //sample for word terms
        int phraseTermPosition=phrase.length-1;
        int phraseTerm = phrase[phraseTermPosition];
        int oldPhraseTopic= topicSequence[phraseTermPosition];
        int numWords=phrase.length-1;

        for (int n = 0; n < numWords; n++) {
            //reset
            int word = phrase[n];
            int oldTopic = topicSequence[n];
            this.statisticsOfWords.decrease(oldTopic, word);
            ndk[oldTopic]--;

            double[] topic_bucket = new double[numTopics];
            double topic_dist_sum = 0;
            for (int k = 0; k < numTopics; k++) {
                //add the impact of words in the phrase
                double p = (ndk[k] + alpha) * (this.statisticsOfWords.nKV[k][word] + beta)
                        / (this.statisticsOfWords.nK_[k] + betaSum);
                if(oldPhraseTopic==k){
                    p*=exp(1/(float)numWords);
                }
                topic_dist_sum += p;
                topic_bucket[k] = topic_dist_sum;
            }
            int newWordTopic = -1;
            double sample = random.nextUniform() * topic_dist_sum;
            for (int k = 0; k < numTopics; k++) {
                if (sample < topic_bucket[k]) {
                    newWordTopic = k;
                    break;
                }
            }
            //update statistics
            this.statisticsOfWords.increase(newWordTopic, word);
            topicSequence[n] = newWordTopic;
            ndk[newWordTopic]++;
        }

        //sample for phrase term
        //reset
        this.statisticsOfPhrases.decrease(oldPhraseTopic, phraseTerm);
        ndk[oldPhraseTopic]--;

        //sample
        double[] topic_bucket = new double[numTopics];
        double topic_dist_sum = 0;
        for (int k = 0; k < numTopics; k++) {
            //add the impact of words in the phrase
            double p = (ndk[k] + alpha) * (this.statisticsOfPhrases.nKV[k][phraseTerm] + beta)
                    / (this.statisticsOfPhrases.nK_[k] + betaSum);
            int numOfWordTopicAsK=0;
            for(int n=0;n<numWords;n++){
                if(topicSequence[n]==k){
                    numOfWordTopicAsK++;
                }
            }
            if(numOfWordTopicAsK!=0){
                p*=exp(numOfWordTopicAsK/(float)numWords);
            }
            topic_dist_sum += p;
            topic_bucket[k] = topic_dist_sum;
        }

        int newPhraseTopic = -1;
        double sample = random.nextUniform() * topic_dist_sum;
        for (int k = 0; k < numTopics; k++) {
            if (sample < topic_bucket[k]) {
                newPhraseTopic = k;
                break;
            }
        }
        //update statistics
        this.statisticsOfPhrases.increase(newPhraseTopic, phraseTerm);
        topicSequence[phraseTermPosition] = newPhraseTopic;
        ndk[newPhraseTopic]++;
    }

    /**
     * compute the topic distribution for a_single_word
     */
    private void sampleForWord(int[] phrase, int[] phraseTopic, int[] ndk) {
        int oldTopic = phraseTopic[0];
        int word = phrase[0];

        double[] topic_bucket = new double[numTopics];
        double word_topic_dist_sum = 0;

        //reset
        this.statisticsOfWords.decrease(oldTopic, word);
        ndk[oldTopic]--;

        //sample
        for (int k = 0; k < numTopics; k++) {

            double p = (ndk[k] + alpha) * (this.statisticsOfWords.nKV[k][word] + beta) /
                    (this.statisticsOfWords.nK_[k] + this.betaSum);
            word_topic_dist_sum += p;
            topic_bucket[k] = word_topic_dist_sum;
        }
        int newTopic = -1;
        double sample = random.nextUniform() * word_topic_dist_sum;
        for (int k = 0; k < numTopics; k++) {
            if (sample < topic_bucket[k]) {
                newTopic = k;
                break;
            }
        }
        //update statistics
        this.statisticsOfWords.increase(newTopic, word);
        phraseTopic[0] = newTopic;
        ndk[newTopic]++;
    }

    public static void portal(String inputFilename) {
        TimeClock clock = new TimeClock();
        InstanceList training = InstanceList.load(new File(inputFilename));
        System.out.println(clock.tick("loading data"));

        int numTopics = 100;
        double alpha = 0.1;
        double beta = 0.25;

        TP tp = new TP(numTopics, alpha, beta);
        tp.addInstances(training);
        System.out.println(clock.tick("initialization model"));

        boolean showDigitNum = true;
        for (int i = 0; i < 1000; i++) {
            tp.sample();
            System.out.println(clock.tick("iteration" + i));
            if (i % 10 == 0) {
                System.out.println(TopicPrintUtil.showTopics(30, numTopics, tp.numWordTypes,
                        tp.statisticsOfWords.nKV, training.getAlphabet(), showDigitNum));
                System.out.println(TopicPrintUtil.showTopics(10, numTopics, tp.numPhraseTypes,
                        tp.statisticsOfPhrases.nKV, training.getPhraseAlphabet(), showDigitNum));
                System.out.println(clock.tick("showing topics"));
            }
        }
//        System.out.println(TopicPrintUtil.showTopics(10, numTopics, tp.numPhraseTypes,
//                tp.statisticsOfPhrases.nKV, training.getPhraseAlphabet(), showDigitNum));
    }


    public static void main(String[] args) {
//        portal("input/20newsgroups.txt.serialized");
        portal("input/Argentina.json.serialized");
    }
}
