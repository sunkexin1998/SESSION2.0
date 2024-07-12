package rule;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;

import edu.stanford.nlp.semgraph.SemanticGraphEdge;

public class PolysemyRule extends BasicRule {
	private TextParser parser;
	public PolysemyRule(TextParser parser) {
		this.parser = parser;
	}
	
	public float turnLikeBycontext(int index,float oriSenti){
		if( isVerb( parser.getTag(index) ) ){
			return oriSenti;
		}
		//以下内容为为应对不标准的写作而增加的内容
//		else {
//			int start = Math.max(1,index-3);
//			for(int i=index-1;i>=start;i--) {
//				if( parser.getWord(i).equals("i") ){
//					return oriSenti;
//				}
//			}
//		}
		return 0;
	}
	
	public float turnLyingBycontext(int index,float oriSenti){
		if(parser.getTag(index+1).equals("RB")) {
			return 0;
		}
		if(parser.getTag(index+1).equals("IN") && !parser.getWord(index+1).equals("to")) {
			return 0;
		}
		if(parser.getTag(index+2).equals("IN") && !parser.getWord(index+2).equals("to")) {
			return 0;
		}
		return oriSenti;
	}
	
	public float onlySentiInVerb(int index,float oriSenti){
		String tag = parser.getTag(index);
		if( isVerb(tag) ) {
			return oriSenti;
		}
		return 0;
	}
	
	public float notSentiInAdv(int index,float oriSenti){
		String tag = parser.getTag(index);
		if( isAdv(tag) ) {
			return 0;
		}
		return oriSenti;
	}
	
	public float turnAgainstBycontext(int index,float oriSenti){
		boolean isCompare = false;
		SemanticGraph dep = parser.getDepGraphByIndex(index);
		for(SemanticGraphEdge edge:dep.edgeIterable()) {
			String reln = edge.getRelation().toString();
			if( reln.indexOf(":against")!=-1 ) {
				isCompare = true;
				break;
			}
		}
		if( isCompare ) {
			return 0;
		}else {
			return oriSenti;
		}
	}
	
	public float turnPainBycontext(int index,float oriSenti){
		IndexedWord node = parser.getIndexedWord(index);
		SemanticGraph dep = parser.getDepGraphByIndex(index);
		IndexedWord gov = dep.getParent(node);
		if( node.tag().equals("NNS") && gov.lemma().equals("take") ) {
			return 0;
		}
		return oriSenti;
	}

}
