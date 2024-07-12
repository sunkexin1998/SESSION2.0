package sentistrength;
import java.util.ArrayList;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import preprocess.PreProcess;
import preprocess.Segment;
import utilities.FileOps;
import utilities.Sort;
import utilities.StringIndex;

public class Paragraph {
   private static PreProcess ppr=new PreProcess();
   private Sentence[] sentence;
   private int igSentenceCount = 0;
   private int[] igSentimentIDList;
   private int igSentimentIDListCount = 0;
   private boolean bSentimentIDListMade = false;
   private int igPositiveSentiment = 0;
   private int igNegativeSentiment = 0;
   private int igTrinarySentiment = 0;
   private int igScaleSentiment = 0;
   private ClassificationResources resources;
   private ClassificationOptions options;
   private Random generator = new Random();
   private String sgClassificationRationale = "";
   private String sgExplainSentiPattern = "";
   
   public void addParagraphToIndexWithPosNegValues(UnusedTermsClassificationIndex unusedTermsClassificationIndex, int iCorrectPosClass, int iEstPosClass, int iCorrectNegClass, int iEstNegClass) {
      for(int i = 1; i <= this.igSentenceCount; ++i) {
         this.sentence[i].addSentenceToIndex(unusedTermsClassificationIndex);
      }

      unusedTermsClassificationIndex.addNewIndexToMainIndexWithPosNegValues(iCorrectPosClass, iEstPosClass, iCorrectNegClass, iEstNegClass);
   }

   public void addParagraphToIndexWithScaleValues(UnusedTermsClassificationIndex unusedTermsClassificationIndex, int iCorrectScaleClass, int iEstScaleClass) {
      for(int i = 1; i <= this.igSentenceCount; ++i) {
         this.sentence[i].addSentenceToIndex(unusedTermsClassificationIndex);
      }

      unusedTermsClassificationIndex.addNewIndexToMainIndexWithScaleValues(iCorrectScaleClass, iEstScaleClass);
   }

   public void addParagraphToIndexWithBinaryValues(UnusedTermsClassificationIndex unusedTermsClassificationIndex, int iCorrectBinaryClass, int iEstBinaryClass) {
      for(int i = 1; i <= this.igSentenceCount; ++i) {
         this.sentence[i].addSentenceToIndex(unusedTermsClassificationIndex);
      }

      unusedTermsClassificationIndex.addNewIndexToMainIndexWithBinaryValues(iCorrectBinaryClass, iEstBinaryClass);
   }

   public int addToStringIndex(StringIndex stringIndex, TextParsingOptions textParsingOptions, boolean bRecordCount, boolean bArffIndex) {
      int iTermsChecked = 0;

      for(int i = 1; i <= this.igSentenceCount; ++i) {
         iTermsChecked += this.sentence[i].addToStringIndex(stringIndex, textParsingOptions, bRecordCount, bArffIndex);
      }

      return iTermsChecked;
   }

   public void addParagraphToIndexWithTrinaryValues(UnusedTermsClassificationIndex unusedTermsClassificationIndex, int iCorrectTrinaryClass, int iEstTrinaryClass) {
      for(int i = 1; i <= this.igSentenceCount; ++i) {
         this.sentence[i].addSentenceToIndex(unusedTermsClassificationIndex);
      }

      unusedTermsClassificationIndex.addNewIndexToMainIndexWithTrinaryValues(iCorrectTrinaryClass, iEstTrinaryClass);
   }
   
