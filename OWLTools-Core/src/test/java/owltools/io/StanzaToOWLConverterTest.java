package owltools.io;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.junit.Test;
import org.obolibrary.obo2owl.OWLAPIObo2Owl;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.parser.OBOFormatParser;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;

public class StanzaToOWLConverterTest extends OWLToolsTestBasics {


	@Test
	public void testConvert() throws Exception{
		OWLGraphWrapper  wrapper = getOBO2OWLOntologyWrapper("caro.obo");
		File f = getResource("GO.xrf_abbs");
		StanzaToOWLConverter sc = new StanzaToOWLConverter(wrapper);
		OWLOntology tgt = wrapper.getManager().createOntology();
		sc.config.targetOntology = tgt;
		sc.config.defaultPrefix = "http://x.org/";
		sc.parse(f);
		assertTrue(tgt.getAxiomCount() > 0);
		for (OWLAxiom ax : tgt.getAxioms()) {
			System.out.println(ax);
		}
		
	}

	
	private OWLGraphWrapper getOBO2OWLOntologyWrapper(String file) throws Exception{
		OBOFormatParser p = new OBOFormatParser();
		OBODoc obodoc = p.parse(new BufferedReader(new FileReader(getResource(file))));
		OWLAPIObo2Owl bridge = new OWLAPIObo2Owl();
		OWLOntology ontology = bridge.convert(obodoc);
		OWLGraphWrapper wrapper = new OWLGraphWrapper(ontology);
		return wrapper;
	}
	
}
