package owltools.gaf.lego;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;

import owltools.gaf.GafDocument;
import owltools.gaf.parser.GafObjectsBuilder;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;
import owltools.util.ModelContainer;

public class PhenoLegoModelGeneratorTest extends AbstractLegoModelGeneratorTest {
	private static Logger LOG = Logger.getLogger(PhenoLegoModelGeneratorTest.class);

	static{
		Logger.getLogger("org.semanticweb.elk").setLevel(Level.ERROR);
		//Logger.getLogger("org.semanticweb.elk.reasoner.indexing.hierarchy").setLevel(Level.ERROR);
	}

	@Test
	public void testMgi() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		FileUtils.forceMkdir(new File("target/lego"));
		w = new FileWriter(new File("target/lego.out"));

		OWLGraphWrapper g = pw.parseToOWLGraph(getResourceIRIString("go-mgi-signaling-test.obo"));
		g.mergeOntology(pw.parseOBO(getResourceIRIString("disease.obo")));
		//g.m

		GafObjectsBuilder builder = new GafObjectsBuilder();
		GafDocument gafdoc = builder.buildDocument("src/test/resources/mgi-signaling.gaf");

		System.out.println("gMGR = "+pw.getManager());
		model = new ModelContainer(g.getSourceOntology(), new ElkReasonerFactory());
		ni = new LegoModelGenerator(model);
		ni.initialize(gafdoc, g);
		//ni.getOWLOntologyManager().removeOntology(ni.getAboxOntology());

		int aboxImportsSize = model.getAboxOntology().getImportsClosure().size();
		int qboxImportsSize = model.getQueryOntology().getImportsClosure().size();

		LOG.info("Abox ontology imports: "+aboxImportsSize);
		LOG.info("Q ontology imports: "+qboxImportsSize);
		assertEquals(2, aboxImportsSize);
		assertEquals(3, qboxImportsSize);

		LOG.info("#process classes in test = "+ni.processClassSet.size());
		//assertEquals(37, ni.processClassSet.size());
		
		
		for (OWLClass p : model.getTboxOntology().getClassesInSignature(true)) {
			if (g.getIdentifier(p) != null && !g.getIdentifier(p).equals("DOID:14330"))
				continue;			
			int nSups = model.getReasoner().getSuperClasses(p, false).getFlattened().size();
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

			ni.extractModule();
			OWLOntology ont = model.getAboxOntology();
			String pid = g.getIdentifier(p);
			String fn = pid.replaceAll(":", "_") + ".owl";
			FileOutputStream os = new FileOutputStream(new File("target/lego/"+fn));
			ont.getOWLOntologyManager().saveOntology(ont, os);
			ont.getOWLOntologyManager().removeOntology(ont);
		}
		FileOutputStream os = new FileOutputStream(new File("target/qont.owl"));
		model.getQueryOntology().getOWLOntologyManager().saveOntology(model.getQueryOntology(), os);

		w.close();

		LOG.info("Num generated individuals = "+ni.getGeneratedIndividuals().size());
		//assertEquals(7, ni.getGeneratedIndividuals().size());
		LOG.info("Score = "+ni.ccp);



	}



}
