package owltools.mooncat;

import static org.junit.Assert.*;

import java.io.FileOutputStream;

import org.junit.Test;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;
import org.semanticweb.owlapi.util.PriorityCollection;

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

	@Test
	public void testReason() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		CatalogXmlIRIMapper cm = new CatalogXmlIRIMapper(getResource("mooncat/eq-lattice/catalog-v001.xml"));
		PriorityCollection<OWLOntologyIRIMapper> im = pw.getManager().getIRIMappers();
		im.add(cm);

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
