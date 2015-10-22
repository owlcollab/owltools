package owltools.mooncat;

import java.io.FileOutputStream;

import org.apache.log4j.Logger;
import org.coode.owlapi.turtle.TurtleOntologyFormat;
import org.junit.Test;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLOntology;

import owltools.OWLToolsTestBasics;
import owltools.io.CatalogXmlIRIMapper;
import owltools.io.ParserWrapper;

/**
 * This is the main test class for PropertyViewOntologyBuilder
 * 
 * @author cjm
 *
 */
public class ProvenanceReasonerWrapperTest extends OWLToolsTestBasics {

	
	private Logger LOG = Logger.getLogger(ProvenanceReasonerWrapperTest.class);

	
	@Test
	public void testReason() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		pw.getManager().addIRIMapper(new CatalogXmlIRIMapper(getResource("mooncat/eq-lattice/catalog-v001.xml")));

		OWLOntology o = pw.parseOWL(getResourceIRIString("mooncat/eq-lattice/eq-with-imports.owl"));
		
		ProvenanceReasonerWrapper pr = new ProvenanceReasonerWrapper(o, new ElkReasonerFactory());
		pr.reason();
		o.getOWLOntologyManager().saveOntology(o,
				new FileOutputStream("target/eq-inf.ttl"));
	}



}
