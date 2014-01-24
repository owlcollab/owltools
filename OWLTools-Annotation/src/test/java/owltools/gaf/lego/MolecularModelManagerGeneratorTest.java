package owltools.gaf.lego;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import owltools.io.ParserWrapper;

public class MolecularModelManagerGeneratorTest extends AbstractLegoModelGeneratorTest {
	private static Logger LOG = Logger.getLogger(MolecularModelManagerGeneratorTest.class);

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
		assertEquals("Expected 15 individuals from the minimal model generation.", 17, inds.size());

	
	}




}
