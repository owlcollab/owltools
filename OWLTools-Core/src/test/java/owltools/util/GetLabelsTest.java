package owltools.util;

import static org.junit.Assert.*;

import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import owltools.OWLToolsTestBasics;

/**
 * This test is primarily for illustrative purposes - it
 * shows how to fetch a label for a class using the OWLAPI
 * 
 * @author cjm
 *
 */
public class GetLabelsTest extends OWLToolsTestBasics {

	OWLOntology ont;
	
	public class MultiLabelException extends Exception {

		public MultiLabelException(OWLNamedObject obj) {
			super("Object has multiple labels: "+obj.toString());
		}

	}

	
	@Test
	public void testGetLabels() throws Exception {
		OWLOntologyManager m = OWLManager.createOWLOntologyManager();
		ont = m.loadOntologyFromOntologyDocument(getResource("caro.owl"));
	
		boolean allHaveLabels = true;
		for (OWLClass c : ont.getClassesInSignature()) {
			String label = getLabel(c);
			if (label == null)
				allHaveLabels = false;
			//System.out.println(c + " LABEL:" + label);
		}
		assertTrue(allHaveLabels);
	}
	
	private String getLabel(OWLEntity obj) throws MultiLabelException {
		String label = null;
		OWLAnnotationProperty labelProperty = ont.getOWLOntologyManager().getOWLDataFactory().getRDFSLabel();
		for (OWLAnnotation ann : OwlHelper.getAnnotations(obj, labelProperty, ont)) {
			if (ann.getProperty().isLabel()) {
				OWLAnnotationValue v = ann.getValue();
				if (v instanceof OWLLiteral) {
					if (label != null) {
						throw new MultiLabelException(obj);
					}
					label = ((OWLLiteral)v).getLiteral();
				}
			}
		}
		return label;
	}
	
	
}
