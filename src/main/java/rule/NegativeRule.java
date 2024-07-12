package rule;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.stanford.nlp.io.EncodingPrintWriter.out;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.trees.Tree;
import sentistrength.ClassificationResources;

public class NegativeRule extends BasicRule{
	private boolean hasNeg;
	private boolean[] isIndexInStandardNeg;
	private boolean[] isIndexInNonStandardNeg;
	private int parseListSize;
	private TextParser parser;
	public ClassificationResources resources;
	public boolean isHasNeg() {
		return hasNeg;
	}
    public ClassificationResources getResources() {
		return resources;
	}
	public NegativeRule(TextParser parser,ClassificationResources resources) {
		this.parser = parser;
		this.parseListSize = parser.getParseListSize();
		this.resources = resources;
		negativeRangeCheck();
	}
	public void negativeRangeCheck() {
		negativeRangeCheck2();
	}
	public boolean isInNegativeRange(int index) {
  		return isInStandardNegative(index) || isInNonStandardNegative(index);
  	}
	public boolean isInStandardNegative(int index) {
  		return getBool(index,isIndexInStandardNeg);
  	}
	public boolean isInNonStandardNegative(int index) {
  		return getBool(index,isIndexInNonStandardNeg);
  	}
	private boolean isNegatingWord(int index) {
		boolean isNegatingWord = false;
		String lemma = parser.getLemma(index);
		if( resources.negatingWords.negatingWord(lemma) ) {
			isNegatingWord = true;
			if( lemma.equals("few") || lemma.equals("little") || lemma.equals("bit") ) {
				  for(int j=Math.max(0,index-2);j<index;j++) {
					String beforeWord = parser.getWord(j);
					if( "a".equals(beforeWord) ) {
						isNegatingWord = false;
					}
				}
			}
			String tag = parser.getTag(index);
			if( lemma.equals("limit") && isNoun(tag) ) {
				isNegatingWord = false;
			}
			if( "UH".equals(tag) ) {
				isNegatingWord = false;
			}else if( "NNS".equals(tag) || "NNP".equals(tag) ) {
				isNegatingWord = false;
			}
		}
		return isNegatingWord;
	}
	
	private String[] quasiNegativePhrase = {"other than","rather than","far from","out of","instead of"};
	private boolean isQuasiNegativePhrase(int index) {
		for(String quasiNegative:quasiNegativePhrase) {
			String[] quasiNegativeElems = quasiNegative.split(" ");
			int quasiNegativeElemsLen = quasiNegativeElems.length;
			int checkedLen = 0;
			while( quasiNegativeElemsLen-checkedLen-1>=0 ) {
				String quasiNegativePhraseWord = quasiNegativeElems[quasiNegativeElemsLen-checkedLen-1];
				String currWord = parser.getLemma(index-checkedLen);
				if( quasiNegativePhraseWord.equals(currWord) ) {
					checkedLen++;
				}else {
					break;
				}
			}
			if( checkedLen==quasiNegativeElemsLen ) {
				return true;
			}
		}
		return false;
	}
	
	//否定范围确认方法一：利用依赖分析确认否定的范围
	public void negativeRangeCheck1() {
		hasNeg = false;
		isIndexInStandardNeg = new boolean[parseListSize];
		isIndexInNonStandardNeg = new boolean[parseListSize];
		for(int i=0;i<parseListSize;i++) {
			boolean isNegatingWord = isNegatingWord(i);
			boolean isQuasiNegativePhrase = isQuasiNegativePhrase(i);
			//进入否定范围检查：
			if( isNegatingWord || isQuasiNegativePhrase ) {
				hasNeg = true;
				SemanticGraph graph = parser.getDepGraphByIndex(i);
				//System.out.println("Dependency Graph:\n " +graph.toString(SemanticGraph.OutputFormat.READABLE));
				IndexedWord node = parser.getIndexedWord(i);
				
				if( isNoun(node.tag()) ) {
					negOnNoun(i,node,graph,isIndexInStandardNeg);
				}else {
					boolean isMatched = false;
					List<IndexedWord> negObjList = new ArrayList<IndexedWord>();
					Set<IndexedWord> parents = graph.getParents(node);
					negObjList.addAll(parents);
					if(  negObjList.size()==0 ) {
						negObjList.add(node);
					}
					for(IndexedWord negObj:negObjList ) {
						String negObjTag = negObj.tag();
						if( isNoun(negObjTag) ) {
							isMatched = negOnNoun(i,negObj,graph,isIndexInStandardNeg);
						}else {
							isMatched = negOnNonNoun(i,negObj,graph,isIndexInStandardNeg);
						}		
					}
					if( !isMatched ) {
						int start = i;
						int end = i+5;
						setNegScope(start,end,isIndexInStandardNeg);
					}
				}
			}
		}
	}
	
