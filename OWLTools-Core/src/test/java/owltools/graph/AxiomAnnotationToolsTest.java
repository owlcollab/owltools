package owltools.graph;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.obolibrary.obo2owl.Obo2Owl;
import org.obolibrary.obo2owl.OWLAPIOwl2Obo;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.parser.OBOFormatParser;
import org.semanticweb.owlapi.model.OWLOntology;

import owltools.OWLToolsTestBasics;

/**
 * Tests for {@link AxiomAnnotationTools}.
 */
public class AxiomAnnotationToolsTest extends OWLToolsTestBasics {

	/**
	 * Test for {@link AxiomAnnotationTools#reduceAxiomAnnotationsToOboBasic(org.semanticweb.owlapi.model.OWLOntology)}.
	 * 
	 * @throws Exception 
	 */
	@Test
	public void testReduceAxiomAnnotationsToOboBasicOWLOntology() throws Exception {
		final File inputFile = getResource("qualifiers/with-qualifiers.obo");
		final File referenceFile = getResource("qualifiers/no-qualifiers.obo");
		
		OBOFormatParser p = new OBOFormatParser();
		OBODoc inputOboDoc = p.parse(inputFile);
		Obo2Owl obo2Owl = new Obo2Owl();
		OWLOntology owlOntology = obo2Owl.convert(inputOboDoc);
		OWLAPIOwl2Obo owl2Obo = new OWLAPIOwl2Obo();
		
		// check round-trip first before removing
		equals(inputFile, owl2Obo.convert(owlOntology));

		// remove axiom annotations
		AxiomAnnotationTools.reduceAxiomAnnotationsToOboBasic(owlOntology);
		equals(referenceFile, owl2Obo.convert(owlOntology));
	}

	
	private static void equals(File reference, OBODoc oboDoc) throws IOException {
		String oboString = renderOBOtoString(oboDoc);
		String referenceString = FileUtils.readFileToString(reference);
		assertEquals(referenceString, oboString);
	}
}
