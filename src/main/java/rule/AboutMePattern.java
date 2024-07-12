package rule;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.trees.Tree;

public class AboutMePattern extends BasicRule {
	private int parseListSize;
	private TextParser parser;
	public AboutMePattern(TextParser parser) {
		this.parser = parser;
		this.parseListSize = parser.getParseListSize();
		aboutMeRangeCheck();
	}
	private boolean[] subjectIsMe;
	private boolean[] modIsMy;
	public boolean isAboutMe(int index) {
		//处理主语为I的情况
		if( subjectIsMe[index] ) {
			return true;
		}
		//处理主语为 my 所属词组
		if( modIsMy[index] ) {
			return true;
		}	
		//处理"me"有关的情况
		boolean hasMeAsObj = objIsMe(index);
		if( hasMeAsObj ) {
			return true;
		}
		//处理"mine"有关的情况
		return isLinkWithMine(index);
	}
	
	public boolean objIsMe(int index) {
		boolean directLinkWithMe = isLinkWithMe2(index);
		return directLinkWithMe;
	}
	
	//版本一：只检查 adj 的 subj ;和 动词的obl和obj
	private boolean isLinkWithMe1(int index) {
		String tag = parser.getTag(index);
		if( isAdj(tag) ) {
			boolean directSubjIsMe = false;
			IndexedWord node = parser.getIndexedWord(index);
			SemanticGraph graph = parser.getDepGraphByIndex(index);
			Set<IndexedWord> childSet = graph.getChildren(node);
			for(IndexedWord child:childSet) {
				String reln = graph.getEdge(node,child).getRelation().toString();
				if( child.word().equals("me") && reln.indexOf("subj")!=-1) {
					directSubjIsMe = true;
				}
			}
			return directSubjIsMe;
		}else if( isVerb(tag) ) {
			boolean directObjIsMe = false;
			IndexedWord node = parser.getIndexedWord(index);
			SemanticGraph graph = parser.getDepGraphByIndex(index);
			Set<IndexedWord> childSet = graph.getChildren(node);
			for(IndexedWord child:childSet) {
				String reln = graph.getEdge(node,child).getRelation().toString();
				if( child.word().equals("me") 
					&& (reln.equals("obl") || reln.equals("obj") )  ) {
					directObjIsMe = true;
				}
			}
			return directObjIsMe;
		}
		return false;
	}
	
	//版本二：检查所有直接联系
	private boolean isLinkWithMe2(int index) {
		String word = "me";
		boolean isLinkWithMe = isHasDirectLink(index,word) || isBothObjectsWithMe(index);
		return isLinkWithMe;
	}
	
	private boolean isLinkWithMine(int index) {
		String word = "mine";
		return isHasDirectLink(index,word);
	}
	
	private boolean isHasDirectLink(int index,String word) {
		boolean hasDirectLinkWithMe = false;
		Set<IndexedWord> checkSet = new HashSet<IndexedWord>();
		IndexedWord node = parser.getIndexedWord(index);
		SemanticGraph graph = parser.getDepGraphByIndex(index);
		Set<IndexedWord> childSet = graph.getChildren(node);
		checkSet.addAll( childSet );
		checkSet.add( graph.getParent(node) );
		for(IndexedWord checkNode:checkSet) {
			if( checkNode!=null && checkNode.word()!=null && checkNode.word().equals(word) ) {
				hasDirectLinkWithMe = true;
				break;
			}
		}
		return hasDirectLinkWithMe;
	}
	
	private boolean isBothObjectsWithMe(int index) {
		boolean isBothObjectsWithMe = false;
		IndexedWord node = parser.getIndexedWord(index);
		SemanticGraph graph = parser.getDepGraphByIndex(index);
		Set<IndexedWord> govSet = graph.getParents(node);
		Set<IndexedWord> checkSet = new HashSet<IndexedWord>();
		for(IndexedWord gov:govSet) {
			String reln = graph.getEdge(gov,node).getRelation().toString();
			if( reln.indexOf("obl")!=-1 || reln.indexOf("obj")!=-1 ) {
				checkSet.add(gov);
			}
		}
		for(IndexedWord gov:checkSet) {
			Set<IndexedWord> childSet = graph.getChildren(gov);
			for(IndexedWord cousin:childSet) {
				if( cousin.word().equals("me")  ) {
					isBothObjectsWithMe = true;
					break;
				}
			}
			if( isBothObjectsWithMe ) {
				break;
			}
		}
		return isBothObjectsWithMe;
	}
	
