package rule;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.stanford.nlp.io.EncodingPrintWriter.out;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.trees.Tree;
import sentistrength.ClassificationResources;

public class PatternProcessor {
	public BasicRule br;
	public ClassificationResources resources;
	private TextParser parser;
	private int parseListSize;
	public ClassificationResources getResources() {
		return resources;
	}
	public void setResources(ClassificationResources resources) {
		this.resources = resources;
	}
	public PatternProcessor() {
		br = new BasicRule();
		parser = new TextParser();
	}
	
	private PersonSentiRule psr;
	private InevitableSentiPattern isp;
	private DomainVocaSentiRule dvsr;
	private BoostWordRule bwr;
	private TurningRule tr;
	private PolysemyRule pr;
	private NegativeRule nr;
	private QuestionSentenceRule qsr;
	private SubjunctiveMoodRule smr;
	private AboutMePattern aboutMePattern;
	public void init(String text,int[] arr){
		//System.out.println(text);
    	parser.initText(text);
    	parseListSize = parser.getParseListSize();
    	translateIndex(arr);
    	
    	//关于否定的处理：
    	nr = new NegativeRule(parser,resources);
    	
    	//关于问句的处理：
    	qsr = new QuestionSentenceRule(parser);
    	
    	//关于虚拟语气的处理：
    	smr = new SubjunctiveMoodRule(parser);
    	
    	//初始化About Me Pattern：
    	aboutMePattern = new AboutMePattern(parser);
    	
    	//处理多义词：
    	pr = new PolysemyRule(parser);
    	
    	//处理转折规则；
    	tr = new TurningRule(parser);
    	
    	//关于辅助词规则：
    	bwr = new BoostWordRule(parser);
    	
    	//情绪对象规则：
    	dvsr = new DomainVocaSentiRule(parser,resources);
    	psr = new PersonSentiRule(parser);
    	
    	//情绪必然表达模型：
    	isp = new InevitableSentiPattern(parser);
    }
	
	//Sentence 的iTerm与内部的index之间的对应关系，indexs[] 与外部的Term组等长
    private int[] indexs;
    //内部的index 与外部的 iTerm之间的对应关系，iTerm[] 与内部的 lemmas[] 等长
    private int[] iTerms;
    private void translateIndex(int[] arr) {
    	HashMap<Integer,Integer> deginIndexMap = parser.getDeginIndexMap();
    	indexs = new int[arr.length];
    	iTerms = new int[parseListSize+1];
    	for(int i=1;i<arr.length;i++) {
    		if(deginIndexMap.containsKey(arr[i])) {
    			indexs[i] = deginIndexMap.get(arr[i]);
    		}else {
    			indexs[i] = indexs[i-1];
    		}
    		iTerms[ indexs[i] ] = i;
    	}
    	
    	for(int i=1;i<parseListSize+1;i++) {
    		if( iTerms[i]==0 ) {
    			iTerms[i] = iTerms[i-1];
    		}
    	}
    }
    private int getIndex(int i) {
    	if(i>=indexs.length) {
    		return indexs[indexs.length-1];
    	}
    	return indexs[i];
    }
    public int getITerm(int i) {
    	return iTerms[i];
    }
    
	// 重写AboutMe Pattern
	public boolean isAboutMe(int iTerm) {
		int index = getIndex(iTerm);
		return aboutMePattern.isAboutMe(index);
	}
	public boolean objIsMe(int iTerm) {
		int index = getIndex(iTerm);
		return aboutMePattern.objIsMe(index);
	}
	
    //关于否定的讨论：
  	public boolean isInNegativeRange(int iTerm) {
  		int index = getIndex(iTerm);
  		boolean isInNegative = nr.isInNegativeRange(index);
  		return isInNegative;
  	}
  	public boolean isInStandardNegativeRange(int iTerm) {
  		int index = getIndex(iTerm);
  		boolean isInStandardNeg = nr.isInStandardNegative(index);
  		return isInStandardNeg;
  	}
  	public boolean isHasNeg() {
  		boolean isHasNeg = nr.isHasNeg();
  		return isHasNeg;
  	}
  	
    //关于问句的探索：
	public boolean isInQuestion(int iTerm) {
		int index = getIndex(iTerm);
		boolean isInQuestion = qsr.isInQuestion(index);
		return isInQuestion;
	}
	public boolean isCensureQs() {
		boolean isCensureQs = qsr.isCensureQs();
		return isCensureQs;
	}
	
