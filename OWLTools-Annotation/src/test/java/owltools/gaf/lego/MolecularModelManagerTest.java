package owltools.gaf.lego;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import owltools.gaf.lego.MolecularModelManager.UnknownIdentifierException;
import owltools.io.ParserWrapper;
import owltools.util.ModelContainer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@SuppressWarnings("deprecation")
public class MolecularModelManagerTest extends AbstractLegoModelGeneratorTest {
	private static Logger LOG = Logger.getLogger(MolecularModelManagerTest.class);

	// JUnit way of creating a temporary test folder
	// will be deleted after the test has run, by JUnit.
	@Rule
    public TemporaryFolder folder = new TemporaryFolder();
	
	MolecularModelManager<Void> mmm;

	static{
		Logger.getLogger("org.semanticweb.elk").setLevel(Level.ERROR);
		//Logger.getLogger(MinimalModelGenerator.class).setLevel(Level.ERROR);
		//Logger.getLogger(LegoModelGenerator.class).setLevel(Level.ERROR);
	}
	
	@Test
	public void testM3() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		//w = new FileWriter(new File("target/lego.out"));

		g = pw.parseToOWLGraph(getResourceIRIString("go-mgi-signaling-test.obo"));
		g.mergeOntology(pw.parseOBO(getResourceIRIString("disease.obo")));
		//g.m

		mmm = new MolecularModelManager<Void>(g);
		File gafPath = getResource("mgi-signaling.gaf");
		mmm.loadGaf("mgi", gafPath);

		OWLClass p = g.getOWLClassByIdentifier("GO:0014029"); // neural crest formation
		String modelId = mmm.generateModel(p, "mgi", null);
		LOG.info("Model: "+modelId);
		ModelContainer model = mmm.getModel(modelId);
		assertNotNull(model);

		Set<OWLNamedIndividual> inds = mmm.getIndividuals(modelId);
		LOG.info("Individuals: "+inds.size());
		for (OWLNamedIndividual i : inds) {
			LOG.info("I="+i);
		}
		assertTrue(inds.size() >= 16); // TODO checkme, warning this seems to be Java version specific

		// GO:0001158 ! enhancer sequence-specific DNA binding
		final String bindingId = mmm.createIndividual(modelId, "GO:0001158", null, null).getKey();
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
		assertTrue(inds.size() >= 4); // TODO checkme, warning this seems to be Java version specific

		q = "'part_of' some 'neural crest formation'";
		inds = mmm.getIndividualsByQuery(modelId, q);
		LOG.info(q + " #inds = "+inds.size());
		assertTrue(inds.size() >= 5); // TODO checkme, warning this seems to be Java version specific


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

		mmm = new MolecularModelManager<Void>(g);
		//File gafPath = getResource("mgi-signaling.gaf");
		///mmm.loadGaf("mgi", gafPath);
		mmm.setPathToGafs(getResource("gene-associations").toString());

		OWLClass p = g.getOWLClassByIdentifier("GO:0014029"); // neural crest formation


		String model1Id = mmm.generateModel(p, "mgi", null);
		LOG.info("Model: "+model1Id);
		ModelContainer model1 = mmm.getModel(model1Id);
		assertNotNull(model1);

		Set<OWLNamedIndividual> inds = mmm.getIndividuals(model1Id);
		LOG.info("Individuals: "+inds.size());
		for (OWLNamedIndividual i : inds) {
			LOG.info("I="+i);
		}
		assertTrue(inds.size() >= 12);

		String model2Id = mmm.generateModel(p, "fake", null);
		LOG.info("Model: "+model2Id);
		ModelContainer model2 = mmm.getModel(model2Id);
		assertNotNull(model2);

		Set<OWLNamedIndividual> inds2 = mmm.getIndividuals(model2Id);
		LOG.info("Individuals: "+inds2.size());
		for (OWLNamedIndividual i : inds2) {
			LOG.info("I="+i);
		}
		assertEquals(inds.size(), inds2.size());

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

	private String renderJSON(String modelId) throws UnknownIdentifierException {

		Map<Object, Object> obj = mmm.getModelObject(modelId);
		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		String js = gson.toJson(obj);
		return js;
	}

	@Test
	public void testDeleteIndividual() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		g = pw.parseToOWLGraph(getResourceIRIString("go-mgi-signaling-test.obo"));

		// GO:0038024 ! cargo receptor activity
		// GO:0042803 ! protein homodimerization activity

		mmm = new MolecularModelManager<Void>(g);

		String modelId = mmm.generateBlankModel(null, null);
		String i1 = mmm.createIndividual(modelId, "GO:0038024", null, null).getKey();

		String i2 = mmm.createIndividual(modelId, "GO:0042803", null, null).getKey();

		mmm.addPartOf(modelId, i1, i2, null);

		//		String js = renderJSON(modelId);
		//		System.out.println("-------------");
		//		System.out.println("INDS:" + js);
		//		
		//		System.out.println("-------------");

		mmm.deleteIndividual(modelId, i2, null);

		//		js = renderJSON(modelId);
		//		System.out.println("INDS:" + js);
		//		System.out.println("-------------");

