package owltools.sim2.preprocessor;

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
public class BruteForceSimPreProcessor extends LCSEnabledSimPreProcessor {
	
	private Logger LOG = Logger.getLogger(BruteForceSimPreProcessor.class);

	
	public void preprocess() {
		
		// TODO - this generates a cross-product of all properties....
		for (OWLObjectProperty p : inputOntology.getObjectPropertiesInSignature(true)) {
			createPropertyView(p);
		}
	}

	

}
