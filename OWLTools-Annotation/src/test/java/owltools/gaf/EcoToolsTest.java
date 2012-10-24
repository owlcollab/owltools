package owltools.gaf;

import static junit.framework.Assert.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import owltools.OWLToolsTestBasics;
import owltools.gaf.GafDocument;
import owltools.gaf.GafObjectsBuilder;
import owltools.gaf.GeneAnnotation;
import owltools.gaf.inference.AnnotationPredictor;
import owltools.gaf.inference.CompositionalClassPredictor;
import owltools.gaf.inference.Prediction;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

@Ignore
public class EcoToolsTest extends OWLToolsTestBasics{

	private static OWLGraphWrapper g;
	private static OWLReasoner r;

	@BeforeClass
	public static void beforeClass() throws Exception {

		// Setup environment.
		ParserWrapper pw = new ParserWrapper();
		OWLOntology ont = pw.parse(getResourceIRIString("eco.obo"));
		g = new OWLGraphWrapper(ont);
		OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();
		r = reasonerFactory.createReasoner(ont);
		g.setReasoner(r);
	}

	@Test
	public void testSimpleEco() throws OWLOntologyCreationException, IOException{

		///
		/// From: http://purl.obolibrary.org/obo/ECO_0000316
		/// IGI should be: "experimental evidence used in manual assertion" in:
	    ///  + evidence
        ///    + experimental evidence
        ///        + experimental phenotypic evidence
        ///            + genetic interaction evidence
        ///                - genetic interaction evidence used in manual assertion
		///

		// Create EcoTools instance.
		EcoTools eco = new EcoTools(g, g.getReasoner(), true);
		
		// Evidence type closure.
		Set<OWLClass> ecoClasses = eco.getClassesForGoCode("IGI");

		// Hopefully we're just getting one here.
		assertTrue("Right now, one code (IGI) should go to one ECO term.",
				ecoClasses.size() == 1);
		for( OWLClass ec : ecoClasses ){

			String ec_str_id = ec.toStringID();
			assertTrue("It should be this long ID here, not: " + ec_str_id,
					ec_str_id.equals("http://purl.obolibrary.org/obo/ECO_0000316"));

			String ec_str_label = checkLabel(ec_str_id);
			assertTrue("It should be this long label here, not: " + ec_str_label,
					ec_str_label.equals("genetic interaction evidence used in manual assertion"));
		}
				
		// Since we're reflexive, our seven ancestors should be:
		HashMap<String, Boolean> foo = new HashMap<String, Boolean>();
	    foo.put("evidence", true);
	    foo.put("experimental evidence", true);
	    foo.put("experimental phenotypic evidence", true);
	    foo.put("genetic interaction evidence", true);
	    foo.put("genetic interaction evidence used in manual assertion", true);
		
		Set<OWLClass> ecoSuperClasses = eco.getAncestors(ecoClasses, true);

		for( OWLClass ec : ecoSuperClasses ){
			String ec_str_id = ec.toStringID();
			String ec_str_label = checkLabel(ec_str_id);
			assertTrue("Actual ancestor should have been in hash, not: " + ec_str_label,
					foo.containsKey(ec_str_label));
		}
			
		assertTrue("There should have been five (5) ancestors, not: " + ecoSuperClasses.size(),
			ecoSuperClasses.size() == 5);

	}
	
	// Helper to pull the label if there is one.
	public String checkLabel(String thingID){
		String retval = null;
		OWLObject obj = g.getOWLObjectByIdentifier(thingID);
		if (obj != null){
			String label = g.getLabel(obj);
			if( label != null ){
				retval = label;
			}
		}		
		return retval;
	}

}