	//关于虚拟语气的探索：
	public boolean isEstimateVerbByITerm(int iTerm) { //关于预估动词的部分
		int index = getIndex(iTerm);
		boolean isEstimateVerb = smr.isEstimateVerb(index);
		return isEstimateVerb;
	}
	public boolean isExpressEstimationByITerm(int iTerm) {
		int index = getIndex(iTerm);
		boolean isExpressEstimation = smr.isExpressEstimation(index);
		return isExpressEstimation;
	}	
	public boolean hasExplicitCondi() {  //是否有显性条件句
		boolean hasExplicitCondi = smr.hasExplicitCondi();
		return hasExplicitCondi;
	}
	public boolean isInImplicitCondi(int iTerm) { //是否在隐性条件句中
		int index = getIndex(iTerm);
		boolean isInImplicitCondi = smr.isInImplicitCondi(index);
		return isInImplicitCondi;
	}
	public boolean isInImplicitConclus(int iTerm) { //是否在隐性结论句中
		int index = getIndex(iTerm);
		boolean isInImplicitConclus = smr.isInImplicitConclus(index);
		return isInImplicitConclus;
	}
    public boolean isInExplicitCondi(int iTerm) { //是否在显性条件句中
    	int index = getIndex(iTerm);
    	boolean isInExplicitCondi = smr.isInExplicitCondi(index);
		return isInExplicitCondi;
	}
	public boolean isInExplicitConclus(int iTerm) { //是否在显性结论句中
		int index = getIndex(iTerm);
    	boolean isInExplicitConclus = smr.isInExplicitConclus(index);
		return isInExplicitConclus;
	}
	public boolean isInWishByiTerm(int iTerm,float oriSenti) {  //是否在某种愿望中
		int index = getIndex(iTerm);
		boolean isInWish = smr.isInWish(index, oriSenti);
		return isInWish;
	}
	
	//关于转折词：
	public boolean isInTurningSatellite(int iTerm) {
		int index = getIndex(iTerm);
		boolean isInTurningSatellite = tr.isInTurningSatellite(index);
		return isInTurningSatellite;
	}
	public boolean isHasTurning() {
		boolean isHasTurning = tr.isHasTurning();
		return isHasTurning;
	}
	
	//关于辅助词规则：
	public void setBoostWord(int iTerm,int boostValue) {
		int index = getIndex(iTerm);
		bwr.setBoostWord(index,boostValue);
	}
	public float getValueAfterBoost(int iTerm,float oriValue) {
		int index = getIndex(iTerm);
		return bwr.getValueAfterBoost(index,oriValue);
	}
	public HashMap<Integer,Integer> getBoostPair(int iTerm) {
		int index = getIndex(iTerm);
		HashMap<Integer,Integer> indexBoostValueMap = bwr.getBoostPair(index);
		HashMap<Integer,Integer> iTermBoostValueMap = new HashMap<Integer,Integer>();
		for(Integer boostIndex:indexBoostValueMap.keySet()) {
			int boostValue = indexBoostValueMap.get(boostIndex);
			int boostITerm = getITerm(boostIndex);
			iTermBoostValueMap.put(boostITerm, boostValue);
		}
		return iTermBoostValueMap;
	}
	public boolean isHasDecreBoost() {
		return bwr.isHasDecreBoost();
	}
	public boolean isAfterBoost(int iTerm) {
		int index = getIndex(iTerm);
		return bwr.isAfterBoost(index);
	}
	
    //处理多义词：
    public float turnLikeBycontext(int iTerm,float oriSenti){
		int index = getIndex(iTerm);
		return pr.turnLikeBycontext(index, oriSenti);
	}
    public float turnLyingBycontext(int iTerm,float oriSenti){
		int index = getIndex(iTerm);
		return pr.turnLyingBycontext(index, oriSenti);
	}
    public float turnAgainstBycontext(int iTerm,float oriSenti){
		int index = getIndex(iTerm);
		return pr.turnAgainstBycontext(index, oriSenti);
	}
    public float turnPainBycontext(int iTerm,float oriSenti){
		int index = getIndex(iTerm);
		return pr.turnPainBycontext(index, oriSenti);
	}
    public float onlySentiInVerb(int iTerm,float oriSenti){
		int index = getIndex(iTerm);
		return pr.onlySentiInVerb(index, oriSenti);
	}
    public float notSentiInAdv(int iTerm,float oriSenti){
		int index = getIndex(iTerm);
		return pr.notSentiInAdv(index, oriSenti);
	}
    
