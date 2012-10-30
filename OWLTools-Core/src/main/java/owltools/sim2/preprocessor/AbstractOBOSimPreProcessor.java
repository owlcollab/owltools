package owltools.sim2.preprocessor;

import java.util.ArrayList;
import java.util.List;

import org.obolibrary.obo2owl.Obo2OWLConstants;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;

public abstract class AbstractOBOSimPreProcessor extends LCSEnabledSimPreProcessor {
	
	//public String HAS_PHENOTYPE = "BFO_0000051"; // TODO
	public String HAS_PHENOTYPE = "RO_0002200"; // TODO
	public String HAS_PART = "BFO_0000051";
	public String COMPOSED_PRIMARILY_OF = "UBREL_0000002";
	public String PART_OF = "BFO_0000050";
	public String INHERES_IN = "BFO_0000052";
	public String INHERES_IN_PART_OF = "RO_0002314";
	public String EXPRESSED_IN = "RO_0002206";
	public String DEVELOPS_FROM = "RO_0002202";
	public String RESULTS_IN_DEVELOPMENT_OF = "RO_0002296";
	public String RESULTS_IN_MORPHOGENESIS_OF = "RO_0002298";
	public String QUALIFIER = "RO_0002181";
	public String DEPENDS_ON = "BFO_0000070";

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
		return IRI.create(Obo2OWLConstants.DEFAULT_IRI_PREFIX+iriSuffix);
	}
	
	protected void addPropertyChain(String p1, String p2, String pInferred) {

		List<OWLObjectPropertyExpression> chain = new ArrayList<OWLObjectPropertyExpression>();
		chain.add(getOWLObjectPropertyViaOBOSuffix(p1));
		chain.add(getOWLObjectPropertyViaOBOSuffix(p2));
				// has_phenotype <- has_phenotype o has_part
		addAxiomToOutput(getOWLDataFactory().getOWLSubPropertyChainOfAxiom(chain , getOWLObjectPropertyViaOBOSuffix(pInferred)), false);
		
	}
	

}
