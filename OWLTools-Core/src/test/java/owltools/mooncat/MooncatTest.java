package owltools.mooncat;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObject;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class MooncatTest extends OWLToolsTestBasics {
	
	private static boolean RENDER_ONTOLOGY_FLAG = false;

	@Test
	public void testMireot() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		
		// this test ontology has a class defined using a caro class, and imports caro_local
		OWLGraphWrapper g =
			pw.parseToOWLGraph(getResourceIRIString("mooncat_caro_test.obo"));
		Mooncat m = new Mooncat(g);
		m.addReferencedOntology(pw.parseOWL("http://purl.obolibrary.org/obo/caro.owl"));
		
		if (RENDER_ONTOLOGY_FLAG) {
			for (OWLEntity e : m.getExternalReferencedEntities()) {
				System.out.println("e="+e);
			}
			for (OWLObject e : m.getClosureOfExternalReferencedEntities()) {
				System.out.println("c="+e);
			}
			for (OWLAxiom ax : m.getClosureAxiomsOfExternalReferencedEntities()) {
				System.out.println("M_AX:"+ax);
			}
		}
		
		m.mergeOntologies();
		
		if (RENDER_ONTOLOGY_FLAG) {
			for (OWLAxiom ax : m.getOntology().getAxioms()) {
				System.out.println(ax);
			}
		}
	}
	
}
