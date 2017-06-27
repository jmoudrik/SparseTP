package topicmodel;

import datastructure.*;
import util.Randoms;
import util.TimeClock;
import util.TopicPrintUtil;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by huangwaleking on 6/24/17.
 */
@SuppressWarnings("Duplicates")
public class LDA {
    protected ArrayList<TopicAssignment> data;//include instance and topic assignments

    private int numTopics;
    private double alpha;
    private double beta;
    protected double betaSum;
    private int numWordTypes;//size of word vocabulary
    private int numPhraseTypes;//size of phrase vocabulary

    private Randoms random;
    protected StatisticsOfWords statisticsOfWords;//nKV, nK_

    public LDA(int numTopics, double alpha, double beta) {
        this(numTopics, alpha, beta, new Randoms(0));
    }

    public LDA(int numTopics, double alpha, double beta, Randoms random) {
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
                    for (int i = 0; i < phrase.length-1; i++) {
                        int wordTopic = random.nextInt(numTopics);
                        phraseTopicSequence[position][i] = wordTopic;
                        statisticsOfWords.increase(wordTopic, phrase[i]);
                    }
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
                for (int j = 0; j < phrase.length-1; j++) {
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
                sampleForPhrase(phrases[position], topicSequence[position], ndk);//TODO
            }
        }
    }

    /**
     * compute the topic distribution for a phrase
     */
    public void sampleForPhrase(int[] phrase, int[] phraseTopic, int[] ndk) {

        for (int i = 0; i < phrase.length-1; i++) {
            //reset
            int word = phrase[i];
            int oldTopic = phraseTopic[i];
            this.statisticsOfWords.decrease(oldTopic, word);
            ndk[oldTopic]--;

            //sample
            double[] topic_bucket = new double[numTopics];
            double topic_dist_sum = 0;
            for (int k = 0; k < numTopics; k++) {
                double p = (ndk[k] + alpha) * (this.statisticsOfWords.nKV[k][word] + beta)
                            / (this.statisticsOfWords.nK_[k] + betaSum);
                topic_dist_sum += p;
                topic_bucket[k] = topic_dist_sum;
            }
            int newTopic = -1;
            double sample = random.nextUniform() * topic_dist_sum;
            for (int k = 0; k < numTopics; k++) {
                if (sample < topic_bucket[k]) {
                    newTopic = k;
                    break;
                }
            }
            //update statistics
            this.statisticsOfWords.increase(newTopic, phrase[i]);
            phraseTopic[i] = newTopic;
            ndk[newTopic]++;
        }
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
        double beta = 0.01;

        LDA phraseLDA = new LDA(numTopics, alpha, beta);
        phraseLDA.addInstances(training);
        System.out.println(clock.tick("initialization model"));

        boolean showDigitNum = true;
        for (int i = 0; i < 1000; i++) {
            phraseLDA.sample();
            System.out.println(clock.tick("iteration" + i));
            if (i % 10 == 0) {

                System.out.println(TopicPrintUtil.showTopics(30, numTopics, phraseLDA.numWordTypes,
                        phraseLDA.statisticsOfWords.nKV, training.getAlphabet(), showDigitNum));
                System.out.println(clock.tick("showing topics"));
            }
        }
    }


    public static void main(String[] args) {
        portal("input/20newsgroups.txt.serialized");
//        portal("input/Argentina.json.serialized");
    }

}