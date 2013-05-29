package owltools.mooncat;

import org.coode.owlapi.obo.parser.OBOOntologyFormat;
import org.junit.Test;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class SpeciesMergeUtilTest extends OWLToolsTestBasics {

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
		
		p.saveOWL(smu.ont, new OBOOntologyFormat(), "target/speciesMergeOut.obo", graph);
		
	}
	
	@Test
	public void testMergeFly() throws Exception {
		ParserWrapper p = new ParserWrapper();
		OWLOntology owlOntology = p.parse(getResourceIRIString("interneuron-fly.obo"));
		OWLGraphWrapper graph = new OWLGraphWrapper(owlOntology);
		OWLReasonerFactory rf = new ElkReasonerFactory();
		OWLReasoner reasoner = rf.createReasoner(graph.getSourceOntology());
		SpeciesMergeUtil smu = new SpeciesMergeUtil(graph);

		smu.viewProperty = graph.getOWLObjectPropertyByIdentifier("BFO:0000050");
		smu.taxClass = graph.getOWLClassByIdentifier("NCBITaxon:7227");
		smu.reasoner = reasoner;
		smu.suffix = "fly";
		smu.merge();
		
		p.saveOWL(smu.ont, new OBOOntologyFormat(), "target/flyMergeOut.obo", graph);
		
	}


}
