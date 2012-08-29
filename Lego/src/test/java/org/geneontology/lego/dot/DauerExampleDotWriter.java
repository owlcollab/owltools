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
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

/**
 * Create a dot file for the Dauer pathway example.
 */
public class DauerExampleDotWriter {

	static void write(String input, final String output, String name) throws Exception {
		ParserWrapper p = new ParserWrapper();
		IRI iri = IRI.create(new File(input));
		final OWLOntology ontology = p.parseOWL(iri);
		final OWLGraphWrapper g = new OWLGraphWrapper(ontology);

		Set<OWLNamedIndividual> individuals = ontology.getIndividualsInSignature();

		OWLReasonerFactory factory = new ElkReasonerFactory();
		
		LegoDotWriter writer = new LegoDotWriter(g, factory.createReasoner(ontology)) {

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
		writer.renderDot(individuals, name, true);
	}
	
	public static void main(String[] args) throws Exception {
		// create work dir
		File file = new File("out");
		file.mkdirs();
		write("examples/dauer-merged.owl", "out/dauer.dot", "dauer");
	}
}
