package rule;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.trees.Tree;

public class PersonSentiRule extends BasicRule {
	private int parseListSize;
	private TextParser parser;
	public PersonSentiRule(TextParser parser) {
		this.parser = parser;
		this.parseListSize = parser.getParseListSize();
		personInfluenceRangeCheck();
		gerundAsSubjRangeCheck();
	}
	private boolean[] inOthersPredicate;
	public boolean isInOthersPredicate(int index) {
		return getBool(index,inOthersPredicate);
	}
	private boolean[] inNonFirstPersonPossessive;
	public boolean isInNonFirstPersonPossessive(int index) {
		return getBool(index,inNonFirstPersonPossessive);
	}
	public String[] theOtherArr = {"you","he","she","they"};
	public String[] nonFirstPersonPossessiveArr = {"your","his","her"};
	private void personInfluenceRangeCheck() {
		inOthersPredicate = new boolean[parseListSize];
		inNonFirstPersonPossessive = new boolean[parseListSize];
		//找到需要查找范围的节点
		for(int i=0;i<parseListSize;i++) {
			String word = parser.getWord(i);
			if( isHave(word,nonFirstPersonPossessiveArr) ) {
				nonFirstPersonPossRangeCheck(i);
			}
			checkPredicateScopeOfTheOther(i);
		}
	}
	
	//讨论一：以整个谓语为范围
	private void checkPredicateScopeOfTheOther(int index) {
		String tag = parser.getTag(index);
		if( !isVerb(tag) ) {
			return;
		}
		IndexedWord node = parser.getIndexedWord(index);
		SemanticGraph graph = parser.getDepGraphByIndex(index);
		IndexedWord subj = getDirectSubj(node,graph);
		if( subj==null || !isHave(subj.word().toLowerCase(),theOtherArr) ) {
			return;
		}
		checkRangeByPhrase(index);
	}
	private void checkRangeByPhrase(int index) {
		IndexedWord node = parser.getIndexedWord(index);
		Tree root = parser.getTreeByIndex(index);
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
    		int nearestForwardParIndex = parser.getNearestForwardParIndex(index-1);
    		int nearestBackwardParIndex = parser.getNearestBackwardParIndex(index-1);
    		startIndex = Math.max(startIndex-1, nearestForwardParIndex);
    		endIndex = Math.min(endIndex-1, nearestBackwardParIndex);
    		for(int i = startIndex; i<=endIndex;i++) {
    			inOthersPredicate[i] = true;
    		}
		}
	}
	//讨论二：仅限谓语动词
	public boolean hasOthersAsSubj(int index) {
		IndexedWord node = parser.getIndexedWord(index);
		SemanticGraph graph = parser.getDepGraphByIndex(index);
		IndexedWord subj = getDirectSubj(node,graph);
		if( subj!=null && !parser.havePartitionInBetween(index,subj.index()-1) ) {
			return isHave(subj.word().toLowerCase(),theOtherArr);
		}
		return false;
	}
	
	//关于 非第一人称所有格的讨论：
	private void nonFirstPersonPossRangeCheck(int index) {
		IndexedWord node = parser.getIndexedWord(index);
		Tree root = parser.getTreeByIndex(index);
		Tree nonFirstPersonPossTree = getTreeLeafByIndex(node.index(),root);
		Tree nounChunkTree = getNounChunkTree(nonFirstPersonPossTree,root);
		if( nounChunkTree!=null ) {
			List<Tree> leafList = nounChunkTree.getLeaves();
    		Tree lastNode = leafList.get(leafList.size()-1);
    		int endIndex =  getNodeIndex(lastNode.label().toString());
    		for(int i=index;i<=endIndex-1;i++) {
    			inNonFirstPersonPossessive[i] = true;
    		}
    	}
	}
	private String[] attachedPPArr = {"of","with","about","on"};
	private Tree getNounChunkTree(Tree SETree,Tree root) {
		Tree nearestTree = null;
		int depth = root.depth();
		for(int i=2;i<depth;i++) {
			Tree ancestor = SETree.ancestor(i,root);
			if( ancestor==null ) {
				break;
			}
			String nodeString = ancestor.value();
			if( nodeString.startsWith("N") ) {
				nearestTree = ancestor;
			}else if( nodeString.equals("PP") ) {
				nearestTree = ancestor;
				//Tree INTree = ancestor.getLeaves().get(0);
				//int treeIndex = getNodeIndex(INTree.label().toString());
				//String lemma = parser.getLemma( treeIndex-1 );
				//if( isHave(lemma,attachedPPArr) ) {
				//	nearestTree = ancestor;
				//}else {
				//	break;
				//}
			}else {
				break;
			}
		}
		return nearestTree;
	}
	
	//动名词作主语的范围探索：
	private boolean isInGerundAsSubjRange[];
	public boolean isInGerundAsSubjRange(int index) {
		return getBool(index,isInGerundAsSubjRange);
	}
	public void gerundAsSubjRangeCheck() {
		isInGerundAsSubjRange = new boolean[parseListSize];
		for(int i=0;i<parseListSize;i++) {
			IndexedWord node = parser.getIndexedWord(i);
			String tag = node.tag();
			//找到一个动名词
			if( tag.equals("VBG") ) {
				SemanticGraph graph = parser.getDepGraphByIndex(i);	
				IndexedWord gov = graph.getParent(node);
				String reln = gov!=null ? graph.getEdge(gov,node).getRelation().toString():"";
				//该动名词为主语
				if( reln.indexOf("subj")!=-1 ) {
					for(int j=i; j<parseListSize;j++) {
						if( parser.isPartitionSymbolByIndex(j) ) {
							break;
						}
						isInGerundAsSubjRange[j] = true;
		    		}
				}
			}
		}
	}
	
}
