package rule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;

public class TextParser {
	private static boolean hasInit = false;
    private static StanfordCoreNLP generParse;
    public TextParser() {
    	if(!hasInit) {
    		init();
    		hasInit = true;
    	}
	}
    public void init() {
    	Properties prop_parse1 = new Properties();
	    prop_parse1.put("annotators", "tokenize, ssplit, pos, lemma, parse, ner");
	    generParse = new StanfordCoreNLP(prop_parse1);
    }
    private String text;
    private ArrayList<String> wordList;
	private ArrayList<String> tagList;
    private ArrayList<String> lemmaList;
    private ArrayList<String> nerList;
    private ArrayList<Integer> beginIndexList;
    private ArrayList<Integer> endIndexList;
    private HashMap<Integer,Integer> deginIndexMap;
    private ArrayList<Integer> coreMapIndexs;
    private ArrayList<SemanticGraph> depList;
    private ArrayList<Tree> treeList;
    private ArrayList<IndexedWord> nodeList;
    private int parseListSize=0;
    public void initText(String str){
    	//System.out.println(str);
    	this.text = str;
    	wordList = new ArrayList<String>();
    	tagList = new ArrayList<String>();
    	lemmaList = new ArrayList<String>();
    	nerList = new ArrayList<String>();
    	beginIndexList = new ArrayList<Integer>();
    	endIndexList = new ArrayList<Integer>();
    	deginIndexMap = new HashMap<Integer,Integer>();
    	coreMapIndexs = new ArrayList<Integer>();
    	depList = new ArrayList<SemanticGraph>();
    	treeList = new ArrayList<Tree>();
    	nodeList = new ArrayList<IndexedWord>();
    	int beginindex;
    	Annotation document = new Annotation(str);
    	generParse.annotate(document);
	    List<CoreMap> sentences = (List<CoreMap>)document.get(CoreAnnotations.SentencesAnnotation.class);
	    for(CoreMap sentence: sentences) {
	    	SemanticGraph graph = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
	    	depList.add( graph );
	    	Tree tree = sentence.get(TreeAnnotation.class);
	    	treeList.add( tree );
	    	for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
	    		wordList.add( token.word() );
	    		tagList.add( token.tag() );
	    		lemmaList.add( token.lemma().toLowerCase() );
	    		beginindex = token.beginPosition();
	    		beginIndexList.add(beginindex);
	    		deginIndexMap.put(beginindex,parseListSize);
	    		endIndexList.add( token.endPosition() );
	    		coreMapIndexs.add(depList.size()-1);
	    		nodeList.add( graph.getNodeByIndex(token.index()) );
	    		nerList.add( token.ner() );
	    		parseListSize++;
	        }
        }
    }
    
    public boolean isFRAGMENT(String word) {
		return word.indexOf("_")!=-1 && word.equals(word.toUpperCase());
	}
	
    public String getTag(int i) {
		if(i>=0 && i<parseListSize) {
			String word = getOriWord(i);
			if( isFRAGMENT( word ) ) {
				return "noResult";
			}else {
				return tagList.get(i);
			}
		}else {
			return "noResult";
		}
	}
    
    public String getOriWord(int i) {
		if(i>=0 && i<parseListSize) {
			return wordList.get(i);
		}else {
			return "noResult";
		}
	}
    
    public String getWord(int i) {
		if(i>=0 && i<parseListSize) {
			return wordList.get(i).toLowerCase();
		}else {
			return "noResult";
		}
	}
    
    public String getNER(int i) {
		if(i>=0 && i<parseListSize) {
			return nerList.get(i).toLowerCase();
		}else {
			return "noResult";
		}
	}
    
    public String getLemma(int i) {
		if(i>=0 && i<parseListSize) {
			String word = getOriWord(i);
			if( isFRAGMENT(word) ) {
				return "noResult";
			}else {
				return lemmaList.get(i);
			}
		}else {
			return "noResult";
		}
	}
    
    public int getBeginPosition(int i) {
		if(i>=0 && i<parseListSize) {
			return beginIndexList.get(i);
		}else {
			return -1;
		}
	}
    
    public int getEndPosition(int i) {
		if(i>=0 && i<parseListSize) {
			return endIndexList.get(i);
		}else {
			return -1;
		}
	}
    
    public int getCoreMapIndex(int i) {
		if(i>=0 && i<parseListSize) {
			return coreMapIndexs.get(i);
		}else {
			return -1;
		}
	}
    
    public SemanticGraph getDepGraphByCoreMapIndex(int i) {
		if(i>=0 && i<parseListSize) {
			return depList.get(i);
		}else {
			return null;
		}
	}
    
    public SemanticGraph getDepGraphByIndex(int index) {
    	int depIndex = getCoreMapIndex(index);
		return getDepGraphByCoreMapIndex(depIndex);
	}
    
    public IndexedWord getIndexedWord(int i) {
		if(i>=0 && i<parseListSize) {
			return nodeList.get(i);
		}else {
			return null;
		}
	}
    
    public Tree getTreeByCoreMapIndex(int coreMapIndex) {
		if(coreMapIndex>=0 && coreMapIndex<treeList.size()) {
			return treeList.get(coreMapIndex);
		}else {
			return null;
		}
	}
    
    public Tree getTreeByIndex(int index) {
    	int depIndex = getCoreMapIndex(index);
		return getTreeByCoreMapIndex(depIndex);
	}
    
    public String[] partitionSymbol = {",",":","."};
	public boolean isPartitionSymbolByIndex(int index) {
		String tag = getTag(index);
		String lemma = getLemma(index);
		if( isHave(getTag(index),partitionSymbol) && !lemma.equals("/") ) {
			return true;
		}else if( tag.equals("HYPH")) {
			if( index-1>=0 && index+1<parseListSize ) {
				int aboveEnd = getEndPosition(index-1);
				int followStart = getBeginPosition(index+1);
				if(followStart-aboveEnd==1) {
					return false;
				}else {
					return true;
				}
			}else {
				return true;
			}
		}
		return false;
	}
	public boolean havePartitionInBetween(int index1,int index2) {
		int start = Math.min(index1, index2);
		int end = Math.max(index1, index2);
		for(int i=start+1;i<end;i++) {
			if( isPartitionSymbolByIndex(i) ) {
				return true;
			}
		}
		return false;
	}
	public int getNearestForwardParIndex(int index) {
		int nearestForwardParIndex = 0;
		for(int i=index-1;i>=0;i--) {
			if( isPartitionSymbolByIndex(i) ) {
				nearestForwardParIndex = i;
				break;
			}
		}
		return nearestForwardParIndex;
	}
	public int getNearestBackwardParIndex(int index) {
		int nearestBackwardParIndex = parseListSize-1;
		for(int i=index+1;i<parseListSize;i++) {
			if( isPartitionSymbolByIndex(i) ) {
				nearestBackwardParIndex = i;
				break;
			}
		}
		return nearestBackwardParIndex;
	}
	
	public boolean isHave(String word,String[] possTag) {
    	for(int i=0;i<possTag.length;i++) {
    		if(word.equals(possTag[i])) {
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
	
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public ArrayList<String> getWordList() {
		return wordList;
	}
	public ArrayList<String> getTagList() {
		return tagList;
	}
	public ArrayList<String> getLemmaList() {
		return lemmaList;
	}
	public ArrayList<String> getNerList() {
		return nerList;
	}
	public ArrayList<Integer> getBeginIndexList() {
		return beginIndexList;
	}
	public ArrayList<Integer> getEndIndexList() {
		return endIndexList;
	}
	public HashMap<Integer, Integer> getDeginIndexMap() {
		return deginIndexMap;
	}
	public ArrayList<Integer> getCoreMapIndexs() {
		return coreMapIndexs;
	}
	public ArrayList<SemanticGraph> getDepList() {
		return depList;
	}
	public ArrayList<Tree> getTreeList() {
		return treeList;
	}
	public int getParseListSize() {
		return parseListSize;
	}
}
