package rule;

import java.util.List;
import java.util.Set;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.trees.Tree;
import sentistrength.ClassificationResources;

public class DomainVocaSentiRule extends BasicRule {
	private int parseListSize;
	private TextParser parser;
	public ClassificationResources resources;
	private boolean[] isInSENounChunk;
	private boolean[] isInSENounChunkSBAR;
	private int[] isInSENounPredicate;
	private boolean[] isBasedOnSENounChunk;
	private boolean[] isSENoun;
	public DomainVocaSentiRule(TextParser parser,ClassificationResources resources) {
		this.parser = parser;
		this.parseListSize = parser.getParseListSize();
		this.resources = resources;
		SENounRangeCheck();
	}
	
	//一个IndexedWord是否是专有名词：
	private boolean isSENoun(IndexedWord node) {
		if( node==null || node.word()==null || node.lemma()==null ||node.tag()==null || node.ner()==null ) {
			return false;
		}
		if( !isNoun(node.tag()) ) {
			return false;
		}
		if( isPerson(node) ) {
			return false;
		}
		if( node.ner().equals("NATIONALITY") ) {
			return false;
		}
		if( hasEmphasis(node.word().toLowerCase()) ) {
			return false;
		}
		if( node.tag().indexOf("NNP")!=-1 ) {
			return true;
		}
		if( resources.seCommonNounList.isSECommonNoun(node.lemma().toLowerCase()) 
		 || resources.seCommonNounList.isSECommonNoun(node.word().toLowerCase()) ) {
			return true;
		}
		String word = node.word();
		boolean isCorrectSpellWord = resources.correctSpellings.correctSpelling( word.toLowerCase() );
		boolean isAllLowerCase = word.equals(word.toLowerCase());
		boolean isQuote = word.equals("QUOTE_TEXT");
		boolean isCodeFragment = word.equals("CODE_FRAGMENT");
		if( word.length()>=2 && !isAllLowerCase && !isCorrectSpellWord && !isQuote && !isCodeFragment) {
			return true;
		}
		return false;
	}
	
	private boolean isPerson(IndexedWord node) {
		if( node.ner()!=null && node.ner().equals("PERSON") ) {
			return true;
		}
		String afterTag = parser.getTag(node.index());
		if( node.tag().indexOf("NNP")!=-1 && node.index()==1 && afterTag.equals(",") ) {
			return true;
		}
		String previousWord = parser.getWord(node.index()-2);
		if ( previousWord.equals("thank") || previousWord.equals("thanks") ) {
			return true;
        }
		return false;
	}
	
	private boolean hasEmphasis(String word) {
		String ori= word;
		String deleteEmphasis = "";
		if ( !resources.correctSpellings.correctSpelling(word) ) {
	         for(int iPos = 1; iPos < word.length(); ++iPos) {
	        	 if (word.substring(iPos, iPos + 1).compareTo(word.substring(iPos - 1, iPos)) == 0) {
	               String sReplaceWord = word.substring(0, iPos) + word.substring(iPos + 1);
	               if (resources.correctSpellings.correctSpelling( sReplaceWord )) {
	            	   deleteEmphasis = sReplaceWord;
	            	   break;
	               }else {
	            	   word = sReplaceWord;
	            	   iPos--;
	               }
	            }
	         }
	    }
		return deleteEmphasis.length()!=0 && !ori.equals(deleteEmphasis);
	}
	
	private void SENounRangeCheck() {
		isSENoun = new boolean[parseListSize];
		isInSENounChunk = new boolean[parseListSize];
		isInSENounChunkSBAR = new boolean[parseListSize];
		isBasedOnSENounChunk = new boolean[parseListSize];
		isInSENounPredicate = new int[parseListSize];
		for(int i=0;i<parseListSize;i++) {
			isInSENounPredicate[i] = -1;
		}
		for(int i=0;i<parseListSize;i++) {
			IndexedWord node = parser.getIndexedWord(i);
			if( isSENoun(node) ) {
				isSENoun[i] = true;
				checkRangeForOneNode(i);
			}
		}
		for(int i=0;i<parseListSize;i++) {
			checkPredicateScopeOfSENoun(i);
		}
	}
	
