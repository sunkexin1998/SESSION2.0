// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) fieldsfirst 
// Source File Name:   EmoticonsList.java

package sentistrength;

import java.io.*;
import java.util.regex.Pattern;

import utilities.FileOps;
import utilities.Sort;

// Referenced classes of package uk.ac.wlv.sentistrength:
//            ClassificationOptions

public class EmoticonsList
{

    private String sgEmoticon[];
    private int igEmoticonStrength[];
    private int igEmoticonCount;
    private int igEmoticonMax;

    public EmoticonsList()
    {
        igEmoticonCount = 0;
        igEmoticonMax = 0;
    }
    
    private boolean isNGramPunc(String text) {
  	  Pattern pattern = Pattern.compile("[\\pP=><|+%$!\\^]+");
  	  return pattern.matcher(text).matches();
    }
    
    public boolean isEmoInTxtEnd(String text) {
    	if(text==null) {
    		return false;
    	}
    	if(text.length()<2) {
    		return false;
    	}
    	String[] words = text.split("\\s+");
    	return isContainEmoticon( words[words.length-1] );
    }
    
    public boolean isContainEmoticon(String word) {
    	if( isEmoticon(word) ) {
    		return true;
    	}else {
    		for(int i=2;i<=5 && i<word.length();i++) {
    			if(word.length()>i) {
    				String sub = word.substring(word.length()-i,word.length());
    				if( isEmoticon(sub) && isNGramPunc(sub) ) {
    					return true;
    				}
    			}
    		}
    		return false;
    	}
    }

    public boolean isEmoticon(String emoticon) {
    	int emoticonSenti = getEmoticon(emoticon);
    	if( emoticonSenti==999 ) {
    		return false;
    	}else{
    		return emoticonSenti!=0;
    	}
    }

    public int getEmoticon(String emoticon)
    {
        int iEmoticon = Sort.i_FindStringPositionInSortedArray(emoticon, sgEmoticon, 1, igEmoticonCount);
        if(iEmoticon >= 0)
            return igEmoticonStrength[iEmoticon];
        else
            return 999;
    }

    public boolean initialise(String sSourceFile, ClassificationOptions options)
    {
        if(igEmoticonCount > 0)
            return true;
        File f = new File(sSourceFile);
        if(!f.exists())
        {
            System.out.println((new StringBuilder("Could not find file: ")).append(sSourceFile).toString());
            return false;
        }
        try
        {
            igEmoticonMax =FileOps.i_CountLinesInTextFile(sSourceFile)+2;
            igEmoticonCount = 0;
            String sEmoticonTemp[] = new String[igEmoticonMax];
            sgEmoticon = sEmoticonTemp;
            int iEmoticonStrengthTemp[] = new int[igEmoticonMax];
            igEmoticonStrength = iEmoticonStrengthTemp;
            BufferedReader rReader;
            if(options.bgForceUTF8)
                rReader = new BufferedReader(new InputStreamReader(new FileInputStream(sSourceFile), "UTF8"));
            else
                rReader = new BufferedReader(new FileReader(sSourceFile));
            String sLine;
            while((sLine = rReader.readLine()) != null) 
                if(sLine != ""){
                	String sData[] = sLine.split("\t");
                    if(sData.length > 1){
                        igEmoticonCount++;
                        sgEmoticon[igEmoticonCount]=sData[0];
                        try{
                            igEmoticonStrength[igEmoticonCount] = Integer.parseInt(sData[1].trim());
                        }
                        catch(NumberFormatException e){
                            System.out.println("Failed to identify integer weight for emoticon! Ignoring emoticon");
                            System.out.println((new StringBuilder("Line: ")).append(sLine).toString());
                            igEmoticonCount--;
                        }
                    }
                }
            rReader.close();
        }
        catch(FileNotFoundException e){
            System.out.println((new StringBuilder("Could not find emoticon file: ")).append(sSourceFile).toString());
            e.printStackTrace();
            return false;
        }
        catch(IOException e)
        {
            System.out.println((new StringBuilder("Found emoticon file but could not read from it: ")).append(sSourceFile).toString());
            e.printStackTrace();
            return false;
        }
        if(igEmoticonCount > 1)
            Sort.quickSortStringsWithInt(sgEmoticon, igEmoticonStrength, 1, igEmoticonCount);
        return true;
    }
    
}
