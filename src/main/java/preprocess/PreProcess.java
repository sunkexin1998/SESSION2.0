package preprocess;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import sentistrength.ClassificationResources;
public class PreProcess {
	private boolean bgPreprocess;
	private FileUtility objFileUtility;
	private ClassificationResources resources;
	private boolean isInit = false;
	
	public boolean isBgPreprocess() {
		return bgPreprocess;
	}
	
	public void setBgPreprocess(boolean bgPreprocess) {
		this.bgPreprocess = bgPreprocess;
	}
	
	public void init(ClassificationResources resources) throws Exception {
		if(!isInit) {
			this.resources = resources;
			objFileUtility = new FileUtility();
			objFileUtility.setClassificationResources(resources);
			isInit = true;
		}
	}
	
	public Segment getProcessedArray(String text){
		// inlet of Preprocess & Segment
		boolean iFiltered = true;
		Segment seg = objFileUtility.GetProcessedArray(text,iFiltered);
		return seg;
	}
	
	public ArrayList<String> conjArray(ArrayList<String> arr){
		if(arr.size()<=1) {
			return arr;
		}
		Pattern p=Pattern.compile("[a-zA-z]");
		ArrayList<String> conjed=new ArrayList<String>();
		String product=arr.get(0).trim();
		int start=1;
		if(!isEmoticon(product) && !p.matcher(product).find()) {
			start++;
			product=arr.get(1).trim();
		}else {
			
		}
		String part=null;
		for(int i=start;i<arr.size();i++) {
			part=arr.get(i).trim();
			if(isEmoticon(part)) {
				addToConjed(product,conjed);
				product=part;
			}else {
				if(p.matcher(part).find()) {
					addToConjed(product,conjed);
					product=part;
				}else {
					if(this.resources.emoticons.isEmoInTxtEnd(product)) {
						product+=" "+part;
					}else if(part.contains("?") || part.contains("!")){
						product = getPureText(product)+part;
					}else if( isNeedToAdd(part) ){
						product+=part;
					}
				}
			}
		}
		addToConjed(product,conjed);
		return conjed;
	}
	
	private boolean isNeedToAdd(String tail) {
		return tail.length() != 1 || tail.equals("!") || tail.equals("?") || tail.equals(".");
	}
	
	private void addToConjed(String text,ArrayList<String> conjed) {
		if(text.length()>1) {
			text = clear(text);
			conjed.add(text);
		}
	}
	
	private Pattern pattern;
	private Matcher matcher;
	private String endWithTwoDot = "(.*?[\\.]{2}$)";
	private String clear(String text) {
		this.pattern = Pattern.compile(endWithTwoDot);
        this.matcher = this.pattern.matcher(text);
        if (this.matcher.matches()) {
        	text = text+"."; 
        } 
		return text;
	}
	
	private String getPureText(String text) {
		int i;
		for(i=text.length()-1;i>=0;i--) {
			if(text.charAt(i) != '.') {
				break;
			}
		}
        return text.substring(0,i+1);
	}
	
	public void readArray(ArrayList<String> list) {
		for(int i = 0 ; i<list.size();i++){
			System.out.println(list.get(i));
		}
    }
	
	private boolean isEmoticon(String word) {
		return this.resources.emoticons.isEmoticon(word);
	}

}