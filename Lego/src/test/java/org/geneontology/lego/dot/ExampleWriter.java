package org.geneontology.lego.dot;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import owltools.graph.OWLGraphWrapper;
import owltools.io.CatalogXmlIRIMapper;
import owltools.io.ParserWrapper;

public class ExampleWriter {

	static void write(String input, final String output, String name, String catalogXml) throws Exception {
		ParserWrapper p = new ParserWrapper();
		if (catalogXml != null) {
			p.addIRIMapper(new CatalogXmlIRIMapper(catalogXml));
		}
		IRI iri = IRI.create(new File(input));
		final OWLOntology ontology = p.parseOWL(iri);
		final OWLGraphWrapper g = new OWLGraphWrapper(ontology);

		Set<OWLNamedIndividual> individuals = ontology.getIndividualsInSignature(true);

		OWLReasonerFactory factory = new ElkReasonerFactory();
		
		final OWLReasoner reasoner = factory.createReasoner(ontology);
		try {
			LegoRenderer renderer = new LegoDotWriter(g, reasoner) {
	
				BufferedWriter fileWriter = null;
				
				@Override
				protected void open() throws IOException {
					fileWriter = new BufferedWriter(new FileWriter(new File(output)));
				}
	
				@Override
				protected void close() {
					IOUtils.closeQuietly(fileWriter);
				}
	
				@Override
				protected void appendLine(CharSequence line) throws IOException {
					System.out.println(line);
					fileWriter.append(line).append('\n');
				}
			};
			renderer.render(individuals, name, true);
		}
		finally {
			reasoner.dispose();
		}
	}
}