	private void checkRangeForOneNode(int index) {
		IndexedWord node = parser.getIndexedWord(index);
		Tree root = parser.getTreeByIndex(index);
		Tree SETree = getTreeLeafByIndex(node.index(),root);
		//查找名词团块的范围：
		checkRangeForSENounChunk(index,SETree,root);
		
		//查找名词团块的从句范围：
		//checkRangeForSENounChunkSBAR(index,SETree,root);
		
		//查找名词团块谓语范围：
		//checkRangeForSENounPredicate(index,SETree,root);
		
		//查找基于名词团块的讨论范围：
		checkRangeForChunkBasedOnSENoun(index,SETree,root);
	}
	
	//SENounChunk
	public boolean isNounInSENounChunk(int index) {
		boolean isNoun = isNoun( parser.getTag(index) );
		boolean isRelatedToSE = isInSENounChunk[index];
		return isNoun && isRelatedToSE;
	}
	public boolean isInSENounChunk(int index) {
		boolean isRelatedToSE = isInSENounChunk[index];
		return isRelatedToSE;
	}
	public String[] attachedPPArr = {"of","with","about","on"};
	private Tree getNounChunkTree(Tree SETree,Tree root) {
		Tree nearestTree = null;
		int depth = root.depth();
		for(int i=1;i<depth;i++) {
			Tree ancestor = SETree.ancestor(i,root);
			if( ancestor==null ) {
				break;
			}
			String nodeString = ancestor.value();
			if( nodeString.startsWith("N") ) {
				nearestTree = ancestor;
			}else if( nodeString.equals("PP") ) {
				Tree INTree = ancestor.getLeaves().get(0);
				int treeIndex = getNodeIndex(INTree.label().toString());
				String lemma = parser.getLemma( treeIndex-1 );
				if( isHave(lemma,attachedPPArr) ) {
					nearestTree = ancestor;
				}else {
					break;
				}
			}else {
				break;
			}
		}
		return nearestTree;
	}
	private void checkRangeForSENounChunk(int index,Tree SETree,Tree root) {
		Tree nounChunkTree = getNounChunkTree(SETree,root);
		if( nounChunkTree!=null ) {
			List<Tree> leafList = nounChunkTree.getLeaves();
    		Tree firstNode = leafList.get(0);
    		Tree lastNode = leafList.get(leafList.size()-1);
    		int startIndex =  getNodeIndex(firstNode.label().toString());
    		int endIndex =  getNodeIndex(lastNode.label().toString());
    		int SEIndex = getNodeIndex(SETree.label().toString());
    		for(int move=0; move>=startIndex-SEIndex ;move--) {
    			int i = index+move;
    			boolean isNeedToJumpOut = isNeedToJumpOutOfSENounChunk(i,nounChunkTree);
    			if( isNeedToJumpOut ) {
    				break; 
    			}
    			isInSENounChunk[i] = true;
    		}
    		for(int move=0; move<=endIndex-SEIndex;move++) {
    			int i = index+move;
    			boolean isNeedToJumpOut = isNeedToJumpOutOfSENounChunk(i,nounChunkTree);
    			if( isNeedToJumpOut ) {
    				break; 
    			}
    			isInSENounChunk[i] = true;
    		}
    	}
	}
	private boolean isNeedToJumpOutOfSENounChunk(int index,Tree root) {
		if( parser.isPartitionSymbolByIndex(index) ) { 
			return true;
		}
		String tag = parser.getTag(index);
		if( tag.equals("CC") ) {
			return true;
		}
		Tree tree = getTreeLeafByIndex(parser.getIndexedWord(index).index(),root);
		boolean isNodeInClause = isNodeInClause(tree,root);
		if( isNodeInClause ) {
			return true;
		}
		return false;
	}
	
