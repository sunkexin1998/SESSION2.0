package rule;

import java.util.List;

import edu.stanford.nlp.io.EncodingPrintWriter.out;
import edu.stanford.nlp.trees.Tree;

public class TurningRule extends BasicRule {
	private boolean hasTurning;
	private int parseListSize;
	private TextParser parser;
	public TurningRule(TextParser parser) {
		hasTurning = false;
		this.parser = parser;
		this.parseListSize = parser.getParseListSize();
		turningRangeCheck();
	}
	public boolean isHasTurning() {
  		return hasTurning;
  	}
	private boolean[] isIndexInTurningSatellite;
	public boolean isInTurningSatellite(int index) {
  		return getBool(index,isIndexInTurningSatellite);
  	}
	public void turningRangeCheck() {
		isIndexInTurningSatellite = new boolean[parseListSize];
		for(int i=0;i<parseListSize;i++) {
			String lemma = parser.getLemma(i);
			if( "though".equals(lemma) ) {
				hasTurning = true;
				Tree tree = parser.getTreeByIndex(i);
				//"though"在句末
				if( parser.getTag(i+1).equals(".") ) {
					setForewordAsSatellite(i,tree);
				}else {
					setBackwordAsSatellite(i,tree);
				}
			}else if( "while".equals(lemma) ) {
				String tag = parser.getTag(i);
				//位于句首，词性为介词
				if( tag.equals("IN") && (i==0 || parser.isPartitionSymbolByIndex(i-1)) ) {
					hasTurning = true;
					Tree tree = parser.getTreeByIndex(i);
					setBackwordAsSatellite(i,tree);
				}
			}else if( "but".equals(lemma) || "however".equals(lemma)) {
				hasTurning = true;
				Tree tree = parser.getTreeByIndex(i);
				setForewordAsSatellite(i,tree);
			}
		}
	}
	private void setBackwordAsSatellite(int trunningIndex,Tree tree) {
		Tree turnningTreeNode = getTreeLeafByIndex(trunningIndex+1,tree);
		Tree nearestMultiBranchNode = null;
		int depth = tree.depth();
		for(int i=1;i<depth;i++) {
			Tree ancestor = turnningTreeNode.ancestor(i,tree);
			if( ancestor==null ) {
				break;
			}
			if( ancestor.getLeaves().size()>1 ) {
				nearestMultiBranchNode = ancestor;
				break;
			}
		}
		if( nearestMultiBranchNode!=null ) {
			List<Tree> leafList = nearestMultiBranchNode.getLeaves();
    		Tree lastNode = leafList.get(leafList.size()-1);
    		int endIndex =  getNodeIndex(lastNode.label().toString());
    		for(int i = trunningIndex+1; i<endIndex ;i++) {
    			isIndexInTurningSatellite[i] = true;
    		}
		}
	}
	private void setForewordAsSatellite(int trunningIndex,Tree tree) {
		Tree turnningTreeNode = getTreeLeafByIndex(trunningIndex+1,tree);
		if( turnningTreeNode==null ) {
			return;
		}
		Tree nearestMultiBranchNode = null;
		int depth = tree.depth();
		for(int i=1;i<depth;i++) {
			Tree ancestor = turnningTreeNode.ancestor(i,tree);
			if( ancestor==null ) {
				break;
			}
			if( ancestor.getLeaves().size()>1 ) {
				nearestMultiBranchNode = ancestor;
				break;
			}
		}
		if( nearestMultiBranchNode!=null ) {
			List<Tree> leafList = nearestMultiBranchNode.getLeaves();
			Tree firstNode = leafList.get(0);
			int startIndex =  getNodeIndex(firstNode.label().toString());
			for(int i=trunningIndex-1;i>=startIndex-1;i--) {
				isIndexInTurningSatellite[i] = true;
			}
		}
	}
}
