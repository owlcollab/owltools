package owltools.solrj;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.solr.common.SolrInputDocument;
import org.junit.Test;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.gaf.EcoTools;
import owltools.gaf.GafDocument;
import owltools.gaf.TaxonTools;
import owltools.gaf.parser.GafObjectsBuilder;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

@SuppressWarnings("deprecation")
public class BioentityClosureTest {

	@Test
	public void testNot() throws Exception {
		GafObjectsBuilder b = new GafObjectsBuilder();
		final GafDocument gaf = b.buildDocument("src/test/resources/not_bioentity_closure/mini-test.gaf");
		
		ParserWrapper pw = new ParserWrapper();
		OWLOntology testOwl = pw.parse(new File("src/test/resources/not_bioentity_closure/mini-test.obo").getCanonicalPath());
		final OWLGraphWrapper g = new OWLGraphWrapper(testOwl);
		
		OWLOntology slimOWL = pw.parse(TaxonTools.TAXON_PURL);
		ElkReasonerFactory rf = new ElkReasonerFactory();
		
		final List<SolrInputDocument> result = new ArrayList<SolrInputDocument>();
		GafSolrDocumentLoader l = new GafSolrDocumentLoader(null, 100) {

			@Override
			protected void add(SolrInputDocument doc) {
				result.add(doc);
			}
		};
		
		OWLReasoner r = null;
		try {
			r = rf.createReasoner(slimOWL);

			l.setGafDocument(gaf);
			l.setGraph(g);
			l.setEcoTools(new EcoTools(pw));
			l.setTaxonTools(new TaxonTools(r, true));
			l.load();
		}
		finally {
			if (r != null) {
				r.dispose();
			}
		}
		
		assertFalse(result.isEmpty());
		
		// find the bioentity documents
		boolean foundNormal = false;
		boolean foundNot = false;
		
		for (SolrInputDocument doc : result) {
			String category = (String) doc.getFieldValue("document_category");
			if ("bioentity".equals(category)) {
				if ("FOO:001".equals(doc.getFieldValue("id"))) {
					foundNormal = true;
					// check closure
					Collection<Object> values = doc.getFieldValues("isa_partof_closure");
					assertEquals(2, values.size());
					assertTrue(values.contains("B:2"));
					assertTrue(values.contains("GO:0005575"));
				}
				if ("FOO:002".equals(doc.getFieldValue("id"))) {
					foundNot = true;
					// check that closure is empty due to NOT annotation
					assertNull(doc.getFieldValue("isa_partof_closure"));
					assertNull(doc.getFieldValue("isa_partof_closure_label"));
					assertNull(doc.getFieldValue("isa_partof_closure_map"));
					assertNull(doc.getFieldValue("regulates_closure"));
					assertNull(doc.getFieldValue("regulates_closure_label"));
					assertNull(doc.getFieldValue("regulates_closure_map"));
					assertNull(doc.getFieldValue("annotation_class_list"));
					assertNull(doc.getFieldValue("annotation_class_list_label"));
				}
			}
		}
		assertTrue(foundNormal);
		assertTrue(foundNot);
	}
}
