
package sentistrength;

import java.util.HashMap;
import java.util.HashSet;
import rule.PatternProcessor;
import utilities.Sort;
import utilities.StringIndex;
import wkaclass.Arff;

public class Sentence {
    private PatternProcessor pp;
	private Term[] term;
	private int[] beginIndexArr;
    private String  mySentence="";
    private boolean[] bgSpaceAfterTerm;
    private int igTermCount = 0;
	private int igPositiveSentiment = 0;
    private int igNegativeSentiment = 0;
    private int igBCTPosSentiment = 0;
	private int igBCTNegSentiment = 0;
    private boolean bgNothingToClassify = true;
    private ClassificationResources resources;
    private ClassificationOptions options;
    private int[] igSentimentIDList;
    private int igSentimentIDListCount = 0;
    private boolean bSentimentIDListMade = false;
    private boolean[] bgIncludeTerm;
    private boolean bgIdiomsApplied = false;
    private boolean bgObjectEvaluationsApplied = false;
    private String sgClassificationRationale = "";
    private boolean bgLydian=false;
    public boolean isBgLydian() {
  		return bgLydian;
  	}
  	public void setBgLydian(boolean bgLydian) {
  		this.bgLydian = bgLydian;
  	}
    public int getIgTermCount() {
		return igTermCount;
	}
	public String getMySentence() {
		return mySentence;
	}
	public void setMySentence(String mySentence) {
		this.mySentence = mySentence;
	}
	
	private boolean isDirectSentiment = false;
    private boolean isDecoratedSentiment = false;
    private boolean isAboutMe = false;
    private boolean isJudgement = false;
    public boolean isDirectSentiment() {
		return isDirectSentiment;
	}
	public void setDirectSentiment(boolean isDirectSentiment) {
		this.isDirectSentiment = isDirectSentiment;
	}
	public boolean isDecoratedSentiment() {
		return isDecoratedSentiment;
	}
	public void setDecoratedSentiment(boolean isDecoratedSentiment) {
		this.isDecoratedSentiment = isDecoratedSentiment;
	}
	public boolean isAboutMe() {
		return isAboutMe;
	}
	public void setAboutMe(boolean isAboutMe) {
		this.isAboutMe = isAboutMe;
	}
	public boolean isJudgement() {
		return isJudgement;
	}
	public void setJudgement(boolean isJudgement) {
		this.isJudgement = isJudgement;
	}
	
	public Sentence() {}
	
	private void initBeginIndexArr(String sSentence) {
	    this.beginIndexArr = new int[this.igTermCount+1];
	    int deginning = 0;
		for(int i=1;i<=this.igTermCount;i++) {
			beginIndexArr[i] = sSentence.indexOf(this.term[i].getOriginalText(),deginning);
			deginning = beginIndexArr[i] + this.term[i].getOriginalText().length();
		}
		
	}
	
    public void addSentenceToIndex(UnusedTermsClassificationIndex unusedTermClassificationIndex) {
        for(int i = 1; i <= this.igTermCount; ++i) {
            unusedTermClassificationIndex.addTermToNewTermIndex(this.term[i].getText());
        }
    }
	public int addToStringIndex(StringIndex stringIndex, TextParsingOptions textParsingOptions, boolean bRecordCount, boolean bArffIndex) {
        String sEncoded = "";
        int iStringPos =1;
        int iTermsChecked = 0;
        if (textParsingOptions.bgIncludePunctuation && textParsingOptions.igNgramSize == 1 && !textParsingOptions.bgUseTranslations && !textParsingOptions.bgAddEmphasisCode) {
            for(int i = 1; i <= this.igTermCount; ++i) {
                stringIndex.addString(this.term[i].getText(), bRecordCount);
            }

            iTermsChecked = this.igTermCount;
        } else {
            String sText = "";
            int iCurrentTerm = 0;
            int iTermCount = 0;
            while(iCurrentTerm < this.igTermCount) {
                ++iCurrentTerm;
                if (textParsingOptions.bgIncludePunctuation || !this.term[iCurrentTerm].isPunctuation()) {
                    ++iTermCount;
                    if (iTermCount > 1) {
                        sText = sText + " ";
                    } else {
                        sText = "";
                    }

                    if (textParsingOptions.bgUseTranslations) {
                        sText = sText + this.term[iCurrentTerm].getTranslation();
                    } else {
                        sText = sText + this.term[iCurrentTerm].getOriginalText();
                    }

                    if (textParsingOptions.bgAddEmphasisCode && this.term[iCurrentTerm].containsEmphasis()) {
                        sText = sText + "+";
                    }
                }

                if (iTermCount == textParsingOptions.igNgramSize) {
                    if (bArffIndex) {
                        sEncoded = Arff.arffSafeWordEncode(sText.toLowerCase(), false);
                        iStringPos = stringIndex.findString(sEncoded);
                        iTermCount = 0;
                        if (iStringPos > -1) {
                            stringIndex.add1ToCount(iStringPos);
                        }
                    } else {
                        stringIndex.addString(sText.toLowerCase(), bRecordCount);
                        iTermCount = 0;
                    }

                    iCurrentTerm += 1 - textParsingOptions.igNgramSize;
                    ++iTermsChecked;
                }
            }
        }

        return iTermsChecked;
    }

    public void setSentence(String sSentence, ClassificationResources classResources, ClassificationOptions newClassificationOptions) {
    	this.resources = classResources;
        this.options = newClassificationOptions;
        if (this.options.bgAlwaysSplitWordsAtApostrophes && sSentence.indexOf("'") >= 0) {
            sSentence = sSentence.replace("'", " ");
        }
        String[] sSegmentList = sSentence.split(" ");
        int iSegmentListLength = sSegmentList.length;
        int iMaxTermListLength = sSentence.length() + 1;
        this.term = new Term[iMaxTermListLength];
        this.bgSpaceAfterTerm = new boolean[iMaxTermListLength];
        int iPos =0;
        this.igTermCount = 0;

        for(int iSegment = 0; iSegment < iSegmentListLength; ++iSegment) {
            for(iPos = 0; iPos >= 0 && iPos < sSegmentList[iSegment].length(); this.bgSpaceAfterTerm[this.igTermCount] = false) {
                this.term[++this.igTermCount] = new Term();
                int iOffset = this.term[this.igTermCount].extractNextWordOrPunctuationOrEmoticon(sSegmentList[iSegment].substring(iPos), this.resources, this.options);
                if (iOffset < 0) {
                    iPos = iOffset;
                } else {
                    iPos += iOffset;
                }
            }

            this.bgSpaceAfterTerm[this.igTermCount] = true;
        }
        this.bgSpaceAfterTerm[this.igTermCount] = false;
        
        setMySentence(sSentence);
        initBeginIndexArr(sSentence);
        pp=new PatternProcessor();
        pp.setResources(classResources);
        pp.init(sSentence, this.beginIndexArr);
    }

