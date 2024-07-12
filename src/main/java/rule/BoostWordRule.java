package rule;

import java.util.HashMap;
import java.util.List;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.trees.Tree;

public class BoostWordRule extends BasicRule {
	private boolean hasIncreBoost;
	private boolean hasDecreBoost;
	private int parseListSize;
	private TextParser parser;
	private int[] boostValueArr;
	private HashMap<Integer,int[]> boostRangeMap;
	public BoostWordRule(TextParser parser) {
		this.parser = parser;
		this.parseListSize = parser.getParseListSize();
		boostValueArr = new int[parseListSize];
		boostRangeMap = new HashMap<Integer,int[]>();
		hasIncreBoost = false;
		hasDecreBoost = false;
	}
	
	public boolean isHasIncreBoost() {
		return hasIncreBoost;
	}

	public void setHasIncreBoost(boolean hasIncreBoost) {
		this.hasIncreBoost = hasIncreBoost;
	}

	public boolean isHasDecreBoost() {
		return hasDecreBoost;
	}

	public void setHasDecreBoost(boolean hasDecreBoost) {
		this.hasDecreBoost = hasDecreBoost;
	}
	
	public void setBoostWord(int index,int boostValue) {
		//设置标志位：
		if( boostValue>0 ) {
			hasIncreBoost = true;
		}else if( boostValue<0 ) {
			hasDecreBoost = true;
		}
		//首先为该位置设置助词分值：
		boostValueArr[index] = boostValue;
		//查找该位置的影响范围：
		int[] range = getBoostRange(index);
		//System.out.println("BoostWord:"+parser.getLemma(index));
		//System.out.println("Range:["+range[0]+","+range[1]+"]");
		boostRangeMap.put(index, range);
	}
	
	private int[] getBoostRange(int index) {
		int[] range = new int[2];
		range[0] = index+1;
		range[1] = index+1;
		IndexedWord boostNode = parser.getIndexedWord(index);
		Tree tree = parser.getTreeByIndex(index);
		Tree boostTree = getTreeLeafByIndex(boostNode.index(),tree);
		Tree nearestMultiChildTree = null;
		int depth = tree.depth();
		for(int i=1;i<depth;i++) {
			Tree ancestor = boostTree.ancestor(i,tree);
			if( ancestor==null ) {
				break;
			}
			if( ancestor.getLeaves().size()>1 ) {
				nearestMultiChildTree = ancestor;
				break;
			}
		}
		if( nearestMultiChildTree!=null ) {
			List<Tree> leafList = nearestMultiChildTree.getLeaves();
    		Tree lastNode = leafList.get(leafList.size()-1);
    		int endIndex =  getNodeIndex(lastNode.label().toString());
    		int nearestBackwardParIndex = parser.getNearestBackwardParIndex(index);
    		endIndex = Math.min(endIndex, nearestBackwardParIndex+1);
    		range[1] = Math.max(endIndex-1,range[1]);
		}
		return range;
	}
	
	public float getValueAfterBoost(int index,float oriValue) {
		//System.out.println("Word:"+parser.getLemma(index));
		float value = oriValue;
		for(int i=index-1;i>=0;i--) {
			int boostValue = boostValueArr[i];
			//遇到了助词
			if( boostValue!=0 ) {
				int[] range = boostRangeMap.get(i);
				int start = range[0];
				int end = range[1];
				if( index>=start && index<=end ) {
					//System.out.println("in range of:"+parser.getLemma(i));
					value = computeValueAfterBoost(value,boostValue);
				}
			}
		}
		return value;
	}
	
	public HashMap<Integer,Integer> getBoostPair(int index) {
		HashMap<Integer,Integer> indexBoostValueMap = new HashMap<Integer,Integer>();
		for(int i=index-1;i>=0;i--) {
			int boostValue = boostValueArr[i];
			//遇到了助词
			if( boostValue!=0 ) {
				int[] range = boostRangeMap.get(i);
				int start = range[0];
				int end = range[1];
				//在助词的范围内
				if( index>=start && index<=end ) {
					int key = i;
					indexBoostValueMap.put(key, boostValue);
				}
			}
		}
		return indexBoostValueMap;
	}
	
	public boolean isAfterBoost(int index) {
		if( index==0 ) {
			return false;
		}else {
			int beforeBoostValue = boostValueArr[index-1];
			return beforeBoostValue!=0;
		}
	}
	
	private float computeValueAfterBoost(float oriValue,int boostValue) {
		float value = oriValue;
		if ( value>0.0F) {
			value += (float)boostValue;
		}else if ( value<0.0F) {
			value -= (float)boostValue;
        }
		//System.out.println("after boost:"+oriValue+"->"+value);
		return value;
	}

}
