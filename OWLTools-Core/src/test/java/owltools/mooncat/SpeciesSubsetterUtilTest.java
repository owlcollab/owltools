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
import org.semanticweb.owlapi.model.OWLClass;
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

public class SpeciesSubsetterUtilTest extends OWLToolsTestBasics {

	static boolean renderObo = true;
	
	@Test
	public void testSubsetterSpecies() throws Exception {
		ParserWrapper p = new ParserWrapper();
		p.setCheckOboDoc(false);
		OWLOntology owlOntology = p.parse(getResourceIRIString("speciesMergeTest.obo"));
		OWLGraphWrapper graph = new OWLGraphWrapper(owlOntology);
		OWLReasonerFactory rf = new ElkReasonerFactory();
		OWLReasoner reasoner = rf.createReasoner(graph.getSourceOntology());
		SpeciesSubsetterUtil smu = new SpeciesSubsetterUtil(graph);
		//smu.viewProperty = graph.getOWLObjectPropertyByIdentifier("BFO:0000050");
		smu.taxClass = graph.getOWLClassByIdentifier("T:1");
		smu.reasoner = reasoner;
		smu.removeOtherSpecies();
		
		p.saveOWL(smu.ont, new OBOOntologyFormat(), "target/speciesSubset.obo", graph);
		//p.saveOWL(smu.ont,  getResourceIRIString("target/speciesSubset.owl"), graph);
		
		OWLClass check = graph.getOWLClassByIdentifier("U:24");
		assertTrue(check == null);
	}

}
