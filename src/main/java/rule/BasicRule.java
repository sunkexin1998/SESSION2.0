package rule;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.trees.Tree;

public class BasicRule {
	public boolean isNGramPunc(String text) {
	    Pattern pattern = Pattern.compile("[\\pP><%!&^=|+$]+");
	    return pattern.matcher(text).matches();
    }
    public boolean getBool(int index,boolean[] boolArr) {
    	if( index<0 || index>=boolArr.length ) {
    		return false;
    	}
    	return boolArr[index];
    }
	public boolean isAdv(String tag) {
		if(tag.length()>2) {
			tag=tag.substring(0,2);
		}
		return tag.equals("RB");
	}
	public boolean isAdj(String tag) {
		if(tag.length()>2) {
			tag=tag.substring(0,2);
		}
		return tag.equals("JJ");
	}
	public boolean isVerb(String tag) {
		if(tag.length()>2) {
			tag=tag.substring(0,2);
		}
		return tag.equals("VB");
	}
	public boolean isNoun(String tag) {
		if(tag.length()>2) {
			tag=tag.substring(0,2);
		}
		return tag.equals("NN");
	}
	public boolean isHave(String word,String[] possTag) {
    	for(int i=0;i<possTag.length;i++) {
    		if( word.equals(possTag[i]) ) {
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
	public int getNodeIndex(String nodeLabel) {
		int numStartIndex = nodeLabel.lastIndexOf("-")+1;
		int numEndIndex = nodeLabel.length();
		String indexString = nodeLabel.substring(numStartIndex, numEndIndex);
		int index = Integer.parseInt( indexString );
		return index;
	}
	public Tree getTreeLeafByIndex(int index,Tree tree) {
		List<Tree> leafList = tree.getLeaves();
		for(Tree leaf:leafList) {
			String nodeLabel = leaf.label().toString();
			int nodeIndex = getNodeIndex(nodeLabel);
			if( nodeIndex==index ) {
				return leaf;
			}
		}
		return null;
	}
	public boolean isNodeInClause(Tree node,Tree root) {
		boolean isNodeInClause = false;
		int depth = root.depth();
		for(int i=1;i<depth;i++) {
			Tree ancestor = node.ancestor(i,root);
			if( ancestor==null ) {
				break;
			}
			String nodeString = ancestor.value();
			if( isClauseLevelTree(nodeString) ) {
				isNodeInClause = true;
				break;
			}
		}
//		System.out.println( "node:"+node.spanString() );
//		System.out.println( "root:"+root.spanString() );
//		System.out.println( isNodeInClause );
		return isNodeInClause;
	}
	
	public String[] primaryAux = {"do","be","have"};
	public String[] modalAux = {"will","would","may","might","can","could","shall","should"};
	public boolean isPrimaryAux(String lemma) {
		return isHave(lemma,primaryAux);
	}
	public boolean isModalAux(String lemma) {
		return isHave(lemma,modalAux);
	}
	public boolean isAux(String lemma) {
		return  isPrimaryAux(lemma) || isModalAux(lemma);
	}
	
	public String[] clauseLevelTreeLabel = {"S","SBAR","SBARQ","SINV","SQ"};
	public boolean isClauseLevelTree(String treeLabel) {
		return  isHave(treeLabel,clauseLevelTreeLabel);
	}
	
	public String[] phraseLevelTreeLabel = {"ADJP","ADVP","CONJP","FRAG","INTJ",
			                                "LST","NAC","NP","NX","PP",
			                                "PRN","PRT","QP","RRC","UCP",
			                                "VP","WHADJP","WHAVP","WHNP","WHPP",
			                                "X"};
	public boolean isPhraseLevelTree(String treeLabel) {
		return  isHave(treeLabel,phraseLevelTreeLabel);
	}
	
	//找到最近的主语
	public IndexedWord getNearestSubj(IndexedWord node,SemanticGraph graph) {
		if( isAdj(node.tag()) ) {
			Set<IndexedWord> childSet = graph.getChildren(node);
			for(IndexedWord child:childSet) {
				String reln = graph.getEdge(node,child).getRelation().toString();
				if( reln.indexOf("subj")!=-1 ) {
					return child;
				}
			}
		}
		ArrayList<IndexedWord> nodeListToRoot = new ArrayList<IndexedWord>();
		nodeListToRoot.add(node);
		nodeListToRoot.addAll(graph.getPathToRoot(node));
		for(int i=0;i<nodeListToRoot.size();i++) {
			IndexedWord nodeToRoot = nodeListToRoot.get(i);
			Set<IndexedWord> childSet = graph.getChildren(nodeToRoot);
			for(IndexedWord child:childSet) {
				String reln = graph.getEdge(nodeToRoot,child).getRelation().toString();
				if( reln.indexOf("subj")!=-1 ) {
					return child;
				}
			}
		}
		return null;
	}
	
	//找到直接主语
	public IndexedWord getDirectSubj(IndexedWord node,SemanticGraph graph) {
		IndexedWord subj = null;
		Set<IndexedWord> childSet = graph.getChildren(node);
		for(IndexedWord child:childSet) {
			String reln = graph.getEdge(node,child).getRelation().toString();
			if( reln.indexOf("subj")!=-1 ) {
				subj = child;
			}
		}
		return subj;
	}
	
	//找到最近的宾语
	public IndexedWord getNearestObj(IndexedWord node,SemanticGraph graph) {
		ArrayList<IndexedWord> nodeListToRoot = new ArrayList<IndexedWord>();
		nodeListToRoot.add(node);
		nodeListToRoot.addAll(graph.getPathToRoot(node));
		for(int i=0;i<nodeListToRoot.size();i++) {
			IndexedWord nodeToRoot = nodeListToRoot.get(i);
			Set<IndexedWord> childSet = graph.getChildren(nodeToRoot);
			for(IndexedWord child:childSet) {
				String reln = graph.getEdge(nodeToRoot,child).getRelation().toString();
				if( reln.indexOf("obj")!=-1 || reln.indexOf("obl")!=-1 ) {
					return child;
				}
			}
		}
		return null;
	}	
}
