package preprocess;
import edu.stanford.nlp.util.CoreMap;


import sentistrength.ClassificationResources;
import sentistrength.EmoticonsList;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileUtility {
	private String regExForCamelCaseStartWithCapLetter = "(^[A-Z][a-z0-9]+[A-Z]+$)|"
		                                               + "(^[A-Z][a-z0-9]+([A-Z][a-z0-9]+)+$)|"
  		                                               + "(^[A-Z]+[a-z0-9]+([A-Z][a-z0-9]+)+$)|"
  		                                               + "(^[A-Z][a-z0-9]+[A-Z]+([A-Z][a-z0-9]+)+$)|"
  		                                               + "(^[A-Z][a-z0-9]+([A-Z][a-z0-9]+)+[A-Z]+$)";
  
    private String regExForCamelCaseStartWithSmallLetter = "^[a-z]+([A-Z][a-z0-9(]+)+";
  
    private String regExForURL = "(http(s?)://|www.|ftp://)[A-Za-z0-9&.]+(([-/&.?#|]+)[A-Za-z0-9&@%=~_]+)*";
  
    private String exForPieces = "[a-z0-9&@%=~_]{2,}([-/&.?#|]+[a-z0-9&@%=~_]{2,}){2,}|"
                                +"[A-Z][a-z0-9&@%=~_]+([-/&.?#|]+[A-Z][a-z0-9&@%=~_]+){2,}";
  
    private String regExForStringWithSlash = ".*[/\\\\].*";
  
    private String exForURLWithSpace = "http[s ](\\s?)://(\\s?)[\\sA-Za-z0-9&.]+((\\s?)([-/&.?#|]+)(\\s?)[A-Za-z0-9&@%=~_]+)*";
  
    private String exForStructWithSpace = "[a-z0-9&@%=~_]{2,}((\\s+)[-/&.?#|]+(\\s+)[a-z0-9&@%=~_]{2,}){2,}|"
                                         +"[A-Z][a-z0-9&@%=~_]+((\\s+)[-/&.?#|]+(\\s+)[A-Z][a-z0-9&@%=~_]+){2,}";
 
    private String exForFragInBracket = "\\((\\s?)[A-Z]+_[A-Z]+(\\s?)\\)";
    
    private String exForVersionNumber = "\\b\\d+\\.\\d+\\.x\\b";
    
    private String exForEmoji = ":[\\w-]+:";
 
    private String exForSuffix = "\\.(xml|php|net|com|cpp|exe|app|txt|hbm|class|io|js|py|html|h|config|aspx)";
  
    private String[] suffix = {"xml","php","net","com","cpp","exe","app","txt","hbm","class","io","js","py","html","h","config","aspx"};
  
    private String[] compareSymb = {"=","||","&"};
  
    private Pattern pattern;
  
    private Matcher matcher;
  
    private Clauser clauser = new Clauser();
  
    private ClassificationResources resources;
  
    public void setClassificationResources(ClassificationResources resources) {
	    if( this.resources==null ) {
		    this.resources = resources;
	    }
    }
  
    private boolean isOneGramPunc(String text) {
	    Pattern pattern = Pattern.compile("[\\pP><%!&^=|+$]");
	    return pattern.matcher(text).matches();
    }
  
    private boolean isNGramPunc(String text) {
	    Pattern pattern = Pattern.compile("[\\pP><%!&^=|+$]+");
	    return pattern.matcher(text).matches();
    }
  
	private boolean isContainEng(String text) {
		Pattern pattern = Pattern.compile(".*?[a-zA-Z]+.*?");
	    return pattern.matcher(text).matches();
	}
  
	private boolean isPureEng(String text) {
		Pattern pattern = Pattern.compile("[_a-zA-Z/]+");
		return pattern.matcher(text).matches();
    }
  
    private boolean isContain(String word,String[] arr) {
    	for(int i=0;i<compareSymb.length;i++) {
    		if(word.contains( compareSymb[i] )) {
    			return true;
    		}
    	}
    	return false;
    }
   
	private boolean isHave(String word,String[] possTag) {
		for(int i=0;i<possTag.length;i++) {
			if(word.equals(possTag[i])) {
			   return true;
		    }
		}
	    return false;
    }
	
	public Segment GetProcessedArray(String rawLine,boolean iFiltered) {
    	ArrayList<String> filterLst;
	    if(iFiltered) {
	    	rawLine =paragraphProcess(rawLine);
	    	filterLst = GetFilteredLine(rawLine);
	    }else {
	    	filterLst = new ArrayList<String>();
	    	filterLst.add(rawLine);
	    }
	    Segment seg = clauser.getSegs(filterLst);
	    return seg;
    }
    
	private String paragraphProcess(String rawLine) {
	    //去除乱码字符：
	    rawLine = filterOffGarbledCode(rawLine);
	    //优化标点符号的间距：
	    rawLine = deleteSpaceBtwPunc(rawLine);
	    //处理缩写：
	    rawLine = breviary(rawLine);
	    //优化“！”的表现，使其更容易被识别到：
	    rawLine = dealWithExclamation(rawLine);
 	    //处理一些代码片段：
	    rawLine = filtTecFragment(rawLine);
	    return rawLine;
    }
    
    private String filterOffGarbledCode(String text){
    	text = text.replaceAll("â€™", "'");
  	    StringBuffer sb = new StringBuffer();
  	    String per;
  	    for (int z = 0; z < text.length(); z++) {
  	    	per = text.substring(z, z + 1);
  		    if (!per.matches("[\u4e00-\u9fa5]+")) {
  		    	sb.append(per);
  		    	//if (per.matches("[\\x00-\\x7F]+")) {
  		    	//	sb.append(per);
  		    	//}
  		    }  
  		}  
  	    return sb.toString();
    }
    
    private String deleteSpaceBtwPunc(String text){
    	StringBuffer sb = new StringBuffer();
  	    String[] words =text.trim().split("\\s+");
  	    boolean beforeIsPunc = false;
  	    for(int i=0;i<words.length;i++) {
  		    String word = words[i];
  		    if( isNGramPunc(word) && !this.resources.emoticons.isEmoticon(word) ) {
  			    if(!beforeIsPunc) {
  				    sb.append(" ");
  			    }
  			    beforeIsPunc = true;
  		    }else {
  			    sb.append(" ");
  			    beforeIsPunc = false;
  		    }
  		    word = word.replace("I'm"," I'm");
  		    word = word.replace("i'm"," i'm");
  		    word = word.trim();
  		    sb.append(word);
  	    }
  	    text = clauser.protectBct(sb.toString());
  	    Pattern pattern = Pattern.compile("[a-zA-Z]+ _ [a-zA-Z]+");
  	    Matcher m = pattern.matcher(text);
  	    String sub;
  	    String newSub;
  	    while( m.find() ) {
  		    sub = m.group();
  		    newSub = sub.replace(" _ ","_");
  		    text = text.replace(sub,newSub);
  		    m = pattern.matcher(text);
  	    }
  	    return text.trim();
    }
    
    private String breviary(String Str){
    	ArrayList<String> replace1=new ArrayList<String>();
 	    ArrayList<String> replace2=new ArrayList<String>();
 	    replace1.add("t ");
 	    replace1.add("d ");
 	    replace1.add("m ");
 	    replace1.add("s ");
 	    replace2.add("re ");
 	    replace2.add("ve ");
 	    replace2.add("ll ");
 	    int pos=Str.indexOf("' ");
 	    String tail1=null;
 	    String tail2=null;
 	    while(pos!=-1) {
 		    if(pos<Str.length()-5) {
 			    tail1=Str.substring(pos+2,pos+4);
 			    tail2=Str.substring(pos+2,pos+5);
 		    }else {
 			    break;
 		    }
 		    if(replace1.contains(tail1)) {
 			    Str=Str.substring(0,pos)+"'"+Str.substring(pos+2,Str.length());
 		    }else if(replace2.contains(tail2)) {
 			    Str=Str.substring(0,pos)+"'"+Str.substring(pos+2,Str.length());
 		    }
 		    pos=Str.indexOf("' ",pos+5);
 	    }
 	    pos=Str.indexOf(" '");
 	    while(pos!=-1) {
 		    if(pos<Str.length()-5) {
 			    tail1=Str.substring(pos+2,pos+4);
 			    tail2=Str.substring(pos+2,pos+5);
 		    }else {
 			    break;
 		    }
 		    if(replace1.contains(tail1)) {
 			    Str=Str.substring(0,pos)+"'"+Str.substring(pos+2,Str.length());
 		    }else if(replace2.contains(tail2)) {
 			    Str=Str.substring(0,pos)+"'"+Str.substring(pos+2,Str.length());
 		    }
 		    pos=Str.indexOf(" '",pos+5);
 	    }
 	    
 	    StringBuffer sb = new StringBuffer();
   	    String[] words = Str.trim().split("\\s+");
   	    for(int i=0;i<words.length;i++) {
   	    	String word = words[i].toLowerCase();
   	    	if( word.equals("arent") 
   	    	 || word.equals("couldnt") 
   	    	 || word.equals("dont") 
   	    	 || word.equals("isnt") 
   	    	 || word.equals("wouldnt") ) {
   	    		words[i] = words[i].replace("t","'t");
   	    	}else if( word.equals("cant") || word.equals("wont") ) {
   	    		if( hasPersonalPronInAbove(i,words) ) {
   	    			words[i] = words[i].replace("t","'t");
   	    		}
   	    	}
   	    	sb.append(words[i]);
   	    	sb.append(" ");
   	    }
   	    Str = sb.toString().trim();
   	    return Str;
 	}
    
    public String[] personalPronArr = {"i","you","he","she","we","they","who"};
    private boolean hasPersonalPronInAbove(int index,String[] words) {
    	boolean hasPersonalPronInAbove = false;
    	int end = Math.max(0,index-3);
    	for(int i=index-1;i>=end;i-- ) {
    		String word = words[i];
    		for(String personalPron:personalPronArr) {
    			if( word.equals(personalPron) ) {
    				hasPersonalPronInAbove = true;
    				break;
    			}
    		}
    	}
    	return hasPersonalPronInAbove;
    }
    
    private String dealWithExclamation(String str){
		int pos=str.indexOf("!");
		while(pos!=-1) {
			if(pos<str.length()-1) {
				char tail=str.charAt(pos+1);
				boolean isLetter=(tail>='a' && tail<='z') || (tail>='A' && tail<='Z');
				if(isLetter) {
					str=str.substring(0,pos)+"! "+str.substring(pos+1,str.length());
				}
			}
			pos=str.indexOf("!",pos+1);
		}
		return str;
    }
    
    private String filtTecFragment(String sentence) {
    	StringBuffer sb = new StringBuffer();
   	    String[] words = sentence.trim().split("\\s+");
   	    boolean beforeIsComPare = false;
   	    for(int i=0;i<words.length;i++) {
   	    	String word = words[i];
   	    	if(isContain(word,compareSymb) && isNGramPunc(word) && !this.resources.emoticons.isEmoticon(word) ) {
   	    		beforeIsComPare = true;
   	    	}else {
   	    		if(!beforeIsComPare) {
   	    			sb.append(" ");
   	    		}
   	    		beforeIsComPare = false;
   	    	}
   	    	sb.append(word);
   	    }
   	    sentence = sb.toString();
   	    sb = new StringBuffer();
   	    words = sentence.trim().split("\\s+");
   	    for(int i=0;i<words.length;i++) {
	    	String word = words[i];
	    	if(isContain(word,compareSymb) && !this.resources.emoticons.isEmoticon(word) ) {
	    		word = "CODE_FRAGMENT"+word.replaceAll("[^,:;?.]+","");
	    	}
	    	sb.append(" ");
    		sb.append(word);
	    }
   	    
   	    sentence = sb.toString();
   	    Pattern pattern = Pattern.compile(exForURLWithSpace);
	    Matcher m = pattern.matcher(sentence);
	    while( m.find() ) {
	    	sentence = sentence.replace(m.group(),"CODE_FRAGMENT");
		    m = pattern.matcher(sentence);
	    }
	    
	    pattern = Pattern.compile(exForStructWithSpace);
	    m = pattern.matcher(sentence);
	    while( m.find() ) {
	    	sentence = sentence.replace(m.group(),"CODE_FRAGMENT");
		    m = pattern.matcher(sentence);
	    }
	    
	    pattern = Pattern.compile(exForSuffix);
   	    sb = new StringBuffer();
   	    words = sentence.trim().split("\\s+");
   	    boolean beforeIsDot = false;
   	    for(int i=0;i<words.length;i++) {
   	    	//纯标点符号
   	    	if( isNGramPunc(words[i]) && words[i].endsWith(".")) {
				if( i+1<words.length && isHave(words[i+1].toLowerCase(),suffix) ) {
					beforeIsDot = true;
				}else {
					sb.append(" ");
					sb.append(words[i]);
					beforeIsDot = false;
				}
			}else {
				sb.append(" ");
				if(beforeIsDot) {
					sb.append(words[i].toUpperCase());
				}else {
					if(pattern.matcher(words[i].toLowerCase()).matches()) {
						sb.append(words[i].toUpperCase().substring(1,words[i].length()));
					}else {
						sb.append(words[i]);
					}
				}
				beforeIsDot = false;
			}
		}
   	    
   	    sentence = sb.toString().trim();
	    pattern = Pattern.compile(exForFragInBracket);
	    m = pattern.matcher(sentence);
	    while( m.find() ) {
	    	sentence = sentence.replace(m.group(),"");
		    m = pattern.matcher(sentence);
	    }
	    
	    sentence = clauser.deleteBctPairs(sentence);
	    sentence=sentence.replaceAll("''", "\"");
	    sentence=sentence.replaceAll("``", "\"");
	    sentence=sentence.replaceAll("\"(.*?)\"", "QUOTE_TEXT");
   	    sentence=sentence.replaceAll("``(.*?\\S+.*?)``", "QUOTE_TEXT");
   	    sentence=sentence.replaceAll("`(.*?\\S+.*?)`", "QUOTE_TEXT");
   	    sentence=sentence.replaceAll("i(\\s?)\\.(\\s?)e(\\s?)\\. ", "i.e. ");
   	    sentence=sentence.replaceAll("e(\\s?)\\.(\\s?)g(\\s?)\\. ", "e.g. ");
	    return sentence.trim();
    }
    
    public ArrayList<String> GetFilteredLine(String sentence) {
    	ArrayList<String> result = new ArrayList<String>();
	    StringBuffer precessing = new StringBuffer();
	    String previousWord = "";
        String[] words =sentence.trim().split(" ");
        int size = words.length;
        for (int i = 0;i<size;i++) {
    	    String word = words[i];
    	    if (!word.isEmpty()) {
        	    if (previousWord.equals("hi") || previousWord.equals("hello") || previousWord.equals("hellow") 
        			|| previousWord.equals("dear") || previousWord.equals("@")) {
            	    word=getPurePersoanl(word);
            	    previousWord = word;
                }
        	    previousWord = word.toLowerCase(); 
            }
		    if(word.contains("SUBSENTENCE_INDEXOF-")) {
			    precessing.append(" "+word);
			    continue;
		    }
		    if(this.resources.emoticons.isContainEmoticon(word)) {
			    precessing.append(" "+word);
			    if( result!=null) {
				    result.add(precessing.toString());
    			    precessing = new StringBuffer();
	    	    }
			    continue;
		    }
		    if(word.length()==0) continue;
		    if( isTecFragment(word) && i>=1 && isTecFragment(words[i-1]) ) {
                continue;
            }
    	    if(word.charAt(0)=='@') {
    		    word = " "+getPunc(word);
            }
    	    
    	    this.pattern = Pattern.compile(this.regExForURL);
            this.matcher = this.pattern.matcher(word);
            while( this.matcher.find() ) {
            	word = word.replace(this.matcher.group(),"CODE_FRAGMENT");
    	    	this.matcher = pattern.matcher(word);
    	    }
            
            this.pattern = Pattern.compile(this.regExForCamelCaseStartWithSmallLetter);
            this.matcher = this.pattern.matcher(word);
            if (this.matcher.matches()) {
        	    word = "NAME_ENTITY";
            } 
        
            this.pattern = Pattern.compile(this.regExForCamelCaseStartWithCapLetter);
            this.matcher = this.pattern.matcher(word);
            if (this.matcher.matches()) {
        	    word = "NAME_ENTITY";
            }
            
//            this.pattern = Pattern.compile(this.exForEmoji);
//            this.matcher = this.pattern.matcher(word);
//            if (this.matcher.matches()) {
//        	    word = "";
//            }
            
            this.pattern = Pattern.compile(exForVersionNumber);
            this.matcher = this.pattern.matcher(word);
            while( matcher.find() ) {
  	    	    word = word.replace(matcher.group(),"NAME_ENTITY");
  	        }
          
            this.pattern = Pattern.compile(this.exForPieces);
            this.matcher = pattern.matcher(sentence);
  	        while( matcher.find() ) {
  	    	    word = word.replace(matcher.group(),"");
  	        }
  	      
  	        if (word.contains(":") && (word.split(":")).length >= 2 && word.split(":")[0].length() > 2 && word.split(":")[1].length() > 2) {
        	    continue; 
            }
  	        
            if ( word.contains("<") || word.contains("/>") || word.contains("[") || word.contains("]")) {
        	    continue; 
            }
            
            if ( (!word.contains("ri8") && !word.contains("gr8") && word.contains("1")) 
          		  || word.contains("0") || word.contains("2") || word.contains("3") 
          		  || word.contains("4") || word.contains("5") || word.contains("6") 
          		  || word.contains("7") || word.contains("8") || word.contains("9") ) {
            	if( isContainEng(word) ) {
            		word = "NAME_ENTITY" + getPunc(word);
            	}else {
            		continue;
          	    }
            }
          
            if (word.contains("()") || word.contains("[]") ||  word.contains("<>") || word.contains("{}") ) {
        	    word = "NAME_ENTITY" + getPunc(word);
            }
            if (i<size-1) {
        	    String next = words[i+1];
        	    if(isNGramPunc(next) && (next.contains("()") || next.contains("[]") || next.contains("<>") || next.contains("{}")) ) {
        		    i++;
        		    word = "NAME_ENTITY" + getPunc(next);
        	    }
            }
            word = getPureWord(word);
    	    word = recoverBreviary(word,precessing.length()==0);
    	    precessing.append(" "+word);
         }  
         String rs = precessing.toString().trim();
         if( result!=null && rs.length()>0) {
    	     result.add(rs);
         }
         return result;
    }
    
    private String getPurePersoanl(String text) {
  	    String punc=text.replaceAll("[a-zA-Z0-9']+","");
  	    String[] arr=text.split("[^a-zA-Z0-9']+");
  	    String rs="";
  	    for(int i=1;i<arr.length;i++) {
  		    rs+=arr[i];
  	    }
  	    return punc+" "+rs;
    }
    
    private boolean isTecFragment(String word) {
    	return word.indexOf("_")!=-1 && word.equals(word.toUpperCase());
    }
    
    private String getPureWord(String word) {
	    if(word.length()>2) {
		    String fir = word.substring(0, 1);
		    String middle = word.substring(1,word.length()-1);
		    String last = word.substring(word.length()-1, word.length());
		    if(fir.equals(last) && !isContainEngLetter(fir) && isContainEngLetter(middle)) {
			    return middle;
		    }
	    }
	    return word;
    }
  
    private String recoverBreviary(String word,boolean isInBegin) {
	    String wordtlc = word.toLowerCase();
	    if(isInBegin) {
		    if(wordtlc.equals("ill")) {
			    return word.replace("ll", "'ll");
		    }else if(wordtlc.equals("its")) {
                return word.replace("s", "'s");
		    }
	    }
	    if(wordtlc.equals("im")) {
		    return word.replace("m", "'m");
	    }else if(wordtlc.equals("ive")) {
		    return word.replace("ve", "'ve");
	    }else if( wordtlc.equals("youre") || wordtlc.equals("they") ) {
		    return word.replace("re", "'re");
	    }else if( wordtlc.equals("shes") || wordtlc.equals("hes") || wordtlc.equals("thats")) {
		    return word.replace("s", "'s");
	    }
	    return word;
    }
    
    private boolean isContainEngLetter(String text) {
	    for(int i=0;i<text.length();i++) {
		    if(Character.isLetter(text.charAt(i))) {
			    return true;
		    }
	    }
	    return false;
    }
    
    private String getPunc(String text) {
  	    int i;
  	    for(i=0;i<text.length();i++) {
  		    if( charIsPunc(text.charAt(i)) ) {
  			    break;
  		    }
  	    }
  	    return text.substring(i,text.length());
    }
    
    private Character[] puncs = {'^',',',':',';','?','!','.'};
    private boolean charIsPunc(Character c) {
  	    for(Character punc:puncs) {
  		    if(c.equals(punc)) {
  			    return true;
  		    }
  	    }
  	    return false;
    }
  
}