    //情绪对象有关的探索：
    public boolean isNounInSENounChunk(int iTerm) {
    	int index = getIndex(iTerm);
		return dvsr.isNounInSENounChunk(index);
    }
    public boolean isInSENounChunk(int iTerm) {
    	int index = getIndex(iTerm);
		return dvsr.isInSENounChunk(index);
    }
    public boolean isInSENounChunkSBAR(int iTerm) {
    	int index = getIndex(iTerm);
		return dvsr.isInSENounChunkSBAR(index);
    }
    public int isInSENounPredicate(int iTerm) {
    	int index = getIndex(iTerm);
    	int SENounIndex = dvsr.isInSENounPredicate(index);
    	int SENounITerm = -1;
    	if( SENounIndex!=-1 ) {
    		SENounITerm = getITerm(SENounIndex);
    	}
		return SENounITerm;
    }
    public boolean isVerbHasSENounObj(int iTerm) {
    	int index = getIndex(iTerm);
		return dvsr.isVerbHasSENounObj(index);
    }
    public boolean isOptionAdjOnSE(int iTerm) {
    	int index = getIndex(iTerm);
		return dvsr.isOptionAdjOnSE(index);
    }
    public boolean isBasedOnSENoun(int iTerm) {
    	int index = getIndex(iTerm);
		return dvsr.isBasedOnSENoun(index);
    }
    public boolean hasOthersAsSubj(int iTerm) {
    	int index = getIndex(iTerm);
		return psr.hasOthersAsSubj(index);
    }
    public boolean isInNonFirstPersonPossessive(int iTerm) {
    	int index = getIndex(iTerm);
		return psr.isInNonFirstPersonPossessive(index);
    }
    public boolean isInGerundAsSubjRange(int iTerm) {
    	int index = getIndex(iTerm);
		return psr.isInGerundAsSubjRange(index);
    }
    
    //情绪必然表达模型
    public boolean isInInevitableSentiPattern(int iTerm) {
    	int index = getIndex(iTerm);
		return isp.isInInevitableSentiPattern(index);
    }
	
	private boolean isWH(String tag) {
		return "WDT".equals(tag) || "WP".equals(tag) || "WRB".equals(tag);
	}
	
	private String[] pronoun={"it","this","that"};
	private boolean isPronoun(int i) {
		return br.isHave(parser.getLemma(i),pronoun) && !"WDT".equals(parser.getTag(i));
	}
	
	private String[] a={"a","an","n"};
	private boolean isA(String word) {
		return br.isHave(word,a);
	}
	
	private String[] beVerb={"look","seem"};
	private boolean isCopula(String word) {
	    return br.isContain(word,beVerb);
	}
	
	private String[] longAdv={"always","even","still"};
	private boolean isLongAdv(String word) {
	    return br.isHave(word,longAdv);
	}
	
	private boolean isBeVerb(String word) {
        String[] beVerb = {"is", "are", "was", "were", "am", "be", "being", "been", "isn't", "aren't", "wasn't", "weren't"};
        return br.isHave(word, beVerb);
    }
	
	private boolean IsBeVerb(String word, String beforedWordTag) {
        if ( isWH(beforedWordTag) ) {
            return false;
        }
        String[] beVerb = {"'m", "'s", "'re"};
        if (br.isHave(word, beVerb) && !beforedWordTag.equals("NNP")) {
            return true;
        }
        return isBeVerb(word);
    }
	
	public boolean isDirectSentiment(int iTerm,int iWordTotal) {
		if( parseListSize==1 ) {
			return false;
		}
		int index = getIndex(iTerm);
		String tag = parser.getTag(index);
		boolean isBeginOfSentence = isBeginOfSentence(iTerm,iWordTotal);
		if (tag.equals("UH")) {
        	return true; 
        } 
		if( isBeginOfSentence ) {
        	return true; 
        }
		return false;
	}
	