	private void aboutMeRangeCheck() {
		subjectIsMe = new boolean[parseListSize];
		modIsMy = new boolean[parseListSize];
		//找到需要查找范围的节点
		HashSet<Integer> subjIIndexSet = new HashSet<Integer>();
		HashSet<Integer> myIndexSet = new HashSet<Integer>();
		for(int i=0;i<parseListSize;i++) {
			String word = parser.getWord(i);
			IndexedWord node = parser.getIndexedWord(i);
			if( word.equals("i") ) {
				subjIIndexSet.add(node.index());
			}else if( word.equals("my") ) {
				myIndexSet.add(node.index());
			}
		}
		//在选区树上找到这些节点
		ArrayList<Tree> subjITreeList = new ArrayList<Tree>();
		ArrayList<Tree> myTreeList = new ArrayList<Tree>();
		for(Tree tree:parser.getTreeList()) {
			List<Tree> leafList = tree.getLeaves();
			for(Tree leaf:leafList) {
				String nodeLabel = leaf.label().toString();
				int index = getNodeIndex(nodeLabel);
				if( subjIIndexSet.contains(index) ) {
					subjITreeList.add( leaf );
				}else if( myIndexSet.contains(index)  ) {
					myTreeList.add( leaf );
				}
			}
		}
		for(Tree subjITree:subjITreeList ) {
			checkRangeByClause(subjITree);
		}
		for(Tree myTree:myTreeList ) {
			checkRangeByNP(myTree);
		}
	}
	
	private void checkRangeByClause(Tree leaf) {
		Tree nearestClauseTree = null;
		String nodeLabel = leaf.label().toString();
		int index = getNodeIndex(nodeLabel);
		Tree root = parser.getTreeByIndex(index-1);
		int depth = root.depth();
		for(int i=1;i<depth;i++) {
			Tree ancestor = leaf.ancestor(i,root);
			if( ancestor==null ) {
				break;
			}
			String label = ancestor.value();
			if( isClauseLevelTree(label) ) {
				nearestClauseTree = ancestor;
				break;
			}
		}
		if( nearestClauseTree!=null ) {
			List<Tree> leafList = nearestClauseTree.getLeaves();
    		Tree firstNode = leafList.get(0);
    		Tree lastNode = leafList.get(leafList.size()-1);
    		int startIndex =  getNodeIndex(firstNode.label().toString());
    		int endIndex =  getNodeIndex(lastNode.label().toString());
    		int nearestForwardParIndex = parser.getNearestForwardParIndex(index-1);
    		int nearestBackwardParIndex = parser.getNearestBackwardParIndex(index-1);
    		startIndex = Math.max(startIndex-1, nearestForwardParIndex);
    		endIndex = Math.min(endIndex-1, nearestBackwardParIndex);
    		for(int i = startIndex; i<=endIndex ;i++) {
    			subjectIsMe[i] = true;
    		}
		}
	}
	
	private boolean isInWHClause(Tree tree,Tree root) {
		boolean isInWHClause = false;
		Tree ancestor = tree.ancestor(1,root);
		if( ancestor==null ) {
			return isInWHClause;
		}
		List<Tree> childList = ancestor.getChildrenAsList();
		for(Tree child:childList) {
			String label = child.value();
			if( label.startsWith("WH")  ) {
				isInWHClause = true;
			}
		}
		return isInWHClause;
	}
	
	private void checkRangeByNP(Tree leaf) {
		Tree nearestPhraseTree = null;
		String nodeLabel = leaf.label().toString();
		int index = getNodeIndex(nodeLabel);
		Tree root = parser.getTreeByIndex(index-1);
		int depth = root.depth();
		for(int i=1;i<depth;i++) {
			Tree ancestor = leaf.ancestor(i,root);
			if( ancestor==null ) {
				break;
			}
			String label = ancestor.value();
			List<Tree> leafList = ancestor.getLeaves();
			if( label.equals("NP") && leafList.size()>1 ) {
				nearestPhraseTree = ancestor;
				break;
			}
		}
		if( nearestPhraseTree!=null ) {
			List<Tree> leafList = nearestPhraseTree.getLeaves();
    		Tree firstNode = leafList.get(0);
    		Tree lastNode = leafList.get(leafList.size()-1);
    		int startIndex =  getNodeIndex(firstNode.label().toString());
    		int endIndex =  getNodeIndex(lastNode.label().toString());
    		for(int i = startIndex-1; i<=endIndex-1 ;i++) {
    			modIsMy[i] = true;
    		}
		}
	}
}
