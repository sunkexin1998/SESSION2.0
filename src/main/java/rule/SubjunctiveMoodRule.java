package rule;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.Tree;

public class SubjunctiveMoodRule extends BasicRule {
	private int parseListSize;
	private TextParser parser;
	public SubjunctiveMoodRule(TextParser parser) {
		this.parser = parser;
		this.parseListSize = parser.getParseListSize();
		subjunctiveMoodRangeCheck();
	}	
	private boolean hasExplicitCondi;
	public boolean hasExplicitCondi() {
		return hasExplicitCondi;
	}
	private boolean[] isIndexInImplicitCondi;
	private boolean[] isIndexInImplicitConclus;
	private boolean[] isIndexInExplicitCondi;
	private boolean[] isIndexInExplicitConclus;
	public boolean isInImplicitCondi(int index) {
		return getBool(index,isIndexInImplicitCondi);
	}
    public boolean isInImplicitConclus(int index) {
    	return getBool(index,isIndexInImplicitConclus);
	}
    public boolean isInExplicitCondi(int index) {
    	return getBool(index,isIndexInExplicitCondi);
	}
    public boolean isInExplicitConclus(int index) {
    	return getBool(index,isIndexInExplicitConclus);
	}
    
    public void subjunctiveMoodRangeCheck() {
		//初始化含蓄结论句的相关内容：
		isIndexInImplicitConclus = new boolean[parseListSize];
		//初始化含蓄条件句的相关内容：
		isIndexInImplicitCondi = new boolean[parseListSize];
		//初始化显性条件句的相关内容：
		hasExplicitCondi = false;
		isIndexInExplicitCondi = new boolean[parseListSize];
		//初始化结论句的相关内容：
		isIndexInExplicitConclus = new boolean[parseListSize];
		//初始化许愿判断的相关内容：
		isIndexInWish = new boolean[parseListSize];
		for(int i=0;i<parseListSize;i++) {
			String word = parser.getWord(i);
			String lemma = parser.getLemma(i);
			String nextLemma = parser.getLemma(i+1);
			if( "otherwise".equals(lemma) ) {
				otherwiseRangeCheck(i);
			}else if( "assuming".equals(word) ) {
				int startIndex = i+1;
				int endIndex = getOneMarkWordCondiAdvClauseEnd(i);
				for(int j=startIndex;j<endIndex;j++) {
					isIndexInImplicitCondi[j] = true;
				}
			}else if( "in".equals(lemma) && "case".equals(nextLemma) ) {
				int startIndex = i+1;
				int endIndex = getMultiMarkWordCondiAdvClauseEnd(i);
				for(int j=startIndex;j<endIndex;j++) {
					isIndexInImplicitCondi[j] = true;
				}
			}else if( "if".equals(lemma) || "unless".equals(lemma) ) {
				int startIndex = i+1;
				int endIndex = getOneMarkWordCondiAdvClauseEnd(i);
				String oneWordPreceding = parser.getLemma(i-1);
				boolean isConcessionClause = oneWordPreceding.equals("even"); //是否是让步状语从句
				if( isConcessionClause ) {
					for(int j=startIndex;j<endIndex;j++) {
						isIndexInImplicitCondi[j] = true;
					}
				}else {
					for(int j=startIndex;j<endIndex;j++) {
						isIndexInExplicitCondi[j] = true;
						hasExplicitCondi = true;
					}
				}
			}
		}
		//确认显性结论句的范围
		Set<Integer> MDIndexSet = getExplicitConclusRange(isIndexInExplicitConclus);
		
		//确认含蓄结论范围
		//implicitRangeCheck( MDIndexSet );
		//implicitRangeCheckByVerb();
		
		//确认是否在对没有达成的事物表达某种期待：
		wishRangeCheak();
	}
	
