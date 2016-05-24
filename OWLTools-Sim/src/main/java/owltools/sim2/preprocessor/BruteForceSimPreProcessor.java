package owltools.sim2.preprocessor;

import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.parameters.Imports;

/**
 * uses ClassAssertions to determine view property
 * 
 * @author cjm
 *
 */
public class BruteForceSimPreProcessor extends LCSEnabledSimPreProcessor {
	
	public void preprocess() {
		
		// TODO - this generates a cross-product of all properties....
		for (OWLObjectProperty p : inputOntology.getObjectPropertiesInSignature(Imports.INCLUDED)) {
			createPropertyView(p);
		}
	}

	

}