	private boolean negOnNonNoun(int iNeg,IndexedWord node,SemanticGraph graph,boolean[] isIndexInNegative) {
		boolean isMatched = false;
		setNegScope(iNeg,node.index(),isIndexInNegative);
		Set<IndexedWord> kids = graph.getChildren(node);
		for(IndexedWord kid:kids) {
			String relation = graph.getEdge(node,kid).getRelation().toString();
			if( parser.havePartitionInBetween(iNeg,kid.index()-1) ) {
				continue;
			}
			if( relation.indexOf("comp")!=-1 ) {
				isMatched = setNegScope(iNeg,kid.index(),isIndexInNegative);
			}else if( relation.indexOf("mod")!=-1 ) {
				isMatched = setNegScope(iNeg,kid.index(),isIndexInNegative);
			}else if( "dep".equals(relation) ) {
				isMatched = setNegScope(iNeg,kid.index(),isIndexInNegative);
			}else if( relation.indexOf("obl")!=-1 || "obj".equals(relation) ) {
				negOnNoun(iNeg,kid,graph,isIndexInNegative);
				isMatched = true;
			}
		}
		return isMatched;
	}
	
	private boolean negOnNoun(int iNeg,IndexedWord noun,SemanticGraph graph,boolean[] isIndexInNegative) {
		boolean isMatched = false;
		setNegScope(iNeg,noun.index(),isIndexInNegative);
		Set<IndexedWord> kids = graph.getChildren(noun);
		for(IndexedWord kid:kids) {
			String relation = graph.getEdge(noun,kid).getRelation().toString();
			if( parser.havePartitionInBetween(iNeg,kid.index()-1) ) {
				continue;
			}
			if(relation.indexOf("acl")!=-1) {
				isMatched = setNegScope(iNeg, kid.index(),isIndexInNegative);
			}else if( relation.indexOf("mod")!=-1 ) {
				isMatched = setNegScope(iNeg, kid.index(),isIndexInNegative);
			}else if("dep".equals(relation)) {
				isMatched = setNegScope(iNeg,kid.index(),isIndexInNegative);
			}
		}
		return isMatched;
    }
	
	private boolean setNegScope(int start,int end,boolean[] isIndexInNegative) {
		boolean hasSetNeg = false;
		end = Math.min(parseListSize,end);
    	for(int i = start+1; i<end ;i++) {
    		if( parser.isPartitionSymbolByIndex(i) ) {
    			return hasSetNeg;
    		}
    		isIndexInNegative[i] = true;
    		hasSetNeg = true;
		}
    	return hasSetNeg;
    }
	
	//否定范围确认方法二：利用选取分析确认否定的范围
	public void negativeRangeCheck2() {
		isIndexInStandardNeg = new boolean[parseListSize];
		isIndexInNonStandardNeg = new boolean[parseListSize];
		for(int i=0;i<parseListSize;i++) {
			String lemma = parser.getLemma(i);
			//是否定词
			boolean isNegatingWord = isNegatingWord(i);
			//是否定词组的末词
			boolean isQuasiNegativePhrase = isQuasiNegativePhrase(i);
			if( isNegatingWord ) {
				hasNeg = true;
				boolean isStandardNeg = resources.negatingWords.isStandardNegating(lemma);
				boolean[] isIndexInNegative = null;
				if( isStandardNeg ) {
					isIndexInNegative = isIndexInStandardNeg;
				}else {
					isIndexInNegative = isIndexInNonStandardNeg;
				}
				boolean hasSetNegRange = rangeCheckByConstituency(i,isIndexInNegative);
				if( !hasSetNegRange && lemma.equals("not") ) {
					int start = i;
					int end = i+5;
					setNegScope(start,end,isIndexInNegative);
				}
			}else if( isQuasiNegativePhrase ) {
				hasNeg = true;
				int start = i;
				int end = i+5;
				setNegScope(start,end,isIndexInNonStandardNeg);
			}
		}
	}
	