    //确认是否在对没有达成的事物表达某种期待：
    private boolean[] isIndexInWish;
    public boolean[] getIsIndexInWish() {
		return isIndexInWish;
	}
	private void wishRangeCheak() {
		//情况一：以"hope""wish"开头表达愿望
		boolean beforeIsPunc = true;
		int startIndex = parseListSize;
		for(int i=0;i<parseListSize;i++) {
			if( beforeIsPunc ) {
				String lemma = parser.getLemma(i);
				if( lemma.equals("hope") || lemma.equals("wish") ) {
					startIndex = i;
				}
			}
			if( parser.isPartitionSymbolByIndex(i) || i==parseListSize-1) {
				for(int j=startIndex;j<i;j++) {
					isIndexInWish[j] = true;
				}
				startIndex = parseListSize;
				beforeIsPunc = true;
			}else {
				beforeIsPunc = false;
			}
		}
		//情况二：子句含有"in advance"
		int inAdvanceIndex = parseListSize;
		for(int i=0;i<parseListSize;i++) {
			String lemmaOne = parser.getLemma(i);
			String lemmaTwo = parser.getLemma(i+1);
			if( lemmaOne.equals("in") && lemmaTwo.equals("advance") ) {
				inAdvanceIndex = i;
			}
		}
		if( inAdvanceIndex!=parseListSize ) {
			int nearestForwardParIndex = parser.getNearestForwardParIndex( inAdvanceIndex );
    		int nearestBackwardParIndex = parser.getNearestBackwardParIndex( inAdvanceIndex+1 );
    		startIndex = Math.max(0, nearestForwardParIndex);
    		int endIndex = Math.min(parseListSize-1, nearestBackwardParIndex);
			for(int i=startIndex;i<endIndex;i++) {
				isIndexInWish[i] = true;
			}
		}
	}
	public boolean isInWish(int index,float oriSenti) {
		if( isIndexInWish[index] ) {
			return true;
		}
		if( oriSenti<=0 ) {
			return false;
		}
		boolean isRelatedToAny = false;
		IndexedWord node = parser.getIndexedWord(index);
		SemanticGraph dep = parser.getDepGraphByIndex(index);
		IndexedWord nearstSubj = getNearestSubj(node,dep);
		IndexedWord nearstObj = getNearestObj(node,dep);
		if( nearstSubj!=null ) {
			Set<IndexedWord> childSet = dep.getChildren(nearstSubj);
			for(IndexedWord child:childSet) {
				if( child.lemma().equals("any") ) {
					isRelatedToAny = true;
					break;
				}
			}
		}
		if( nearstObj!=null ) {
			Set<IndexedWord> childSet = dep.getChildren(nearstObj);
			for(IndexedWord child:childSet) {
				if( child.lemma().equals("any") ) {
					isRelatedToAny = true;
					break;
				}
			}
		}
		return isRelatedToAny;
	}
	
	//关于"otherwise"含蓄条件范围的查找
	private void otherwiseRangeCheck(int index) {
		IndexedWord condAdv = parser.getIndexedWord(index);
		SemanticGraph dep = parser.getDepGraphByIndex(index);
		//System.out.println("Dependency Graph:\n " +dep.toString(SemanticGraph.OutputFormat.READABLE));
		IndexedWord condAdv_Gov = dep.getParent(condAdv);
		//若条件状语的母节点是根结点,则整个句子都在含蓄条件句的范围内；
		if( dep.getFirstRoot().equals(condAdv_Gov) ) {
			for(int i = 0; i<parseListSize ;i++) {
				if( i==index ) {
					continue;
				}
				isIndexInImplicitConclus[i] = true;
			}
	    }else {
	    	for(int i = 0; i<parseListSize ;i++) {
	    		if( i==index ) {
					continue;
				}
	    		IndexedWord node = parser.getIndexedWord(i);
	    		List<SemanticGraphEdge> edgeList = dep.getShortestDirectedPathEdges(condAdv_Gov, node);
	    		if( edgeList!=null && !edgeList.isEmpty() ) {
	    			isIndexInImplicitConclus[i] = true;
	    		}
			}
	    }
	}
	
	//查找条件状语从句的范围——方法一：利用依赖分析的方法
	private int getCondiAdvClauseEnd1(int index) {
		int startIndex = index+1;
		int endIndex = parseListSize;
		//情况一：以引导词后的第二个主语 为条件状语从句范围的结束
		IndexedWord subj = null;
		IndexedWord subjGov = null;
		for(int i=startIndex;i<parseListSize;i++) {
			SemanticGraph dep = parser.getDepGraphByIndex(index);
			IndexedWord node = parser.getIndexedWord(i);
			IndexedWord gov = dep.getParent(node);
			String reln = gov!=null ? dep.getEdge(gov, node).getRelation().toString() : "";
			if( reln.equals("nsubj") ) {
				if( subj==null ) {
					subj = node;
					subjGov = gov;
				}
				//找到了第二个主语
				else if( subj!=null && !gov.equals(subjGov)){
					int minIndex = node.index()-1;
					Set<IndexedWord> secondSubjChildSet = dep.getChildren(node);
					for(IndexedWord child:secondSubjChildSet) {
						minIndex = Math.min(minIndex, child.index()-1);
					}
					endIndex = Math.min(endIndex,minIndex);
					break;
				}
			}
		}
		//情况二：以句内间隔符为结束
		for(int i=startIndex;i<parseListSize;i++) {
			if( parser.isPartitionSymbolByIndex(i) ) {
				endIndex = Math.min(endIndex, i);
				break;
			}
		}
		return endIndex;
	}
	
