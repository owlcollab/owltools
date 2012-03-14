package owltools.gaf.rules;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;


/**
 * This class reads the annotation_qc.xml file and builds {@link AnnotationRule}
 * objects from the qc file.
 * 
 * @author shahidmanzoor
 *
 */
public class AnnotationRulesFactoryImpl implements AnnotationRulesFactory {

	private static Logger LOG = Logger.getLogger(AnnotationRulesFactoryImpl.class);
	
	private List<AnnotationRule> rules = null;
	
	/**
	 * @param path location of the annotation_qc.xml file
	 */
	protected AnnotationRulesFactoryImpl(String path){
		init(path);
	}
	
	private void init(String path) {
		rules = new ArrayList<AnnotationRule>();
		SAXBuilder builder = new SAXBuilder();
		Document doc = null;
		try {
			URI uri;
			if(!(path.startsWith("http://") || path.startsWith("file:///"))){
				File f = new File(path);
				uri = f.toURI();
			}
			else {
				uri = new URI(path);
			}
			doc = builder.build(uri.toURL());
		} catch (Exception e) {
			LOG.error("Unable to load document form: " + path, e);
		}
		
		if(doc == null)
			return;
		
		loadRegex(doc);
		loadJava(doc);
	}

	private void loadJava(Document doc) {
		try{
			XPath javaRule = XPath.newInstance("//implementation/script[@language='java']");			
		    Iterator<?> itr = javaRule.selectNodes(doc).iterator();
		    
		    while(itr.hasNext()){
		    	Element script = (Element)itr.next();
		    	Element idElement = script.getParentElement().getParentElement().getParentElement().getChild("id");
		    	String id = "";
		    	
		    	if(idElement != null){
		    		id = idElement.getTextNormalize();
		    	}
		    	
		    	String className = script.getAttributeValue("source");
		    	if(className != null){
		    		try{
		    			AnnotationRule rule = getClassForName(className);
		    			rule.setRuleId(id);
		    			rules.add(rule);
		    		}catch(Exception ex){
		    			LOG.error(ex.getMessage(), ex);
		    		}
		    	}
		    }
		}catch(Exception ex){
			LOG.error(ex.getMessage(), ex);
		}
	}

	private void loadRegex(Document doc) {
		try{
			XPath regexRule = XPath.newInstance("//implementation/script[@language='regex']");			
		    List<?> regexRules = regexRule.selectNodes(doc);
	
		    for(Object obj: regexRules){
		    	Element script = (Element) obj;
		    	
		    	String regex = script.getTextNormalize();
		    	boolean isCaseInsensitive = regex.endsWith("/i");
		    	
		    	//java doesn't like the /^ switch so it is replaced by ^
		    	regex = regex.replace("/^", "^");
		    	//java does not support the /i case in-sensitivity switch so it is removed
		    	regex = regex.replace("/i", "");
		    	
		    	//title is child of the rule element
		    	Element title= script.getParentElement().getParentElement().getParentElement().getChild("title");
		    	
		    	
		    	String titleString = "";
		    	if(title != null){
		    		titleString = title.getTextNormalize();
		    	}
		    	
		    	Element idElement = title.getParentElement().getChild("id");
		    	String id = "";
		    	if(idElement != null){
		    		id = idElement.getTextNormalize();
		    	}
		    	
		    	Pattern pattern = null;
		    	
		    	if(isCaseInsensitive)
		    		pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		    	else
		    		pattern = Pattern.compile(regex);
		    	
		    	
		    	AnnotationRegularExpressionFromXMLRule rule = new AnnotationRegularExpressionFromXMLRule();
		    	rule.setRuleId(id);
		    	rule.setErrorMessage(titleString);
		    	rule.setPattern(pattern);
		    	rule.setRegex(regex);
		    	
		    	rules.add(rule);
		    }
		}catch(Exception ex){
			LOG.error(ex.getMessage(), ex);
		}
	}

	/**
	 * @param className
	 * @return annotation rule (never null)
	 * @throws Exception
	 */
	protected AnnotationRule getClassForName(String className) throws Exception {
		return (AnnotationRule) Class.forName(className).newInstance();
	}
	
	@Override
	public List<AnnotationRule> getRules(){
		return this.rules;
	}
	
}