	private boolean rangeCheckByConstituency(int index,boolean[] isIndexInNegative) {
		boolean hasSetNegRange = false;
		//先找到否定的对象
		IndexedWord node = parser.getIndexedWord(index);
		SemanticGraph graph = parser.getDepGraphByIndex(index);
		//System.out.println("Dependency Graph:\n " +graph.toString(SemanticGraph.OutputFormat.READABLE));	
		List<IndexedWord> negObjList = getNegObj(node,graph);
		
		//确定否定对象的范围：
		for(IndexedWord negObj:negObjList) {
			//首先,要求否定的对象出现在否定词的后方
			if( negObj.index()-1<index ) {
				continue;
			}
			//找到否定对象的范围：
			//步骤一：找到否定对象的选区节点：
			Tree root = parser.getTreeByIndex(index);
			Tree negObjTree = getTreeLeafByIndex(negObj.index(),root);
			if( negObjTree==null ) {
				continue;
			}
			
			//步骤二：找到否定对象的选区范围：
			Tree negObjPhrase = null;
			String phraseLabel = null;
			int depth = root.depth();
			for(int i=1;i<=depth;i++) {
				Tree ancestor = negObjTree.ancestor(i,root);
				if( ancestor==null ) {
					break;
				}
				String nodeLabel = ancestor.value();
				//找到多分支：
				if( ancestor.getLeaves().size()>1 ) {
					//首次更新标签
					if( phraseLabel==null ) {
						negObjPhrase = ancestor;
						phraseLabel = nodeLabel;
					}else {
						if( nodeLabel.equals(phraseLabel) ) {
							negObjPhrase = ancestor;
						}else {
							break;
						}
					}
				}
			}
			
			//步骤三：根据否定对象的选区范围设置否定范围
			if( negObjPhrase!=null ) {
				int start = index ;
				List<Tree> leafList = negObjPhrase.getLeaves();
	    		Tree lastNode = leafList.get(leafList.size()-1);
	    		int endNodeIndex =  getNodeIndex(lastNode.label().toString()) ;
	    		int end = endNodeIndex;
				for(int i = start+1; i<end ;i++) {
	        		String tag = parser.getTag(i);
	        		String lemma = parser.getLemma(i);
	        		if( parser.isPartitionSymbolByIndex(i)
	        		    || tag.equals("CC") 
	        		    || lemma.equals("because") ) {
	        			break;
	        		}
	        		isIndexInNegative[i] = true;
	        		hasSetNegRange = true;
	    		}
			}
		}
		return hasSetNegRange;
	}
	
	private List<IndexedWord> getNegObj(IndexedWord node,SemanticGraph graph){	
		List<IndexedWord> negObjList = new ArrayList<IndexedWord>();
		if( isVerb(node.tag()) ) {
			Set<IndexedWord> kids = graph.getChildren(node);
			for(IndexedWord kid:kids) {
				if( kid.index()>node.index() ) {
					negObjList.add(kid);
				}
			}
		}
		else if( isNoun(node.tag()) ) {
			Set<IndexedWord> parents = graph.getParents(node);
			negObjList.addAll(parents);
			Set<IndexedWord> kids = graph.getChildren(node);
			for(IndexedWord kid:kids) {
				String relation = graph.getEdge(node,kid).getRelation().toString();
				if( relation.indexOf("mod")!=-1 || relation.indexOf("acl")!=-1 ) {
					negObjList.add(kid);
				}
			}
		}
		else if( isAdj(node.tag()) || isAdv(node.tag()) ) {
			Set<IndexedWord> parents = graph.getParents(node);
			negObjList.addAll(parents);
			Set<IndexedWord> kids = graph.getChildren(node);
			for(IndexedWord kid:kids) {
				String relation = graph.getEdge(node,kid).getRelation().toString();
				if( relation.indexOf("mod")!=-1 ) {
					negObjList.add(kid);
				}
			}
		}
		else {
			Set<IndexedWord> parents = graph.getParents(node);
			negObjList.addAll(parents);
		}
		return negObjList;
	}
	
}
