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
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import owltools.OWLToolsTestBasics;
import owltools.gaf.GafDocument;
import owltools.gaf.GafObjectsBuilder;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;
import owltools.util.MinimalModelGeneratorTest;
import owltools.vocab.OBOUpperVocabulary;

public class MGILegoModelGeneratorTest extends AbstractLegoModelGeneratorTest {
	private static Logger LOG = Logger.getLogger(MGILegoModelGeneratorTest.class);


	static{
		Logger.getLogger("org.semanticweb.elk").setLevel(Level.ERROR);
		//Logger.getLogger("org.semanticweb.elk.reasoner.indexing.hierarchy").setLevel(Level.ERROR);
	}

	@Test
	public void testMgi() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		w = new FileWriter(new File("target/lego.out"));

		g = pw.parseToOWLGraph(getResourceIRIString("go-mgi-signaling-test.obo"));
		g.mergeOntology(pw.parseOBO(getResourceIRIString("disease.obo")));
		//g.m

		parseGAF("mgi-signaling.gaf");

		System.out.println("gMGR = "+pw.getManager());
		ni = new LegoModelGenerator(g.getSourceOntology(), new ElkReasonerFactory());
		ni.initialize(gafdoc, g);

		mmg = ni;

		int aboxImportsSize = mmg.getAboxOntology().getImportsClosure().size();
		int qboxImportsSize = mmg.getQueryOntology().getImportsClosure().size();

		LOG.info("Abox ontology imports: "+aboxImportsSize);
		LOG.info("Q ontology imports: "+qboxImportsSize);
		assertEquals(2, aboxImportsSize);
		assertEquals(3, qboxImportsSize);

		LOG.info("#process classes in test = "+ni.processClassSet.size());

		OWLClass p = g.getOWLClassByIdentifier("GO:0014029"); // neural crest formation
		int nSups = ni.getReasoner().getSuperClasses(p, false).getFlattened().size();
		LOG.info("supers(p) = "+nSups);
		//assertEquals(22, nSups);

		//ni = new LegoGenerator(g.getSourceOntology(), new ElkReasonerFactory());
		//ni.initialize(gafdoc, g);

		Set<String> seedGenes = ni.getGenes(p);

		LOG.info("\n\nP="+render(p));
		ni.buildNetwork(p, seedGenes);

		Map<String, Object> stats = ni.getGraphStatistics();
		for (String k : stats.keySet()) {
			writeln("# "+k+" = "+stats.get(k));
		}

		for (String gene : seedGenes) {
			writeln("  SEED="+render(gene));
		}

		this.expectedOPAs("OPA", null);
		
		this.expectedIndividiuals(getClass(OBOUpperVocabulary.GO_molecular_function), 4); // TODO - checkme
		this.expectedIndividiuals(getClass(OBOUpperVocabulary.GO_biological_process), 14); // TODO - checkme
		
		ni.extractModule();
		saveByClass(p);

		FileOutputStream os = new FileOutputStream(new File("target/qont.owl"));
		ni.getQueryOntology().getOWLOntologyManager().saveOntology(ni.getQueryOntology(), os);

		w.close();

		LOG.info("Num generated individuals = "+ni.getGeneratedIndividuals().size());
		//assertEquals(13, ni.getGeneratedIndividuals().size());
		LOG.info("Score = "+ni.ccp);
		assertTrue(ni.ccp < 0.25);

	}




}
