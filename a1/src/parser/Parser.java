package parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/**
 * Created by athinavandame on 2016-10-04.
 */
public class Parser {

    public Parser(String startingWord, String[] sentenceSpec) {
        this.startingWord = startingWord;
        this.sentenceSpec = sentenceSpec;
    }

    public Map<String, Map<String, Node>> mapMain = new HashMap<>(); // only has valid sentence spec lines
    public String startingWord;
    public String[] sentenceSpec;
    public ArrayList<Sequence> validSequences = new ArrayList<>();
    public int nodesConsidered = 0;

    public void parseGraphToMainMap(String graph) {
        try (Stream<String> stream = Files.lines(Paths.get(graph))) {
            for (Iterator<String> i = stream.iterator(); i.hasNext(); ) {
                String line = i.next();
                String[] parts = line.split("/");
                String word1 = parts[0];
                String partOfSpeech1 = parts[1];
                String word2 = parts[3];
                String partOfSpeech2 = parts[4];
                String probabilityString = parts[6];
                Float probability = Float.valueOf(probabilityString);
                this.addToMap(word1, partOfSpeech1, word2, partOfSpeech2, probability);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addToMap(String word1, String partOfSpeech1, String word2, String partOfSpeech2, float probability) {
        Map <String, Node> map;
        if (this.mapMain.get(word1) != null) {
            map = this.mapMain.get(word1);
        } else {
            map = new HashMap <>();
        }

        if (this.checkSpecIsValid(partOfSpeech1, partOfSpeech2)) {
            Node node = new Node(partOfSpeech1, partOfSpeech2, probability);
            map.put(word2, node);
            this.mapMain.put(word1, map);
        }
    }

    private boolean checkSpecIsValid(String partOfSpeech1, String partOfSpeech2) {
        boolean valid = false;
        for (int i = 0; i < sentenceSpec.length-1; i++) {
            if (partOfSpeech1.equals(sentenceSpec[i]) && partOfSpeech2.equals(sentenceSpec[i+1])) {
                valid = true;
            }
        }
        return valid;
    }

    public void findSequencesBFS() { // Using out Sequence Structure as a Queue
        Map <String, Node> mapValues = this.getValues(this.startingWord);
        Map.Entry<String, Node> firstEntry = mapValues.entrySet().iterator().next();
        String partOfSpeech1 = firstEntry.getValue().partOfSpeech1;

        if (partOfSpeech1.equals(this.sentenceSpec[0])) {
            for (int i = 0; i < this.sentenceSpec.length-1; i++) {
                if (i == 0) {
                    ArrayList<Float> probabilityArray = new ArrayList<>();
                    Sequence s = new Sequence(this.startingWord, partOfSpeech1, 1, 1, probabilityArray);
                    this.parseFromLastWord(s);
                } else {
                    ArrayList<Sequence> currentSequences = new ArrayList<>(this.validSequences);
                    for(Sequence s: currentSequences) {
                        this.parseFromLastWord(s);
                    }
                }
            }
        }
    }

    public void findSequencesDFS(){ // Using our Sequence Structure as a Stack
        Map <String, Node> mapValues = this.getValues(this.startingWord);
        Map.Entry<String, Node> firstEntry = mapValues.entrySet().iterator().next();
        String partOfSpeech1 = firstEntry.getValue().partOfSpeech1;

        ArrayList<Float> probabilityArray = new ArrayList<>();
        Sequence s = new Sequence(this.startingWord, partOfSpeech1, 1, 1, probabilityArray);
        ArrayList<Integer> levelCounters = new ArrayList<>();
        for (int i = 0; i < this.sentenceSpec.length; i++) {
            levelCounters.add(0);
        }

        boolean goneThroughAll = false;
        while (!goneThroughAll) {
            if (s.level == this.sentenceSpec.length) {
                if (this.isSequenceValid(s)) {
                    Sequence seq = new Sequence(s.sentence, s.sentenceSpec, s.level, s.probability, s.probabilityArray);
                    this.validSequences.add(seq);
                }
                s.removeWordFromSequence();
                levelCounters.set(s.level, levelCounters.get(s.level)+1);
            } else {
                ArrayList<String> word2s = this.getNextWords(s.lastWord);
                if (word2s.size() > 0) {
                    if (levelCounters.get(s.level) >= word2s.size()) {
                        if (s.level == 1) {
                            goneThroughAll = true;
                        } else {
                            s.removeWordFromSequence();
                            levelCounters.set(s.level+1, 0);
                            levelCounters.set(s.level, levelCounters.get(s.level)+1);
                        }
                    } else {
                        String word2 = word2s.get(levelCounters.get(s.level));
                        float probabilityOfWord2 = this.getProbability(s.lastWord, word2);
                        s = s.addWordToSequence(word2, this.getPartOfSpeech2(s.lastWord, word2), probabilityOfWord2);
                    }
                } else {
                    s.removeWordFromSequence();
                    levelCounters.set(s.level+1, 0);
                    levelCounters.set(s.level, levelCounters.get(s.level)+1);
                }
            }
        }

    }

    public Sequence findSequencesHS() { // Greedy Approach
        Map<String, Node> mapValues = this.getValues(this.startingWord);
        Map.Entry<String, Node> firstEntry = mapValues.entrySet().iterator().next();
        String partOfSpeech1 = firstEntry.getValue().partOfSpeech1;

        float greatestProbabilitySoFar = 0;
        String nextWordSoFar = "";
        Node nextNodeSoFar = null;
        ArrayList<Float> probabilityArray = new ArrayList<>();
        Sequence s = new Sequence(this.startingWord, partOfSpeech1, 1, 1, probabilityArray);

        for (int i = 1; i < sentenceSpec.length; i++) {
            if (mapValues != null) {
                for (Map.Entry<String, Node> entry : mapValues.entrySet()) {
                    if (sentenceSpec[i].equals(entry.getValue().partOfSpeech2)) {
                        nodesConsidered++;
                        if (entry.getValue().probability > greatestProbabilitySoFar) {
                            greatestProbabilitySoFar = entry.getValue().probability;
                            nextWordSoFar = entry.getKey();
                            nextNodeSoFar = entry.getValue();
                        }
                    }
                }
                s = s.addWordToSequence(nextWordSoFar, nextNodeSoFar.partOfSpeech2, nextNodeSoFar.probability);
                greatestProbabilitySoFar = 0;
                mapValues = this.getValues(nextWordSoFar);
            }
        }
        return s;
    }

    public float getProbability(String word1, String word2) {
        return (this.mapMain.get(word1).get(word2).probability);
    }

    public String getPartOfSpeech2(String word1, String word2) {
        return this.mapMain.get(word1).get(word2).partOfSpeech2;
    }

    public Map <String, Node> getValues(String word1) {
        return this.mapMain.get(word1);
    }

    public ArrayList<String> getNextWords(String word1) {
        Map <String, Node> mapValues = this.getValues(word1);
        ArrayList<String> output = new ArrayList<>();
        if (mapValues != null) {
            String word2;
            for (Map.Entry<String,Node> entry : mapValues.entrySet()) {
                word2 = entry.getKey();
                output.add(word2);
            }
        }
        return output;
    }

    public void parseFromLastWord(Sequence seq) {
        ArrayList <String> word2List = this.getNextWords(seq.lastWord);
        for (String word2: word2List) {
            ArrayList<Float> probabilityArray = new ArrayList<>();
            Sequence s = new Sequence(seq.sentence, seq.sentenceSpec, seq.level, seq.probability, probabilityArray);
            float probabilityOfWord2 = this.getProbability(seq.lastWord, word2);
            s = s.addWordToSequence(word2, this.getPartOfSpeech2(seq.lastWord, word2), probabilityOfWord2);
            if (isSequenceValid(s)) {
                this.validSequences.add(this.validSequences.size(), s);
            }
        }
    }

    public boolean isSequenceValid(Sequence seq) {
        boolean valid = true;
        String[] seqArray = seq.sentenceSpec.split("-");
        for (int i = 0; i < seqArray.length; i++) {
            if (!seqArray[i].equals(this.sentenceSpec[i])) { valid = false; }
        }
        return valid;
    }

    public Sequence getBestSequence(int level) {
        ArrayList<Float> probabilityArray = new ArrayList<>();
        Sequence best = new Sequence("TTAV", "TT-AV", -1, 0, probabilityArray);
        float probSoFar = 0;
        for(Sequence s: this.validSequences) {
            if (s.level == level && s.probability > probSoFar) {
                probSoFar = s.probability;
                best = s;
            }
            this.nodesConsidered++;
        }
        return best;
    }

    public void printSequences() {
        for (Sequence s: this.validSequences) {
            s.printSequence();
        }
    }

}
