package owltools.mooncat;

import static org.junit.Assert.*;

import java.io.FileOutputStream;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.coode.owlapi.turtle.TurtleOntologyFormat;
import org.junit.Test;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLOntology;

import owltools.OWLToolsTestBasics;
import owltools.io.CatalogXmlIRIMapper;
import owltools.io.ParserWrapper;
import owltools.mooncat.ProvenanceReasonerWrapper.OWLEdge;

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
		
		// OWLEdge [c=<http://example.org/test/BCell-size>, p=<http://example.org/test/ImmuneCell-size>] REQUIRES: [http://example.org/test/auto, http://example.org/test/entity.owl]
		// OWLEdge [c=<http://example.org/test/Bone-size>, p=<http://example.org/test/Bone-morphology>] REQUIRES: [http://example.org/test/auto, http://example.org/test/quality.owl]
			
		assertEquals(143, pr.edges.size());
		o.getOWLOntologyManager().saveOntology(o,
				new FileOutputStream("target/eq-inf.ttl"));
	}



}