   //2.1相比于2.0主要修改的是对括号的处理
   String bstStrRegex = "\\(SUBSENTENCE_INDEXOF\\-([0-9]+)\\)";
   Pattern bctPattern = Pattern.compile(bstStrRegex);
   public void setParagraph(String sParagraph, ClassificationResources classResources, ClassificationOptions newClassificationOptions){
	   this.resources = classResources;
	   this.options = newClassificationOptions;
	   ArrayList<String> processedArray=null;
	   ArrayList<String> bctArr=null;
	   Sentence[] bctSentence=null;
	   try {
		   ppr.setBgPreprocess(options.bgPreprocess);
		   ppr.init(resources);
		   Segment seg = ppr.getProcessedArray(sParagraph);
		   processedArray = seg.getCommonSentences();
		   bctArr = seg.getBctSentences();
	   } catch (Exception e) {
		e.printStackTrace();
	   }
	   
	   int bctArrSize=bctArr.size();
	   int[] bctPosArr =null;
 	   int[] bctNegArr =null;
 	   boolean[] bctLydianArr =null;
 	   boolean[] bctDirectSentimentLydianArr =null;
 	   boolean[] bctDecoratedSentimentLydianArr =null;
 	   boolean[] bctAboutMeLydianArr =null;
 	   boolean[] bctJudgementLydianArr =null;
	   if(bctArrSize!=0) {
		   bctSentence=new Sentence[bctArrSize];
		   bctPosArr =new int[bctArrSize];
		   bctNegArr =new int[bctArrSize];
		   bctLydianArr=new boolean[bctArrSize];
		   bctDirectSentimentLydianArr = new boolean[bctArrSize];
		   bctDecoratedSentimentLydianArr = new boolean[bctArrSize];
		   bctAboutMeLydianArr = new boolean[bctArrSize];
		   bctJudgementLydianArr = new boolean[bctArrSize];
		   for(int i=0;i<bctArrSize;i++){
			   bctSentence[i] = new Sentence();
			   bctSentence[i].setSentence(bctArr.get(i), this.resources, this.options);
			   int posScore=bctSentence[i].getSentencePositiveSentiment();
    		   int negScore=bctSentence[i].getSentenceNegativeSentiment();
    		   bctPosArr[i]=posScore;
    		   bctNegArr[i]=negScore;
    		   bctLydianArr[i]=bctSentence[i].isBgLydian();
    		   bctDirectSentimentLydianArr[i]=bctSentence[i].isDirectSentiment();
    		   bctDecoratedSentimentLydianArr[i]=bctSentence[i].isDecoratedSentiment();
    		   bctAboutMeLydianArr[i]=bctSentence[i].isAboutMe();
    		   bctJudgementLydianArr[i]=bctSentence[i].isJudgement();
    	   }
	   }
	   
	   int pedArrayLength=processedArray.size();
	   this.sentence = new Sentence[pedArrayLength+1];
	   this.igSentenceCount = 0;
	   for(int i=0;i<pedArrayLength;i++) {
		   String sNextSentence=processedArray.get(i);
		   boolean sentenceLydian = false;
		   boolean bct_isDirectSentiment = false;
		   boolean bct_isDecoratedSentiment = false;
		   boolean bct_isAboutMe = false;
		   boolean bct_isJudgement = false;
		   int maxBCTPos = 1;
		   int minBCTNeg = -1;
		   /*float totalBCTPos = 0;
		   float totalBCTNeg = 0;*/
		   //段落中存在括号子句
		   if(bctArrSize!=0) {
			   Matcher matcher = bctPattern.matcher(sNextSentence);
		       while (matcher.find()) {
		    	   String bctString = matcher.group();
		           int bctIndexStart = "(SUBSENTENCE_INDEXOF-".length();
		           int index = Integer.parseInt( bctString.substring(bctIndexStart,bctString.length()-1) );
		           boolean bctLydian = bctLydianArr[index];
		           boolean bctDirectLydian = bctDirectSentimentLydianArr[index];
		           boolean bctDecoratedLydian = bctDecoratedSentimentLydianArr[index];
		           boolean bctAboutMeLydian = bctAboutMeLydianArr[index];
		           boolean bctJudgementLydian = bctJudgementLydianArr[index];
		           int bctPos = bctPosArr[index];
				   int bctNeg = bctNegArr[index];
				   if(bctLydian) { sentenceLydian = true;}
				   if(bctDirectLydian) { bct_isDirectSentiment = true;}
				   if(bctDecoratedLydian) { bct_isDecoratedSentiment = true;}
				   if(bctAboutMeLydian) { bct_isAboutMe = true;}
				   if(bctJudgementLydian) { bct_isJudgement = true;}
				   if(bctPos>maxBCTPos) {
					   maxBCTPos = bctPos;
				   }
				   if(bctNeg<minBCTNeg) {
					   minBCTNeg = bctNeg;
				   } 
				   /* totalBCTPos+=bctPos;
				   totalBCTNeg+=bctNeg; */
				   sNextSentence = sNextSentence.replace(bctString,"");
		       }
		   }
		   ++this.igSentenceCount;
		   this.sentence[this.igSentenceCount] = new Sentence();
		   this.sentence[this.igSentenceCount].setSentence(sNextSentence,this.resources, this.options);
	       this.sentence[this.igSentenceCount].setBgLydian(sentenceLydian);
	       this.sentence[this.igSentenceCount].setIgBCTPosSentiment(maxBCTPos);
	       this.sentence[this.igSentenceCount].setIgBCTNegSentiment(minBCTNeg);
	       /* this.sentence[this.igSentenceCount].setDirectSentiment(bct_isDirectSentiment);
	       this.sentence[this.igSentenceCount].setDecoratedSentiment(bct_isDecoratedSentiment);
	       this.sentence[this.igSentenceCount].setAboutMe(bct_isAboutMe);
	       this.sentence[this.igSentenceCount].setJudgement(bct_isJudgement);*/
	       /* this.sentence[this.igSentenceCount].setIgTotalNeg(totalBCTNeg);
	       this.sentence[this.igSentenceCount].setIgTotalPos(totalBCTPos); */
	   }
   }
   
