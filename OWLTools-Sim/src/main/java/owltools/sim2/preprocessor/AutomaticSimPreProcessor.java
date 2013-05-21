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
 * uses all properties as view properties, no role chains
 * 
 * @author cjm
 *
 */
public class AutomaticSimPreProcessor extends LCSEnabledSimPreProcessor {
	
	private Logger LOG = Logger.getLogger(AutomaticSimPreProcessor.class);
	Set<OWLObjectProperty> viewProperties = new HashSet<OWLObjectProperty>();
	Set<OWLIndividual> visited = new HashSet<OWLIndividual>();
	
	public void preprocess() {
		
		for (OWLIndividual ind : inputOntology.getIndividualsInSignature(true)) {
			gatherProperties(ind);
		}
		
		for (OWLObjectProperty p : viewProperties) {
			createPropertyView(p);
		}
		flush();
	}


	private void gatherProperties(OWLIndividual ind) {
		gatherProperties(ind, 0);
	}
	
	private void gatherProperties(OWLIndividual ind, int depth) {
		if (visited.contains(ind))
			return;
		if (depth > 1) {
			return;
			// TODO: create property chains
		}
		LOG.info("Gathering props from: "+ind);
		visited.add(ind);
		for (OWLClassExpression x : ind.getTypes(inputOntology)) {
			gatherProperties(x);
		}
		Map<OWLObjectPropertyExpression, Set<OWLIndividual>> pvs = ind.getObjectPropertyValues(inputOntology);
		for (OWLObjectPropertyExpression pe : pvs.keySet()) {
			gatherProperties(pe);
			for (OWLIndividual k : pvs.get(pe)) {
				gatherProperties(ind, depth+1);
			}
		}
	}
	
	private void gatherProperties(OWLObjectPropertyExpression pe) {
		if (pe instanceof OWLObjectProperty) {
			viewProperties.add((OWLObjectProperty) pe);
		}
		else {
			// TODO
		}
		
	}

	private void gatherProperties(OWLClassExpression x) {
		if (x instanceof OWLClass) {
			return;
		}
		else if (x instanceof OWLObjectIntersectionOf) {
			for (OWLClassExpression y : ((OWLObjectIntersectionOf)x).getOperands()) {
				gatherProperties(y);
			}		
		}
		else if (x instanceof OWLObjectSomeValuesFrom) {
			OWLObjectSomeValuesFrom svf = (OWLObjectSomeValuesFrom)x;
			gatherProperties(svf.getProperty());
			gatherProperties(svf.getFiller());
		}
		else {
			//
		}
	}

}
