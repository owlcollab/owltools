package owltools.gaf;

import static junit.framework.Assert.*;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class EcoToolsTest extends OWLToolsTestBasics{

	private static OWLGraphWrapper g;
	private static OWLReasoner r;

	@BeforeClass
	public static void beforeClass() throws Exception {

		// Setup environment.
		ParserWrapper pw = new ParserWrapper();

		//NOTE: Yes, the GO here is unnecessary, but we're trying to also catch a certain behavior
		// where auxilery ontologies are not caught. The best wat to do that here is to load ECO
		// second and then do the merge.
		OWLOntology ont_main = pw.parse(getResourceIRIString("go_xp_predictor_test_subset.obo"));
		OWLOntology ont_scnd = pw.parse(getResourceIRIString("eco.obo"));
		g = new OWLGraphWrapper(ont_main);
		g.addSupportOntology(ont_scnd);
		
		// NOTE: This step is necessary or things will get ignored!
		// (This cropped-up in the loader at one point.)
		for (OWLOntology ont : g.getSupportOntologySet())
			g.mergeOntology(ont);
		
		OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();
		r = reasonerFactory.createReasoner(g.getSourceOntology());
		g.setReasoner(r);
	}
	
	@AfterClass
	public static void afterClass() throws Exception {
		if (g != null) {
			OWLReasoner reasoner = g.getReasoner();
			if (reasoner != null) {
				reasoner.dispose();
			}
		}
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
		assertEquals("Right now, one code (IGI) should go to one ECO term.", 1, ecoClasses.size());
		OWLClass igi = ecoClasses.iterator().next();

		IRI igiIRI = igi.getIRI();
		assertEquals("http://purl.obolibrary.org/obo/ECO_0000316", igiIRI.toString());
		String igiId = g.getIdentifier(igi);
		assertEquals("ECO:0000316", igiId);

		String igiLabel = g.getLabel(igi);
		assertEquals("genetic interaction evidence used in manual assertion", igiLabel);
				
		// Since we're reflexive, our six ancestors should be:
		Set<String> foo = new HashSet<String>();
	    foo.add("evidence"); // ECO:0000000
	    foo.add("experimental evidence"); // ECO:0000006
	    foo.add("experimental phenotypic evidence"); // ECO:0000059
	    foo.add("genetic interaction evidence"); // ECO:0000011
	    foo.add(igiLabel); // ECO:0000316
		
	    // inferred by reasoner using cross products
	    foo.add("experimental evidence used in manual assertion"); // ECO:0000269
	    
		Set<OWLClass> ecoSuperClasses = eco.getAncestors(ecoClasses, true);

		for( OWLClass ec : ecoSuperClasses ){
			String ec_str_label = g.getLabel(ec);
			assertTrue("Actual ancestor should have been in hash, not: " + ec_str_label,
					foo.contains(ec_str_label));
		}
			
		assertEquals(6, ecoSuperClasses.size());

	}
	

}
