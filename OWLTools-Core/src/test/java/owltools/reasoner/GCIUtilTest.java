package owltools.reasoner;

import static org.junit.Assert.*;

import java.util.Set;

import org.junit.Test;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;

/**
 * tests GCIUtil
 * 
 * @author cjm
 *
 */
public class GCIUtilTest extends AbstractReasonerTest {

	OWLGraphWrapper g;
	OWLPrettyPrinter pp;

	@Test
	public void testOrganismPair() throws Exception{
		g =  getOntologyWrapper("limb_gci.owl");
		ElkReasonerFactory rf = new ElkReasonerFactory();
		OWLReasoner r = rf.createReasoner(g.getSourceOntology());
		try {
			Set<OWLSubClassOfAxiom> axioms = GCIUtil.getSubClassOfSomeValuesFromAxioms(r);
			int n = 0;
			for (OWLSubClassOfAxiom axiom : axioms) {
				String c = ((OWLClass) axiom.getSubClass()).getIRI().toString();
				OWLObjectSomeValuesFrom svf = (OWLObjectSomeValuesFrom) axiom.getSuperClass();
				String rel = ((OWLObjectProperty) svf.getProperty()).getIRI().toString();
				String p = ((OWLClass) svf.getFiller()).getIRI().toString();
				String axstr = c + " " + rel + " " + p;
				System.out.println(axstr);
				if ("http://x.org/phalanx-development http://x.org/part-of http://x.org/digit-development".equals(axstr)) {
					n |= 1;
				}
				if ("http://x.org/digit-development http://x.org/part-of http://x.org/autopod-development".equals(axstr)) {
					n |= 2;
				}
				if ("http://x.org/limb-development http://x.org/part-of http://x.org/organism-development".equals(axstr)) {
					n |= 4;
				}
				if ("http://x.org/autopod-development http://x.org/part-of http://x.org/limb-development".equals(axstr)) {
					n |= 8;
				}
			}
			assertEquals(4, axioms.size());
			assertEquals(15, n);
		}
		finally {
			r.dispose();
		}

	}
}
