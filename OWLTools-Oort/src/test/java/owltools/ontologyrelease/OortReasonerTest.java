package owltools.ontologyrelease;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.obolibrary.obo2owl.OWLAPIOwl2Obo;
import org.obolibrary.oboformat.model.Clause;
import org.obolibrary.oboformat.model.Frame;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.parser.OBOFormatConstants.OboFormatTag;
import org.obolibrary.oboformat.writer.OBOFormatWriter;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.RemoveAxiom;

import owltools.InferenceBuilder;
import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class OortReasonerTest extends OWLToolsTestBasics {

	static boolean renderObo = true;
	
	/**
	 * Test in response to bug report 
	 * https://code.google.com/p/owltools/issues/detail?id=2
	 * 
	 * @throws Exception 
	 */
	@Test
	public void testIssueTwo() throws Exception {
		ParserWrapper p = new ParserWrapper();
		OWLOntology owlOntology = p.parse(getResourceIRIString("foo.obo"));
		OWLGraphWrapper graph = new OWLGraphWrapper(owlOntology);
		InferenceBuilder b = new InferenceBuilder(graph, "hermit");
		try {
			List<OWLAxiom> axioms = b.buildInferences();
			for(OWLAxiom ax: axioms) {
				if (ax instanceof OWLSubClassOfAxiom && 
						((OWLSubClassOfAxiom)ax).getSuperClass().isOWLThing()) {
					continue;
				}
				graph.getManager().applyChange(new AddAxiom(owlOntology, ax));
			}
			for(OWLAxiom ax : b.getRedundantAxioms()) {
				graph.getManager().applyChange(new RemoveAxiom(owlOntology, ax));					
			}
			OWLAPIOwl2Obo owl2Obo = new  OWLAPIOwl2Obo(OWLManager.createOWLOntologyManager() );
			OBODoc oboDoc = owl2Obo.convert(owlOntology);

			if (renderObo) {
				OBOFormatWriter w = new OBOFormatWriter();
				StringWriter s = new StringWriter();
				BufferedWriter writer = new BufferedWriter(s);
				w.write(oboDoc, writer);
				writer.close();
				System.out.println(s.getBuffer().toString());
			}
			Frame termFrame = oboDoc.getTermFrame("test:2234");
			Collection<Clause> intersections = termFrame.getClauses(OboFormatTag.TAG_INTERSECTION_OF);
			// check that the intersection is preserved
			assertEquals(2, intersections.size());
			boolean found1 = false;
			boolean found2 = false;
			for (Clause clause : intersections) {
				Collection<Object> values = clause.getValues();
				if (values.size() == 1) {
					found1 = "test:1234".equals(clause.getValue());
				}
				else if (values.size() == 2) {
					found2 = "rtest:1234".equals(clause.getValue()) && "test:3234".equals(clause.getValue2());
				}
			}
			assertTrue("IS_A part of the intersection not found", found1);
			assertTrue("RELATIONSHIP part of intersection not found", found2);

			// also check that the base relations are added
			// is_a
			Collection<Clause> isas = termFrame.getClauses(OboFormatTag.TAG_IS_A);
			assertEquals(1, isas.size());
			assertEquals("test:1234", isas.iterator().next().getValue());

			// relationship
			Collection<Clause> rels = termFrame.getClauses(OboFormatTag.TAG_RELATIONSHIP);
			assertEquals(1, rels.size());
			Clause rel = rels.iterator().next();
			assertEquals("rtest:1234", rel.getValue());
			assertEquals("test:3234", rel.getValue2());
		}
		finally {
			b.dispose();
		}
		
	}

}
