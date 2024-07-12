package rule;

import java.util.LinkedList;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.trees.Tree;

public class QuestionSentenceRule extends BasicRule {
	private TextParser parser;
	public QuestionSentenceRule(TextParser parser) {
		this.parser = parser;
		questionRangeCheck();
	}
	//是否是某种质问
	//质问条件一：出现大于等于2个的"?"
	//质问条件二：输入的文本不为一个完整的句子
	public boolean isCensureQs() {
		boolean isCensureQs = false;
		//纯符号不算
		if( isNGramPunc(parser.getText()) ) {
			return isCensureQs;
		}
		//出现大于等于2个的"?"
		int countQs = countAttendance(parser.getText(),"?");
		if( countQs<2 ) {
			return isCensureQs;
		}
		//输入的文本不为一个完整的句子
		boolean hasClause = false;
		for(Tree root:parser.getTreeList() ) {
			LinkedList<Tree> queue = new LinkedList<Tree>(); 
			queue.add(root);
			while(!queue.isEmpty()) {
				root = (Tree)queue.poll();
				String nodeString = root.value();
				if( isClauseLevelTree(nodeString) ) {
					hasClause = true;
					break;
				}
				if( root.getLeaves().size()>1 ) {
					List<Tree> rootChildrenList = root.getChildrenAsList();
		        	for(int i=0;i<rootChildrenList.size();i++) {
		        		Tree child = rootChildrenList.get(i);
		        		queue.add(child);
		        	}
				}
	        }
			if( hasClause ) {
				break;
			}
		}
		if( !hasClause ) {
			isCensureQs = true;
		}
		return isCensureQs;
	}
	private int countAttendance(String text,String sub) {
		int count = 0;
		int startIndex = 0;
		while( startIndex<text.length() ) {
			startIndex = text.indexOf(sub, startIndex);
			if( startIndex!=-1 ) {
				count++;
				startIndex += sub.length();
			}else {
				break;
			}
		}
		return count;
	}
	
	private boolean[] isIndexInQuestion;
	public void questionRangeCheck() {
		questionRangeCheck1();
	}
	
	public boolean isInQuestion(int index) {
		return getBool(index,isIndexInQuestion);
	}
	
