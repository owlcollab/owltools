package owltools.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.obolibrary.obo2owl.Obo2Owl;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.parser.OBOFormatParser;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLOntology;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;

public class OWLGsonRendererTest extends OWLToolsTestBasics {

	private static Logger LOG = Logger.getLogger(OWLGsonRendererTest.class);
	
	@Rule
	public TemporaryFolder folder= new TemporaryFolder();

	private static final boolean RENDER_FLAG = true;

	@Test
	public void testAxioms() throws Exception{
		OWLGraphWrapper  wrapper = getOBO2OWLOntologyWrapper("caro.obo");
		final StringWriter stringWriter = new StringWriter();
		OWLGsonRenderer gr = new OWLGsonRenderer(new PrintWriter(stringWriter));
		OWLOntology ont = wrapper.getSourceOntology();
		for (OWLAxiom a : ont.getAxioms()) {
			gr.render(a);
		}
		if (RENDER_FLAG) {
			LOG.debug(stringWriter.toString());
		}
	}

	@Test
	public void testGEdges() throws Exception{
		OWLGraphWrapper  wrapper = getOBO2OWLOntologyWrapper("caro.obo");
		final StringWriter stringWriter = new StringWriter();
		OWLGsonRenderer gr = new OWLGsonRenderer(new PrintWriter(stringWriter));
		OWLOntology ont = wrapper.getSourceOntology();
		for (OWLClass c : ont.getClassesInSignature()) {
			for (OWLGraphEdge e : wrapper.getOutgoingEdgesClosure(c)) {
				gr.render(e);
			}
		}
		if (RENDER_FLAG) {
			LOG.debug(stringWriter.toString());
		}
	}

	@Test
	public void testOnt() throws Exception{
		OWLGraphWrapper  wrapper = getOBO2OWLOntologyWrapper("caro.obo");
		final StringWriter stringWriter = new StringWriter();
		OWLGsonRenderer gr = new OWLGsonRenderer(new PrintWriter(stringWriter));
		gr.render(wrapper.getSourceOntology());
		if (RENDER_FLAG) {
			LOG.debug(stringWriter.toString());
			ParserWrapper pw = new ParserWrapper();
			OWLDocumentFormat owlFormat = new OWLJSONFormat();
			File foo = folder.newFile("foo.json");
			pw.saveOWL(wrapper.getSourceOntology(), owlFormat , foo.getAbsolutePath());
		}
	}

	
	private OWLGraphWrapper getOBO2OWLOntologyWrapper(String file) throws Exception{
		OBOFormatParser p = new OBOFormatParser();
		OBODoc obodoc = p.parse(new BufferedReader(new FileReader(getResource(file))));
		Obo2Owl bridge = new Obo2Owl();
		OWLOntology ontology = bridge.convert(obodoc);
		OWLGraphWrapper wrapper = new OWLGraphWrapper(ontology);
		return wrapper;
	}
	
}
