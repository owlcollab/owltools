package owltools.gaf.lego;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import owltools.OWLToolsTestBasics;
import owltools.gaf.GafDocument;
import owltools.gaf.GafObjectsBuilder;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;
import owltools.util.MinimalModelGeneratorTest;
import owltools.vocab.OBOUpperVocabulary;

public class MolecularModelManagerTest extends AbstractLegoModelGeneratorTest {
	private static Logger LOG = Logger.getLogger(MolecularModelManagerTest.class);

	MolecularModelManager mmm;

	static{
		Logger.getLogger("org.semanticweb.elk").setLevel(Level.ERROR);
		//Logger.getLogger("org.semanticweb.elk.reasoner.indexing.hierarchy").setLevel(Level.ERROR);
	}

	@Test
	public void testM3() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		//w = new FileWriter(new File("target/lego.out"));

		g = pw.parseToOWLGraph(getResourceIRIString("go-mgi-signaling-test.obo"));
		g.mergeOntology(pw.parseOBO(getResourceIRIString("disease.obo")));
		//g.m

		mmm = new MolecularModelManager(g);
		File gafPath = getResource("mgi-signaling.gaf");
		mmm.loadGaf("mgi", gafPath);

		OWLClass p = g.getOWLClassByIdentifier("GO:0014029"); // neural crest formation
		String modelId = mmm.generateModel(p, "mgi");
		LOG.info("Model: "+modelId);
		LegoModelGenerator model = mmm.getModel(modelId);

		Set<OWLNamedIndividual> inds = mmm.getIndividuals(modelId);
		LOG.info("Individuals: "+inds.size());
		for (OWLNamedIndividual i : inds) {
			LOG.info("I="+i);
		}
		assertTrue(inds.size() == 15);
		
		// GO:0001158 ! enhancer sequence-specific DNA binding
		String bindingId = mmm.createActivityIndividual(modelId, g.getOWLClassByIdentifier("GO:0001158"));
		LOG.info("New: "+bindingId);
		// GO:0005654 ! nucleoplasm
		mmm.addOccursIn(modelId, bindingId, "GO:0005654");

		mmm.addEnabledBy(modelId, bindingId, "PR:P123456");

		// todo - add a test that results in an inconsistency
		
		List<Map> objs = mmm.getIndividualObjects(modelId);
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
	
		String js = gson.toJson(objs);
		LOG.info("INDS:" + js);
		
		LOG.info(mmm.generateDot(modelId));
//		LOG.info(mmm.generateImage(modelId)); // problematic due to missing dot application
		
	}




}