    public int[] getSentimentIDList() {
        if (!this.bSentimentIDListMade) {
            this.makeSentimentIDList();
        }

        return this.igSentimentIDList;
    }

    public void makeSentimentIDList() {
        int iSentimentIDTemp = 0;
        this.igSentimentIDListCount = 0;

        int i;
        for(i = 1; i <= this.igTermCount; ++i) {
            if (this.term[i].getSentimentID() > 0) {
                ++this.igSentimentIDListCount;
            }
        }

        if (this.igSentimentIDListCount > 0) {
            this.igSentimentIDList = new int[this.igSentimentIDListCount + 1];
            this.igSentimentIDListCount = 0;

            for(i = 1; i <= this.igTermCount; ++i) {
                iSentimentIDTemp = this.term[i].getSentimentID();
                if (iSentimentIDTemp > 0) {
                    for(int j = 1; j <= this.igSentimentIDListCount; ++j) {
                        if (iSentimentIDTemp == this.igSentimentIDList[j]) {
                            iSentimentIDTemp = 0;
                            break;
                        }
                    }

                    if (iSentimentIDTemp > 0) {
                        this.igSentimentIDList[++this.igSentimentIDListCount] = iSentimentIDTemp;
                    }
                }
            }

            Sort.quickSortInt(this.igSentimentIDList, 1, this.igSentimentIDListCount);
        }

        this.bSentimentIDListMade = true;
    }

    public String getTaggedSentence() {
        String sTagged = "";
        for(int i = 1; i <= this.igTermCount; ++i) {
            if (this.bgSpaceAfterTerm[i]) {
                sTagged = sTagged + this.term[i].getTag() + " ";
            } else {
                sTagged = sTagged + this.term[i].getTag();
            }
        }
        return sTagged + "<br>";
    }

    public String getClassificationRationale() {
        return this.sgClassificationRationale;
    }

    public String getTranslatedSentence() {
        String sTranslated = "";
        for(int i = 1; i <= this.igTermCount; ++i) {
            if (this.term[i].isWord()) {
                sTranslated = sTranslated + this.term[i].getTranslatedWord();
            } else if (this.term[i].isPunctuation()) {
                sTranslated = sTranslated + this.term[i].getTranslatedPunctuation();
            } else if (this.term[i].isEmoticon()) {
                sTranslated = sTranslated + this.term[i].getEmoticon();
            }

            if (this.bgSpaceAfterTerm[i]) {
                sTranslated = sTranslated + " ";
            }
        }
        return sTranslated + "<br>";
    }

    public void recalculateSentenceSentimentScore() {
        this.calculateSentenceSentimentScore();
    }

    public void reClassifyClassifiedSentenceForSentimentChange(int iSentimentWordID) {
        if (this.igNegativeSentiment == 0) {
            this.calculateSentenceSentimentScore();
        } else {
            if (!this.bSentimentIDListMade) {
                this.makeSentimentIDList();
            }

            if (this.igSentimentIDListCount != 0) {
                if (Sort.i_FindIntPositionInSortedArray(iSentimentWordID, this.igSentimentIDList, 1, this.igSentimentIDListCount) >= 0) {
                    this.calculateSentenceSentimentScore();
                }

            }
        }
    }
    
    public int getSentencePositiveSentiment() {
        if (this.igPositiveSentiment == 0) {
        	this.calculateSentenceSentimentScore(); 
        }
        return Math.max(this.igPositiveSentiment,this.igBCTPosSentiment);
    }

    public int getSentenceNegativeSentiment() {
        if (this.igNegativeSentiment == 0) {
            this.calculateSentenceSentimentScore();
        }
        return Math.min(this.igNegativeSentiment,this.igBCTNegSentiment);
    }

    private void markTermsValidToClassify() {
        this.bgIncludeTerm = new boolean[this.igTermCount + 1];
        int iTermsSinceValid;
        if (this.options.bgIgnoreSentencesWithoutKeywords) {
            this.bgNothingToClassify = true;

            int iTerm;
            for(iTermsSinceValid = 1; iTermsSinceValid <= this.igTermCount; ++iTermsSinceValid) {
                this.bgIncludeTerm[iTermsSinceValid] = false;
                if (this.term[iTermsSinceValid].isWord()) {
                    for(iTerm = 0; iTerm < this.options.sgSentimentKeyWords.length; ++iTerm) {
                        if (this.term[iTermsSinceValid].matchesString(this.options.sgSentimentKeyWords[iTerm], true)) {
                            this.bgIncludeTerm[iTermsSinceValid] = true;
                            this.bgNothingToClassify = false;
                        }
                    }
                }
            }

            if (!this.bgNothingToClassify) {
                iTermsSinceValid = 100000;

                for(iTerm = 1; iTerm <= this.igTermCount; ++iTerm) {
                    if (this.bgIncludeTerm[iTerm]) {
                        iTermsSinceValid = 0;
                    } else if (iTermsSinceValid < this.options.igWordsToIncludeAfterKeyword) {
                        this.bgIncludeTerm[iTerm] = true;
                        if (this.term[iTerm].isWord()) {
                            ++iTermsSinceValid;
                        }
                    }
                }

                iTermsSinceValid = 100000;

                for(iTerm = this.igTermCount; iTerm >= 1; --iTerm) {
                    if (this.bgIncludeTerm[iTerm]) {
                        iTermsSinceValid = 0;
                    } else if (iTermsSinceValid < this.options.igWordsToIncludeBeforeKeyword) {
                        this.bgIncludeTerm[iTerm] = true;
                        if (this.term[iTerm].isWord()) {
                            ++iTermsSinceValid;
                        }
                    }
                }
            }
        } else {
            for(iTermsSinceValid = 1; iTermsSinceValid <= this.igTermCount; ++iTermsSinceValid) {
                this.bgIncludeTerm[iTermsSinceValid] = true;
            }

            this.bgNothingToClassify = false;
        }

    }
    