   public int[] getSentimentIDList() {
      if (!this.bSentimentIDListMade) {
         this.makeSentimentIDList();
      }

      return this.igSentimentIDList;
   }

   public String getClassificationRationale() {
      return this.sgClassificationRationale;
   }
   
   public String getSentiPattern() {
       return this.sgExplainSentiPattern;
   }

   public void makeSentimentIDList() {
      boolean bIsDuplicate = false;
      this.igSentimentIDListCount = 0;

      int i;
      for(i = 1; i <= this.igSentenceCount; ++i) {
         if (this.sentence[i].getSentimentIDList() != null) {
            this.igSentimentIDListCount += this.sentence[i].getSentimentIDList().length;
         }
      }

      if (this.igSentimentIDListCount > 0) {
         this.igSentimentIDList = new int[this.igSentimentIDListCount + 1];
         this.igSentimentIDListCount = 0;

         for(i = 1; i <= this.igSentenceCount; ++i) {
            int[] sentenceIDList = this.sentence[i].getSentimentIDList();
            if (sentenceIDList != null) {
               for(int j = 1; j < sentenceIDList.length; ++j) {
                  if (sentenceIDList[j] != 0) {
                     bIsDuplicate = false;

                     for(int k = 1; k <= this.igSentimentIDListCount; ++k) {
                        if (sentenceIDList[j] == this.igSentimentIDList[k]) {
                           bIsDuplicate = true;
                           break;
                        }
                     }

                     if (!bIsDuplicate) {
                        this.igSentimentIDList[++this.igSentimentIDListCount] = sentenceIDList[j];
                     }
                  }
               }
            }
         }

         Sort.quickSortInt(this.igSentimentIDList, 1, this.igSentimentIDListCount);
      }

      this.bSentimentIDListMade = true;
   }

   public String getTaggedParagraph() {
      String sTagged = "";

      for(int i = 1; i <= this.igSentenceCount; ++i) {
         sTagged = sTagged + this.sentence[i].getTaggedSentence();
      }

      return sTagged;
   }

   public String getTranslatedParagraph() {
      String sTranslated = "";

      for(int i = 1; i <= this.igSentenceCount; ++i) {
         sTranslated = sTranslated + this.sentence[i].getTranslatedSentence();
      }

      return sTranslated;
   }

