package rule;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import edu.stanford.nlp.trees.Tree;

//情绪必然表达模型
public class InevitableSentiPattern extends BasicRule {
	private boolean isInevitable;
	private boolean[] isInInevitableSentiPattern;
	private int parseListSize;
	private TextParser parser;
	public InevitableSentiPattern(TextParser parser) {
		isInevitable = false;
		this.parser = parser;
		this.parseListSize = parser.getParseListSize();
		inevitableSentiCheck();
	}
	public boolean isInevitable() {
		return isInevitable;
	}
	public void setInevitable(boolean isInevitable) {
		this.isInevitable = isInevitable;
	}
	public boolean isInInevitableSentiPattern(int index) {
		return getBool(index,isInInevitableSentiPattern);
	}
	private void inevitableSentiCheck() {
		isInInevitableSentiPattern = new boolean[parseListSize];
		predicativeRangeCheck();
	}
	//表语范围查询
	private void predicativeRangeCheck() {
		ArrayList<Tree> predicationTreeList = new ArrayList<Tree>();
		for(Tree root:parser.getTreeList()) {
			LinkedList<Tree> queue = new LinkedList<Tree>(); 
			queue.add(root);
			while(!queue.isEmpty()) {
				root = (Tree)queue.poll();
				String nodeString = root.value();
				List<Tree> rootChildrenList = root.getChildrenAsList();
				if( nodeString.equals("VP") ) {
					int copulaTreeIndex = -1;
					for(int i=0;i<rootChildrenList.size();i++) {
		        		Tree child = rootChildrenList.get(i);
		        		if( child.getLeaves().size()==1 ) {
		        			Tree leaf = child.getLeaves().get(0);
		        			String nodeLabel = leaf.label().toString();
		    				int nodeIndex = getNodeIndex(nodeLabel);
		    				String lemma = parser.getLemma(nodeIndex-1);
		    				if( lemma.equals("be") ) {
		    					copulaTreeIndex = i;
		    				}
		        		}
		        	}
					if( copulaTreeIndex!=-1 ) {
						for(int i=copulaTreeIndex+1;i<rootChildrenList.size();i++) {
			        		Tree child = rootChildrenList.get(i);
			        		nodeString = child.value();
			        		if( nodeString.equals("NP") 
									|| nodeString.equals("ADVP") 
									|| nodeString.equals("ADJP")) {
			        			predicationTreeList.add( rootChildrenList.get(i) );
							}
			        	}
				    }
				}
				if( root.getLeaves().size()>1 ) {
		        	for(int i=0;i<rootChildrenList.size();i++) {
		        		Tree child = rootChildrenList.get(i);
		        		queue.add(child);
		        	}
				}
	        }
		}
		for(Tree predicationTree:predicationTreeList ) {
			List<Tree> leafList = predicationTree.getLeaves();
			for(int i=0;i<leafList.size();i++) {
				Tree leaf = leafList.get(i);
				if( isNodeInClause(leaf,predicationTree) ) {
					break;
				}
				String nodeLabel = leaf.label().toString();
				int nodeIndex = getNodeIndex(nodeLabel);
				isInInevitableSentiPattern[nodeIndex-1] = true;
			}
		}
	}
}