    private void setCheckReason(HashMap<Integer,boolean[]> checkReasonMap,int index,int checkReasonIndex) {
    	boolean[] checkReasonArr = new boolean[4];
        if( checkReasonMap.containsKey(index) ) {
        	checkReasonArr = checkReasonMap.get(index);
        }
        checkReasonArr[checkReasonIndex] = true;
        checkReasonMap.put(index, checkReasonArr);
    }
    
    private void calculateSentenceSentimentScore() {
    	if (this.options.bgExplainClassification && this.sgClassificationRationale.length() > 0) {
            this.sgClassificationRationale = "";
        }
    	
    	/*checkReasonMap:
         * key: word index in fSentiment[]
         * value: boolean[4] 因为符合哪一个模式而进入检查
         * boolean[0]: isDirectSentiment
         * boolean[1]: isDecoratedSentiment
         * boolean[2]: isAboutMe
         * boolean[3]: isJudgement
         * false: 未符合哪一模式
         * true: 符合哪一模式
         * */
        HashMap<Integer,boolean[]> checkReasonMap = new HashMap<Integer,boolean[]>();
    	HashSet<Integer> checkPoint=new HashSet<Integer>();
        this.igNegativeSentiment = 1;
        this.igPositiveSentiment = 1;
        int iWordTotal = 0;
        int iLastBoosterWordScore = 0;
        int iTemp =0;
        boolean hasNegative = pp.isHasNeg();
        boolean hasNegatedOnNegativeWord =false;
        boolean hasCondiAdvClause=false;
        boolean hasQsMark=false;
        boolean hasTurning=false;
        if (this.igTermCount == 0) {
            this.bgNothingToClassify = true;
            this.igNegativeSentiment = -1;
            this.igPositiveSentiment = 1;
        } else {
            this.markTermsValidToClassify();
            if (this.bgNothingToClassify) {
                this.igNegativeSentiment = -1;
                this.igPositiveSentiment = 1;
            } else {
            	boolean bSentencePunctuationBoost = false;
            	for(int iTerm = 1; iTerm <= this.igTermCount; ++iTerm) {
            		if(this.term[iTerm].punctuationContains("!")){
                		bSentencePunctuationBoost = true; 
                	}
            	}
                float[] fSentiment = new float[this.igTermCount + 1];
                if (this.options.bgUseIdiomLookupTable) {
                    this.overrideTermStrengthsWithIdiomStrengths(false);
                }

                if (this.options.bgUseObjectEvaluationTable) {
                    this.overrideTermStrengthsWithObjectEvaluationStrengths(false);
                }
                for(int iTerm = 1; iTerm <= this.igTermCount; ++iTerm) {
                	if (this.bgIncludeTerm[iTerm]) {
                        int iTermsChecked;
                        if (!this.term[iTerm].isWord()) {
                        	iLastBoosterWordScore=0;
                        	if (this.term[iTerm].isEmoticon()) {
                        		iTermsChecked = this.term[iTerm].getEmoticonSentimentStrength();
                                if (iTermsChecked != 0) {
          	    	        	    ++iWordTotal;
          	    	        	    fSentiment[iWordTotal] = (float)iTermsChecked;
          	    	        	    //setBgLydian(true);
          	    	        	    checkPoint.add(iWordTotal);
          	    	        	    setCheckReason(checkReasonMap,iWordTotal,0);
          	    	        	    if (this.options.bgExplainClassification) {
          	    	        	    	this.sgClassificationRationale = this.sgClassificationRationale + this.term[iTerm].getEmoticon() + " [" + this.term[iTerm].getEmoticonSentimentStrength() + " emoticon]";
                                    }                  
                                }
                            }else if (this.term[iTerm].isPunctuation()) {
                            	if(this.term[iTerm].punctuationContains("?")){
                    				hasQsMark = true;
                    			}
                            	if (this.term[iTerm].getPunctuationEmphasisLength() >= this.options.igMinPunctuationWithExclamationToChangeSentenceSentiment && iWordTotal > 0) {
                                	//System.out.println(this.term[iTerm].getOriginalText());
                            		if(this.term[iTerm].punctuationContains("!")){
                                		this.isDirectSentiment = true;
                                		bSentencePunctuationBoost = true;
                                        if (this.options.bgExplainClassification) {
                                        	this.sgClassificationRationale = this.sgClassificationRationale + this.term[iTerm].getOriginalText();
                                        } 
                                	}
                                }else if (this.options.bgExplainClassification) {
                                	this.sgClassificationRationale = this.sgClassificationRationale + this.term[iTerm].getOriginalText();
                                }
                            }
                        } else {
                        	++iWordTotal;
                        	String lemma = pp.getLemmaByiTerm(iTerm);
                        	String tag = pp.getTagByiTerm(iTerm);
                        	boolean isBeginOfSentence = pp.isBeginOfSentence(iTerm,iWordTotal);
                        	boolean isAboutMe = pp.isAboutMe(iTerm);
                        	
                        	if( isCurseWord(lemma) ) {
                            	fSentiment[iWordTotal] = (float)this.term[iTerm].getSentimentValue();
                            	//setBgLydian(true);
                            	checkPoint.add(iWordTotal);
      	    	        	    setCheckReason(checkReasonMap,iWordTotal,0);
                            	continue;
                            }
                        	
                        	if ( !"SYM".equals(tag) && ( isBeginOfSentence || !"NNP".equals(tag)) ){
                            	fSentiment[iWordTotal] = (float)this.term[iTerm].getSentimentValue(); 
                            	if (this.options.bgExplainClassification) {
                            		 iTemp = this.term[iTerm].getSentimentValue();
                            	     if (iTemp < 0) {
                            	    	 --iTemp;
                                     } else {
                                    	 ++iTemp;
                                     }
                                     if (iTemp == 1) {
                                    	 this.sgClassificationRationale = this.sgClassificationRationale + this.term[iTerm].getOriginalText() + " ";
                                     } else {
                                    	 this.sgClassificationRationale = this.sgClassificationRationale + this.term[iTerm].getOriginalText() + "[" + iTemp + "] ";
                                     }
                                }
                            }else if (this.options.bgExplainClassification ) {
                                this.sgClassificationRationale = this.sgClassificationRationale + this.term[iTerm].getOriginalText() + " [proper noun] ";
                            }
                        	
                        	 //处理多义词:
                            if(options.bgDealWithPolysemy){
                            	if(fSentiment[iWordTotal]!=0) {
                            		float oriSenti = fSentiment[iWordTotal];
                            		//处理有固定搭配的多意词：
                            		overrideTermStrengthsWithPhrase(iTerm);
                            		fSentiment[iWordTotal] = (float)this.term[iTerm].getSentimentValue();
                                	if( lemma.equals("like") ){
                                		fSentiment[iWordTotal] = pp.turnLikeBycontext(iTerm,oriSenti);
                                	}else if( lemma.equals("lie") ){
                                		fSentiment[iWordTotal] = pp.turnLyingBycontext(iTerm,oriSenti);
                                	}else if( lemma.equals("force") || lemma.equals("block") || lemma.equals("value")) {
                                		fSentiment[iWordTotal] = pp.onlySentiInVerb(iTerm,oriSenti);
                                	}else if( lemma.equals("pretty") || lemma.equals("super") || lemma.equals("regardless")) {
                                		fSentiment[iWordTotal] = pp.notSentiInAdv(iTerm, oriSenti);
                                		if(fSentiment[iWordTotal]==0) this.term[iTerm].setBoosterWordScore(2);
                                	}else if( lemma.equals("against") ) {
                                		fSentiment[iWordTotal] = pp.turnAgainstBycontext(iTerm, oriSenti);
                                	}else if( lemma.equals("pain") ) {
                                		fSentiment[iWordTotal] = pp.turnPainBycontext(iTerm, oriSenti);
                                	}
                                	//else if( pp.isEstimateVerbByITerm(iTerm) ) {
                                	//	boolean isExpressEstimation = pp.isExpressEstimationByITerm(iTerm);
                                	//	if( isExpressEstimation ) {
                                	//		fSentiment[iWordTotal] = 0;
                                	//	}
                                	//}
                            	}
                            }
                            
                            //优化SentiStrength原始的boost word处理：
                            int[] boostValueArr = new int[this.igTermCount+1];
                            int boostWordValue = this.term[iTerm].getBoosterWordScore();
                        	if( boostWordValue!=0 ) {
                        		pp.setBoostWord(iTerm, boostWordValue);
                        	}
                        	float oriValue = fSentiment[iWordTotal];
                        	if( oriValue!=0 ) {
                        		HashMap<Integer,Integer> iTermBoostValueMap = pp.getBoostPair(iTerm);
                        		changeBoostValueArr(boostValueArr,iTermBoostValueMap);
                        	}
                        	boolean isAfterBoost = pp.isAfterBoost(iTerm);
                        	if( isAfterBoost ) {
                        		checkPoint.add(iWordTotal);
      	    	        	    setCheckReason(checkReasonMap,iWordTotal,1);
                        	}
                        	if (this.options.bgBoosterWordsChangeEmotion) {
                            	fSentiment[iWordTotal] = updateValue(iTerm,fSentiment[iWordTotal],boostValueArr);
                            }
                            
                        	
                            if (this.options.bgMultipleLettersBoostSentiment && this.term[iTerm].getWordEmphasisLength() >= this.options.igMinRepeatedLettersForBoost && (iTerm == 1 || !this.term[iTerm - 1].isPunctuation() || !this.term[iTerm - 1].getOriginalText().equals("@"))) {
                                String sEmphasis = this.term[iTerm].getWordEmphasis().toLowerCase();
                                if (sEmphasis.indexOf("xx") < 0 && sEmphasis.indexOf("ww") < 0 && sEmphasis.indexOf("ha") < 0) {
                                	if (fSentiment[iWordTotal] < 0.0F) {
                                        fSentiment[iWordTotal] = (float)((double)fSentiment[iWordTotal] - 0.6D);
                                        if (this.options.bgExplainClassification) {
                                            this.sgClassificationRationale = this.sgClassificationRationale + "[-0.6 spelling emphasis] ";
                                        }
                                    } else if (fSentiment[iWordTotal] > 0.0F) {
                                        fSentiment[iWordTotal] = (float)((double)fSentiment[iWordTotal] + 0.6D);
                                        if (this.options.bgExplainClassification) {
                                            this.sgClassificationRationale = this.sgClassificationRationale + "[+0.6 spelling emphasis] ";
                                        }
                                    } else if (this.options.igMoodToInterpretNeutralEmphasis > 0) {
                                        fSentiment[iWordTotal] = (float)((double)fSentiment[iWordTotal] + 0.6D);
                                        if (this.options.bgExplainClassification) {
                                            this.sgClassificationRationale = this.sgClassificationRationale + "[+0.6 spelling mood emphasis] ";
                                        }
                                    } else if (this.options.igMoodToInterpretNeutralEmphasis < 0) {
                                        fSentiment[iWordTotal] = (float)((double)fSentiment[iWordTotal] - 0.6D);
                                        if (this.options.bgExplainClassification) {
                                            this.sgClassificationRationale = this.sgClassificationRationale + "[-0.6 spelling mood emphasis] ";
                                        }
                                    }
                                }
                            }
                            
                            
                            if (this.options.bgCapitalsBoostTermSentiment && fSentiment[iWordTotal] != 0.0F && this.term[iTerm].isAllCapitals()) {
                            	if (fSentiment[iWordTotal] > 0.0F) {
                                    fSentiment[iWordTotal]++;
                                    if (this.options.bgExplainClassification) {
                                        this.sgClassificationRationale = this.sgClassificationRationale + "[+1 CAPITALS] ";
                                    }
                                } else {
                                    fSentiment[iWordTotal]--;
                                    if (this.options.bgExplainClassification) {
                                        this.sgClassificationRationale = this.sgClassificationRationale + "[-1 CAPITALS] ";
                                    }
                                }
                            }
                            
                            
                            if (this.options.bgAllowMultipleNegativeWordsToIncreaseNegativeEmotion && fSentiment[iWordTotal] < -1.0F && iWordTotal > 1 && fSentiment[iWordTotal - 1] < -1.0F) {
                                fSentiment[iWordTotal]--;
                                if (this.options.bgExplainClassification) {
                                    this.sgClassificationRationale = this.sgClassificationRationale + "[-1 consecutive negative words] ";
                                }
                            }

                            
                            if (this.options.bgAllowMultiplePositiveWordsToIncreasePositiveEmotion && fSentiment[iWordTotal] > 1.0F && iWordTotal > 1 && fSentiment[iWordTotal - 1] > 1.0F) {
                                fSentiment[iWordTotal]++;
                                if (this.options.bgExplainClassification) {
                                    this.sgClassificationRationale = this.sgClassificationRationale + "[+1 consecutive positive words] ";
                                }
                            }
                            
                            
                            // 处理否定：
                            if(options.bgDealWithNegative){
                            	String oneWordPreceding = iTerm-1>=1 ? this.term[iTerm-1].getText() : "noResult";
                            	String twoWordPreceding = iTerm-2>=1 ? this.term[iTerm-2].getText() : "noResult";
                            	String precedingText = twoWordPreceding+oneWordPreceding;
                            	if( precedingText.equals("non-") || precedingText.equals("no-") ) {
                            		hasNegative = true;
                            		if( fSentiment[iWordTotal]<0 ) {
                            			hasNegatedOnNegativeWord = true;
                        			}
                            		fSentiment[iWordTotal] = 0;
                            	}
                            	boolean isInNegativeRange = pp.isInNegativeRange(iTerm);
                            	boolean isInStandardNeg = pp.isInStandardNegativeRange(iTerm);
                            	if( isInNegativeRange ) {
                            		if( fSentiment[iWordTotal]<0 && isInStandardNeg) {
                            			hasNegatedOnNegativeWord = true;
                        			}
                            		if( fSentiment[iWordTotal] != 0 ) {
                            			fSentiment[iWordTotal] = 0;
                            		}
                            	}
                            	
                            	//关于问句的探索：
                                boolean isInDoubt = pp.isInQuestion(iTerm);
                                if( isInDoubt ) {
                            		fSentiment[iWordTotal] = 0;
                            	}
                            }
                            
                            
                            //关于虚拟语气的探索：
                            if( this.options.bgDealWithIf ) {
                            	//是否在许愿中
                                boolean isInWish = pp.isInWishByiTerm(iTerm,fSentiment[iWordTotal]);
                                if( !bSentencePunctuationBoost && isInWish ) {
                                	fSentiment[iWordTotal] = 0;
                            	}
                                //是否在含蓄条件句中
                                boolean isInImplicitCondi = pp.isInImplicitCondi(iTerm);
                                if( isInImplicitCondi ) {
                                	hasCondiAdvClause = true;
                            		fSentiment[iWordTotal] = 0;
                            	}
                                //是否在含蓄结论句中
                                boolean isInImplicitConclus = pp.isInImplicitConclus(iTerm);
                                if( isInImplicitConclus ) {
                            		fSentiment[iWordTotal] = 0;
                            	}
                                //是否在显性条件句中
                          	    boolean isInExplicitCondi = pp.isInExplicitCondi(iTerm);
                          	    if( isInExplicitCondi ) {
                          	    	hasCondiAdvClause = true;
                          	    	fSentiment[iWordTotal] = 0;
                          	    }
                          	    //是否在有显性条件句配套的显性结论句中
//                          	    boolean hasExplicitCondi = pp.hasExplicitCondi();
//                          	    boolean isInExplicitConclus = pp.isInExplicitConclus(iTerm);
//                          	    if( hasExplicitCondi && isInExplicitConclus ) {
//                          	    	if( !bSentencePunctuationBoost && !isAboutMe ) {
//                          	    		fSentiment[iWordTotal] = 0;
//                          	    	}
//                          	    }
                            }
                            
                            
                            //试金石相关的集中处理：
                            if(fSentiment[iWordTotal]!=0.0F){
                            	if( (lemma.equals("please") || lemma.equals("plz")) && fSentiment[iWordTotal]>0 ) {
                        			fSentiment[iWordTotal] = 0; 
                        		}
                        		if( pp.isDirectSentiment(iTerm, iWordTotal) ) {
                        			//System.out.println(lemma+": isDirectSentiment");
                        			checkPoint.add(iWordTotal);
          	    	        	    setCheckReason(checkReasonMap,iWordTotal,0);
                        		}
                        		if( pp.isDecoratedSentiment(iTerm) ) {
                        			//System.out.println(lemma+": isDecoratedSentiment");
                        			checkPoint.add(iWordTotal);
          	    	        	    setCheckReason(checkReasonMap,iWordTotal,1);
                        		}
                        		if( isAboutMe ) {
                        			//System.out.println(lemma+": isAboutMe");
                        			checkPoint.add(iWordTotal);
          	    	        	    setCheckReason(checkReasonMap,iWordTotal,2);
                        		}
                        		if( pp.isJudgementSentiment(iTerm) ) {
                        			//System.out.println(lemma+": isJudgementSentiment");
                        			checkPoint.add(iWordTotal);
          	    	        	    setCheckReason(checkReasonMap,iWordTotal,3);
                        		}
                            }
                            
//                            if(fSentiment[iWordTotal]!=0.0F){
//                        		System.out.println(iWordTotal+". "+lemma+":"+fSentiment[iWordTotal]);
//                            }
                            
                         }
                    }
                }
                
                //感叹号规则作用位置一：处理有情绪词的情况
                if(bSentencePunctuationBoost) {
                	int finalSentiPos=-1;
                	for(int i=iWordTotal; i>=1; --i) {
                    	if(fSentiment[i]!=0.0F) {
                    		finalSentiPos=i;
                    		break;
                    	}
                    }
                	if(finalSentiPos!=-1) {
                    	if(fSentiment[finalSentiPos]>0.0F) {
                    		fSentiment[finalSentiPos]++;
                    	}else{
                    		fSentiment[finalSentiPos]--;
                    	}
                    }
                }
                
                float fTotalNeg = 0.0F;
                float fTotalPos = 0.0F;
                float fMaxNeg = 0.0F;
                float fMaxPos = 0.0F;
                int sentiWords = 0;
                int iPosWords = 0;
                int iNegWords = 0;
                float sentiDentity = 0.0F;
                for(int i= 1; i<= iWordTotal; ++i) {
                	if(fSentiment[i]!=0.0F ) {
                		sentiWords++;
                	}
                	if (fSentiment[i] < 0.0F) {
                		fTotalNeg += fSentiment[i];
                        ++iNegWords;
                        if (fMaxNeg > fSentiment[i]) {
                            fMaxNeg = fSentiment[i];
                        }
                    } else if (fSentiment[i] > 0.0F) {
                    	fTotalPos += fSentiment[i];
                        ++iPosWords;
                        if (fMaxPos < fSentiment[i]) {
                            fMaxPos = fSentiment[i];
                        }
                    }
                }
                
                // 试金石相关的集中处理：
//                sentiDentity = (float) (sentiWords) / iWordTotal;
//                if (sentiDentity - 0.3 > 0) {
//                	this.isDirectSentiment = true;
//                } 
                boolean isSimpleSentence = pp.isSimpleSentence();
            	if( isSimpleSentence && sentiWords>0) {
            		//setBgLydian(true);
            		//this.isJudgement = true;
            		this.isDirectSentiment = true;
            	}
            	boolean isCensureQs = pp.isCensureQs();
            	if( isCensureQs ) {
            		//setBgLydian(true);
            		this.isDirectSentiment = true;
                }
            	for(int point:checkPoint) {
            		if( fSentiment[point]!=0.0F ) {
            			boolean[] checkReasonArr = checkReasonMap.get(point);
                        if( checkReasonArr!=null ) {
                        	if( checkReasonArr[0] ) {                         	
                        		this.isDirectSentiment = true;
                            }
                            if( checkReasonArr[1] ) {
                            	this.isDecoratedSentiment = true;
                            }
                            if( checkReasonArr[2] ) {
                            	this.isAboutMe = true;
                            }
                            if( checkReasonArr[3] ) {
                            	this.isJudgement = true;
                            }
                        }
                	}
            	}
            	if( this.isDirectSentiment || this.isDecoratedSentiment || this.isAboutMe || this.isJudgement ) {
                	setBgLydian(true);
                }
                
                --fMaxNeg;
                ++fMaxPos;
                int igEmotionSentenceCombineMethod = this.options.igEmotionSentenceCombineMethod;
                if (igEmotionSentenceCombineMethod == 1) {
                    if (iPosWords == 0) {
                        this.igPositiveSentiment = 1;
                    } else {
                        this.igPositiveSentiment = (int)Math.round(((double)(fTotalPos + (float)iPosWords) + 0.45D) / (double)iPosWords);
                    }

                    if (iNegWords == 0) {
                        this.igNegativeSentiment = -1;
                    } else {
                        this.igNegativeSentiment = (int)Math.round(((double)(fTotalNeg - (float)iNegWords) + 0.55D) / (double)iNegWords);
                    }
                } else if(igEmotionSentenceCombineMethod == 2){
                	this.igPositiveSentiment = Math.round(fTotalPos) + iPosWords;
                    this.igNegativeSentiment = Math.round(fTotalNeg) - iNegWords;
                }else {
                	this.igPositiveSentiment = Math.round(fMaxPos);
                    this.igNegativeSentiment = Math.round(fMaxNeg);
                }
                
                //bgReduceNegativeEmotionInQuestionSentences:false
                if (this.options.bgReduceNegativeEmotionInQuestionSentences && this.igNegativeSentiment < -1) {
                    for(int i=1; i<=this.igTermCount; ++i) {
                        if (this.term[i].isWord()) {
                            if (this.resources.questionWords.questionWord(this.term[i].getTranslatedWord().toLowerCase())) {
                                ++this.igNegativeSentiment;
                                if (this.options.bgExplainClassification) {
                                    this.sgClassificationRationale = this.sgClassificationRationale + "[+1 negative for question word]";
                                }
                                break;
                            }
                        } else if (this.term[i].isPunctuation() && this.term[i].punctuationContains("?")) {
                            ++this.igNegativeSentiment;
                            if (this.options.bgExplainClassification) {
                                this.sgClassificationRationale = this.sgClassificationRationale + "[+1 negative for question mark ?]";
                            }
                            break;
                        }
                    }
                }
                if (this.igPositiveSentiment == 1 && this.options.bgMissCountsAsPlus2) {
                    for(int i= 1; i<= this.igTermCount; ++i) {
                        if (this.term[i].isWord() && this.term[i].getTranslatedWord().toLowerCase().compareTo("miss") == 0) {
                            this.igPositiveSentiment = 2;
                            if (this.options.bgExplainClassification) {
                                this.sgClassificationRationale = this.sgClassificationRationale + "[pos = 2 for term 'miss']";
                            }
                            break;
                        }
                    }
                }
               
                if( isCensureQs ) {
                	if( this.igPositiveSentiment==1 && this.igNegativeSentiment==-1) {
                		--this.igNegativeSentiment;
            			if (this.options.bgExplainClassification) {
                            this.sgClassificationRationale = this.sgClassificationRationale + "[-1 for censure] ";
                        }
                	}
                }
                
                //感叹号规则作用位置二：处理纯中性的情况
                if (bSentencePunctuationBoost) {
                	//是纯中性文本：
                	if( this.igPositiveSentiment==1 && this.igNegativeSentiment==-1) {
                		//同时出现了"?"
                		if( hasQsMark ) {
                			--this.igNegativeSentiment;
                			if (this.options.bgExplainClassification) {
                                this.sgClassificationRationale = this.sgClassificationRationale + "[-1 for ?!] ";
                            }
                		}else {
                			if (this.options.igMoodToInterpretNeutralEmphasis > 0) {
                				boolean hasPatternConflictWithPosTend = ( options.bgDealWithIf && hasCondiAdvClause ) 
                                                                     || ( options.bgDealWithNegative && hasNegative && !hasNegatedOnNegativeWord );
                				if( !hasPatternConflictWithPosTend ) {
                    				++this.igPositiveSentiment;
                                    if (this.options.bgExplainClassification) {
                                        this.sgClassificationRationale = this.sgClassificationRationale + "[+1 for !] ";
                                    }
                    			}else {
                    				if (this.options.bgExplainClassification) {
                                        this.sgClassificationRationale = this.sgClassificationRationale + "[some patterns conflicts with the positive tendency of \"!\"] ";
                                    }
                    			}
                            } else if (this.options.igMoodToInterpretNeutralEmphasis < 0) {
                                --this.igNegativeSentiment;
                                if (this.options.bgExplainClassification) {
                                    this.sgClassificationRationale = this.sgClassificationRationale + "[-1 for !] ";
                                }
                            }
                		}
                	}
                }
                
                //bgExclamationInNeutralSentenceCountsAsPlus2:false;
                if (this.igPositiveSentiment == 1 && this.igNegativeSentiment == -1 && this.options.bgExclamationInNeutralSentenceCountsAsPlus2) {
                    for(int i= 1; i<= this.igTermCount; ++i) {
                        if (this.term[i].isPunctuation() && this.term[i].punctuationContains("!")) {
                            this.igPositiveSentiment = 2;
                            if (this.options.bgExplainClassification) {
                                this.sgClassificationRationale = this.sgClassificationRationale + "[pos = 2 for !]";
                            }
                            break;
                        }
                    }
                }
               
                //bgYouOrYourIsPlus2UnlessSentenceNegative:false;
                if (this.igPositiveSentiment == 1 && this.igNegativeSentiment == -1 && this.options.bgYouOrYourIsPlus2UnlessSentenceNegative) {
                    for(int i= 1; i<= this.igTermCount; ++i) {
                        if (this.term[i].isWord()) {
                            String sTranslatedWord = this.term[i].getTranslatedWord().toLowerCase();
                            if (sTranslatedWord.compareTo("you") == 0 || sTranslatedWord.compareTo("your") == 0 || sTranslatedWord.compareTo("whats") == 0) {
                                this.igPositiveSentiment = 2;
                                if (this.options.bgExplainClassification) {
                                    this.sgClassificationRationale = this.sgClassificationRationale + "[pos = 2 for you/your/whats]";
                                }
                                break;
                            }
                        }
                    }
                }
                
                this.adjustSentimentForIrony();
                
                if (igEmotionSentenceCombineMethod != 2) {
                	 if (this.igPositiveSentiment > 5) {
                         this.igPositiveSentiment = 5;
                     }
                     if (this.igNegativeSentiment < -5) {
                         this.igNegativeSentiment = -5;
                     }
                }

                if (this.options.bgExplainClassification) {
                    this.sgClassificationRationale = this.sgClassificationRationale + "[sentence: " + this.igPositiveSentiment + "," + this.igNegativeSentiment + "]";
                }
            }
        }
    }
    
