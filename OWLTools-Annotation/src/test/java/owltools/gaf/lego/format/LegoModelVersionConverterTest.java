package owltools.gaf.lego.format;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import owltools.gaf.lego.MolecularModelJsonRenderer;
import owltools.gaf.lego.MolecularModelManager;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;
import owltools.util.ModelContainer;

import com.google.common.collect.Sets;

public class LegoModelVersionConverterTest {

	@Test
	public void testConvertLegoModelToAllIndividuals() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		OWLGraphWrapper graph = new OWLGraphWrapper(pw.parse("http://purl.obolibrary.org/obo/go.owl"));
		MolecularModelManager<?> m3 = new MolecularModelManager<Object>(graph);
		m3.setPathToOWLFiles("src/test/resources/lego-conversion");
		Set<String> modelIds = m3.getAvailableModelIds();
		assertEquals(1, modelIds.size());
		
		final String modelId = modelIds.iterator().next();
		final ModelContainer model = m3.getModel(modelId);
		assertNotNull(model);
		OWLOntology aboxOntology = model.getAboxOntology();
		Set<OWLNamedIndividual> allIndividualsOld = aboxOntology.getIndividualsInSignature();
		
		LegoModelVersionConverter converter = new LegoModelVersionConverter();
		converter.convertLegoModelToAllIndividuals(model, modelId);
		
		
		Set<OWLNamedIndividual> allIndividualsNew = aboxOntology.getIndividualsInSignature();
		assertTrue(allIndividualsNew.size() >= allIndividualsOld.size());
		assertTrue(allIndividualsNew.containsAll(allIndividualsOld));
		Set<OWLNamedIndividual> newIndividuals = Sets.difference(allIndividualsNew, allIndividualsOld);
		Set<OWLNamedIndividual> ecoIndividuals = new HashSet<OWLNamedIndividual>();
		for (OWLNamedIndividual newIndividual : newIndividuals) {
			IRI iri = newIndividual.getIRI();
			if (iri.toString().contains("-ECO-")) {
				ecoIndividuals.add(newIndividual);
			}
		}
		assertEquals(3, ecoIndividuals.size());
		
		System.out.println("---------");
		System.out.println(renderModel(model));
		System.out.println("---------");
		
		System.out.println("----------");
		System.out.println(rendertoJson(model));
		System.out.println("----------");
		
	}

	static String rendertoJson(ModelContainer model) {
		return MolecularModelJsonRenderer.renderToJson(model.getAboxOntology(), true, true);
	}
	
	static String renderModel(ModelContainer model) throws Exception {
		OWLOntology aboxOntology = model.getAboxOntology();
		OWLOntologyManager m = aboxOntology.getOWLOntologyManager();
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try {
			m.saveOntology(aboxOntology, outputStream);
			return outputStream.toString();
		}
		finally {
			IOUtils.closeQuietly(outputStream);
		}
	}
}
