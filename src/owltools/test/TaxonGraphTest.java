package owltools.test;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;
import junit.framework.TestCase;
import org.obolibrary.oboformat.model.FrameMergeException;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

public class TaxonGraphTest extends TestCase {
	
	OWLGraphWrapper gw;
	
	@Override
	public void setUp() throws Exception {
		System.out.println("Setting up: " + this);
		List<String> files = new Vector<String>();
		//files.add("test_resources/cell_prolif_placenta.obo");
		files.add("test_resources/ncbi_taxon_slim.obo");
		files.add("test_resources/taxon_union_terms.obo");

		ParserWrapper pw = new ParserWrapper();

		OWLOntology ont = pw.parseOBOFiles(files);
		gw = new OWLGraphWrapper(ont);
	}
	
	public void testUnion() throws OWLOntologyCreationException, IOException, FrameMergeException {
		OWLObject cls = gw.getOWLObjectByIdentifier("NCBITaxon:6239"); // C elegans
		OWLObject uc = gw.getOWLObjectByIdentifier("NCBITaxon_Union:0000005"); // C elegans
		Set<OWLGraphEdge> edges = gw.getOutgoingEdgesClosure(cls);
		// TODO - test includes union
		boolean ok = false;
		for (OWLGraphEdge e : edges) {
			System.out.println(e);
			OWLObject t = e.getTarget();
			String tid = gw.getIdentifier(t);
			System.out.println(" "+tid);
			// Nematoda or Protostomia
			if (tid.equals("NCBITaxon_Union:0000005")) {
				ok = true;
			}
		}
		assertTrue(ok);
		
		assertTrue(gw.getAncestorsReflexive(cls).contains(uc));
	}
}
