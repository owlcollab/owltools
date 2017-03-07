package owltools.mooncat;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.obolibrary.obo2owl.OboInOwlCardinalityTools;
import org.obolibrary.obo2owl.OWLAPIOwl2Obo;
import org.obolibrary.oboformat.model.Clause;
import org.obolibrary.oboformat.model.Frame;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.parser.OBOFormatConstants.OboFormatTag;
import org.obolibrary.oboformat.writer.OBOFormatWriter;
import org.semanticweb.owlapi.model.OWLOntology;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

/**
 * Tests for the mal-formed and duplicate subsetdef tags after mireot. 
 * with {@link Mooncat}.
 */
public class SubsetDefMooncatTest extends OWLToolsTestBasics {

	private static boolean RENDER_ONTOLOGY_FLAG = false;
	
	/**
	 * Test for missing comments and duplicate header entries.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testImportProperties2() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		
		OWLGraphWrapper g = pw.parseToOWLGraph(getResourceIRIString("mooncat/D.obo"));
		Mooncat m = new Mooncat(g);
		
		m.addReferencedOntology(pw.parseOBO(getResourceIRIString("mooncat/E-complex.obo")));
		m.addReferencedOntology(pw.parseOBO(getResourceIRIString("mooncat/F.obo")));
		
		m.mergeOntologies();
		
		OWLOntology sourceOntology = g.getSourceOntology();
		OboInOwlCardinalityTools.checkAnnotationCardinality(sourceOntology);
		
		OWLAPIOwl2Obo owl2Obo = new OWLAPIOwl2Obo();
		OBODoc oboDoc = owl2Obo.convert(sourceOntology);
		if (RENDER_ONTOLOGY_FLAG) {
			renderOBO(oboDoc);
		}
		Frame headerFrame = oboDoc.getHeaderFrame();
		List<Clause> clauses = new ArrayList<Clause>(headerFrame.getClauses(OboFormatTag.TAG_SUBSETDEF));
		OBOFormatWriter.sortTermClauses(clauses);
		assertTrue(clauses.size() > 2);
		Clause prev = null;
		for(Clause clause : clauses) {
			String value = clause.getValue(String.class);
			assertNotNull(value);
			assertFalse(value.isEmpty());
			String value2 = clause.getValue2(String.class);
			assertNotNull(value2);
			assertFalse(value2.isEmpty());
			if (clause.equals(prev)) {
				fail("Duplicate clause: "+clause);
			}
			prev = clause;
		}
	}
}