    private void changeBoostValueArr(int[] boostValueArr,HashMap<Integer,Integer> iTermBoostValueMap) {
    	if( iTermBoostValueMap==null || iTermBoostValueMap.size()==0 ) {
    		return;
    	}
    	for(int iTerm:iTermBoostValueMap.keySet()) {
    		int boostValue = iTermBoostValueMap.get(iTerm);
    		boostValueArr[iTerm] = boostValue;
    	}
    }
    
    private float updateValue(int iTerm,float oriValue,int[] boostValueArr) {
    	if( oriValue==0 ) {
    		return oriValue;
    	}
    	//System.out.println("Word:"+this.term[iTerm].getOriginalText()+", "+oriValue);
    	float value = oriValue;
    	for(int i=iTerm-1;i>=1;i--) {
			int boostValue = boostValueArr[i];
			//遇到了助词
			if( boostValue!=0 ) {
				//System.out.println(this.term[i].getOriginalText()+": "+boostValue );
				value = computeValueAfterBoost(value,boostValue);
			}
		}
    	//System.out.println("After Update:"+value);
    	return value;
    }
    
    private float computeValueAfterBoost(float oriValue,int boostValue) {
		float value = oriValue;
		if ( value>0.0F) {
			value += (float)boostValue;
		}else if ( value<0.0F) {
			value -= (float)boostValue;
        }
		return value;
	}

