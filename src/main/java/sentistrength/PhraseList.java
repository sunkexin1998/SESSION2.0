package sentistrength;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;

import utilities.FileOps;

public class PhraseList {
	public HashSet<String> allWords;
    public String sgPhrases[];
    public int igPhraseCount;
    public String sgPhraseWords[][];
    int igPhraseWordCount[];
    
    public PhraseList(){
    	igPhraseCount = 0;
    }
    
    public boolean isInPhrases(String word) {
    	return allWords!=null && allWords.contains(word);
    }
    
    public boolean initialise(String sFilename, ClassificationOptions options, int iExtraBlankArrayEntriesToInclude){
    	allWords = new HashSet<String>();
        int iLinesInFile = 0;
        if(sFilename == "") {
        	return false;
        }
        File f = new File(sFilename);
        if(!f.exists()){
            System.out.println((new StringBuilder("Could not find idiom list file: ")).append(sFilename).toString());
            return false;
        }
        iLinesInFile = FileOps.i_CountLinesInTextFile(sFilename);
        sgPhrases = new String[iLinesInFile + 2 + iExtraBlankArrayEntriesToInclude];
        igPhraseCount = 0;
        try{
            BufferedReader rReader;
            if(options.bgForceUTF8) {
            	rReader = new BufferedReader(new InputStreamReader(new FileInputStream(sFilename), "UTF8"));
            }else {
            	rReader = new BufferedReader(new FileReader(sFilename));
            }
            String sLine;
            while((sLine = rReader.readLine()) != null) 
                if(sLine != ""){
                	if(sLine.startsWith("#")) {
                		continue;
                	}
                	sLine = sLine.trim();
                	sLine = sLine.replaceAll("\\s+"," ");
                	igPhraseCount++;
                	sgPhrases[igPhraseCount] = sLine;
                }
            rReader.close();
        }
        catch(FileNotFoundException e) {
            System.out.println((new StringBuilder("Could not find idiom list file: ")).append(sFilename).toString());
            e.printStackTrace();
            return false;
        }
        catch(IOException e) {
            System.out.println((new StringBuilder("Found idiom list file but could not read from it: ")).append(sFilename).toString());
            e.printStackTrace();
            return false;
        }
        convertPhraseStringsToWordLists();
        return true;
    }

    public void convertPhraseStringsToWordLists(){
    	sgPhraseWords = new String[igPhraseCount + 1][10];
    	igPhraseWordCount = new int[igPhraseCount + 1];
        for(int iPhrase = 1; iPhrase <= igPhraseCount; iPhrase++){
            String sWordList[] = sgPhrases[iPhrase].split(" ");
            if(sWordList.length >= 9) {
                System.out.println((new StringBuilder("Ignoring idiom! Too many words in it! (>9): ")).append(sgPhrases[iPhrase]).toString());
            } 
            else {
            	igPhraseWordCount[iPhrase] = sWordList.length;
                for(int iTerm = 0; iTerm < sWordList.length; iTerm++) {
                	sgPhraseWords[iPhrase][iTerm] = sWordList[iTerm];
                    allWords.add( sWordList[iTerm] );
                }
            }
        }

    }

    public String getPhrase(int iPhraseID){
        if(iPhraseID > 0 && iPhraseID < igPhraseCount)
            return sgPhrases[iPhraseID];
        else
            return "";
    }
}
