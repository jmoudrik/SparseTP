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
public class PhraseLDA {
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

    public PhraseLDA(int numTopics, double alpha, double beta) {
        this(numTopics, alpha, beta, new Randoms());
    }

    public PhraseLDA(int numTopics, double alpha, double beta, Randoms random) {
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
                    for (int i = 0; i < phrase.length-1; i++) {
                        int wordTopic = random.nextInt(numTopics);
                        phraseTopicSequence[position][i] = wordTopic;
                        statisticsOfWords.increase(wordTopic, phrase[i]);
                    }
                    int phraseTopic=random.nextInt(numTopics);
                    phraseTopicSequence[position][phrase.length-1]=phraseTopic;
                    statisticsOfPhrases.increase(phraseTopic,phrase[phrase.length-1]);
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
        //reset
        for (int i = 0; i < phrase.length-1; i++) {
            int word = phrase[i];
            int oldTopic = phraseTopic[i];
            this.statisticsOfWords.decrease(oldTopic, word);
            ndk[oldTopic]--;
        }

        //sample
        double[] topic_bucket = new double[numTopics];
        double topic_dist_sum = 0;
        for (int k = 0; k < numTopics; k++) {
            double p = 1;
            //add the impact of words in the phrase
            for (int n = 0; n < phrase.length-1; n++) {
                int word = phrase[n];
                p *= (ndk[k] + alpha + n) * (this.statisticsOfWords.nKV[k][word] + beta)
                        / (this.statisticsOfWords.nK_[k] + betaSum + n);
            }
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
        assert (newTopic > 0);
        //update statistics
        for (int i = 0; i < phrase.length-1; i++) {
            this.statisticsOfWords.increase(newTopic, phrase[i]);
            phraseTopic[i] = newTopic;
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
            assert (p >= 0);
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
        assert (newTopic >= 0);
        this.statisticsOfWords.increase(newTopic, word);
        phraseTopic[0] = newTopic;
    }

    public void setStatisticsOfPhrases(Alphabet alphabet, PhraseAlphabet phraseAlphabet) {
        for (int doc = 0; doc < data.size(); doc++) {
            TopicAssignment t = data.get(doc);
            int[][] phrases = t.instance.getPhrases();
            int[][] topicSequence = t.topicSequence;
            for (int i = 0; i < phrases.length; i++) {
                int[] phrase = phrases[i];
                if (phrase.length > 1) {
                    StringBuilder sb = new StringBuilder();
                    for (int j = 0; j < phrase.length-1; j++) {
                        sb.append(alphabet.lookupObject(phrase[j]) + " ");
                    }
                    String strPhrase = sb.toString().trim();
                    this.statisticsOfPhrases.increase(topicSequence[i][0], phraseAlphabet.lookupIndex(strPhrase));//TODO, maybe there's out index bug
                }
            }
        }
    }

    public static void portal(String inputFilename) {
        TimeClock clock = new TimeClock();
        InstanceList training = InstanceList.load(new File(inputFilename));
        System.out.println(clock.tick("loading data"));

        int numTopics = 100;
        double alpha = 0.1;
        double beta = 0.01;

        PhraseLDA phraseLDA = new PhraseLDA(numTopics, alpha, beta);
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
        phraseLDA.setStatisticsOfPhrases(training.getAlphabet(), training.getPhraseAlphabet());
        System.out.println(TopicPrintUtil.showTopics(10, numTopics, phraseLDA.numPhraseTypes,
                phraseLDA.statisticsOfPhrases.nKV, training.getPhraseAlphabet(), showDigitNum));
    }


    public static void main(String[] args) {
//        portal("input/20newsgroups.txt.serialized");
        portal("input/Argentina.json.serialized");
    }

}