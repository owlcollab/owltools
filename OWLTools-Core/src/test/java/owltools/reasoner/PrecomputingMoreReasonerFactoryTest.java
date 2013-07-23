package owltools.reasoner;

import static org.junit.Assert.*;

import java.util.Set;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.OWLToolsTestBasics;
import owltools.io.ParserWrapper;

public class PrecomputingMoreReasonerFactoryTest extends OWLToolsTestBasics {

	@Test
	public void test() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		OWLOntology o = pw.parse(getResourceIRIString("wine.owl"));
		
		PrecomputingMoreReasonerFactory factory = PrecomputingMoreReasonerFactory.getMoreHermitFactory();
		OWLReasoner reasoner = factory.createReasoner(o);
		
		Set<OWLClass> unsatisfiable = reasoner.getUnsatisfiableClasses().getEntitiesMinusBottom();
		assertEquals(0, unsatisfiable.size());
		
	}

}