   public void recalculateParagraphSentimentScores() {
      for(int iSentence = 1; iSentence <= this.igSentenceCount; ++iSentence) {
         this.sentence[iSentence].recalculateSentenceSentimentScore();
      }

      this.calculateParagraphSentimentScores();
   }

   public void reClassifyClassifiedParagraphForSentimentChange(int iSentimentWordID) {
      if (this.igNegativeSentiment == 0) {
         this.calculateParagraphSentimentScores();
      } else {
         if (!this.bSentimentIDListMade) {
            this.makeSentimentIDList();
         }

         if (this.igSentimentIDListCount != 0) {
            if (Sort.i_FindIntPositionInSortedArray(iSentimentWordID, this.igSentimentIDList, 1, this.igSentimentIDListCount) >= 0) {
               for(int iSentence = 1; iSentence <= this.igSentenceCount; ++iSentence) {
                  this.sentence[iSentence].reClassifyClassifiedSentenceForSentimentChange(iSentimentWordID);
               }

               this.calculateParagraphSentimentScores();
            }

         }
      }
   }

   public int getParagraphPositiveSentiment() {
      if (this.igPositiveSentiment == 0) {
         this.calculateParagraphSentimentScores();
      }

      return this.igPositiveSentiment;
   }

   public int getParagraphNegativeSentiment() {
      if (this.igNegativeSentiment == 0) {
         this.calculateParagraphSentimentScores();
      }

      return this.igNegativeSentiment;
   }
   
   public int getParagraphTrinarySentiment() {
      if (this.igNegativeSentiment == 0) {
         this.calculateParagraphSentimentScores();
      }
      return this.igTrinarySentiment;
   }

   public int getParagraphScaleSentiment() {
      if (this.igNegativeSentiment == 0) {
         this.calculateParagraphSentimentScores();
      }

      return this.igScaleSentiment;
   }
   
   private void updateScoreBasedPattern(int patternIndex,int iPosTemp, int iNegTemp, int[] iPosMaxForPatterns,int[] iNegMaxForPatterns,int[] iPosTotalForPatterns,int[] iNegTotalForPatterns) {
   	if (iNegTemp != 0 || iPosTemp != 0) {
			iNegTotalForPatterns[patternIndex] += iNegTemp;
           if (iNegMaxForPatterns[patternIndex] > iNegTemp) {
           	iNegMaxForPatterns[patternIndex] = iNegTemp;
           }
           iPosTotalForPatterns[patternIndex] += iPosTemp;
           if (iPosMaxForPatterns[patternIndex] < iPosTemp) {
           	iPosMaxForPatterns[patternIndex] = iPosTemp;
           }
       }
   }
   
   private void updateExplainPattern(int[] iPosMaxForPatterns,int[] iNegMaxForPatterns,int[] iPosTotalForPatterns,int[] iNegTotalForPatterns) {
	   //观察符合哪些表达模式
       if( this.options.bgExplainSentiPattern ) {
    	   // this.sgExplainSentiPattern = isDirectSentiment+"\t"+isDecoratedSentiment+"\t"+isAboutMe+"\t"+isJudgement+"\t";
      	   for(int i=0;i<5;i++) {
      		   this.sgExplainSentiPattern += iPosMaxForPatterns[i]+"\t"+iNegMaxForPatterns[i]+"\t"+iPosTotalForPatterns[i]+"\t"+iNegTotalForPatterns[i]+"\t";
           }
       }
   }
   
