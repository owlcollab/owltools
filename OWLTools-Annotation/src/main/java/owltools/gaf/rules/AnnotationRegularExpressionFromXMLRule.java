package owltools.gaf.rules;

import java.util.Collections;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import owltools.gaf.GeneAnnotation;

/**
 * This class implements execution of the regular expression rules. All the regular expression rules
 * are run by the single instance of this class.
 * @author Shahid Manzoor
 *
 */
public class AnnotationRegularExpressionFromXMLRule extends
		AbstractAnnotationRule {

	private String regex;
	private Pattern pattern;
	private String errorMessage;
	
	public AnnotationRegularExpressionFromXMLRule(){
	}

	@Override
	public Set<AnnotationRuleViolation> getRuleViolations(GeneAnnotation a) {

		if(a == null){
			throw new IllegalArgumentException("GeneAnnotation argument is null");
		}
		return getRuleViolationsRegEx(a);
	}
	
	private Set<AnnotationRuleViolation> getRuleViolationsRegEx(GeneAnnotation ann){

		Matcher m = pattern.matcher(ann.toString());
		
		if(m.find()){
			AnnotationRuleViolation v = new AnnotationRuleViolation(this.getRuleId(), this.errorMessage, ann);
			return Collections.singleton(v);
		}
		return Collections.emptySet();
	}

	public String getRegex() {
		return regex;
	}

	public Pattern getPattern() {
		return pattern;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setRegex(String regex) {
		this.regex = regex;
	}

	public void setPattern(Pattern pattern) {
		this.pattern = pattern;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

}
