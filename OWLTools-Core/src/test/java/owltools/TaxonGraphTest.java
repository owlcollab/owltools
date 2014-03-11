package owltools;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

import org.junit.Before;
import org.junit.Test;
import org.obolibrary.oboformat.model.FrameMergeException;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

public class TaxonGraphTest extends OWLToolsTestBasics {
	
	OWLGraphWrapper gw;
	
	@Before
	public void before() throws Exception {
		System.out.println("Setting up: " + this);
		List<String> files = new Vector<String>();
		//files.add(getResource("cell_prolif_placenta.obo").getAbsolutePath());
		files.add(getResource("ncbi_taxon_slim.obo").getAbsolutePath());
		files.add(getResource("taxon_union_terms.obo").getAbsolutePath());

		ParserWrapper pw = new ParserWrapper();

		OWLOntology ont = pw.parseOBOFiles(files);
		gw = new OWLGraphWrapper(ont);
	}
	
	@Test
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
			if ("NCBITaxon_Union:0000005".equals(tid)) {
				ok = true;
			}
		}
		assertTrue(ok);
		
		assertTrue(gw.getAncestorsReflexive(cls).contains(uc));
	}
}
