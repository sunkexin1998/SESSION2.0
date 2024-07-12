package sentistrength;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

import utilities.FileOps;
import utilities.Sort;

public class SECommonNounList {
	private String sgSECommonNoun[];
    private int igSECommonNounCount;
    private int igSECommonNounMax;

    public SECommonNounList(){
    	igSECommonNounCount = 0;
    	igSECommonNounMax = 0;
    }

    public boolean initialise(String sFilename, ClassificationOptions options){
        if(igSECommonNounMax > 0) {
        	return true;
        }
            
        File f = new File(sFilename);
        if(!f.exists()){
            System.out.println((new StringBuilder("Could not find the negating words file: ")).append(sFilename).toString());
            return false;
        }
        igSECommonNounMax = FileOps.i_CountLinesInTextFile(sFilename) + 2;
        sgSECommonNoun = new String[igSECommonNounMax];
        igSECommonNounCount = 0;
        try{
            BufferedReader rReader;
            if(options.bgForceUTF8)
                rReader = new BufferedReader(new InputStreamReader(new FileInputStream(sFilename), "UTF8"));
            else
                rReader = new BufferedReader(new FileReader(sFilename));
            String sLine;
            while((sLine = rReader.readLine()) != null) 
                if(sLine != ""){
                	int iFirstTabLocation = sLine.indexOf("\t");
                	if(iFirstTabLocation >= 0){
                		sLine = sLine.substring(0, iFirstTabLocation).trim();
                	}
                	igSECommonNounCount++;
                	sgSECommonNoun[igSECommonNounCount] = sLine;
                }
            rReader.close();
            Sort.quickSortStrings(sgSECommonNoun, 1, igSECommonNounCount);
        }
        catch(FileNotFoundException e){
            System.out.println((new StringBuilder("Could not find negating words file: ")).append(sFilename).toString());
            e.printStackTrace();
            return false;
        }
        catch(IOException e){
            System.out.println((new StringBuilder("Found negating words file but could not read from it: ")).append(sFilename).toString());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean isSECommonNoun(String sWord){
        return Sort.i_FindStringPositionInSortedArray(sWord, sgSECommonNoun, 1, igSECommonNounCount) >= 0;
    }
}
