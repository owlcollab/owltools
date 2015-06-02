package owltools.gaf.parser;

import static org.junit.Assert.*;

import java.io.File;
import java.util.List;

import org.junit.Test;

import owltools.OWLToolsTestBasics;
import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;

public class GAF21ParserTest extends OWLToolsTestBasics {

	@Test
	public void testParseGaf21WithColumn() throws Exception {
		File gaf = getResource("pipecomma.gaf");
		GafObjectsBuilder b = new GafObjectsBuilder();
		GafDocument document = b.buildDocument(gaf);
		List<GeneAnnotation> annotations = document.getGeneAnnotations();
		assertEquals(4, annotations.size());
		for (GeneAnnotation geneAnnotation : annotations) {
			assertEquals(2, geneAnnotation.getWithInfos().size());
		}
	}

}