	// 方法一：根据 TreeAnnotation 确定问句范围
	private void questionRangeCheck1() {
		boolean hasSetQsRange = false;
		int parseListSize = parser.getParseListSize();
		isIndexInQuestion = new boolean[parseListSize];
		//句中是否有感叹号感叹号
		boolean hasExclamatoryMark = parser.getText().indexOf("!")!=-1;
		if( !hasExclamatoryMark ) {
			for(Tree root:parser.getTreeList()) {
				LinkedList<Tree> queue = new LinkedList<Tree>(); 
				queue.add(root);
				while(!queue.isEmpty()) {
					root = (Tree)queue.poll();
					String nodeString = root.value();
					List<Tree> rootChildrenList = root.getChildrenAsList();
					if( nodeString.equals("SQ") ) {
						boolean hasSmallSQ = false;
						for(int i=0;i<rootChildrenList.size();i++) {
							Tree child = rootChildrenList.get(i);
							String childNodeString = child.toString();
							if( childNodeString.indexOf("(SQ ")!=-1 ) {
								hasSmallSQ = true;
							}
						}
						if( !hasSmallSQ ) {
							List<Tree> leafList = root.getLeaves();
			        		Tree firstNode = leafList.get(0);
			        		Tree lastNode = leafList.get(leafList.size()-1);
			        		int startIndex =  getNodeIndex(firstNode.label().toString());
			        		int endIndex =  getNodeIndex(lastNode.label().toString());
			        		for(int i = startIndex; i<endIndex ;i++) {
			        			isIndexInQuestion[i] = true;
			        			hasSetQsRange = true;
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
		}
		//问句特殊情况：以any*;maybe;or为开头
		if( !hasSetQsRange ) {
			//先找到"?"的位置：
			int endIndex = parseListSize;
			for(int i=parseListSize-1;i>=0;i--) {
				if( parser.getWord(i).indexOf("?")!=-1 ){
					endIndex = i;
					break;
				}
			}
			//找到了问号：
			if( endIndex!=parseListSize ) {
				int startIndex = endIndex;
				for(int i=endIndex-1;i>=0;i--) {
					String lemma = parser.getLemma(i);
					int beforeIndex = i-1;
					if( isSpecialQsOpenWord(lemma) && (i==0 || parser.isPartitionSymbolByIndex(beforeIndex)) ) {
						startIndex = i;
					}
				}
				for(int i=startIndex;i<endIndex;i++) {
					isIndexInQuestion[i] = true;
        			hasSetQsRange = true;
				}
			}
		}
	}
	private boolean isSpecialQsOpenWord(String lemma) {
		return lemma.startsWith("any") || lemma.equals("maybe") || lemma.equals("or");
	}
	
	//方法二：自定义方法
	private void questionRangeCheck2() {
		int parseListSize = parser.getParseListSize();
		isIndexInQuestion = new boolean[parseListSize];
		//句中不能出现感叹号
		for(int i=parseListSize-1;i>=0;i--) {
			if( parser.getWord(i).indexOf("!")!=-1 ){
				return;
			}
		}
		//开始查找"?"的位置
		int qsEndIndex = parseListSize;
		for(int i=parseListSize-1;i>=0;i--) {
			if( parser.getWord(i).indexOf("?")!=-1 ){
				qsEndIndex = i;
				break;
			}
		}
		//没有"?"的情况
		if( qsEndIndex==parseListSize ) {
			return;
		}
		//开始查找问句的启始位置：
		int qsStartIndex = qsEndIndex;
		//问句启始情况一：以any*;maybe为开头
		String firstWord = parser.getLemma(0);
		if( firstWord.startsWith("any") || firstWord.equals("maybe") ) {
			qsStartIndex = 0;
		}
		//问句启始情况二、三：
		int noSubjAuxNodeindex = parseListSize;
		for(int i=0;i<parseListSize;i++) {
			String lemma = parser.getLemma(i);
			if( isAux(lemma) ) {
				IndexedWord possibleAuxNode = parser.getIndexedWord(i);
	    		SemanticGraph dep = parser.getDepGraphByIndex(i);
	    		//System.out.println("Dependency Graph:\n " +dep.toString(SemanticGraph.OutputFormat.READABLE));
	    		IndexedWord possAuxGov = dep.getParent(possibleAuxNode);
	    		//特殊情况：疑问助词即为整个句子的root动词
	    		if( possAuxGov==null ) {
	    			qsStartIndex = Math.min(qsStartIndex,i);
	    			continue;
	    		}
	    		String relation = dep.getEdge(possAuxGov, possibleAuxNode).getRelation().toString();
	    		if ( !"aux".equals(relation) && !"cop".equals(relation) ) {
	    			continue;
	    		}
	    		boolean hasSubj = false;
	    		Set<IndexedWord> kids = dep.getChildren(possAuxGov);
	    		for(IndexedWord kid:kids) {
	    			relation = dep.getEdge(possAuxGov,kid).getRelation().toString();
	    			if( relation.indexOf("subj")!=-1 ) {
	    				hasSubj = true;
	    				//问句启始情况二：助动词的主语的WH-Word
	    				if( kid.tag().equals("WP") ) {
	    					qsStartIndex = Math.min(qsStartIndex,kid.index()-1);
	    				}
	    				//问句启始情况三：助动词和主语之间呈现倒置关系
	    				else if( kid.index()>possibleAuxNode.index() ) {
	    					qsStartIndex = Math.min(qsStartIndex,possibleAuxNode.index()-1);
	    				}
	    			}
	    		}
	    		if( !hasSubj ) {
	    			noSubjAuxNodeindex = Math.min(noSubjAuxNodeindex,i);
	    		}
	    	}
    	}
		//问句启始情况四：助动词缺少主语，此类为特殊情况
		if( noSubjAuxNodeindex!=parseListSize ) {
			qsStartIndex = Math.min(qsStartIndex,noSubjAuxNodeindex);
		}
		for(int i = qsStartIndex; i<qsEndIndex ;i++) {
			isIndexInQuestion[i] = true;
		}
	}

}