		Set<OWLNamedIndividual> individuals = mmm.getIndividuals(modelId);
		assertEquals(1, individuals.size());
	}

	@Test
	public void testExportImport() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		g = pw.parseToOWLGraph(getResourceIRIString("go-mgi-signaling-test.obo"));

		// GO:0038024 ! cargo receptor activity
		// GO:0042803 ! protein homodimerization activity
		// GO:0008233 ! peptidase activity

		mmm = new MolecularModelManager<Void>(g);

		final String modelId = mmm.generateBlankModel(null, null);
		final Pair<String,OWLNamedIndividual> i1 = mmm.createIndividual(modelId, "GO:0038024", null, null);

		final Pair<String,OWLNamedIndividual> i2 = mmm.createIndividual(modelId, "GO:0042803", null, null);

		mmm.addPartOf(modelId, i1.getKey(), i2.getKey(), null);
		
		// export
		final String modelContent = mmm.exportModel(modelId);
		System.out.println("-------------------");
		System.out.println(modelContent);
		System.out.println("-------------------");
		
		// add an additional individual to model after export
		final Pair<String,OWLNamedIndividual> i3 = mmm.createIndividual(modelId, "GO:0008233", null, null);
		assertEquals(3, mmm.getIndividuals(modelId).size());

		
		// import
		final String modelId2 = mmm.importModel(modelContent);
		
		assertEquals(modelId, modelId2);
		Set<OWLNamedIndividual> loaded = mmm.getIndividuals(modelId2);
		assertEquals(2, loaded.size());
		for (OWLNamedIndividual i : loaded) {
			IRI iri = i.getIRI();
			// check that the model only contains the individuals created before the export
			assertTrue(iri.equals(i1.getRight().getIRI()) || iri.equals(i2.getRight().getIRI()));
			assertFalse(iri.equals(i3.getRight().getIRI()));
		}
	}
	
	@Test
	public void testSaveModel() throws Exception {
		final File saveFolder = folder.newFolder();
		final ParserWrapper pw1 = new ParserWrapper();
		g = pw1.parseToOWLGraph(getResourceIRIString("go-mgi-signaling-test.obo"));

		mmm = new MolecularModelManager<Void>(g);
		mmm.setPathToOWLFiles(saveFolder.getCanonicalPath());
		
		// GO:0038024 ! cargo receptor activity
		// GO:0042803 ! protein homodimerization activity
		// GO:0008233 ! peptidase activity

		final String modelId = mmm.generateBlankModel(null, null);
		final Pair<String,OWLNamedIndividual> i1 = mmm.createIndividual(modelId, "GO:0038024", null, null);

		final Pair<String,OWLNamedIndividual> i2 = mmm.createIndividual(modelId, "GO:0042803", null, null);

		mmm.addPartOf(modelId, i1.getKey(), i2.getKey(), null);
		
		// save
		mmm.saveModel(modelId, null, null);
		
		// add an additional individual to model after export
		final Pair<String,OWLNamedIndividual> i3 = mmm.createIndividual(modelId, "GO:0008233", null, null);
		assertEquals(3, mmm.getIndividuals(modelId).size());

		// discard mmm
		mmm.dispose();
		mmm = null;
		
		final ParserWrapper pw2 = new ParserWrapper();
		g = pw2.parseToOWLGraph(getResourceIRIString("go-mgi-signaling-test.obo"));
		
		
		mmm = new MolecularModelManager<Void>(g);
		mmm.setPathToOWLFiles(saveFolder.getCanonicalPath());
		
		Set<String> availableModelIds = mmm.getAvailableModelIds();
		assertTrue(availableModelIds.contains(modelId));
		
		final ModelContainer model = mmm.getModel(modelId);
		assertNotNull(model);
		
		Collection<OWLNamedIndividual> loaded = mmm.getIndividuals(modelId);
		assertEquals(2, loaded.size());
		for (OWLNamedIndividual i : loaded) {
			IRI iri = i.getIRI();
			// check that the model only contains the individuals created before the save
			assertTrue(iri.equals(i1.getRight().getIRI()) || iri.equals(i2.getRight().getIRI()));
			assertFalse(iri.equals(i3.getRight().getIRI()));
		}
	}

	@Test
	public void testInferredType() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		g = pw.parseToOWLGraph(getResourceIRIString("go-mgi-signaling-test.obo"));

		// GO:0038024 ! cargo receptor activity
		// GO:0042803 ! protein homodimerization activity

		mmm = new MolecularModelManager<Void>(g);

		String modelId = mmm.generateBlankModel(null, null);
		String cc = mmm.createIndividual(modelId, "GO:0004872", null, null).getKey(); // receptor activity

		
		String mit = mmm.createIndividual(modelId, "GO:0007166", null, null).getKey(); // cell surface receptor signaling pathway

		mmm.addPartOf(modelId, mit, cc, null);

		// we expect inference to be to: GO:0038023  signaling receptor activity
		// See discussion here: https://github.com/kltm/go-mme/issues/3

		System.out.println(renderJSON(modelId));
		//List<Map<Object, Object>> gson = mmm.getIndividualObjects(modelId);
		//assertEquals(1, individuals.size());
	}

}