	public boolean isDecoratedSentiment(int iTerm) {
		if( parseListSize==1 ) {
			return false;
		}
		int index = getIndex(iTerm);
		String tag = parser.getTag(index);
		String beforewrd = parser.getLemma(index-1);
		String beforeTwowrd = parser.getLemma(index-2) + parser.getLemma(index-1);
		String beforetag = parser.getTag(index-1);
		String afterwrd = parser.getLemma(index+1);
		if ( br.isAdv(tag) ) {
			return true; 
        }
		if( br.isAdj(tag) ) {
			if( br.isAdj(beforetag) || br.isAdv(beforetag) || beforewrd.equals("how") || afterwrd.equals("enough")) {
				return true; 
            }
		}
		if( beforeTwowrd.equals("sortof") ){
			return true; 
		}
		for(int i=index-1;i>=0;i--) {
			if(  parser.isPartitionSymbolByIndex(i) ) {
				break;
			}
			beforewrd = parser.getLemma(i);
			if( isLongAdv(beforewrd) ) {
				return true; 
			}
		}
		return false;
	}
	
	public boolean isJudgementSentiment(int iTerm) {
		if( parseListSize==1 ) {
			return false;
		}
		int index = getIndex(iTerm);
		String tag = parser.getTag(index);
		String beforewrd = parser.getLemma(index-1);
		String beforetag = parser.getTag(index-1);
		String afterwrd = parser.getLemma(index+1);
        for(int i=1;i<=3;i++) {
			if( "get".equals( parser.getLemma(index-i) ) ) {
				return true;
			}
		}
        if( br.isNoun(tag) || br.isAdj(tag)  ) {
			for(int i=1;i<=2;i++) {
				if( "be".equals( parser.getLemma(index-i)) ) {
					return true;
				}
			}
		}
        //SN1.0版本：
        //if( br.isNoun(tag) || br.isAdj(tag)  ) {
		//	for(int i=1;i<=2;i++) {
		//		if ( IsBeVerb(parser.getWord(index-i), parser.getTag(index-i-1)) ) {
        //            return true;
        //        }
		//	}
		//}
        if( br.isNoun(tag) ) {
			if( isA(beforewrd) || br.isAdj(beforetag) || "be".equals(afterwrd) ) {
				return true;
			}
		}
        if ( br.isAdj(tag) ) {
        	if( beforetag.equals("DT") || isA(beforewrd) || beforetag.equals("CC") || isCopula(beforewrd)) {
            	return true;
            }
        }
        if ( br.isVerb(tag) ) {
            if ( isPronoun(index-1) ) {
            	return true;
            }
        }
		return false;
	}
	
	public int getRealiTerm(int iTerm) {
		int index = getIndex(iTerm);
		return getITerm(index);
	}
	public String getTagByiTerm(int iTerm) {
		int index = getIndex(iTerm);
		return parser.getTag(index);
	}
	public String getLemmaByiTerm(int iTerm) {
		int index = getIndex(iTerm);
		return parser.getLemma(index);
	}
	public String getWordByiTerm(int iTerm) {
		int index = getIndex(iTerm);
		return parser.getWord(index);
	}
	public String getNERByiTerm(int iTerm) {
		int index = getIndex(iTerm);
		return parser.getNER(index);
	}
	public boolean isWHByiTerm(int iTerm) {
		int index = getIndex(iTerm);
		return isWH( parser.getTag(index) );
	}
	public boolean isPartitionSymbolByITerm(int iTerm) {
		int index = getIndex(iTerm);
		return parser.isPartitionSymbolByIndex(index);
	}
	
	//关于是否是简单句：
	public boolean isSimpleSentence() {
    	boolean isSimpleSentence = true;
    	for(Tree tree:parser.getTreeList()) {
    		int clauseLevelTreeNum = 0;
    		LinkedList<Tree> queue = new LinkedList<Tree>(); 
			queue.add(tree);
			while(!queue.isEmpty()) {
				tree = (Tree)queue.poll();
				String nodeString = tree.value();
				if( br.isClauseLevelTree(nodeString) && !nodeString.startsWith("SBAR") ) {
					clauseLevelTreeNum++;
				}
				if( clauseLevelTreeNum>1 ) {
					break;
				}
				if( tree.getLeaves().size()>1 ) {
					List<Tree> rootChildrenList = tree.getChildrenAsList();
		        	for(int i=0;i<rootChildrenList.size();i++) {
		        		Tree child = rootChildrenList.get(i);
		        		queue.add(child);
		        	}
				}
			}
			if( clauseLevelTreeNum>1 ) {
				isSimpleSentence = false;
			}
    	}
    	return isSimpleSentence;
    }
	
