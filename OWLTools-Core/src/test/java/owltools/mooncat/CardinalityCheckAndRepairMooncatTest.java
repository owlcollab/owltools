package owltools.mooncat;

import static org.junit.Assert.*;

import java.util.Collection;

import org.junit.Test;
import org.obolibrary.obo2owl.OboInOwlCardinalityTools;
import org.obolibrary.obo2owl.OWLAPIOwl2Obo;
import org.obolibrary.oboformat.model.Clause;
import org.obolibrary.oboformat.model.Frame;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.parser.OBOFormatConstants.OboFormatTag;
import org.semanticweb.owlapi.model.OWLOntology;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

/**
 * Test for {@link Mooncat} and {@link OboInOwlCardinalityTools}. This test 
 * case simulates conflicting values for a single term definition from 
 * different ontology files.
 */
public class CardinalityCheckAndRepairMooncatTest extends OWLToolsTestBasics {

	private static boolean RENDER_ONTOLOGY_FLAG = false;
	
	/**
	 * Test and repair cardinalities of DEF and COMMENT tags after merging
	 * ontologies (B contains MIREOTed terms from C) via {@link Mooncat}.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMireot() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		
		OWLGraphWrapper g = pw.parseToOWLGraph(getResourceIRIString("mooncat/A.obo"));
		Mooncat m = new Mooncat(g);
		m.addReferencedOntology(pw.parseOBO(getResourceIRIString("mooncat/B.obo")));
		m.addReferencedOntology(pw.parseOBO(getResourceIRIString("mooncat/C.obo")));
		
		m.mergeOntologies();
		
		OWLOntology sourceOntology = g.getSourceOntology();
		OboInOwlCardinalityTools.checkAnnotationCardinality(sourceOntology);
		
		OWLAPIOwl2Obo owl2Obo = new  OWLAPIOwl2Obo(OWLManager.createOWLOntologyManager() );
		OBODoc oboDoc = owl2Obo.convert(sourceOntology);
		if (RENDER_ONTOLOGY_FLAG) {
			renderOBO(oboDoc);
		}
		Frame termFrame = oboDoc.getTermFrame("C:0000001");
		Collection<Clause> defClauses = termFrame.getClauses(OboFormatTag.TAG_DEF);
		assertEquals(1, defClauses.size());
		
		Collection<Clause> commentClauses = termFrame.getClauses(OboFormatTag.TAG_COMMENT);
		assertEquals(1, commentClauses.size());
	}
}
