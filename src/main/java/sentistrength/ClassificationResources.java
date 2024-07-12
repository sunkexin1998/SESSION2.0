// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) fieldsfirst 
// Source File Name:   ClassificationResources.java

package sentistrength;

import java.io.File;

import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import utilities.FileOps;

// Referenced classes of package uk.ac.wlv.sentistrength:
//            EmoticonsList, CorrectSpellingsList, SentimentWords, NegatingWordList, 
//            QuestionWords, BoosterWordsList, IdiomList, EvaluativeTerms, 
//            IronyList, Lemmatiser, ClassificationOptions

public class ClassificationResources
{
	public EmoticonsList emoticons;
    public CorrectSpellingsList correctSpellings;
    public SentimentWords sentimentWords;
    public NegatingWordList negatingWords;
    public QuestionWords questionWords;
    public BoosterWordsList boosterWords;
    public IdiomList idiomList;
    public EvaluativeTerms evaluativeTerms;
    public IronyList ironyList;
    public Lemmatiser lemmatiser;
    public PhraseList phraseList;
    public SECommonNounList seCommonNounList;
    
    public String sgSentiStrengthFolder;
    public String sgSentimentWordsFile;
    public String sgSentimentWordsFile2;
    public String sgEmoticonLookupTable;
    public String sgCorrectSpellingFileName;
    public String sgCorrectSpellingFileName2;
    public String sgSlangLookupTable;
    public String sgNegatingWordListFile;
    public String sgBoosterListFile;
    public String sgIdiomLookupTableFile;
    public String sgQuestionWordListFile;
    public String sgIronyWordListFile;
    public String sgAdditionalFile;
    public String sgLemmaFile;
    public String sgPhraseLookupTableFile;
    public String sgSECommonNounListFile;

    public ClassificationResources()
    {
    	emoticons = new EmoticonsList();
        correctSpellings = new CorrectSpellingsList();
        sentimentWords = new SentimentWords();
        negatingWords = new NegatingWordList();
        questionWords = new QuestionWords();
        boosterWords = new BoosterWordsList();
        idiomList = new IdiomList();
        evaluativeTerms = new EvaluativeTerms();
        ironyList = new IronyList();
        lemmatiser = new Lemmatiser();
        phraseList = new PhraseList();
        seCommonNounList = new SECommonNounList();
        
        sgSentiStrengthFolder =System.getProperty("user.dir")+"/src/main/resources/SentStrength_Data/";
        sgSentimentWordsFile = "EmotionLookupTable.txt";
        sgSentimentWordsFile2 = "SentimentLookupTable.txt";
        sgEmoticonLookupTable = "EmoticonLookupTable.txt";
        sgCorrectSpellingFileName = "Dictionary.txt";
        sgCorrectSpellingFileName2 = "EnglishWordList.txt";
        sgSlangLookupTable = "SlangLookupTable_NOT_USED.txt";
        sgNegatingWordListFile = "NegatingWordList.txt";
        sgBoosterListFile = "BoosterWordList.txt";
        sgIdiomLookupTableFile = "IdiomLookupTable.txt";
        sgQuestionWordListFile = "QuestionWords.txt";
        sgIronyWordListFile = "IronyTerms.txt";
        sgAdditionalFile = "";
        sgLemmaFile = "";
        sgPhraseLookupTableFile = "PhraseLookUpTable.txt";
        sgSECommonNounListFile = "SECommonNounTable.txt";
    }

