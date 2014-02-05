package owltools.gaf.lego;

import static org.junit.Assert.*;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import owltools.gaf.lego.MolecularModelManager.OWLOperationResponse;
import owltools.io.ParserWrapper;

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
		String bindingId = MolecularModelJsonRenderer.getId(response.getIndividuals().get(0), g);
		LOG.info("New: "+bindingId);
		// GO:0005654 ! nucleoplasm
		mmm.addOccursIn(modelId, bindingId, "GO:0005654");

		mmm.addEnabledBy(modelId, bindingId, "PR:P123456");

		// todo - add a test that results in an inconsistency

		List<Map<Object, Object>> objs = mmm.getIndividualObjects(modelId);
		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		String js = gson.toJson(objs);
		LOG.info("INDS:" + js);

		//		LOG.info(mmm.generateDot(modelId));
		//		LOG.info(mmm.generateImage(modelId)); // problematic due to missing dot application

		String q = "'molecular_function'";
		inds = mmm.getIndividualsByQuery(modelId, q);
		LOG.info(q + " #inds = "+inds.size());
		assertEquals(5, inds.size());

		q = "'part_of' some 'neural crest formation'";
		inds = mmm.getIndividualsByQuery(modelId, q);
		LOG.info(q + " #inds = "+inds.size());
		assertEquals(6, inds.size());


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

		String js = renderJSON(model1Id);
		LOG.info("INDS:" + js);


	}

	private String renderJSON(String modelId) {
		List<Map<Object, Object>> objs = mmm.getIndividualObjects(modelId);
		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		String js = gson.toJson(objs);
		return js;
	}

	@Test
	public void testDeleteIndividual() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		g = pw.parseToOWLGraph(getResourceIRIString("go-mgi-signaling-test.obo"));

		// GO:0038024 ! cargo receptor activity
		// GO:0042803 ! protein homodimerization activity

		mmm = new MolecularModelManager(g);

		String modelId = mmm.generateBlankModel(null);
		OWLOperationResponse resp = mmm.createIndividual(modelId, "GO:0038024");
		OWLNamedIndividual i1 = resp.getIndividuals().get(0);

		resp = mmm.createIndividual(modelId, "GO:0042803");
		OWLNamedIndividual i2 = resp.getIndividuals().get(0);

		mmm.addPartOf(modelId, i1, i2);

		//		String js = renderJSON(modelId);
		//		System.out.println("-------------");
		//		System.out.println("INDS:" + js);
		//		
		//		System.out.println("-------------");

		mmm.deleteIndividual(modelId, i2);

		//		js = renderJSON(modelId);
		//		System.out.println("INDS:" + js);
		//		System.out.println("-------------");

		Set<OWLNamedIndividual> individuals = mmm.getIndividuals(modelId);
		assertEquals(1, individuals.size());
	}

	@Ignore("Will not work until a bug in the OWL-API is fixed!")
	@Test
	public void testExportImport() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		g = pw.parseToOWLGraph(getResourceIRIString("go-mgi-signaling-test.obo"));

		// GO:0038024 ! cargo receptor activity
		// GO:0042803 ! protein homodimerization activity

		mmm = new MolecularModelManager(g);

		String modelId = mmm.generateBlankModel(null);
		OWLOperationResponse resp = mmm.createIndividual(modelId, "GO:0038024");
		OWLNamedIndividual i1 = resp.getIndividuals().get(0);

		resp = mmm.createIndividual(modelId, "GO:0042803");
		OWLNamedIndividual i2 = resp.getIndividuals().get(0);

		mmm.addPartOf(modelId, i1, i2);
		
		// export
		String modelContent = mmm.exportModel(modelId);
		System.out.println("-------------------");
		System.out.println(modelContent);
		System.out.println("-------------------");
		
		// import
		String modelId2 = mmm.importModel(modelContent);
		
		assertEquals(modelId, modelId2);
		assertEquals(2, mmm.getIndividuals(modelId2));
	}

	@Test
	public void testInferredType() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		g = pw.parseToOWLGraph(getResourceIRIString("go-mgi-signaling-test.obo"));

		// GO:0038024 ! cargo receptor activity
		// GO:0042803 ! protein homodimerization activity

		mmm = new MolecularModelManager(g);

		String modelId = mmm.generateBlankModel(null);
		OWLOperationResponse resp = mmm.createIndividual(modelId, "GO:0004872"); // receptor activity
		OWLNamedIndividual cc = resp.getIndividuals().get(0);

		
		resp = mmm.createIndividual(modelId, "GO:0007166"); // cell surface receptor signaling pathway
		OWLNamedIndividual mit = resp.getIndividuals().get(0);

		mmm.addPartOf(modelId, mit, cc);

		// we expect inference to be to: GO:0038023  signaling receptor activity
		// See discussion here: https://github.com/kltm/go-mme/issues/3

		System.out.println(renderJSON(modelId));
		//List<Map<Object, Object>> gson = mmm.getIndividualObjects(modelId);
		//assertEquals(1, individuals.size());
	}

}
