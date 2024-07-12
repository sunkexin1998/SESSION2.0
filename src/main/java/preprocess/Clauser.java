package preprocess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class Clauser {
	private static StanfordCoreNLP pipeline;
	private ArrayList<String> bctSentenceArr;
	private int bctSentenceArrSize=0;
	public int getBctSentenceArrSize() {
		return bctSentenceArrSize;
	}
	public void setBctSentenceArrSize(int bctSentenceArrSize) {
		this.bctSentenceArrSize = bctSentenceArrSize;
	}
    public ArrayList<String> getBctSentenceArr() {
    	return bctSentenceArr;
	}
    public void setBctSentenceArr(ArrayList<String> bctSentenceArr) {
    	this.bctSentenceArr = bctSentenceArr;
	}
    public Clauser() {
    	init();
    }
    public void init() {
  	  Properties prop_split = new Properties();
  	  prop_split.put("annotators", "tokenize, ssplit");
  	  pipeline = new StanfordCoreNLP( prop_split );
    }
    private Pattern regNotEmpty = Pattern.compile(".*?\\S+.*?");
    private Pattern regAlgorithmComp = Pattern.compile("[-n0-9/&amp+*^]+");
    private boolean isSentenceInBct(String sub) {
    	return regNotEmpty.matcher(sub).matches() && !regAlgorithmComp.matcher(sub).matches();
    }
    
    public Segment getSegs(ArrayList<String> filterLst) {
    	ArrayList<String> allBctSubs = new ArrayList<String>();
    	ArrayList<String> allCommonSubs = new ArrayList<String>();
    	for(int i=0;i<filterLst.size();i++) {
    		ArrayList<String> subs = new ArrayList<String>();
    		String newString = getSentenceInBct(filterLst.get(i),subs,allBctSubs.size());
    		allBctSubs.addAll(subs);
			filterLst.set(i,newString);
    	}
    	for(int i=0;i<filterLst.size();i++) {
    		String para = filterLst.get(i);
    		Annotation document = new Annotation(para);
    		  pipeline.annotate(document);
    		  List<CoreMap> sentences = (List<CoreMap>)document.get(CoreAnnotations.SentencesAnnotation.class);
    		  for (CoreMap sentence :sentences) {
    			  allCommonSubs.add(sentence.get(TextAnnotation.class));
    		  }
    	}
    	return new Segment(allBctSubs,allCommonSubs);
    }
    
    public String deleteBctPairs(String text) {
  	  Map<Character,Character> bracket  = new HashMap<Character,Character>();
        bracket.put('>','<');
        bracket.put(']','[');
        bracket.put('}','{');
        Stack stack = new Stack();
        String rs = text;
        for (int i = 0; i < text.length(); i++) {
            Character nowStr = text.charAt(i); //转换成字符串
            //是否为左括号
            if (bracket.containsValue(nowStr)){
                stack.push(i);
            }
            //是否为右括号
            else if (bracket.containsKey(nowStr)){ 
                if (stack.empty()) {
              	  break;
                } 
                //左右括号匹配
                int lbctpos = (Integer) stack.peek();
                Character lbct = text.charAt( lbctpos );
                if (lbct.equals(bracket.get(nowStr))){ 
                	stack.pop();
              	    String sub = text.substring(lbctpos+1, i);
              	    if( regNotEmpty.matcher(sub).matches() ) {
              	    	rs = rs.replace(text.substring(lbctpos,i+1)," ");
                    }
                }else {
              	  break;
                }
            }
        }
        return rs;
    }
    
    public String protectBct(String text) {
    	StringBuilder sb = new StringBuilder();
    	boolean havePairsBct = isHavePairsBct(text);
    	if( havePairsBct ) {
    		for (int i = 0; i < text.length(); i++) {
    			Character nowStr = text.charAt(i); 
    			if(nowStr.equals('(') && i>=1) {
    				Character above = text.charAt(i-1); 
    				if( islBct(above) ) {
    					sb.append(nowStr);
    				}else {
    					sb.append(" "+nowStr);
    				}
    			}else if(nowStr.equals(')') && i<text.length()-1) {
    				Character after = text.charAt(i+1); 
    				if(  isrBct(after) ) {
    					sb.append(nowStr);
    				}else {
    					sb.append(nowStr+" ");
    				}
    			}else {
    				sb.append(nowStr);
    			}
    		}
    		return sb.toString().trim();
    	}else {
    		return text;
    	}
    }
    
    private boolean isHavePairsBct(String text) {
    	Map<Character,Character> bracket  = new HashMap<Character,Character>();
        bracket.put(')','(');
        Stack stack = new Stack();
        for (int i = 0; i < text.length(); i++) {
            Character nowStr = text.charAt(i); //转换成字符串
            //是否为左括号
            if (bracket.containsValue(nowStr)){
                stack.push(i);
            }
            //是否为右括号
            else if (bracket.containsKey(nowStr)){ 
                if (stack.empty()) {
              	  return false;
                } 
                stack.pop();
            }
        }
        return stack.empty();
    }
    
    public String getSentenceInBct(String text,ArrayList<String> subs,int bctStartIndex) {
    	Map<Character,Character> bracket  = new HashMap<Character,Character>();
        bracket.put(')','(');
        Stack stack = new Stack();
        int i=0;
        while(i<text.length()) {
            Character nowStr = text.charAt(i); //转换成字符串
            if(bracket.containsValue(nowStr)) {
            	if( i==0 || (i-1>=0 && islBct(text.charAt(i-1))) ) {
            		stack.push(i);
            	}
            }else if ( bracket.containsKey(nowStr) ) {
            	if( i==text.length()-1 || (i+1<text.length() && isrBct(text.charAt(i+1))) ) {
            		if (stack.empty()) {
            			i++;
                    	continue;
                    } 
                    //左右括号匹配
                    int lbctpos = (Integer) stack.pop();
                    if(stack.empty()) {
                    	String sub = text.substring(lbctpos+1, i);
                        if( isSentenceInBct(sub) ) {
                        	boolean needReplace = false;
            				for(int k = lbctpos;k>=0;k--) {
            					if( Character.isLetter(text.charAt(k)) ) {
            						needReplace = true;
            						break;
            					}
            				}
            				if(!needReplace) {
            					for(int k = i;k<text.length();k++) {
                					if( Character.isLetter(text.charAt(k)) ) {
                						needReplace = true;
                						break;
                					}
                				}
            				}
            				if(needReplace) {
            					String above = text.substring(0,lbctpos);
            					String[] aboves = above.split("\\s+");
            					int aboveSize = aboves.length;
            					if( aboveSize>=1 && isFRAGMENT(aboves[aboveSize-1]) ) {
            						text = text.substring(0,lbctpos) + text.substring(i+1,text.length());
            						i = lbctpos+1;
            					}else {
            						String replace = "SUBSENTENCE_INDEXOF-"+(bctStartIndex+subs.size());
                					text = text.substring(0,lbctpos+1) + replace + text.substring(i,text.length());
                					subs.add(sub);
                					i = lbctpos+replace.length()+2;
            					}
            					continue;
            				}else {
            					text = text.replace("\\(|//)","");
            					break;
            				}
                        }
                    }
                }
            }
            i++;
        }
        return text;
    }
    
    private boolean isFRAGMENT(String word) {
		return word.indexOf("_")!=-1 && word.equals(word.toUpperCase());
	}
    
    private boolean islBct(Character above) {
    	Character[] beforelBct= {'-','\'',':',',','_','B','=','o'};
    	for(int i=0;i<beforelBct.length;i++) {
    		if(above.equals(beforelBct[i])) {
    			return false;
    		}
    	}
    	return true;
    }
    
    
    
    private boolean isrBct(Character after) {
    	Character[] afrBct= {';','^','o','=','-',':'};
    	for(int i=0;i<afrBct.length;i++) {
    		if(after.equals(afrBct[i])) {
    			return false;
    		}
    	}
    	return true;
    }
    
    private Character[] separators = {',',':',';','?','!','.'};
    private boolean isContainSeparator(String text) {
  	    int i;
  	    for(i=0;i<text.length();i++) {
  		    if( charIsSeparator(text.charAt(i)) ) {
  			    return true;
  		    }
  	    }
  	    return false;
    }
    private boolean charIsSeparator(Character c) {
  	    for(Character separator:separators) {
  		    if(c.equals(separator)) {
  			    return true;
  		    }
  	    }
  	    return false;
    }
    
    public List<CoreMap> GetAllSentences(String documentText) {
    	Annotation document = new Annotation(documentText);
  	    pipeline.annotate(document);
  	    List<CoreMap> sentences = (List<CoreMap>)document.get(CoreAnnotations.SentencesAnnotation.class);
  	    return sentences;
    }

}