    public boolean initialise(ClassificationOptions options){
        int iExtraLinesToReserve = 0;
        
        if(sgAdditionalFile.compareTo("") != 0){
            iExtraLinesToReserve = FileOps.i_CountLinesInTextFile((new StringBuilder(String.valueOf(sgSentiStrengthFolder))).append(sgAdditionalFile).toString());
            if(iExtraLinesToReserve < 0){
                System.out.println((new StringBuilder("No lines found in additional file! Ignoring ")).append(sgAdditionalFile).toString());
                return false;
            }
        }
        
        if(options.bgUseLemmatisation && !lemmatiser.initialise((new StringBuilder(String.valueOf(sgSentiStrengthFolder))).append(sgLemmaFile).toString(), false)){
            System.out.println((new StringBuilder("Can't load lemma file! ")).append(sgLemmaFile).toString());
            return false;
        }
        
        File f = new File((new StringBuilder(String.valueOf(sgSentiStrengthFolder))).append(sgSentimentWordsFile).toString());
        if(!f.exists() || f.isDirectory()) {
            sgSentimentWordsFile = sgSentimentWordsFile2;
        }
        
        File f2 = new File((new StringBuilder(String.valueOf(sgSentiStrengthFolder))).append(sgCorrectSpellingFileName).toString());
        if(!f2.exists() || f2.isDirectory()) {
            sgCorrectSpellingFileName = sgCorrectSpellingFileName2;
        }
        
        if(emoticons.initialise((new StringBuilder(String.valueOf(sgSentiStrengthFolder))).append(sgEmoticonLookupTable).toString(), options) 
           && correctSpellings.initialise((new StringBuilder(String.valueOf(sgSentiStrengthFolder))).append(sgCorrectSpellingFileName).toString(), options) 
           && sentimentWords.initialise((new StringBuilder(String.valueOf(sgSentiStrengthFolder))).append(sgSentimentWordsFile).toString(), options, iExtraLinesToReserve) 
           && negatingWords.initialise((new StringBuilder(String.valueOf(sgSentiStrengthFolder))).append(sgNegatingWordListFile).toString(), options) 
           && questionWords.initialise((new StringBuilder(String.valueOf(sgSentiStrengthFolder))).append(sgQuestionWordListFile).toString(), options) 
           && ironyList.initialise((new StringBuilder(String.valueOf(sgSentiStrengthFolder))).append(sgIronyWordListFile).toString(), options) 
           && boosterWords.initialise((new StringBuilder(String.valueOf(sgSentiStrengthFolder))).append(sgBoosterListFile).toString(), options, iExtraLinesToReserve) 
           && idiomList.initialise((new StringBuilder(String.valueOf(sgSentiStrengthFolder))).append(sgIdiomLookupTableFile).toString(), options, iExtraLinesToReserve)
           && phraseList.initialise((new StringBuilder(String.valueOf(sgSentiStrengthFolder))).append(sgPhraseLookupTableFile).toString(), options, iExtraLinesToReserve)
           && seCommonNounList.initialise((new StringBuilder(String.valueOf(sgSentiStrengthFolder))).append(sgSECommonNounListFile).toString(), options)) {   
        	  
        }
        else {
            return false;
        }
        
        if(iExtraLinesToReserve > 0) {
	    	return evaluativeTerms.initialise((new StringBuilder(String.valueOf(sgSentiStrengthFolder))).append(sgAdditionalFile).toString(), options, idiomList, sentimentWords);
	    }
        else {
        	return true;
        }
    }
    
    public boolean isResourceWord(String word) {
    	if(emoticons!=null && emoticons.isContainEmoticon(word)) {
    		return true;
    	}
    	if(sentimentWords!=null && sentimentWords.isSentimentWord(word)) {
    		return true;
    	}
    	if(negatingWords!=null && negatingWords.negatingWord(word)) {
    		return true;
    	}
    	if(questionWords!=null && questionWords.questionWord(word)) {
    		return true;
    	}
    	if(ironyList!=null && ironyList.termIsIronic(word)) {
    		return true;
    	}
    	if(boosterWords!=null && boosterWords.isBoosterWord(word)) {
    		return true;
    	}
    	if(idiomList!=null && idiomList.isInIdioms(word)) {
    		return true;
    	}
    	if(phraseList!=null && phraseList.isInPhrases(word)) {
    		return true;
    	}
    	return false;
    }
}
