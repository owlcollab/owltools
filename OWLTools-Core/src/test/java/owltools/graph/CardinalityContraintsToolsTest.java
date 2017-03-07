package owltools.graph;

import static org.junit.Assert.*;

import java.util.Collection;

import org.junit.Test;
import org.obolibrary.obo2owl.OWLAPIOwl2Obo;
import org.obolibrary.oboformat.model.Clause;
import org.obolibrary.oboformat.model.Frame;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.model.QualifierValue;
import org.obolibrary.oboformat.parser.OBOFormatConstants.OboFormatTag;
import org.semanticweb.owlapi.model.OWLOntology;

import owltools.OWLToolsTestBasics;

/**
 * Tests for {@link CardinalityContraintsTools}.
 */
public class CardinalityContraintsToolsTest extends OWLToolsTestBasics {

	@Test
	public void testRemoveCardinalityConstraints() throws Exception {
		OWLGraphWrapper graph = getGraph("cardinality.obo");
		
		OWLOntology owlOntology = graph.getSourceOntology();
		CardinalityContraintsTools.removeCardinalityConstraints(owlOntology);
		
		OWLAPIOwl2Obo owl2Obo = new OWLAPIOwl2Obo();
		OBODoc obo = owl2Obo.convert(owlOntology);
		
		assertNoQualifiers("TEST:1000", obo);
		assertNoQualifiers("TEST:1001", obo);
		assertNoQualifiers("TEST:1002", obo);
		assertNoQualifiers("TEST:1003", obo);
	}
	
	private void assertNoQualifiers(String id, OBODoc obo) {
		Frame frame = obo.getTermFrame(id);
		Collection<Clause> clauses = frame.getClauses(OboFormatTag.TAG_INTERSECTION_OF);
		final String message = "Expected intersection clauses for id: "+id;
		assertNotNull(message, clauses);
		assertEquals(message, 2, clauses.size());
		for (Clause clause : clauses) {
			Collection<QualifierValue> qualifierValues = clause.getQualifierValues();
			if (qualifierValues != null && !qualifierValues.isEmpty()) {
				fail("No qualifiers expected, but was: "+frame);
			}
		}
	}

}