	//SBAR Of SENounChunk:
	public boolean isInSENounChunkSBAR(int index) {
		return getBool(index,isInSENounChunkSBAR);
	}
	private void checkRangeForSENounChunkSBAR(int index,Tree SETree,Tree root) {
		//找到名词团块的节点
		Tree nounChunkTree = getNounChunkTree(SETree,root);
		int depth = root.depth();
		for(int i=0;i<depth;i++) {
			//名词团块中的一个名词节点树
			Tree nounTree = SETree.ancestor(i,root);
			checkSBARForOneTree(nounTree,root);
			if( nounTree.equals(nounChunkTree) ) {
				break;
			}
		}
	}
	private void checkSBARForOneTree(Tree tree,Tree root) {
		Tree ancestor = tree.ancestor(1,root);
		if( ancestor==null ) {
			return;
		}
		int SETreeIndex = -1;
		List<Tree> childrenList = ancestor.getChildrenAsList();
		for(int i=0;i<childrenList.size();i++) {
			Tree child = childrenList.get(i);
			if( child.equals(tree) ) {
				SETreeIndex = i;
				break;
			}
		}
		if( SETreeIndex!=-1 ) {
			for(int i=SETreeIndex+1;i<childrenList.size();i++) {
				Tree treePeer = childrenList.get(i);
				String nodeString = treePeer.value();
				int childSize = treePeer.getChildrenAsList().size();
				//childSize>1 : 要求SBAR中有明确出现引导词
				if( nodeString.equals("SBAR") && childSize>1 ) {
					List<Tree> leafList = treePeer.getLeaves();
		    		Tree firstNode = leafList.get(0);
		    		Tree lastNode = leafList.get(leafList.size()-1);
		    		int startIndex =  getNodeIndex(firstNode.label().toString());
		    		int endIndex =  getNodeIndex(lastNode.label().toString());
		    		for(int k = startIndex-1; k<=endIndex-1; k++) {
		    			if( parser.isPartitionSymbolByIndex(k) ) {
		    				break;
		    			}
		    			isInSENounChunkSBAR[k] = true;
		    		}
		    	}
			}
		}
	}
	
