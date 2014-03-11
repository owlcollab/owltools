package owltools.flex;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.obolibrary.obo2owl.Obo2Owl;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.parser.OBOFormatParser;
import org.obolibrary.oboformat.parser.OBOFormatParserException;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import owltools.graph.OWLGraphWrapper;

public class FlexTest {

	@Test
	public void testFlexReflectionLoading() throws IOException, OWLOntologyCreationException, OBOFormatParserException{

		String fstr = getResourceString("trivial.obo");
		//ParserWrapper pw = new ParserWrapper();
		//OWLGraphWrapper g = pw.parseOBOFiles(files);

		OBOFormatParser p = new OBOFormatParser();
		OBODoc obodoc = p.parse(fstr);
		Obo2Owl bridge = new Obo2Owl();
		OWLOntology ont = bridge.convert(obodoc);
		OWLGraphWrapper g = new OWLGraphWrapper(ont);
		
		// Get an object and a FlexCollection to play with.
		OWLObject a1 = g.getOWLObjectByIdentifier("A:0000001");
		FlexCollection fc = new FlexCollection(g);
		
		// Trivial 1
		List<String> sexpr_t1 = new ArrayList<String>();
		sexpr_t1.add("getIdentifier");
		String ans_t1 = fc.getExtString(a1, sexpr_t1);
		assertNotNull("Hope not!", ans_t1);
		assertEquals("Get identifier", "A:0000001", ans_t1);

		// Trivial 2
		List<String> sexpr_t2 = new ArrayList<String>();
		sexpr_t2.add("getLabel");
		String ans_t2 = fc.getExtString(a1, sexpr_t2);
		assertEquals("Get label a1", "a1", ans_t2);
		
		// Non-trivial 1
		List<String> sexpr_t3 = new ArrayList<String>();
		sexpr_t3.add("getAnnotationPropertyValues");
		sexpr_t3.add("alt_id");
		List<String> ans_t3 = fc.getExtStringList(a1, sexpr_t3);
		assertTrue("Get label a1 alt_id a:0000001", ans_t3.contains("a:0000001"));
		assertTrue("Get label a1 alt_id AY:0000001", ans_t3.contains("AY:0000001"));
	}
	
	// A little helper from Chris stolen from somewhere else...
	protected static String getResourceString(String name) {
		assertNotNull(name);
		assertFalse(name.length() == 0);
		// TODO: Replace this with a mechanism not relying on the relative path.
		return "src/test/resources/" + name;
	}
}
