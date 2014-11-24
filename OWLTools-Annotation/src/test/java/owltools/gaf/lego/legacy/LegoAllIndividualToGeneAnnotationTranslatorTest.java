package owltools.gaf.lego.legacy;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import owltools.OWLToolsTestBasics;
import owltools.gaf.BioentityDocument;
import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.gaf.eco.EcoMapperFactory;
import owltools.gaf.eco.SimpleEcoMapper;
import owltools.gaf.io.GafWriter;
import owltools.gaf.lego.MolecularModelManager;
import owltools.gaf.lego.format.LegoModelVersionConverter;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;
import owltools.util.ModelContainer;

public class LegoAllIndividualToGeneAnnotationTranslatorTest extends OWLToolsTestBasics {

	static SimpleEcoMapper mapper = null;
	static ModelContainer model = null;
	static String modelId = null;
	static MolecularModelManager<?> m3 = null;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		OWLGraphWrapper go = pw.parseToOWLGraph("http://purl.obolibrary.org/obo/go.owl");
		MolecularModelManager<?> m3 = new MolecularModelManager<Object>(go);
		m3.setPathToOWLFiles(new File("src/test/resources/lego-conversion").getCanonicalPath());
		
		Set<String> modelIds = m3.getAvailableModelIds();
		assertEquals(1, modelIds.size());
		modelId = modelIds.iterator().next();
		model = m3.getModel(modelId);
		mapper = EcoMapperFactory.createSimple();
	}
	
	@AfterClass
	public static void afterClass() throws Exception {
		if (m3 != null) {
			m3.dispose();
		}
	}

	@Test
	public void test() throws Exception {
		// old conversion
		LegoToGeneAnnotationTranslator oldTranslator = 
				new LegoToGeneAnnotationTranslator(new OWLGraphWrapper(model.getAboxOntology()), model.getReasoner(), mapper);
		
		Pair<GafDocument, BioentityDocument> oldPair = oldTranslator.translate(modelId, model.getAboxOntology(), null);
		GafDocument oldGaf = oldPair.getLeft();
		BioentityDocument oldBioentityDocument = oldPair.getRight();
//		System.out.println("-----");
//		System.out.println(renderGaf(oldGaf, oldBioentityDocument));
//		System.out.println("-----");
		
		// new conversion
		// #1 convert model to all individuals
		LegoModelVersionConverter converter = new LegoModelVersionConverter();
		converter.convertLegoModelToAllIndividuals(model, modelId);
		
		// #2 translate to legacy
		LegoAllIndividualToGeneAnnotationTranslator newTranslator = 
				new LegoAllIndividualToGeneAnnotationTranslator(new OWLGraphWrapper(model.getAboxOntology()), model.getReasoner(), mapper);
		
		Pair<GafDocument, BioentityDocument> newPair = newTranslator.translate(modelId, model.getAboxOntology(), null);
		GafDocument newGaf = newPair.getLeft();
		BioentityDocument newBioentityDocument = newPair.getRight();
//		System.out.println("-------------");
//		System.out.println(renderGaf(newGaf, newBioentityDocument));
//		System.out.println("-------------");
		
		// assert same amount of bioentities
		assertEquals(oldBioentityDocument.getBioentities().size(), newBioentityDocument.getBioentities().size());
		
		// assert same amount of annotations
		List<GeneAnnotation> oldAnnotations = oldGaf.getGeneAnnotations();
		List<GeneAnnotation> newAnnotations = newGaf.getGeneAnnotations();
		assertEquals(oldAnnotations.size(), newAnnotations.size());
		
		// assert that the one mf annotation has the same amount of c16
		GeneAnnotation oldMf = findMfAnnotation(oldAnnotations);
		GeneAnnotation newMf = findMfAnnotation(newAnnotations);
		assertNotNull(oldMf);
		assertNotNull(newMf);
		assertEquals(oldMf.getExtensionExpressions().size(), newMf.getExtensionExpressions().size());
		
	}
	
	private GeneAnnotation findMfAnnotation(List<GeneAnnotation> annotations) {
		for (GeneAnnotation annotation : annotations) {
			if ("F".equals(annotation.getAspect())) {
				return annotation;
			}
		}
		return null;
	}

	static String renderGaf(GafDocument gaf, BioentityDocument bioentities) throws IOException {
		GafWriter gafWriter = new GafWriter();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			gafWriter.setStream(new PrintStream(out));
			gafWriter.write(gaf);
		}
		finally {
			gafWriter.close();
			out.close();
		}
		String s = out.toString();
		return s;
	}
}
