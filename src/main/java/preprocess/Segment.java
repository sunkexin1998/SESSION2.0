package preprocess;

import java.util.ArrayList;

public class Segment {
	private ArrayList<String> bctSentences;
	private ArrayList<String> commonSentences;
	public Segment(ArrayList<String> bctSentences, ArrayList<String> commonSentences) {
		super();
		this.bctSentences = bctSentences;
		this.commonSentences = commonSentences;
	}
	public ArrayList<String> getBctSentences() {
		return bctSentences;
	}
	public void setBctSentences(ArrayList<String> bctSentences) {
		this.bctSentences = bctSentences;
	}
	public ArrayList<String> getCommonSentences() {
		return commonSentences;
	}
	public void setCommonSentences(ArrayList<String> commonSentences) {
		this.commonSentences = commonSentences;
	}

}