    private void adjustSentimentForIrony() {
        int iTerm;
        if (this.igPositiveSentiment >= this.options.igMinSentencePosForQuotesIrony) {
            for(iTerm = 1; iTerm <= this.igTermCount; ++iTerm) {
                if (this.term[iTerm].isPunctuation() && this.term[iTerm].getText().indexOf(34) >= 0) {
                    if (this.igNegativeSentiment > -this.igPositiveSentiment) {
                        this.igNegativeSentiment = 1 - this.igPositiveSentiment;
                    }

                    this.igPositiveSentiment = 1;
                    this.sgClassificationRationale = this.sgClassificationRationale + "[Irony change: pos = 1, neg = " + this.igNegativeSentiment + "]";
                    return;
                }
            }
        }

        if (this.igPositiveSentiment >= this.options.igMinSentencePosForPunctuationIrony) {
            for(iTerm = 1; iTerm <= this.igTermCount; ++iTerm) {
                if (this.term[iTerm].isPunctuation() && this.term[iTerm].punctuationContains("!") && this.term[iTerm].getPunctuationEmphasisLength() > 0) {
                    if (this.igNegativeSentiment > -this.igPositiveSentiment) {
                        this.igNegativeSentiment = 1 - this.igPositiveSentiment;
                    }

                    this.igPositiveSentiment = 1;
                    this.sgClassificationRationale = this.sgClassificationRationale + "[Irony change: pos = 1, neg = " + this.igNegativeSentiment + "]";
                    return;
                }
            }
        }

        if (this.igPositiveSentiment >= this.options.igMinSentencePosForTermsIrony) {
            for(iTerm = 1; iTerm <= this.igTermCount; ++iTerm) {
                if (this.resources.ironyList.termIsIronic(this.term[iTerm].getText())) {
                    if (this.igNegativeSentiment > -this.igPositiveSentiment) {
                        this.igNegativeSentiment = 1 - this.igPositiveSentiment;
                    }
                    this.igPositiveSentiment = 1;
                    this.sgClassificationRationale = this.sgClassificationRationale + "[Irony change: pos = 1, neg = " + this.igNegativeSentiment + "]";
                    return;
                }
            }
        }

    }

