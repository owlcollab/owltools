package owltools.mooncat;

import static org.junit.Assert.*;

import java.util.Collection;

import org.junit.Test;
import org.obolibrary.obo2owl.OWLAPIOwl2Obo;
import org.obolibrary.obo2owl.OboInOwlCardinalityTools;
import org.obolibrary.oboformat.model.Clause;
import org.obolibrary.oboformat.model.Frame;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.parser.OBOFormatConstants.OboFormatTag;
import org.semanticweb.owlapi.model.OWLOntology;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

/**
 * Tests for the detection of import properties during 
 * mireot with {@link Mooncat}.
 */
public class ImportPropertyMooncatTest extends OWLToolsTestBasics {

	private static boolean RENDER_ONTOLOGY_FLAG = false;
	
	/**
	 * Test a conflict in a relation with a direct import
	 * (rel_f is both declared in D and F). Uses the import
	 * property to resolve the conflict during the mireot.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testImportProperties() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		
		OWLGraphWrapper g = pw.parseToOWLGraph(getResourceIRIString("mooncat/D.obo"));
		Mooncat m = new Mooncat(g);
		m.addReferencedOntology(pw.parseOBO(getResourceIRIString("mooncat/E.obo")));
		m.addReferencedOntology(pw.parseOBO(getResourceIRIString("mooncat/F.obo")));
		
		m.mergeOntologies();
		
		OWLOntology sourceOntology = g.getSourceOntology();
		OboInOwlCardinalityTools.checkAnnotationCardinality(sourceOntology);
		
		OWLAPIOwl2Obo owl2Obo = new OWLAPIOwl2Obo(sourceOntology.getOWLOntologyManager());
		OBODoc oboDoc = owl2Obo.convert(sourceOntology);
		if (RENDER_ONTOLOGY_FLAG) {
			renderOBO(oboDoc);
		}
		for (Frame termFrame : oboDoc.getTermFrames()) {
			Collection<Clause> clauses = termFrame.getClauses(OboFormatTag.TAG_PROPERTY_VALUE);
			assertTrue(clauses == null || clauses.size() <= 1);
		}
		boolean relFound = false;
		for (Frame typedefFrame : oboDoc.getTypedefFrames()) {
			Collection<Clause> clauses = typedefFrame.getClauses(OboFormatTag.TAG_PROPERTY_VALUE);
			assertTrue(clauses == null || clauses.size() <= 1);
			if ("rel_f".equals(typedefFrame.getId())) {
				relFound = true;
				assertEquals(1, clauses.size());
				Clause c = clauses.iterator().next();
				assertEquals("IAO:0000412", c.getValue());
				assertEquals("http://purl.obolibrary.org/obo/f.owl", c.getValue2());
			}
		}
		assertTrue(relFound);
	}
	
	/**
	 * Test a conflict in a relation with an direct and indirect import
	 * (rel_f is declared in D, E, and F). Uses the import
	 * property to resolve the conflict during the mireot.
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
		
		OWLAPIOwl2Obo owl2Obo = new OWLAPIOwl2Obo(sourceOntology.getOWLOntologyManager());
		OBODoc oboDoc = owl2Obo.convert(sourceOntology);
		if (RENDER_ONTOLOGY_FLAG) {
			renderOBO(oboDoc);
		}
		for (Frame termFrame : oboDoc.getTermFrames()) {
			Collection<Clause> clauses = termFrame.getClauses(OboFormatTag.TAG_PROPERTY_VALUE);
			assertTrue(clauses == null || clauses.size() <= 1);
		}
		boolean relFound = false;
		for (Frame typedefFrame : oboDoc.getTypedefFrames()) {
			Collection<Clause> clauses = typedefFrame.getClauses(OboFormatTag.TAG_PROPERTY_VALUE);
			assertTrue(clauses == null || clauses.size() <= 1);
			if ("rel_f".equals(typedefFrame.getId())) {
				relFound = true;
				assertEquals(1, clauses.size());
				Clause c = clauses.iterator().next();
				assertEquals("IAO:0000412", c.getValue());
				assertEquals("http://purl.obolibrary.org/obo/f.owl", c.getValue2());
			}
		}
		assertTrue(relFound);
	}
}
