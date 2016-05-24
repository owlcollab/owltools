package ontologymetadata;

import java.io.File;

import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;
import owltools.io.CatalogXmlIRIMapper;
import owltools.io.ParserWrapper;
import owltools.mooncat.ontologymetadata.ImportChainDotWriter;
import owltools.mooncat.ontologymetadata.OntologyMetadataMarkdownWriter;

public class ImportChainExtractorTest extends OWLToolsTestBasics {
	

	@Test
	public void testRender() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		pw.getManager().getIRIMappers().add(new CatalogXmlIRIMapper(getResource("catalog-v001.xml")));
		
		IRI iri = IRI.create(getResource("test-import-chain-root.owl"));
		final OWLOntology ontology = pw.parseOWL(iri);
		final OWLGraphWrapper g = new OWLGraphWrapper(ontology);
		
		File file = new File("out");
		file.mkdirs();
		write(g.getSourceOntology(), "out/food-imports.dot", "food");

	
	}
	
	static void write(OWLOntology ontology, final String output, String name) throws Exception {
		final OWLGraphWrapper g = new OWLGraphWrapper(ontology);

		ImportChainDotWriter writer = new ImportChainDotWriter(g);
		writer.renderDot(ontology, name, output, true);
		
		String info = OntologyMetadataMarkdownWriter.renderMarkdown(g, "out", true);
		System.out.println(info);
	}
	
}