    public void overrideTermStrengthsWithObjectEvaluationStrengths(boolean recalculateIfAlreadyDone) {
        boolean bMatchingObject = false;
        boolean bMatchingEvaluation = false;
        if (!this.bgObjectEvaluationsApplied || recalculateIfAlreadyDone) {
            for(int iObject = 1; iObject < this.resources.evaluativeTerms.igObjectEvaluationCount; ++iObject) {
                bMatchingObject = false;
                bMatchingEvaluation = false;

                int iTerm;
                for(iTerm = 1; iTerm <= this.igTermCount; ++iTerm) {
                    if (this.term[iTerm].isWord() && this.term[iTerm].matchesStringWithWildcard(this.resources.evaluativeTerms.sgObject[iObject], true)) {
                        bMatchingObject = true;
                        break;
                    }
                }

                if (bMatchingObject) {
                    for(iTerm = 1; iTerm <= this.igTermCount; ++iTerm) {
                        if (this.term[iTerm].isWord() && this.term[iTerm].matchesStringWithWildcard(this.resources.evaluativeTerms.sgObjectEvaluation[iObject], true)) {
                            bMatchingEvaluation = true;
                            break;
                        }
                    }
                }

                if (bMatchingEvaluation) {
                    if (this.options.bgExplainClassification) {
                        this.sgClassificationRationale = this.sgClassificationRationale + "[term weight changed by object/evaluation]";
                    }

                    this.term[iTerm].setSentimentOverrideValue(this.resources.evaluativeTerms.igObjectEvaluationStrength[iObject]);
                }
            }

            this.bgObjectEvaluationsApplied = true;
        }

    }

