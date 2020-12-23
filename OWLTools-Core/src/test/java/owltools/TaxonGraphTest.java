package owltools;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.obolibrary.obo2owl.OWLAPIObo2Owl;
import org.obolibrary.oboformat.model.FrameMergeException;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.parser.OBOFormatParser;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;

public class TaxonGraphTest extends OWLToolsTestBasics {

	private static Logger LOG = Logger.getLogger(TaxonGraphTest.class);
	OWLGraphWrapper gw = null;
	
	@Before
	public void before() throws Exception {
		LOG.debug("Setting up: " + this);
		List<String> files = new Vector<String>();
		//files.add(getResource("cell_prolif_placenta.obo").getAbsolutePath());
		files.add(getResource("ncbi_taxon_slim.obo").getAbsolutePath());
		files.add(getResource("taxon_union_terms.obo").getAbsolutePath());

		OBOFormatParser p = new OBOFormatParser();
		OWLAPIObo2Owl obo2Owl = new OWLAPIObo2Owl(setupManager());
		OBODoc merged = null;
		for(String file : files) {
			OBODoc oboDoc = p.parse(file);
			if (merged == null) {
				merged = oboDoc;
			}
			else {
				merged.mergeContents(oboDoc);
			}
		}
		
		OWLOntology ont = obo2Owl.convert(merged);
		gw = new OWLGraphWrapper(ont);
		gw.getConfig().isGraphReasonedAndRelaxed = false;
	}
	
	@Test
	public void testUnion() throws OWLOntologyCreationException, IOException, FrameMergeException {
		OWLObject cls = gw.getOWLObjectByIdentifier("NCBITaxon:6239"); // C elegans
		OWLObject uc = gw.getOWLObjectByIdentifier("NCBITaxon_Union:0000005"); // C elegans
		Set<OWLGraphEdge> edges = gw.getOutgoingEdgesClosure(cls);
		// TODO - test includes union
		boolean ok = false;
		for (OWLGraphEdge e : edges) {
			LOG.debug(e);
			OWLObject t = e.getTarget();
			String tid = gw.getIdentifier(t);
			LOG.debug(" "+tid);
			// Nematoda or Protostomia
			if ("NCBITaxon_Union:0000005".equals(tid)) {
				ok = true;
			}
		}
		assertTrue(ok);
		
		assertTrue(gw.getAncestorsReflexive(cls).contains(uc));
	}
}
