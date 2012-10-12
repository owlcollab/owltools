package owltools.gaf.rules;

import java.io.File;
import java.net.URI;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

import owltools.graph.OWLGraphWrapper;


/**
 * This class reads the annotation_qc.xml file and builds {@link AnnotationRule}
 * objects from the qc file.
 * 
 * @author shahidmanzoor
 *
 */
public class AnnotationRulesFactoryImpl implements AnnotationRulesFactory {

	private static Logger LOG = Logger.getLogger(AnnotationRulesFactoryImpl.class);
	
	public static final ThreadLocal<DateFormat> status_df = new ThreadLocal<DateFormat>() {

		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MM-dd");
		}
	};
	
	private final List<AnnotationRule> annotationRules;
	private final List<AnnotationRule> documentRules;
	private final List<AnnotationRule> owlRules;
	private final String path;
	private final OWLGraphWrapper graph;
	
	private boolean isInitalized = false;
	
	/**
	 * @param path location of the annotation_qc.xml file
	 * @param graph
	 */
	protected AnnotationRulesFactoryImpl(String path, OWLGraphWrapper graph){
		this.path = path;
		this.graph = graph;
		annotationRules = new ArrayList<AnnotationRule>();
		documentRules = new ArrayList<AnnotationRule>();
		owlRules = new ArrayList<AnnotationRule>();
	}
	
	@Override
	public synchronized void init() {
		if (isInitalized) {
			return;
		}
		LOG.info("Start loading GAF validation checks from: "+path);
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
		
		if(doc != null) {
			loadRegex(doc);
			loadJava(doc);
			isInitalized = true;
			
			LOG.info("Finished loading GAF validation checks");
		}
	}

	private void loadJava(Document doc) {
		try{
			XPath javaRule = XPath.newInstance("//implementation/script[@language='java']");			
			List<?> regexRules = javaRule.selectNodes(doc);

			for (Object obj : regexRules) {
				final Element scriptElement = (Element) obj;
				final Element ruleElement = scriptElement.getParentElement().getParentElement().getParentElement();
				final String id = getElementValue(ruleElement.getChild("id"), "");
				final String title = getElementValue(ruleElement.getChild("title"), "");
				final String description = getElementValue(ruleElement.getChild("description"), null);
				
				String status = null;
				Date date = null;
				final Element statusElement = ruleElement.getChild("status");
				if (statusElement != null) {
					status = statusElement.getTextNormalize();
					String dateString = statusElement.getAttributeValue("date", (String) null);
					if (dateString != null) {
						try {
							date = status_df.get().parse(dateString);
						} catch (ParseException e) {
							LOG.warn("Could not parse String as status date: "+dateString, e);
						}
					}
				}

				// TODO add parsing for grand fathering date
//				Date grandFatheringDate = null;
				
				String className = scriptElement.getAttributeValue("source");
				if(className != null){
					try{
						AnnotationRule rule = getClassForName(className);
						if (rule == null) {
							LOG.warn("No implementation found for: "+className);
						}
						else {
							rule.setRuleId(id);
							rule.setName(title);
							rule.setDescription(description);
							rule.setStatus(status);
							rule.setDate(date);
//							rule.setGrandFatheringDate(grandFatheringDate);
							if (rule.isAnnotationLevel()) {
								annotationRules.add(rule);
							}
							if (rule.isDocumentLevel()) {
								documentRules.add(rule);
							}
							if(rule.isOwlDocumentLevel()) {
								owlRules.add(rule);
							}
						}
					}catch(Exception ex){
						LOG.error(ex.getMessage(), ex);
					}
				}
			}
		}catch(Exception ex){
			LOG.error(ex.getMessage(), ex);
		}
	}
	
	private String getElementValue(Element element, String defaultValue) {
		if (element == null) {
			return defaultValue;
		}
		String value = element.getTextNormalize();
		return value;
	}

	private void loadRegex(Document doc) {
		try{
			XPath regexRule = XPath.newInstance("//implementation/script[@language='regex']");			
			List<?> regexRules = regexRule.selectNodes(doc);

			for (Object obj : regexRules) {
				Element scriptElement = (Element) obj;

				String regex = scriptElement.getTextNormalize();
				boolean isCaseInsensitive = regex.endsWith("/i");

				//java doesn't like the /^ switch so it is replaced by ^
				regex = regex.replace("/^", "^");
				//java does not support the /i case in-sensitivity switch so it is removed
				regex = regex.replace("/i", "");

				final Element ruleElement = scriptElement.getParentElement().getParentElement().getParentElement();
				
				final String id = getElementValue(ruleElement.getChild("id"), "");
				final String title = getElementValue(ruleElement.getChild("title"), "");
				final String description = getElementValue(ruleElement.getChild("description"), null);
				
				String status = null;
				Date date = null;
				final Element statusElement = ruleElement.getChild("status");
				if (statusElement != null) {
					status = statusElement.getTextNormalize();
					String dateString = statusElement.getAttributeValue("date", (String) null);
					if (dateString != null) {
						try {
							date = status_df.get().parse(dateString);
						} catch (ParseException e) {
							LOG.warn("Could not parse String as status date: "+dateString, e);
						}
					}
				}
				
				// TODO add parsing for grand fathering date
//				Date grandFatheringDate = null;
				

				Pattern pattern;

				if (isCaseInsensitive) {
					pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
				} else {
					pattern = Pattern.compile(regex);
				}

				AnnotationRegularExpressionFromXMLRule rule = new AnnotationRegularExpressionFromXMLRule();
				rule.setRuleId(id);
				rule.setName(title);
				rule.setDescription(description);
				rule.setStatus(status);
				rule.setDate(date);
//				rule.setGrandFatheringDate(grandFatheringDate);
				rule.setErrorMessage(title);
				rule.setPattern(pattern);
				rule.setRegex(regex);

				annotationRules.add(rule);
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
	public List<AnnotationRule> getGeneAnnotationRules(){
		if (!isInitalized) {
			throw new IllegalStateException("This factory needs to be initialzed before use. Call init()");
		}
		return this.annotationRules;
	}

	@Override
	public List<AnnotationRule> getGafDocumentRules() {
		if (!isInitalized) {
			throw new IllegalStateException("This factory needs to be initialzed before use. Call init()");
		}
		return documentRules;
	}

	@Override
	public List<AnnotationRule> getOwlRules() {
		if (!isInitalized) {
			throw new IllegalStateException("This factory needs to be initialzed before use. Call init()");
		}
		return owlRules;
	}

	@Override
	public OWLGraphWrapper getGraph() {
		return graph;
	}

}