	// SENoun + Verb
	public int isInSENounPredicate(int index) {
		return isInSENounPredicate[index];
	}
	//讨论一：
	private void checkRangeForSENounPredicate(int index,Tree SETree,Tree root) {
		Tree nounChunkTree = getNounChunkTree(SETree,root);
		Tree sentenceTree = nounChunkTree.ancestor(1,root);
		Tree predicateTree = null;
		if( sentenceTree!=null ) {
			for(Tree child :sentenceTree.getChildrenAsList()) {
				String nodeString = child.value();
				if( nodeString.equals("VP") ) {
					predicateTree = child;
					break;
				}
			}
		}
		if( predicateTree!=null ) {
			List<Tree> leafList = predicateTree.getLeaves();
    		Tree firstNode = leafList.get(0);
    		Tree lastNode = leafList.get(leafList.size()-1);
    		int startIndex =  getNodeIndex(firstNode.label().toString());
    		int endIndex =  getNodeIndex(lastNode.label().toString());
    		int nearestForwardParIndex = parser.getNearestForwardParIndex(index);
    		int nearestBackwardParIndex = parser.getNearestBackwardParIndex(index);
    		startIndex = Math.max(startIndex-1, nearestForwardParIndex);
    		endIndex = Math.min(endIndex-1, nearestBackwardParIndex);
    		for(int i = startIndex; i<=endIndex ;i++) {
    			isInSENounPredicate[i] = index;
    		}
		}
	}
	//讨论二：
	private void checkPredicateScopeOfSENoun(int index) {
		String tag = parser.getTag(index);
		String lemma = parser.getLemma(index);
		if( !isVerb(tag) || lemma.equals("be") ) {
			return;
		}
		IndexedWord node = parser.getIndexedWord(index);
		SemanticGraph graph = parser.getDepGraphByIndex(index);	
		IndexedWord subj = getNearestSubj(node,graph); //找到主语
		//确认主语为领域词汇
		if( subj==null 
			|| parser.havePartitionInBetween(index, subj.index()-1) 
			|| !isInSENounChunk[ subj.index()-1 ] ) { 
			return;
		}
		isInSENounPredicate[index] = subj.index()-1;
		
		//通过依赖分析确认范围：
		checkRangeByDependence(subj.index()-1,node,graph);
		
		//通过选区分析确认范围：
		//checkRangeByPhrase(subj.index()-1,index);
	}
	private void checkRangeByDependence(int SEIndex,IndexedWord verb,SemanticGraph graph) {
		Set<IndexedWord> childSet = graph.getChildren(verb);
		for(IndexedWord child:childSet) {
			String reln = graph.getEdge(verb,child).getRelation().toString();
			if( reln.indexOf("mod")!=-1 ) {
				int nodeIndex = child.index();
				isInSENounPredicate[nodeIndex-1] = SEIndex;
			}
		}
	}
	private void checkRangeByPhrase(int SEIndex,int VerbIndex) {
		IndexedWord node = parser.getIndexedWord(VerbIndex);
		Tree root = parser.getTreeByIndex(VerbIndex);
		Tree leaf = getTreeLeafByIndex(node.index(),root);
		Tree nearestMultiLeafTree = null;
		int depth = root.depth();
		for(int i=1;i<depth;i++) {
			Tree ancestor = leaf.ancestor(i,root);
			if( ancestor==null ) {
				break;
			}
			if( ancestor.getLeaves().size()>1 ) {
				nearestMultiLeafTree = ancestor;
				break;
			}
		}
		if( nearestMultiLeafTree!=null ) {
			List<Tree> leafList = nearestMultiLeafTree.getLeaves();
    		Tree firstNode = leafList.get(0);
    		Tree lastNode = leafList.get(leafList.size()-1);
    		int startIndex =  getNodeIndex(firstNode.label().toString());
    		int endIndex =  getNodeIndex(lastNode.label().toString());
    		int nearestForwardParIndex = parser.getNearestForwardParIndex(VerbIndex-1);
    		int nearestBackwardParIndex = parser.getNearestBackwardParIndex(VerbIndex-1);
    		startIndex = Math.max(startIndex-1, nearestForwardParIndex);
    		endIndex = Math.min(endIndex-1, nearestBackwardParIndex);
    		for(int i = startIndex; i<=endIndex;i++) {
    			isInSENounPredicate[i] = SEIndex;
    		}
		}
	}
	
	// Verb + SENoun
	public boolean isVerbHasSENounObj(int index) {
		boolean isVerb = isVerb( parser.getTag(index) );
		boolean isSENounChunkObj = isHasSENounObj(index);
		return isVerb && isSENounChunkObj;
	}
	private boolean isHasSENounObj(int index) {
		IndexedWord node = parser.getIndexedWord(index);
		SemanticGraph graph = parser.getDepGraphByIndex(index);
		IndexedWord obj = null;
		Set<IndexedWord> childSet = graph.getChildren(node);
		for(IndexedWord child:childSet) {
			String reln = graph.getEdge(node,child).getRelation().toString();
			if( reln.indexOf("obj")!=-1 ) {
				obj = child;
			}
		}
		if( obj!=null && !parser.havePartitionInBetween(index,obj.index()-1) ) {
			return isInSENounChunk[ obj.index()-1 ];
		}
		return false;
	}
	