	private String[] sentenceConjArr = {"because","but","so"};
	public boolean isBeginOfSentence(int iTerm,int iWordTotal) {
		int index = getIndex(iTerm);
		int before_index = index-1;
		String before_lemma = parser.getLemma(before_index);
		if (iWordTotal==1) {
			return true;
		}else if (parser.isPartitionSymbolByIndex(before_index)) {
			return true;
		}else if( br.isHave(before_lemma,sentenceConjArr) ){
			return true;
		}
		return false;
	}
	
	//以下的规则讨论均因为效果不佳，暂不启用：
	//关于过去时对情绪影响的探讨：
	public boolean isInPastTense(int iTerm) {
		int index = getIndex(iTerm);
		SemanticGraph dep = parser.getDepGraphByIndex(index);
		IndexedWord node = parser.getIndexedWord(index);
		String tag = node.tag();
		//形容词优先找系动词
		if( br.isAdj(tag) ) {
			Set<IndexedWord> childList = dep.getChildren(node);
			for(IndexedWord child:childList) {
				String childTag = child.tag();
				String reln = dep.getEdge(node,child).getRelation().toString();
				if( reln.equals("cop") || reln.equals("aux") ) {
					if( childTag.equals("VBD") ) {
						return true;
					}else {
						return false;
					}
				}
			}
		}
		ArrayList<IndexedWord> nodeListToRoot = new ArrayList<IndexedWord>();
		nodeListToRoot.add( node );
		nodeListToRoot.addAll( dep.getPathToRoot(node) );
		for(IndexedWord nodeToRoot:nodeListToRoot) {
			//找到就近的第一个动词
			if( br.isVerb(nodeToRoot.tag()) ) {
				IndexedWord proximateVerb = nodeToRoot;
				String proximateVerbTag = proximateVerb.tag();
				int minIndex = Math.min(node.index()-1, proximateVerb.index()-1);
				int maxIndex = Math.max(node.index()-1, proximateVerb.index()-1);
				if( parser.havePartitionInBetween(minIndex,maxIndex) ) {
					return false;
				}
				if( !proximateVerbTag.equals("VBD") ) {
					return false;
				}
				boolean isPresentPerfectTense = false;
				Set<IndexedWord> kids = dep.getChildren(nodeToRoot);
				for(IndexedWord kid:kids) {
					String relation = dep.getEdge(nodeToRoot,kid).getRelation().toString();
					if( relation.equals("aux") && kid.tag().equals("VBP") ) {
						isPresentPerfectTense = true;
						break;
					}
				}
				if( isPresentPerfectTense ) {
					return false;
				}
				IndexedWord subjIndex = br.getNearestSubj(proximateVerb,dep);
				if( subjIndex==null ) {
					return false;
				}
				minIndex = Math.min(subjIndex.index()-1, proximateVerb.index()-1);
				maxIndex = Math.max(subjIndex.index()-1, proximateVerb.index()-1);
				if( parser.havePartitionInBetween(minIndex,maxIndex) ) {
					return false;
				}
				return true;
			}
		}
		return false;
	}		
	
	//关于反义词的讨论：
	public ArrayList<String> getAntonym(int iTerm) {
		//名词：dis,in,un
		//形容词：dis,in,un
		//动词：dis,un
		int index = getIndex(iTerm);
		ArrayList<String> possAnt = new ArrayList<String>();
		String word = parser.getWord(index);
		String tag = parser.getTag(index);
		if( word.length()>1) {
			String fir = word.substring(0, 1);
			if(fir.equals("m") || fir.equals("b") || fir.equals("p")) {
				possAnt.add("im"+word);
			}else if(fir.equals("l")) {
				possAnt.add("il"+word);
			}else if(fir.equals("r")) {
				possAnt.add("ir"+word);
			}else {
				if( br.isNoun(tag) ) {
					possAnt.add("dis"+word);
					possAnt.add("in"+word);
					possAnt.add("un"+word);
				}else if( br.isAdj(tag) ) {
					possAnt.add("dis"+word);
					possAnt.add("in"+word);
					possAnt.add("un"+word);
				}else if( br.isVerb(tag) ) {
					possAnt.add("dis"+word);
					possAnt.add("un"+word);
				}
			}
		}
		return possAnt;
	}	
	
}