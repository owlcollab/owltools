package owltools.mooncat;

import static org.junit.Assert.*;

import java.util.Collection;

import org.junit.Test;
import org.obolibrary.obo2owl.OWLAPIOwl2Obo;
import org.obolibrary.oboformat.model.Clause;
import org.obolibrary.oboformat.model.Frame;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.parser.OBOFormatConstants.OboFormatTag;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.io.OWLOntologyDocumentTarget;
import org.semanticweb.owlapi.io.SystemOutDocumentTarget;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class RemoveDirectivesTest extends OWLToolsTestBasics {

	// set this to true for debugging the ontology content
	public static boolean USE_SYSTEM_OUT = false;
	
	@Test
	public void testRemove() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		OWLGraphWrapper g = pw.parseToOWLGraph(getResourceIRIString("mooncat/remove-directives-test1.obo"));
		
		System.out.println(g.getSourceOntology().getAxioms(AxiomType.EQUIVALENT_CLASSES));
		OWLOntology secondary = pw.parse(getResourceIRIString("mooncat/remove-directives-test2.obo"));
        System.out.println(secondary.getAxioms(AxiomType.EQUIVALENT_CLASSES));
		g.addSupportOntology(secondary);
		
		Mooncat mooncat = new Mooncat(g);
		
		//mooncat.mergeOntologies();
		g.mergeOntology(secondary);
		
		OWLOntology merged = g.getSourceOntology();

		System.out.println(merged.getAxioms(AxiomType.EQUIVALENT_CLASSES));

		OWLAPIOwl2Obo owl2Obo = new OWLAPIOwl2Obo(merged.getOWLOntologyManager());
		OBODoc mergedObo = owl2Obo.convert(merged);
		
		if (USE_SYSTEM_OUT) {
			System.out.println("------------------------");
			OWLOntologyManager manager = merged.getOWLOntologyManager();
			OWLOntologyDocumentTarget documentTarget = new SystemOutDocumentTarget();
			manager.saveOntology(merged, new RDFXMLDocumentFormat(), documentTarget);
			System.out.println("------------------------");
			String oboString = renderOBOtoString(mergedObo);
			System.out.println(oboString);
			System.out.println("------------------------");
		}
		
		Frame headerFrame = mergedObo.getHeaderFrame();
		String owlAxiomString = headerFrame.getTagValue(OboFormatTag.TAG_OWL_AXIOMS, String.class);
		assertNotNull(owlAxiomString);
		
		
		Frame frame = mergedObo.getTermFrame("X:1");
		Collection<Clause> clauses = frame.getClauses(OboFormatTag.TAG_INTERSECTION_OF);
		assertEquals(2, clauses.size());
	}
}
