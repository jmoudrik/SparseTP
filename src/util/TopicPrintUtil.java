package util;

import datastructure.Alphabet;
import datastructure.IDSorter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

/**
 * Created by huangwaleking on 5/5/17.
 */
public class TopicPrintUtil {

    /**
     * sort itemsV according to value
     * @param numTypes
     * @return
     */
    public static TreeSet<IDSorter> getSortedWords (int numTypes, int[] itemsV) {
        TreeSet<IDSorter> sortedWords=new TreeSet<IDSorter>();
        for(int v=0;v<numTypes;v++){
            sortedWords.add(new IDSorter(v,itemsV[v]));
        }
        return sortedWords;
    }


    /**
     * sort itemsKV according to value
     * @param numTopics
     * @param numTypes
     * @param itemsKV
     * @return
     */
    public static ArrayList<TreeSet<IDSorter>> getSortedWords (int numTopics, int numTypes,
                                                               int[][] itemsKV) {

        ArrayList<TreeSet<IDSorter>> topicSortedWords = new ArrayList<TreeSet<IDSorter>>(numTopics);

        // Initialize the tree sets
        for (int topic = 0; topic < numTopics; topic++) {
            topicSortedWords.add(new TreeSet<IDSorter>());
        }

        // Collect gamma
        for(int topic=0;topic<numTopics;topic++){
            TreeSet<IDSorter> sortedWords=topicSortedWords.get(topic);
            for(int v=0;v<numTypes;v++){
                sortedWords.add(new IDSorter(v,itemsKV[topic][v]));
            }
        }
        return topicSortedWords;
    }

    public static ArrayList<TreeSet<IDSorter>> getSortedWords (int numTopics, int numTypes,
                                                               double[][] itemsKV) {

        ArrayList<TreeSet<IDSorter>> topicSortedWords = new ArrayList<TreeSet<IDSorter>>(numTopics);

        // Initialize the tree sets
        for (int topic = 0; topic < numTopics; topic++) {
            topicSortedWords.add(new TreeSet<IDSorter>());
        }

        // Collect gamma
        for(int topic=0;topic<numTopics;topic++){
            TreeSet<IDSorter> sortedWords=topicSortedWords.get(topic);
            for(int v=0;v<numTypes;v++){
                sortedWords.add(new IDSorter(v,itemsKV[topic][v]));
            }
        }
        return topicSortedWords;
    }

    public static String showTopics(int numEntityToShow, int topicNum, int numTypes,
                                    double[][] itemsKV, Alphabet alphabet,boolean showDigitNum){
        ArrayList<TreeSet<IDSorter>> topicSortedWords=getSortedWords(topicNum, numTypes, itemsKV);
        return showTopics(numEntityToShow,topicNum,topicSortedWords,alphabet,showDigitNum);
    }


    public static String showTopics(int numEntityToShow, int topicNum, int numTypes,
                                    int[][] itemsKV, Alphabet alphabet,boolean showDigitNum){
        ArrayList<TreeSet<IDSorter>> topicSortedWords=getSortedWords(topicNum, numTypes, itemsKV);
        return showTopics(numEntityToShow,topicNum,topicSortedWords,alphabet,showDigitNum);
    }

    /**
     * different from its variation version that only contains 1 topic.
     * @param numEntityToShow
     * @param numTypes
     * @param itemsV
     * @param alphabet
     * @return
     */
    public static String showSingleTopic(int numEntityToShow, int numTypes,
                                    int[] itemsV, Alphabet alphabet){
        StringBuilder out = new StringBuilder();

        TreeSet<IDSorter> sortedWords=getSortedWords(numTypes, itemsV);
        Iterator<IDSorter> iterator = sortedWords.iterator();
        int i=0;
        out.append ("topic"  + ":\t");
        while (iterator.hasNext() && i < numEntityToShow) {
            IDSorter info = iterator.next();
            out.append(alphabet.lookupObject(info.getID()) + "(" + info.getWeight() + "),");
            i++;
        }
        out.append ("\n");
        return out.toString();
    }

    private static String showTopics(int numEntityToShow, int topicNum,
                                     ArrayList<TreeSet<IDSorter>> topicSortedWords,Alphabet alphabet,boolean showDigitNum){
        StringBuilder out = new StringBuilder();

        // Print results for each topic
        for (int topic = 0; topic < topicNum; topic++) {
            TreeSet<IDSorter> sortedWords = topicSortedWords.get(topic);
            Iterator<IDSorter> iterator = sortedWords.iterator();

            int i=0;
            out.append ("topic" + Integer.toString(topic) + ":\t");
            while (iterator.hasNext() && i < numEntityToShow) {
                IDSorter info = iterator.next();
                if(showDigitNum==true){
                    out.append(alphabet.lookupObject(info.getID()) + "(" + info.getWeight() + "),");
                }else{
                    out.append(alphabet.lookupObject(info.getID()) + ",");
                }
                i++;
            }
            out.append ("\n");
        }
        return out.toString();
    }
}
