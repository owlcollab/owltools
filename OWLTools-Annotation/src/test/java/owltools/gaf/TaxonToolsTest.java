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

public class TaxonToolsTest extends OWLToolsTestBasics{

	private static OWLGraphWrapper g;
	private static OWLReasoner r;

	@BeforeClass
	public static void beforeClass() throws Exception {

		// Setup environment.
		ParserWrapper pw = new ParserWrapper();

		//NOTE: Yes, the GO here is unnecessary, but we're trying to also catch a certain behavior
		// where auxilery ontologies are not caught. The best wat to do that here is to load taxslim
		// second and then do the merge.
		OWLOntology ont_main = pw.parse(getResourceIRIString("go_xp_predictor_test_subset.obo"));
		OWLOntology ont_scnd = pw.parse(getResourceIRIString("taxslim.obo"));
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
	public void testSimpleTaxon() throws OWLOntologyCreationException, IOException{

		///
		/// From: AmiGO 2 alpha view
        /// [is a] [NCBITaxon:1] root [i]
        ///       [is a] [NCBITaxon:131567] cellular organisms [i]
        ///            [is a] [NCBITaxon:2759] Eukaryota [i]
        ///                [is a] [NCBITaxon:33154] Fungi/Metazoa group [i]
        ///                    [is a] [NCBITaxon:33208] Metazoa [i]
        ///                        [is a] [NCBITaxon:6072] Eumetazoa [i]
        ///                            [is a] [NCBITaxon:33213] Bilateria [i]
        ///                                [is a] [NCBITaxon:33316] Coelomata [i]
        ///                                    [[[]]] [NCBITaxon:33511] Deuterostomia [i]
        ///                                        [is a] [NCBITaxon:7711] Chordata [i]
        ///                                        [is a] [NCBITaxon:7586] Echinodermata [i]		
		///

		// Create TaxonTools instance.
		TaxonTools taxo = new TaxonTools(g, g.getReasoner(), true);
		
		// Taxon closure.
		OWLClass taxonClass = g.getOWLClassByIdentifier("NCBITaxon:33511");
		
		// Hopefully we're just getting one here.
		assertTrue("Got the taxon class", taxonClass != null);
		
		//IRI taxIRI = taxonClass.getIRI();
		//assertEquals("http://purl.obolibrary.org/obo/???_???", taxIRI.toString());
		String taxId = g.getIdentifier(taxonClass);
		assertEquals("NCBITaxon:33511", taxId);

		String taxLabel = g.getLabel(taxonClass);
		assertEquals("Deuterostomia", taxLabel);
				
		// Since we're reflexive, our seven ancestors should be:
		Set<String> foo = new HashSet<String>();
	    foo.add("root");
	    foo.add("cellular organisms");
	    foo.add("Eukaryota");
	    foo.add("Fungi/Metazoa group");
	    foo.add("Metazoa");
	    foo.add("Eumetazoa");
	    foo.add("Bilateria");
	    foo.add("Coelomata");
	    foo.add(taxLabel);
		
	    // // inferred by reasoner using cross products
	    // foo.add("experimental evidence used in manual assertion");
	    
		Set<OWLClass> taxonSuperClasses = taxo.getAncestors(taxonClass, true);

		for( OWLClass ec : taxonSuperClasses ){
			String ec_str_label = g.getLabel(ec);
			assertTrue("Actual ancestor should have been in hash, not: " + ec_str_label,
					foo.contains(ec_str_label));
		}
			
		assertEquals(9, taxonSuperClasses.size());

	}
	
}
