package owltools.gaf.lego;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import owltools.gaf.GafDocument;
import owltools.gaf.GafObjectsBuilder;
import owltools.graph.OWLGraphWrapper;
import owltools.io.CatalogXmlIRIMapper;
import owltools.vocab.OBOUpperVocabulary;

public class PombaseLegoModelGeneratorTest extends AbstractLegoModelGeneratorTest {
	private static Logger LOG = Logger.getLogger(PombaseLegoModelGeneratorTest.class);

	static{
		Logger.getLogger("org.semanticweb.elk").setLevel(Level.ERROR);
		//Logger.getLogger("org.semanticweb.elk.reasoner.indexing.hierarchy").setLevel(Level.ERROR);
	}

	@Test
	@Ignore("Ignore for now until fixed")
	public void testPombeImports() throws Exception {
		OWLOntologyManager m = OWLManager.createOWLOntologyManager();
		
		OWLOntology sourceOntology = 
				m.loadOntologyFromOntologyDocument(getResource("go-iron-transport-subset.owl"));
		
		OWLOntology modelOntology = m.createOntology(IRI.create("foo/bar"));
		createImports(modelOntology,
			sourceOntology.getOntologyID().getOntologyIRI(),
			IRI.create("http://purl.obolibrary.org/obo/ro.owl"),
			IRI.create("http://purl.obolibrary.org/obo/go/extensions/ro_pending.owl"));
		
		g = new OWLGraphWrapper(sourceOntology);
		OWLClass p = g.getOWLClassByIdentifier("GO:0033215"); // iron
		assertNotNull(p);
		
		LegoModelGenerator molecularModelGenerator = new LegoModelGenerator(modelOntology, new ElkReasonerFactory());
		
		molecularModelGenerator.setPrecomputePropertyClassCombinations(false);
		Set<String> seedGenes = new HashSet<String>();
		
		parseGAF("pombase-test.gaf");
		GafObjectsBuilder builder = new GafObjectsBuilder();
		GafDocument ppidoc = builder.buildDocument(getResource("pombase-test-ppi.gaf"));
		gafdoc.getGeneAnnotations().addAll(ppidoc.getGeneAnnotations());
		
		molecularModelGenerator.initialize(gafdoc, g);
		seedGenes.addAll(molecularModelGenerator.getGenes(p));
		molecularModelGenerator.setContextualizingSuffix("test");
		
		
		molecularModelGenerator.buildNetwork(p, seedGenes);
		
		Collection<OWLNamedIndividual> individuals = molecularModelGenerator.getGeneratedIndividuals();
		System.out.println("constructed imports, individual count: "+individuals.size());
		for (OWLNamedIndividual i : individuals) {
			System.out.println(i.getIRI().toString());
		}
		assertEquals(7, individuals.size());
	}
	
	private void createImports(OWLOntology ont, IRI...imports) throws OWLOntologyCreationException {
		OWLOntologyManager m = ont.getOWLOntologyManager();
		OWLDataFactory f = m.getOWLDataFactory();
		List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
		for (IRI importIRI : imports) {
			OWLImportsDeclaration importDeclaration = f.getOWLImportsDeclaration(importIRI);
			m.loadOntology(importIRI);
			changes.add(new AddImport(ont, importDeclaration));
		}
		m.applyChanges(changes);
	}
	
	@Test
	public void testPombe() throws Exception {
		OWLOntologyManager m = OWLManager.createOWLOntologyManager();
		m.addIRIMapper(new CatalogXmlIRIMapper(getResource("catalog-v001.xml")));
		OWLOntology tbox = 
				m.loadOntologyFromOntologyDocument(getResource("go-iron-transport-subset-importer.owl"));
		//g = pw.parseToOWLGraph(getResourceIRIString("go-iron-transport-subset-importer.owl"));
		g = new OWLGraphWrapper(tbox);
		
		//ParserWrapper pw = new ParserWrapper();
		FileUtils.forceMkdir(new File("target/lego"));
		w = new FileWriter(new File("target/lego.out"));


		parseGAF("pombase-test.gaf");
		GafObjectsBuilder builder = new GafObjectsBuilder();
		GafDocument ppidoc = builder.buildDocument(getResource("pombase-test-ppi.gaf"));
		gafdoc.getGeneAnnotations().addAll(ppidoc.getGeneAnnotations());

		//System.out.println("gMGR = "+pw.getManager());

		ni = new LegoModelGenerator(g.getSourceOntology(), new ElkReasonerFactory());
		ni.initialize(gafdoc, g);
		//ni.getOWLOntologyManager().removeOntology(ni.getAboxOntology());

		mmg = ni;
		int aboxImportsSize = mmg.getAboxOntology().getImportsClosure().size();
		int qboxImportsSize = mmg.getQueryOntology().getImportsClosure().size();

		LOG.info("Abox ontology imports: "+aboxImportsSize);
		LOG.info("Q ontology imports: "+qboxImportsSize);
		//assertEquals(2, aboxImportsSize);
		//assertEquals(3, qboxImportsSize);


		OWLClass p = g.getOWLClassByIdentifier("GO:0033215"); // iron

		int nSups = ni.getReasoner().getSuperClasses(p, false).getFlattened().size();
		LOG.info("supers(p) = "+nSups);
		assertEquals(22, nSups);

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
		
		this.expectedIndividiuals(getClass(OBOUpperVocabulary.GO_molecular_function), 3);
		this.expectedIndividiuals(getClass(OBOUpperVocabulary.GO_biological_process), 4);


		ni.extractModule();
		saveByClass(p);
		
		OWLObjectProperty ENABLED_BY = 
				OBOUpperVocabulary.GOREL_enabled_by.getObjectProperty(m.getOWLDataFactory());
		
		Set<OWLNamedIndividual> mfinds =
				mmg.getReasoner().getInstances(getClass(OBOUpperVocabulary.GO_molecular_function), 
						false).getFlattened();
		int nGPs = 0;
		for (OWLNamedIndividual i : mfinds) {
			for (OWLClassExpression cx : i.getTypes(ni.getAboxOntology())) {
				if (cx instanceof OWLObjectSomeValuesFrom) {
					OWLObjectSomeValuesFrom svf = (OWLObjectSomeValuesFrom)cx;
					if (svf.getProperty().equals(ENABLED_BY)) {
						//System.out.println("MF "+i+" ==> "+cx);
						nGPs++;
					}
					
				}
			}
		}
		assertEquals(3, nGPs);
		
		//			OWLOntology ont = ni.getAboxOntology();
		//			String pid = g.getIdentifier(p);
		//			String fn = pid.replaceAll(":", "_") + ".owl";
		//			FileOutputStream os = new FileOutputStream(new File("target/lego/"+fn));
		//			ont.getOWLOntologyManager().saveOntology(ont, os);
		//			ont.getOWLOntologyManager().removeOntology(ont);

		FileOutputStream os = new FileOutputStream(new File("target/qont.owl"));
		ni.getQueryOntology().getOWLOntologyManager().saveOntology(ni.getQueryOntology(), os);
		os.close();
		
		os = new FileOutputStream(new File("target/aont.owl"));
		ni.getQueryOntology().getOWLOntologyManager().saveOntology(ni.getAboxOntology(), os);

		w.close();

		LOG.info("Num generated individuals = "+ni.getGeneratedIndividuals().size());
		assertEquals(7, ni.getGeneratedIndividuals().size());
		LOG.info("Score = "+ni.ccp);



	}



}
