package owltools.flex;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.obolibrary.obo2owl.OWLAPIObo2Owl;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.parser.OBOFormatParser;
import org.obolibrary.oboformat.parser.OBOFormatParserException;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;
import owltools.yaml.golrconfig.ConfigManager;
import owltools.yaml.golrconfig.GOlrField;

public class FlexTest {

	@Test
	public void testFlexReflectionLoading() throws IOException, OWLOntologyCreationException, OBOFormatParserException{

		String fstr = FlexTest.class.
                getResource("/trivial.obo").getFile();
		//ParserWrapper pw = new ParserWrapper();
		//OWLGraphWrapper g = pw.parseOBOFiles(files);

		OBOFormatParser p = new OBOFormatParser();
		OBODoc obodoc = p.parse(fstr);
		OWLAPIObo2Owl bridge = new OWLAPIObo2Owl();
		OWLOntology ont = bridge.convert(obodoc);
		OWLGraphWrapper g = new OWLGraphWrapper(ont);
		
		// Get an object and a FlexCollection to play with.
		OWLObject a1 = g.getOWLObjectByIdentifier("A:0000001");
		FlexCollection fc = new FlexCollection(g);
		
		// Trivial 1
		List<String> sexpr_t1 = new ArrayList<String>();
		sexpr_t1.add("getIdentifier");
		String ans_t1 = fc.getExtString(a1, sexpr_t1, null);
		assertNotNull("Hope not!", ans_t1);
		assertEquals("Get identifier", "A:0000001", ans_t1);

		// Trivial 2
		List<String> sexpr_t2 = new ArrayList<String>();
		sexpr_t2.add("getLabel");
		String ans_t2 = fc.getExtString(a1, sexpr_t2, null);
		assertEquals("Get label a1", "a1", ans_t2);
		
		// Non-trivial 1
		List<String> sexpr_t3 = new ArrayList<String>();
		sexpr_t3.add("getAnnotationPropertyValues");
		sexpr_t3.add("alt_id");
		List<String> ans_t3 = fc.getExtStringList(a1, sexpr_t3, null);
		assertTrue("Get label a1 alt_id a:0000001", ans_t3.contains("a:0000001"));
		assertTrue("Get label a1 alt_id AY:0000001", ans_t3.contains("AY:0000001"));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testFlexParseYaml() throws Exception {
		final String yamlFile = FlexTest.class.
                getResource("/test-ont-category-config.yaml").getFile();
		final String ontFile = FlexTest.class.
                getResource("/test-ont-category-config.obo").getFile();
		ParserWrapper pw = new ParserWrapper();
		OWLOntology ont = pw.parseOWL(IRI.create(new File(ontFile).getCanonicalFile()));
		OWLGraphWrapper g = new OWLGraphWrapper(ont);
		
		// verify config details
		ConfigManager configManager = new ConfigManager();
		configManager.add(yamlFile);
		List<GOlrField> fields = configManager.getFields();
		assertEquals(2, fields.size());
		GOlrField category = fields.get(1);
		assertNotNull(category);
		assertNotNull(category.property_config);
		String foo = (String) category.property_config.get("foo");
		assertEquals(foo, "bar");
		Collection<String> useNamespace = (Collection<String>) category.property_config.get("use-namespace");
		assertNotNull(useNamespace);
		assertEquals(1, useNamespace.size());
		assertEquals("GO", useNamespace.iterator().next());
		Map<String, String> idspaceMap = (Map<String, String>) category.property_config.get("idspace-map");
		assertNotNull(idspaceMap);
		assertEquals(5, idspaceMap.size());
		assertEquals("mouse anatomy", idspaceMap.get("MA"));
		assertEquals("cell", idspaceMap.get("CL"));
		assertEquals("animal anatomy", idspaceMap.get("UBERON"));
		assertEquals("embryonic mouse", idspaceMap.get("EMAP"));
		assertEquals("chemical", idspaceMap.get("CHEBI"));
		
		FlexCollection coll = new FlexCollection(configManager, g);
		Iterator<FlexDocument> iterator = coll.iterator();
		int lineCount = 0;
		Set<String> sourceCategories = new HashSet<String>();
		/*
		 * three classes, two source_category lines, because use-fallback is set to false
		 */
		while (iterator.hasNext()) {
			FlexDocument flexDocument = iterator.next();
			lineCount += 1;
			for (FlexLine flexLine : flexDocument) {
				if (flexLine.field.equals(category.id)) {
					sourceCategories.addAll(flexLine.value);
				}
			}
		}
		assertEquals(3, lineCount);
		assertEquals(2, sourceCategories.size());
		assertTrue(sourceCategories.contains("cell"));
		assertTrue(sourceCategories.contains("go namespace")); // removed under score
	}
}
