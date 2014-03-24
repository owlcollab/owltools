package owltools.gaf.lego;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import owltools.OWLToolsTestBasics;
import owltools.gaf.GafDocument;
import owltools.gaf.parser.GafObjectsBuilder;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;
import owltools.mooncat.PropertyViewOntologyBuilder;
import owltools.util.MinimalModelGeneratorTest;
import owltools.vocab.OBOUpperVocabulary;

public class MGILegoModelTranslatorTest extends AbstractLegoModelGeneratorTest {
	private static Logger LOG = Logger.getLogger(MGILegoModelTranslatorTest.class);


	static{
		Logger.getLogger("org.semanticweb.elk").setLevel(Level.ERROR);
		//Logger.getLogger("org.semanticweb.elk.reasoner.indexing.hierarchy").setLevel(Level.ERROR);
	}

	@Test
	public void testMgi() throws Exception {
		ParserWrapper pw = new ParserWrapper();

		g = pw.parseToOWLGraph(getResourceIRIString("go-mgi-signaling-test.obo"));
		g.mergeOntology(pw.parseOBO(getResourceIRIString("disease.obo")));
		//g.m

		parseGAF("mgi-signaling.gaf");

		GafToLegoTranslator translator = new GafToLegoTranslator(g, null);
		OWLOntology ont = translator.minimizedTranslate(gafdoc);
		FileOutputStream os = new FileOutputStream(new File("target/mgi-lego-tr.owl"));
		ont.getOWLOntologyManager().saveOntology(ont, os);
		os.close();
	}

	@Test
	public void testMgiWithView() throws Exception {
		ParserWrapper pw = new ParserWrapper();

		g = pw.parseToOWLGraph(getResourceIRIString("go-mgi-signaling-test.obo"));
		g.mergeOntology(pw.parseOBO(getResourceIRIString("disease.obo")));
		g.mergeOntology(pw.parseOWL(IRI.create("http://purl.obolibrary.org/obo/ro.owl")));
		//g.m

		parseGAF("mgi-signaling.gaf");

		GafToLegoTranslator translator = new GafToLegoTranslator(g, null);
		OWLOntology ont = translator.minimizedTranslate(gafdoc);
		g.setSourceOntology(ont);
		OWLObjectProperty vp;
		vp = OBOUpperVocabulary.RO_involved_in.getObjectProperty(ont);
		PropertyViewOntologyBuilder pvob = 
				new PropertyViewOntologyBuilder(ont, vp);
		pvob.setInferredViewOntology(ont);
		
		OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();
		pvob.build(reasonerFactory);
		
		g.mergeOntology(pvob.getInferredViewOntology());
		
		FileOutputStream os = new FileOutputStream(new File("target/mgi-lego-tr2.owl"));
		ont.getOWLOntologyManager().saveOntology(g.getSourceOntology(), os);
		os.close();
	}


}