    public void overrideTermStrengthsWithIdiomStrengths(boolean recalculateIfAlreadyDone) {
        if (!this.bgIdiomsApplied || recalculateIfAlreadyDone) {
            for(int iTerm = 1; iTerm <= this.igTermCount; ++iTerm) {
                if (this.term[iTerm].isWord()) {
                    for(int iIdiom = 1; iIdiom <= this.resources.idiomList.igIdiomCount; ++iIdiom) {
                        if (iTerm + this.resources.idiomList.igIdiomWordCount[iIdiom] - 1 <= this.igTermCount) {
                            boolean bMatchingIdiom = true;

                            int iIdiomTerm;
                            for(iIdiomTerm = 0; iIdiomTerm < this.resources.idiomList.igIdiomWordCount[iIdiom]; ++iIdiomTerm) {
                                if (!this.term[iTerm + iIdiomTerm].matchesStringWithWildcard(this.resources.idiomList.sgIdiomWords[iIdiom][iIdiomTerm], true)) {
                                    bMatchingIdiom = false;
                                    break;
                                }
                            }

                            if (bMatchingIdiom) {
                                if (this.options.bgExplainClassification) {
                                    this.sgClassificationRationale = this.sgClassificationRationale + "[term weight(s) changed by idiom " + this.resources.idiomList.getIdiom(iIdiom) + "]";
                                }

                                this.term[iTerm].setSentimentOverrideValue(this.resources.idiomList.igIdiomStrength[iIdiom]);

                                for(iIdiomTerm = 1; iIdiomTerm < this.resources.idiomList.igIdiomWordCount[iIdiom]; ++iIdiomTerm) {
                                    this.term[iTerm + iIdiomTerm].setSentimentOverrideValue(0);
                                }
                            }
                        }
                    }
                }
            }

            this.bgIdiomsApplied = true;
        }

    }
    
