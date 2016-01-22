package owltools.mooncat;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.formats.OBODocumentFormat;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class SpeciesMergeUtilTest extends OWLToolsTestBasics {

	private Logger LOG = Logger.getLogger(SpeciesMergeUtilTest.class);

	static boolean renderObo = true;
	
	@Test
	public void testMergeSpecies() throws Exception {
		ParserWrapper p = new ParserWrapper();
		OWLOntology owlOntology = p.parse(getResourceIRIString("speciesMergeTest.obo"));
		OWLGraphWrapper graph = new OWLGraphWrapper(owlOntology);
		OWLReasonerFactory rf = new ElkReasonerFactory();
		OWLReasoner reasoner = rf.createReasoner(graph.getSourceOntology());
		SpeciesMergeUtil smu = new SpeciesMergeUtil(graph);
		smu.viewProperty = graph.getOWLObjectPropertyByIdentifier("BFO:0000050");
		smu.taxClass = graph.getOWLClassByIdentifier("T:1");
		smu.reasoner = reasoner;
		smu.suffix = "coelocanth";
		smu.merge();
		
		p.saveOWL(smu.ont, new OBODocumentFormat(), "target/speciesMergeOut.obo");
		
	}
	
	@Test
	public void testMergeFly() throws Exception {
		ParserWrapper p = new ParserWrapper();
		OWLOntology owlOntology = p.parse(getResourceIRIString("interneuron-fly.obo"));
		Set<OWLClass> clsBefore = owlOntology.getClassesInSignature();
		OWLGraphWrapper graph = new OWLGraphWrapper(owlOntology);
		OWLReasonerFactory rf = new ElkReasonerFactory();
		OWLReasoner reasoner = rf.createReasoner(graph.getSourceOntology());
		SpeciesMergeUtil smu = new SpeciesMergeUtil(graph);

		smu.viewProperty = graph.getOWLObjectPropertyByIdentifier("BFO:0000050");
		smu.taxClass = graph.getOWLClassByIdentifier("NCBITaxon:7227");
		smu.reasoner = reasoner;
		smu.suffix = "fly";
		smu.includedProperties = Collections.singleton(smu.viewProperty);
		//smu.includedProperties = Collections.singleton(graph.getOWLObjectPropertyByIdentifier("BFO:0000051"));
		smu.merge();
		
		Set<OWLClass> clsAfter = smu.ont.getClassesInSignature();
		
		LOG.info("Before: "+clsBefore.size());
		LOG.info("After: "+clsAfter.size());
		assertEquals(100, clsBefore.size());
		assertEquals(90, clsAfter.size());
		p.saveOWL(smu.ont, new OBODocumentFormat(), "target/flyMergeOut.obo");
	
	}


}
