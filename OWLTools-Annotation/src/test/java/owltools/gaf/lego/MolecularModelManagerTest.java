package owltools.gaf.lego;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import owltools.gaf.lego.MolecularModelManager.OWLOperationResponse;
import owltools.io.ParserWrapper;
import owltools.util.MinimalModelGenerator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class MolecularModelManagerTest extends AbstractLegoModelGeneratorTest {
	private static Logger LOG = Logger.getLogger(MolecularModelManagerTest.class);

	MolecularModelManager mmm;

	static{
		Logger.getLogger("org.semanticweb.elk").setLevel(Level.ERROR);
		//Logger.getLogger(MinimalModelGenerator.class).setLevel(Level.ERROR);
		//Logger.getLogger(LegoModelGenerator.class).setLevel(Level.ERROR);
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
		assertEquals(17, inds.size()); // TODO checkme
		
		// GO:0001158 ! enhancer sequence-specific DNA binding
		OWLOperationResponse response = mmm.createIndividual(modelId, g.getOWLClassByIdentifier("GO:0001158"));
		String bindingId = response.getIndividualIds().get(0);
		LOG.info("New: "+bindingId);
		// GO:0005654 ! nucleoplasm
		mmm.addOccursIn(modelId, bindingId, "GO:0005654");

		mmm.addEnabledBy(modelId, bindingId, "PR:P123456");

		// todo - add a test that results in an inconsistency
		
		List<Map<Object, Object>> objs = mmm.getIndividualObjects(modelId);
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
	
		String js = gson.toJson(objs);
		LOG.info("INDS:" + js);
		
		LOG.info(mmm.generateDot(modelId));
//		LOG.info(mmm.generateImage(modelId)); // problematic due to missing dot application
		
	}

	/**
	 * The purpose of this test is to ensure that different models are properly insulated from each other.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSeedingMultipleModels() throws Exception {
		ParserWrapper pw = new ParserWrapper();

		g = pw.parseToOWLGraph(getResourceIRIString("go-mgi-signaling-test.obo"));

		mmm = new MolecularModelManager(g);
		//File gafPath = getResource("mgi-signaling.gaf");
		///mmm.loadGaf("mgi", gafPath);
		mmm.setPathToGafs(getResource("gene-associations").toString());

		OWLClass p = g.getOWLClassByIdentifier("GO:0014029"); // neural crest formation
		
		
		String model1Id = mmm.generateModel(p, "mgi");
		LOG.info("Model: "+model1Id);
		LegoModelGenerator model1 = mmm.getModel(model1Id);
		assertNotNull(model1);

		Set<OWLNamedIndividual> inds = mmm.getIndividuals(model1Id);
		LOG.info("Individuals: "+inds.size());
		for (OWLNamedIndividual i : inds) {
			LOG.info("I="+i);
		}
		assertEquals(17, inds.size());
		
		String model2Id = mmm.generateModel(p, "fake");
		LOG.info("Model: "+model2Id);
		LegoModelGenerator model2 = mmm.getModel(model2Id);
		assertNotNull(model2);

		Set<OWLNamedIndividual> inds2 = mmm.getIndividuals(model2Id);
		LOG.info("Individuals: "+inds2.size());
		for (OWLNamedIndividual i : inds2) {
			LOG.info("I="+i);
		}
		assertEquals(17, inds2.size());
		
		for (OWLNamedIndividual i : inds) {
			assertFalse(inds2.contains(i));
		}
		for (OWLNamedIndividual i : inds2) {
			assertFalse(inds.contains(i));
		}
		
//		// GO:0001158 ! enhancer sequence-specific DNA binding
//		OWLOperationResponse response = mmm.createIndividual(model1Id, g.getOWLClassByIdentifier("GO:0001158"));
//		String bindingId = response.getIndividualIds().get(0);
//		LOG.info("New: "+bindingId);
//		// GO:0005654 ! nucleoplasm
//		mmm.addOccursIn(model1Id, bindingId, "GO:0005654");
//
//		mmm.addEnabledBy(model1Id, bindingId, "PR:P123456");
//
//		// todo - add a test that results in an inconsistency
		
		List<Map<Object, Object>> objs = mmm.getIndividualObjects(model1Id);
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
	
		String js = gson.toJson(objs);
		LOG.info("INDS:" + js);
		
		
	}


}
