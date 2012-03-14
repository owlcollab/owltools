package owltools.gaf.rules;

import java.util.List;

public interface AnnotationRulesFactory {

	public void init();
	
	public List<AnnotationRule> getRules();

}