	//查找条件状语从句的范围——方法二：借助选区分析
	private int getMultiMarkWordCondiAdvClauseEnd(int index) {
		IndexedWord node = parser.getIndexedWord(index);
		Tree root = parser.getTreeByIndex(index);
		Tree markWordTree = getTreeLeafByIndex(node.index(),root);
		Tree nearestPPTree = null;
		int depth = root.depth();
		for(int i=1;i<depth;i++) {
			Tree ancestor = markWordTree.ancestor(i,root);
			if( ancestor==null ) {
				break;
			}
			String label = ancestor.value();
			if( label.equals("PP") ) {
				nearestPPTree = ancestor;
				break;
			}
		}
		int endIndex = getCondiAdvClauseEnd(index,nearestPPTree,root);
		return endIndex;
	}
	
	private int getOneMarkWordCondiAdvClauseEnd(int index) {
		Tree root = parser.getTreeByIndex(index);
		Tree markWordTree = getTreeLeafByIndex(index+1,root);
		int endIndex = getCondiAdvClauseEnd(index,markWordTree,root);
		return endIndex;
	}
	
	private int getCondiAdvClauseEnd(int index,Tree markWordTree,Tree root) {
		int endIndex = index;
		if( markWordTree==null ) {
			return endIndex;
		}
		int depth = root.depth();
		Tree nearestClauseTree = null;
		for(int i=1;i<depth;i++) {
			Tree ancestor = markWordTree.ancestor(i,root);
			if( ancestor==null ) {
				break;
			}
			if( ancestor.getChildrenAsList().size()>1 ) {
				nearestClauseTree = ancestor;
				break;
			}
		}
		if( nearestClauseTree!=null ) {
			List<Tree> leafList = nearestClauseTree.getLeaves();
    		Tree lastNode = leafList.get(leafList.size()-1);
    		endIndex =  getNodeIndex(lastNode.label().toString());
    		int nearestBackwardParIndex = parser.getNearestBackwardParIndex(index);
    		endIndex = Math.min(endIndex, nearestBackwardParIndex+1);
		}
		return endIndex;
	}
	
	private Set<Integer> getExplicitConclusRange(boolean[] isIndexInExplicitConclus) {
		Set<Integer> MDIndexSet = new HashSet<Integer>();
    	for(Tree tree:parser.getTreeList()) {
    		Tree root = tree;
    		LinkedList<Tree> queue = new LinkedList<Tree>(); 
    		queue.add(root);
    		while(!queue.isEmpty()) {
    			root = (Tree)queue.poll();
    			String rootValue = root.value();
    			List<Tree> rootChildList = root.getChildrenAsList();
    			//是谓语组
    			if( rootValue.equals("VP") ) {
    				//该谓语组含有情态动词
    				boolean isContainTargetMD = false;
    				for(Tree rootChild :rootChildList) {
    					String rootChildValue = rootChild.value();
    					if( rootChildValue.equals("MD") ) {
    						isContainTargetMD = true;
    						String MDLabel = rootChild.getLeaves().get(0).label().toString();
    						int MDIndex = getNodeIndex(MDLabel)-1;
    						MDIndexSet.add(MDIndex);
    						break;
    					}
    				}
    				if( isContainTargetMD ) {
    					List<Tree> leafList = root.getLeaves();
    					String startLabel = leafList.get(0).label().toString();
    					String endLabel = leafList.get(leafList.size()-1).label().toString();
    					int startIndex = getNodeIndex(startLabel);
    					int endIndex = getNodeIndex(endLabel);
    					for(int j=startIndex-1;j<=endIndex-1;j++) {
    						isIndexInExplicitConclus[j] = true;
    					}
    				}
    			}
    			if( root.getLeaves().size()>1 ) {
    				for(int i=0;i<rootChildList.size();i++) {
    	        		Tree child = rootChildList.get(i);
    	        		queue.add(child);
    	        	}
    			}
    		}
    	}
    	return MDIndexSet;
    }
	
	//关于没有显性标识符的含蓄条件范围的查找
	private void implicitRangeCheck(Set<Integer> MDIndexSet) {
		for(Integer MDIndex:MDIndexSet) {
			SemanticGraph dep = parser.getDepGraphByIndex(MDIndex);
			IndexedWord MDNode= parser.getIndexedWord(MDIndex);
			IndexedWord verbNode = dep.getParent(MDNode);
			if( verbNode==null ) {
				continue;
			}
			Set<IndexedWord> verbChildSet = dep.getChildren(verbNode);
			for(IndexedWord child:verbChildSet ) {
				String reln = dep.getEdge(verbNode, child).getRelation().toString();
				//以-ing、-ed分词在句首作含蓄条件的情况
				if( reln.equals("advcl") 
					&& (child.tag().equals("VBG") || child.tag().equals("VBN") ) ) {
					if( child.index()-1==0 || parser.isPartitionSymbolByIndex(child.index()-2) ) {
						int implConclusStartIndex = MDIndex;
						for(int i=child.index()-1;i<MDIndex;i++) {
							if( parser.isPartitionSymbolByIndex(i) ) {
								implConclusStartIndex = i;
								break;
							}
							isIndexInImplicitCondi[i] = true;
						}
						for(int i=implConclusStartIndex;i<parseListSize;i++) {
							if( parser.isPartitionSymbolByIndex(i) ) {
								break;
							}
							isIndexInImplicitConclus[i] = true;
						}
					}
				}
				//以-ing分词作主语的情况
				if( reln.indexOf("subj")!=-1 && child.tag().equals("VBG") ) {
					int startIndex = child.index()-1;
					for(int i=startIndex;i<parseListSize;i++) {
						if( parser.isPartitionSymbolByIndex(i) ) {
							break;
						}
						isIndexInImplicitConclus[i] = true;
					}
				}
			}
		}
	}
	