   private void calculateParagraphSentimentScores() {
	  this.igPositiveSentiment = 1;
      this.igNegativeSentiment = -1;
      this.igTrinarySentiment = 0;
      if (this.options.bgExplainClassification && this.sgClassificationRationale.length() > 0) {
         this.sgClassificationRationale = "";
      }
      int iPosTotal = 0;
      int iPosMax = 0;
      int iNegTotal = 0;
      int iNegMax = 0;
      int iPosTemp = 0;
      int iNegTemp= 0;
      int[] iPos =new int[this.igSentenceCount+1];
      int[] iNeg =new int[this.igSentenceCount+1];
      int iSentencesUsed = 0;
      
      //观察符合哪些表达模式：
      int isDirectSentiment = 0;
      int isDecoratedSentiment = 0;
      int isAboutMe = 0;
      int isJudgement = 0;
      /*以下四个arr记录每种情绪表达模式下的情况：
       * 1:DirectSentiment
       * 2:DecoratedSentiment
       * 3:AboutMe
       * 4:Judgement
       * 0: 符合四种情绪表达模式下的任意一种
       * */
      int[] iPosMaxForPatterns = new int[5];
      int[] iNegMaxForPatterns = new int[5];
      int[] iPosTotalForPatterns = new int[5];
      int[] iNegTotalForPatterns = new int[5];
          
      if (this.igSentenceCount != 0) {
    	  int iNegTot;
    	  for(iNegTot =1; iNegTot <= this.igSentenceCount; ++iNegTot) {
    		  iPos[iNegTot]=this.sentence[iNegTot].getSentencePositiveSentiment();
        	  iNeg[iNegTot]=this.sentence[iNegTot].getSentenceNegativeSentiment();
        	  iPosTemp=iPos[iNegTot];
              iNegTemp=iNeg[iNegTot];
              boolean neutral=iPosTemp==1 && iNegTemp==-1;
              if(options.bgLydian && !neutral && !this.sentence[iNegTot].isBgLydian()) {
            	  //System.out.println(this.sentence[iNegTot].getMySentence());
                  iPosTemp=1;
            	  iNegTemp=-1;
              }
              if (iNegTemp != 0 || iPosTemp != 0) {
            	  iNegTotal += iNegTemp;
                  ++iSentencesUsed;
                  if (iNegMax > iNegTemp) {
                	  iNegMax = iNegTemp;
                  }
                  iPosTotal += iPosTemp;
                  if (iPosMax < iPosTemp) {
                	  iPosMax = iPosTemp;
                  }
                  updateScoreBasedPattern(0, iPosTemp, iNegTemp, iPosMaxForPatterns, iNegMaxForPatterns, iPosTotalForPatterns, iNegTotalForPatterns);
              }

              if (this.options.bgExplainClassification) {
            	  this.sgClassificationRationale = this.sgClassificationRationale + this.sentence[iNegTot].getClassificationRationale() + " ";
              }
              
              if( this.sentence[iNegTot].isDirectSentiment() ) {
        		  isDirectSentiment = 1;
        		  updateScoreBasedPattern(1, iPosTemp, iNegTemp, iPosMaxForPatterns, iNegMaxForPatterns, iPosTotalForPatterns, iNegTotalForPatterns);
        	  }
        	  if( this.sentence[iNegTot].isDecoratedSentiment() ) {
        		  isDecoratedSentiment = 1;
        		  updateScoreBasedPattern(2, iPosTemp, iNegTemp, iPosMaxForPatterns, iNegMaxForPatterns, iPosTotalForPatterns, iNegTotalForPatterns);
        	  }
        	  if( this.sentence[iNegTot].isAboutMe() ) {
        		  isAboutMe = 1;
        		  updateScoreBasedPattern(3, iPosTemp, iNegTemp, iPosMaxForPatterns, iNegMaxForPatterns, iPosTotalForPatterns, iNegTotalForPatterns);
        	  }
        	  if( this.sentence[iNegTot].isJudgement() ) {
        		  isJudgement = 1;
        		  updateScoreBasedPattern(4, iPosTemp, iNegTemp, iPosMaxForPatterns, iNegMaxForPatterns, iPosTotalForPatterns, iNegTotalForPatterns);
        	  }
        	  
        	  //if(this.sentence[iNegTot].isDirectSentiment() || this.sentence[iNegTot].isDecoratedSentiment() || this.sentence[iNegTot].isAboutMe() || this.sentence[iNegTot].isJudgement()) {
        	  //	  updateScoreBasedPattern(0, iPosTemp, iNegTemp, iPosMaxForPatterns, iNegMaxForPatterns, iPosTotalForPatterns, iNegTotalForPatterns);
              //}
        	  
         }
    	 
    	 //解释情绪模式
    	 updateExplainPattern(iPosMaxForPatterns,iNegMaxForPatterns,iPosTotalForPatterns,iNegTotalForPatterns);
    	 
    	 int igEmotionParagraphCombineMethod = this.options.igEmotionParagraphCombineMethod;
         if (iNegTotal == 0) {
            if (igEmotionParagraphCombineMethod != 2) {
               this.igPositiveSentiment = 0;
               this.igNegativeSentiment = 0;
               this.igTrinarySentiment = this.binarySelectionTieBreaker();
               return;
            }
         }
         //走取平均的路线
         if (igEmotionParagraphCombineMethod == 1) {
            this.igPositiveSentiment = (int)((double)((float)iPosTotal / (float)iSentencesUsed) + 0.5D);
            this.igNegativeSentiment = (int)((double)((float)iNegTotal / (float)iSentencesUsed) - 0.5D);
            if (this.options.bgExplainClassification) {
               this.sgClassificationRationale = this.sgClassificationRationale + "[result = average (" + iPosTotal + " and " + iNegTotal + ") of " + iSentencesUsed + " sentences]";
            }
         } 
         //走取总量的路线
         else if(igEmotionParagraphCombineMethod == 2){
        	 this.igPositiveSentiment = iPosTotal;
             this.igNegativeSentiment = iNegTotal;
             if (this.options.bgExplainClassification) {
                this.sgClassificationRationale = this.sgClassificationRationale + "[result: total positive; total negative]";
             }
         }
         //走取最大值的路线
         else {
        	 this.igPositiveSentiment = iPosMax;
             this.igNegativeSentiment = iNegMax;
             if (this.options.bgExplainClassification) {
                this.sgClassificationRationale = this.sgClassificationRationale + "[result: max + and - of any sentence]";
             }
         }

         if (igEmotionParagraphCombineMethod != 2) {
            if (this.igPositiveSentiment == 0) {
               this.igPositiveSentiment = 1;
            }

            if (this.igNegativeSentiment == 0) {
               this.igNegativeSentiment = -1;
            }
         }

         if (this.options.bgScaleMode) {
            this.igScaleSentiment = this.igPositiveSentiment + this.igNegativeSentiment;
            if (this.options.bgExplainClassification) {
               this.sgClassificationRationale = this.sgClassificationRationale + "[scale result = sum of pos and neg scores]";
            }

         } else {
            if (igEmotionParagraphCombineMethod == 2) {
               if (this.igPositiveSentiment == 0 && this.igNegativeSentiment == 0) {
                  if (this.options.bgBinaryVersionOfTrinaryMode) {
                     this.igTrinarySentiment = this.options.igDefaultBinaryClassification;
                     if (this.options.bgExplainClassification) {
                        this.sgClassificationRationale = this.sgClassificationRationale + "[binary result set to default value]";
                     }
                  } else {
                     this.igTrinarySentiment = 0;
                     if (this.options.bgExplainClassification) {
                        this.sgClassificationRationale = this.sgClassificationRationale + "[trinary result 0 as pos=1, neg=-1]";
                     }
                  }
               } else {
                  if ((float)this.igPositiveSentiment > this.options.fgNegativeSentimentMultiplier * (float)(-this.igNegativeSentiment)) {
                     this.igTrinarySentiment = 1;
                     if (this.options.bgExplainClassification) {
                        this.sgClassificationRationale = this.sgClassificationRationale + "[overall result 1 as pos > -neg * " + this.options.fgNegativeSentimentMultiplier + "]";
                     }

                     return;
                  }

                  if ((float)this.igPositiveSentiment < this.options.fgNegativeSentimentMultiplier * (float)(-this.igNegativeSentiment)) {
                     this.igTrinarySentiment = -1;
                     if (this.options.bgExplainClassification) {
                        this.sgClassificationRationale = this.sgClassificationRationale + "[overall result -1 as pos < -neg * " + this.options.fgNegativeSentimentMultiplier + "]";
                     }

                     return;
                  }

                  if (this.options.bgBinaryVersionOfTrinaryMode) {
                     this.igTrinarySentiment = this.options.igDefaultBinaryClassification;
                     if (this.options.bgExplainClassification) {
                        this.sgClassificationRationale = this.sgClassificationRationale + "[binary result = default value as pos = -neg * " + this.options.fgNegativeSentimentMultiplier + "]";
                     }
                  } else {
                     this.igTrinarySentiment = 0;
                     if (this.options.bgExplainClassification) {
                        this.sgClassificationRationale = this.sgClassificationRationale + "[trinary result = 0 as pos = -neg * " + this.options.fgNegativeSentimentMultiplier + "]";
                     }
                  }
               }
            } else {
            	if (this.igPositiveSentiment == 1 && this.igNegativeSentiment == -1) {
                  if (this.options.bgBinaryVersionOfTrinaryMode) {
                     this.igTrinarySentiment = this.binarySelectionTieBreaker();
                     if (this.options.bgExplainClassification) {
                        this.sgClassificationRationale = this.sgClassificationRationale + "[binary result = default value as pos=1 neg=-1]";
                     }
                  }else{
                     this.igTrinarySentiment = 0;
                     if (this.options.bgExplainClassification) {
                        this.sgClassificationRationale = this.sgClassificationRationale + "[trinary result = 0 as pos=1 neg=-1]";
                     }
                  }
                  return;
               }
               if (this.igPositiveSentiment > -this.igNegativeSentiment) {
                  this.igTrinarySentiment = 1;
                  if (this.options.bgExplainClassification) {
                     this.sgClassificationRationale = this.sgClassificationRationale + "[overall result = 1 as pos>-neg]";
                  }
                  return;
               }

               if (this.igPositiveSentiment < -this.igNegativeSentiment) {
                  this.igTrinarySentiment = -1;
                  if (this.options.bgExplainClassification) {
                     this.sgClassificationRationale = this.sgClassificationRationale + "[overall result = -1 as pos<-neg]";
                  }
                  return;
               }
               
               iNegTot = 0;
               int iPosTot = 0;
               for(int iSentence = 1; iSentence <= this.igSentenceCount; ++iSentence) {
            	   iPosTot=iPos[iSentence];
                   iNegTot+=iNeg[iSentence];
               }
               if (this.options.bgBinaryVersionOfTrinaryMode && iPosTot == -iNegTot) {
            	   this.igTrinarySentiment = this.binarySelectionTieBreaker();
                   if (this.options.bgExplainClassification) {
                    	this.sgClassificationRationale = this.sgClassificationRationale + "[binary result = default as posSentenceTotal>-negSentenceTotal]";
                   }
               }else{
            	   if (this.options.bgExplainClassification) {
                     this.sgClassificationRationale = this.sgClassificationRationale + "[overall result = largest of posSentenceTotal, negSentenceTotal]";
                   }
            	   if (iPosTot > -iNegTot) {
                     this.igTrinarySentiment = 1;
                   }else{
                     this.igTrinarySentiment = -1;
                   }
               }
            }
         }
      }
      else {
    	  
    	  //解释情绪模式
     	  updateExplainPattern(iPosMaxForPatterns,iNegMaxForPatterns,iPosTotalForPatterns,iNegTotalForPatterns);
     	 
      }
      
   }

   private int binarySelectionTieBreaker() {
      if (this.options.igDefaultBinaryClassification != 1 && this.options.igDefaultBinaryClassification != -1) {
         return this.generator.nextDouble() > 0.5D ? 1 : -1;
      } else {
         return this.options.igDefaultBinaryClassification != 1 && this.options.igDefaultBinaryClassification != -1 ? this.options.igDefaultBinaryClassification : this.options.igDefaultBinaryClassification;
      }
   }
}
