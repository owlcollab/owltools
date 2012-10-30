package owltools.sim2.preprocessor;

import org.semanticweb.owlapi.model.OWLObjectProperty;

public class ExpressionSimPreProcessor extends AbstractOBOSimPreProcessor {
	
	public void preprocess() {
		
		addPropertyChain(EXPRESSED_IN, PART_OF, EXPRESSED_IN);
		createPropertyView(this.getOWLObjectPropertyViaOBOSuffix(EXPRESSED_IN),
				getOWLDataFactory().getOWLThing(), "expressed in %s");
		trim();
	
	}
	
	public OWLObjectProperty getAboxProperty() {
		return this.getOWLObjectPropertyViaOBOSuffix(EXPRESSED_IN);
	}

}