	//关于观点词的探索；
	private static String[] estimateVerb = {"afraid","doubt"};
	public boolean isEstimateVerb(int index) {
		String lemma = parser.getLemma(index);
		return isHave(lemma,estimateVerb);
	}
	public boolean isExpressEstimation(int index) {
		SemanticGraph dep = parser.getDepGraphByIndex(index);
		IndexedWord node = parser.getIndexedWord(index);
		if( isClauseInFutureTense(node,dep) ) {
			return true;
		}
		return false;
	}
	private boolean isClauseInFutureTense(IndexedWord estimateVerb,SemanticGraph graph) {
		IndexedWord compNode = null;
		Set<IndexedWord> kids = graph.getChildren(estimateVerb);
		for(IndexedWord kid:kids) {
			String relation = graph.getEdge(estimateVerb,kid).getRelation().toString();
			if( relation.indexOf("comp")!=-1 ) {
				compNode = kid;
			}
		}
		if( compNode != null ) {
			IndexedWord compAux = null;
			kids = graph.getChildren(compNode);
			for(IndexedWord kid:kids) {
				String relation = graph.getEdge(compNode,kid).getRelation().toString();
				if( relation.equals("aux") ) {
					compAux = kid;
				}
			}
			if( compAux!=null 
				&& ( "will".equals(compAux.lemma()) || "would".equals(compAux.lemma()) 
					|| "may".equals(compAux.lemma()) || "might".equals(compAux.lemma()) ) ) {
				return true;
			}
		}
		return false;
	}
	
	//根据引导动词查找含蓄结论句：
	private String[] leadingVerbArr = {"ensure","guess","recommend"};
	public void implicitRangeCheckByVerb() {
		//先找到引导动词的nodeIndex
		Set<Integer> leadingVerbIndexSet = new HashSet<Integer>();
		for(int i=0;i<parseListSize;i++) {
			String lemma = parser.getLemma(i);
			if( isHave(lemma,leadingVerbArr) ) {
				leadingVerbIndexSet.add(i+1);
			}
		}
		//找到引导动词在选区中的树节点：
		Set<Tree> leadingVerbTreeSet = new HashSet<Tree>();
		for(Tree tree:parser.getTreeList()) { //遍历每一颗树
    		Tree root = tree.firstChild();
    		List<Tree> leafList = root.getLeaves();
    		for(Tree leaf:leafList) {
    			String leafLabel = leaf.label().toString();
				int index = getNodeIndex(leafLabel);
				if( leadingVerbIndexSet.contains(index) ) {
					leadingVerbTreeSet.add(leaf);
				}
    		}
    	}
		//寻找引导动词是否有从句跟随
		Set<Tree> clauseTreeSet = new HashSet<Tree>();
		for(Tree leadingVerbTree:leadingVerbTreeSet) {
			String leadingVerbTreeLabel = leadingVerbTree.label().toString();
			int TreeLabel = getNodeIndex(leadingVerbTreeLabel);
			int index = TreeLabel-1;
			Tree root = parser.getTreeByIndex(index).firstChild();
			Tree ancestor = leadingVerbTree.ancestor(2,root);
			List<Tree> peerList = ancestor.getChildrenAsList();
			for(Tree peer:peerList) {
				String nodeValue = peer.value();
				if( isClauseLevelTree(nodeValue) ) {
					clauseTreeSet.add(peer);
				}
			}
		}
		//将从句范围设置为含蓄结论句的范围：
		for(Tree clauseTree:clauseTreeSet) {
			List<Tree> leafList = clauseTree.getLeaves();
			String startLabel = leafList.get(0).label().toString();
			String endLabel = leafList.get(leafList.size()-1).label().toString();
			int startIndex = getNodeIndex(startLabel);
			int endIndex = getNodeIndex(endLabel);
			for(int j=startIndex-1;j<=endIndex-1;j++) {
				isIndexInImplicitConclus[j] = true;
			}
		}
		
	}
}
