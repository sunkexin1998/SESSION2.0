// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) fieldsfirst 
// Source File Name:   NegatingWordList.java

package sentistrength;

import java.io.*;

import utilities.FileOps;
import utilities.Sort;

// Referenced classes of package uk.ac.wlv.sentistrength:
//            ClassificationOptions

public class NegatingWordList
{

    private String sgNegatingWord[];
    private boolean isStandardNegating[];
    private int igNegatingWordCount;
    private int igNegatingWordMax;

    public NegatingWordList()
    {
        igNegatingWordCount = 0;
        igNegatingWordMax = 0;
    }

    public boolean initialise(String sFilename, ClassificationOptions options)
    {
        if(igNegatingWordMax > 0)
            return true;
        File f = new File(sFilename);
        if(!f.exists())
        {
            System.out.println((new StringBuilder("Could not find the negating words file: ")).append(sFilename).toString());
            return false;
        }
        igNegatingWordMax = FileOps.i_CountLinesInTextFile(sFilename) + 2;
        sgNegatingWord = new String[igNegatingWordMax];
        isStandardNegating = new boolean[igNegatingWordMax];
        igNegatingWordCount = 0;
        try
        {
            BufferedReader rReader;
            if(options.bgForceUTF8)
                rReader = new BufferedReader(new InputStreamReader(new FileInputStream(sFilename), "UTF8"));
            else
                rReader = new BufferedReader(new FileReader(sFilename));
            String sLine;
            while((sLine = rReader.readLine()) != null) 
                if(sLine != "" && !sLine.startsWith("#")){
                	String  negatingWord = "";
                	String  isNegString = "";
                	String[] elems = sLine.split("\t");
                	if( elems.length>=1 ) {
                		negatingWord = elems[0].trim();
                	}
                	if( elems.length>=2 ) {
                		isNegString = elems[1].trim();
                	}
                	igNegatingWordCount++;
                    sgNegatingWord[igNegatingWordCount] = negatingWord;
                    boolean isNeg = true;
                    if( isNegString.length()!=0 && Integer.parseInt(isNegString)==0 ) {
                    	isNeg = false;
                    }
                    isStandardNegating[igNegatingWordCount] = isNeg;
                }
            rReader.close();
            Sort.quickSortStringsWithBooleans(sgNegatingWord,isStandardNegating,1,igNegatingWordCount);
        }
        catch(FileNotFoundException e)
        {
            System.out.println((new StringBuilder("Could not find negating words file: ")).append(sFilename).toString());
            e.printStackTrace();
            return false;
        }
        catch(IOException e)
        {
            System.out.println((new StringBuilder("Found negating words file but could not read from it: ")).append(sFilename).toString());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean negatingWord(String sWord){
        return Sort.i_FindStringPositionInSortedArray(sWord, sgNegatingWord, 1, igNegatingWordCount) >= 0;
    }
    
    public boolean isStandardNegating(String sWord){
    	int index = Sort.i_FindStringPositionInSortedArray(sWord, sgNegatingWord, 1, igNegatingWordCount);
        return isStandardNegating[index];
    }
}
