package owltools.mooncat;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.List;

import org.coode.owlapi.obo.parser.OBOOntologyFormat;
import org.junit.Test;
import org.obolibrary.obo2owl.Owl2Obo;
import org.obolibrary.oboformat.model.Clause;
import org.obolibrary.oboformat.model.Frame;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.parser.OBOFormatConstants.OboFormatTag;
import org.obolibrary.oboformat.writer.OBOFormatWriter;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.RemoveAxiom;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import owltools.InferenceBuilder;
import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;
import owltools.mooncat.SpeciesMergeUtil;

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
