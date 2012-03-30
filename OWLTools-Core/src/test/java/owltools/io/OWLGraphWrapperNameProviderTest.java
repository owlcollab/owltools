package owltools.io;

import static junit.framework.Assert.*;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.coode.owlapi.obo.parser.OBOOntologyFormat;
import org.junit.Test;
import org.obolibrary.obo2owl.Obo2Owl;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.parser.OBOFormatParser;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;

public class OWLGraphWrapperNameProviderTest extends OWLToolsTestBasics {


	@Test
	public void testWriter() throws Exception{
		OBOFormatParser p = new OBOFormatParser();
		ParserWrapper pw = new ParserWrapper();
		OWLGraphWrapper  wrapper = getOBO2OWLOntologyWrapper("regulation_of_anti_apoptosis_xp_addon.obo");

		Obo2Owl bridge = new Obo2Owl();
		OBODoc obodoc = p.parse(new BufferedReader(new FileReader(getResource("regulation_of_anti_apoptosis_xp-baseline.obo"))));
		OWLOntology ontology = bridge.convert(obodoc);
		wrapper.addSupportOntology(ontology);
		
		ByteArrayOutputStream s = new ByteArrayOutputStream();

		pw.saveOWL(wrapper.getSourceOntology(), new OBOOntologyFormat(), s, wrapper);
		
		String oboString = s.toString();
		
		String[] lines = oboString.split("\n");
		int n = 0;
		for (String line : lines) {
			System.out.println("LINE: "+line);	
			if (line.startsWith("id: GO:0043066 ! negative regulation of apoptosis")) {
				n++;
			}
			if (line.startsWith("id: GO:0043069 ! negative regulation of programmed cell death")) {
				n++;
			}
		}
		
		assertEquals("ids do not have comments added", 2, n);

	}

	private OWLGraphWrapper getOBO2OWLOntologyWrapper(String file) throws OWLOntologyCreationException, FileNotFoundException, IOException{
		OBOFormatParser p = new OBOFormatParser();
		OBODoc obodoc = p.parse(new BufferedReader(new FileReader(getResource(file))));
		Obo2Owl bridge = new Obo2Owl();
		OWLOntology ontology = bridge.convert(obodoc);
		OWLGraphWrapper wrapper = new OWLGraphWrapper(ontology);
		return wrapper;
	}
	
	

}
