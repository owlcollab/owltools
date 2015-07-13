package owltools.gaf.parser;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import owltools.OWLToolsTestBasics;
import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;

public class GAFWithColumnTest extends OWLToolsTestBasics {

	@Test
	public void testSimple() throws Exception {
		GafObjectsBuilder b = new GafObjectsBuilder();
		GafDocument doc = b.buildDocument(getResource("gene-associations/with-column/with_column_simple.gaf"));
		List<GeneAnnotation> annotations = doc.getGeneAnnotations();
		assertEquals(1, annotations.size());
		GeneAnnotation annotation = annotations.get(0);
		assertTrue(annotation.isNegated());
		assertTrue(annotation.isColocatesWith());
		assertFalse(annotation.isContributesTo());
		assertFalse(annotation.isCut());
		assertFalse(annotation.isIntegralTo());
	}
	
	@Test
	public void testRepair() throws Exception {
		GafObjectsBuilder b = new GafObjectsBuilder();
		final List<String> errors = new ArrayList<String>();
		final List<String> warnings = new ArrayList<String>();
		b.getParser().addParserListener(new ParserListener() {
			
			@Override
			public boolean reportWarnings() {
				return true;
			}
			
			@Override
			public void parsing(String line, int lineNumber) {
				// ignore
			}
			
			@Override
			public void parserWarning(String message, String line, int lineNumber) {
				warnings.add(message);
			}
			
			@Override
			public void parserError(String errorMessage, String line, int lineNumber) {
				errors.add(errorMessage);
			}
		});
		GafDocument doc = b.buildDocument(getResource("gene-associations/with-column/with_column_repair.gaf"));
		List<GeneAnnotation> annotations = doc.getGeneAnnotations();
		assertEquals(1, annotations.size());
		GeneAnnotation annotation = annotations.get(0);
		assertFalse(annotation.isNegated());
		assertFalse(annotation.isColocatesWith());
		assertTrue(annotation.isContributesTo());
		assertFalse(annotation.isCut());
		assertFalse(annotation.isIntegralTo());
		assertEquals(1, errors.size());
		assertEquals(0, warnings.size());
	}
	
	@Test
	public void testUnknown() throws Exception {
		GafObjectsBuilder b = new GafObjectsBuilder();
		final List<String> errors = new ArrayList<String>();
		final List<String> warnings = new ArrayList<String>();
		b.getParser().addParserListener(new ParserListener() {
			
			@Override
			public boolean reportWarnings() {
				return true;
			}
			
			@Override
			public void parsing(String line, int lineNumber) {
				// ignore
			}
			
			@Override
			public void parserWarning(String message, String line, int lineNumber) {
				warnings.add(message);
			}
			
			@Override
			public void parserError(String errorMessage, String line, int lineNumber) {
				errors.add(errorMessage);
			}
		});
		GafDocument doc = b.buildDocument(getResource("gene-associations/with-column/with_column_unknown.gaf"));
		List<GeneAnnotation> annotations = doc.getGeneAnnotations();
		assertEquals(1, annotations.size());
		GeneAnnotation annotation = annotations.get(0);
		assertFalse(annotation.isNegated());
		assertFalse(annotation.isColocatesWith());
		assertFalse(annotation.isContributesTo());
		assertFalse(annotation.isCut());
		assertFalse(annotation.isIntegralTo());
		assertEquals(1, errors.size());
		assertEquals(0, warnings.size());
	}
	
	@Test
	public void testDuplicate() throws Exception {
		GafObjectsBuilder b = new GafObjectsBuilder();
		final List<String> errors = new ArrayList<String>();
		final List<String> warnings = new ArrayList<String>();
		b.getParser().addParserListener(new ParserListener() {
			
			@Override
			public boolean reportWarnings() {
				return true;
			}
			
			@Override
			public void parsing(String line, int lineNumber) {
				// ignore
			}
			
			@Override
			public void parserWarning(String message, String line, int lineNumber) {
				warnings.add(message);
			}
			
			@Override
			public void parserError(String errorMessage, String line, int lineNumber) {
				errors.add(errorMessage);
			}
		});
		GafDocument doc = b.buildDocument(getResource("gene-associations/with-column/with_column_duplicate.gaf"));
		List<GeneAnnotation> annotations = doc.getGeneAnnotations();
		assertEquals(1, annotations.size());
		GeneAnnotation annotation = annotations.get(0);
		assertTrue(annotation.isNegated());
		assertFalse(annotation.isColocatesWith());
		assertFalse(annotation.isContributesTo());
		assertFalse(annotation.isCut());
		assertFalse(annotation.isIntegralTo());
		assertEquals(0, errors.size());
		assertEquals(1, warnings.size());
	}
}