    public void overrideTermStrengthsWithPhrase(int iTerm) {
    	if (this.term[iTerm].isWord()) {
    		for(int iPhrase = 1; iPhrase <= this.resources.phraseList.igPhraseCount; ++iPhrase) {
            	int iTermIndexInPhrase = -1;
            	boolean bMatchingPhrase = true;
            	for(int iPhraseTerm = 0; iPhraseTerm < this.resources.phraseList.igPhraseWordCount[iPhrase]; ++iPhraseTerm ) {
            		if (this.term[iTerm].matchesStringWithWildcard(this.resources.phraseList.sgPhraseWords[iPhrase][iPhraseTerm], true)) {
            			iTermIndexInPhrase = iPhraseTerm;
            	    }
            	}
            	if( iTermIndexInPhrase==-1 ) {
            		bMatchingPhrase = false;
            		continue;
            	}
            	for(int iPhraseTerm = 0; iPhraseTerm < this.resources.phraseList.igPhraseWordCount[iPhrase]; ++iPhraseTerm ) {
            		if( iTerm-iTermIndexInPhrase+iPhraseTerm<1 || iTerm-iTermIndexInPhrase+iPhraseTerm>this.igTermCount) {
            			bMatchingPhrase = false;
            			break;
            		}
            		if ( !this.term[iTerm-iTermIndexInPhrase+iPhraseTerm].matchesStringWithWildcard(this.resources.phraseList.sgPhraseWords[iPhrase][iPhraseTerm], true) ) {
            			bMatchingPhrase = false;
            			break;
                    }
            	}
            	if (bMatchingPhrase) {
                    if (this.options.bgExplainClassification) {
                        this.sgClassificationRationale = this.sgClassificationRationale + "[term weight(s) changed by phrase " + this.resources.phraseList.getPhrase(iPhrase) + "]";
                    }
                    for(int iPhraseTerm = 0; iPhraseTerm < this.resources.phraseList.igPhraseWordCount[iPhrase]; ++iPhraseTerm ) {
                    	this.term[iTerm-iTermIndexInPhrase+iPhraseTerm].setSentimentOverrideValue( 0 );
                	}
                }
            }
        }
    }
    
    private boolean dealWith_I(int iTerm) {
    	if(!isBgLydian()) {
    		String word=this.term[iTerm].getText().toLowerCase();
    		if(word.equals("always") || word.equals("even") || word.equals("still")){
    			return true;
    		}
    	}
    	return false;
    }
    
    public boolean isHave(String word,String[] possWrd) {
    	for(int i=0;i<possWrd.length;i++) {
    		if(word.equals(possWrd[i])) {
				return true;
			}
		}
		return false;
	}
    public boolean isContain(String word,String[] possWrd) {
    	for(int i=0;i<possWrd.length;i++) {
    		if(word.indexOf(possWrd[i])!=-1) {
				return true;
			}
		}
		return false;
	}
    
    private static String[] curseWord={"fuck","damn","shit","hell","suck"};
    private boolean isCurseWord(String word) {
    	return isContain(word,curseWord);
    }
    
    public int getIgBCTPosSentiment() {
		return igBCTPosSentiment;
	}
	public void setIgBCTPosSentiment(int igBCTPosSentiment) {
		this.igBCTPosSentiment = igBCTPosSentiment;
	}
	public int getIgBCTNegSentiment() {
		return igBCTNegSentiment;
	}
	public void setIgBCTNegSentiment(int igBCTNegSentiment) {
		this.igBCTNegSentiment = igBCTNegSentiment;
	}
}