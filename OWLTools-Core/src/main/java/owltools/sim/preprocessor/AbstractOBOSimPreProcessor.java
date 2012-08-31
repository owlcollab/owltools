package owltools.sim.preprocessor;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObjectProperty;

public abstract class AbstractOBOSimPreProcessor extends LCSEnabledSimPreProcessor {
	
	//public String HAS_PHENOTYPE = "BFO_0000051"; // TODO
	public String HAS_PHENOTYPE = "RO_0002200"; // TODO
	public String HAS_PART = "BFO_0000051";
	public String COMPOSED_PRIMARILY_OF = "UBREL_0000002";
	public String PART_OF = "BFO_0000050";
	public String INHERES_IN = "BFO_0000052";
	public String INHERES_IN_PART_OF = "RO_0002314";
	public String DEVELOPS_FROM = "RO_0002202";
	public String RESULTS_IN_DEVELOPMENT_OF = "RO_0002296";
	public String RESULTS_IN_MORPHOGENESIS_OF = "RO_0002298";
	public String QUALIFIER = "RO_0002181";

	public void makePartOfReflexive() {
		makeReflexive(PART_OF);
	}
	
	public void makeReflexive(String oboId) {
		makeReflexive(getOWLObjectPropertyViaOBOSuffix(oboId));
	}
	
	protected OWLObjectProperty getOWLObjectPropertyViaOBOSuffix(String iriSuffix) {
		return this.getOWLDataFactory().getOWLObjectProperty(getIRIViaOBOSuffix(iriSuffix));
	}

	protected OWLClass getOWLClassViaOBOSuffix(String iriSuffix) {
		return this.getOWLDataFactory().getOWLClass(getIRIViaOBOSuffix(iriSuffix));
	}

	protected IRI getIRIViaOBOSuffix(String iriSuffix) {
		return IRI.create("http://purl.obolibrary.org/obo/"+iriSuffix);
	}

}
