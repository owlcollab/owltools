package owltools.sim.preprocessor;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;

/**
 * uses ClassAssertions to determine view property
 * 
 * @author cjm
 *
 */
public class NullSimPreProcessor extends LCSEnabledSimPreProcessor {
	
	private Logger LOG = Logger.getLogger(NullSimPreProcessor.class);

	
	public void preprocess() {
	
	}

	

}
