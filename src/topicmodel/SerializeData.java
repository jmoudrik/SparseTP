package topicmodel;

import datastructure.*;
import org.json.JSONObject;
import util.MyFile;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

/**
 * Created by huangwaleking on 5/4/17.
 */
public class SerializeData {
    public static void serialize(String inputFilename, String outputFilename, String fileType){
        MyFile reader=new MyFile(""+inputFilename,"r");
        ArrayList<String> lines=reader.readAll();

        Alphabet alphabet=new Alphabet();
        PhraseAlphabet phraseAlphabet=new PhraseAlphabet();
        DocAlphabet docAlphabet=new DocAlphabet();
        InstanceList instances=new InstanceList(alphabet,docAlphabet,phraseAlphabet);

        for(int i=0;i<lines.size();i++){
            String line=lines.get(i).trim();
            if(fileType=="json"){
                try{
                    JSONObject instanceJson=new JSONObject(line);
                    Instance instance=new Instance(instanceJson,alphabet,phraseAlphabet,"isJson");
                    instances.add(instance);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }else{
                Instance instance=new Instance(line,alphabet,phraseAlphabet);
                instances.add(instance);
            }
            if(i%100==0){
                System.out.println("processed "+i+" lines");
            }
        }
        System.out.println("Alphabet's size="+alphabet.size());
        System.out.println("Phrase Alphabets's size="+phraseAlphabet.size());
        System.out.println("Instance number="+instances.size());
        ObjectOutputStream oos;
        try {
            oos = new ObjectOutputStream(new FileOutputStream(outputFilename));
            oos.writeObject(instances);
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args){
        SerializeData.serialize("input/20newsgroups.txt",
                "input/20newsgroups.txt.serialized","txt");
//        SerializeData.serialize("/Users/huangwaleking/git/topicxonomy/data_Argentina/Argentina_final.json",
//                "input/Argentina.json.serialized","json");
    }
}