	//adj + 讨论SE的从句/讨论SE的PP
	public boolean isOptionAdjOnSE(int index) {
		boolean isAdj = isAdj( parser.getTag(index) );
		boolean isOptionOnSE = isOptionOnSE(index);
		return isAdj && isOptionOnSE;
	}
	public boolean isOptionOnSE(int index) {
		boolean isOptionOnSE = false;
		IndexedWord optionNode = parser.getIndexedWord(index);
		Tree root = parser.getTreeByIndex(index);
		Tree optionTree = getTreeLeafByIndex(optionNode.index(),root);
		Tree nearestADJPTree = null;
		int depth = root.depth();
		for(int i=1;i<depth;i++) {
			Tree ancestor = optionTree.ancestor(i,root);
			if( ancestor==null ) {
				break;
			}
			String nodeString = ancestor.value();
			if( nodeString.equals("ADJP") ) {
				nearestADJPTree = ancestor;
				break;
			}
			if( isPhraseLevelTree(nodeString) || isClauseLevelTree(nodeString) ) {
				break;
			}
		}
		if( nearestADJPTree!=null ) {
			int optionTreeIndex = -1;
			List<Tree> childrenList = nearestADJPTree.getChildrenAsList();
			for(int i=0;i<childrenList.size();i++) {
				Tree child = childrenList.get(i);
				boolean isContainOptionTree = child.getLeaves().contains(optionTree);
				if( isContainOptionTree ) {
					optionTreeIndex = i;
					break;
				}
			}
			if( optionTreeIndex!=-1 ) {
				for(int i=optionTreeIndex+1;i<childrenList.size();i++) {
					Tree treePeer = childrenList.get(i);
					String nodeString = treePeer.value();
					//nodeString.equals("SBAR")：I'm afraid that's not java ;)
					//nodeString.equals("PP")：Your code is extremely vulnerable to SQL injection
					//if( nodeString.equals("SBAR") ) {
					if( nodeString.equals("PP") ) {
						Tree INTree = treePeer.getLeaves().get(0);
						int treeIndex = getNodeIndex(INTree.label().toString());
						String lemma = parser.getLemma( treeIndex-1 );
						if( !isHave(lemma,new String[]{"to","about","at"}) ) {
							continue;
						}
						List<Tree> leafList = treePeer.getLeaves();
			    		Tree firstNode = leafList.get(0);
			    		Tree lastNode = leafList.get(leafList.size()-1);
			    		int startIndex =  getNodeIndex(firstNode.label().toString());
			    		int endIndex =  getNodeIndex(lastNode.label().toString());
			    		for(int k = startIndex-1; k<=endIndex-1; k++) {
			    			if( parser.isPartitionSymbolByIndex(k) ) {
			    				break;
			    			}
			    			boolean inSENounChunk = isInSENounChunk[k];
			    			if( inSENounChunk ) {
			    				isOptionOnSE = true;
			    				break;
			    			}
			    		}
			    	}
				}
			}
		}
		return isOptionOnSE;
	}
	
	// chunk + 介词 + SENounChunk
	public String[] INArr = {"to","in","on","at"};
	public boolean isBasedOnSENoun(int index) {
		boolean isBasedOnSENoun = isBasedOnSENounChunk[index];
		return isBasedOnSENoun;
	}
	public void checkRangeForChunkBasedOnSENoun(int index,Tree SETree,Tree root) {
		Tree nearestPPTree = null;
		int depth = root.depth();
		for(int i=1;i<depth;i++) {
			Tree ancestor = SETree.ancestor(i,root);
			if( ancestor==null ) {
				break;
			}
			String nodeString = ancestor.value();
			if( isClauseLevelTree(nodeString) ) {
				break;
			}
			if( nodeString.equals("PP") ) {
				nearestPPTree = ancestor;
				break;
			}
		}
		if( nearestPPTree!=null ) {
			Tree INTree = nearestPPTree.getLeaves().get(0);
			int treeIndex = getNodeIndex(INTree.label().toString());
			String lemma = parser.getLemma( treeIndex-1 );
			if( !isHave(lemma,INArr) ) {
				return;
			}
		}
		Tree PPAncestor = null;
		if( nearestPPTree!=null ) {
			PPAncestor = nearestPPTree.ancestor(1,root);
		}
		if( PPAncestor!=null ) {
			for(Tree child:PPAncestor.getChildrenAsList()) {
				if( child.equals(nearestPPTree) ) {
					break;
				}
				String label = child.value();
				if( !label.equals("VP") && !label.startsWith("VB")) {
					List<Tree> leafList = child.getLeaves();
		    		Tree firstNode = leafList.get(0);
		    		Tree lastNode = leafList.get(leafList.size()-1);
		    		int startIndex =  getNodeIndex(firstNode.label().toString());
		    		int endIndex =  getNodeIndex(lastNode.label().toString());
		    		for(int i = startIndex-1; i<=endIndex-1 ;i++) {
		    			isBasedOnSENounChunk[i] = true;
		    		}
				}
			}
		}
	}

}
