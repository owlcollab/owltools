package org.geneontology.lego.dot;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

import org.semanticweb.HermiT.Reasoner.ReasonerFactory;
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

	static void write(String input, String output, String name) throws Exception {
		ParserWrapper p = new ParserWrapper();
		IRI iri = IRI.create(new File(input));
		final OWLOntology ontology = p.parseOWL(iri);
		final OWLGraphWrapper g = new OWLGraphWrapper(ontology);

		Set<OWLNamedIndividual> individuals = ontology.getIndividualsInSignature();

		OWLReasonerFactory factory = new ReasonerFactory();
		
		final BufferedWriter fileWriter = new BufferedWriter(new FileWriter(new File(output)));
		
		LegoDotWriter writer = new LegoDotWriter(g, factory.createReasoner(ontology)) {

			@Override
			protected void close() throws IOException {
				fileWriter.close();
			}

			@Override
			protected void appendLine(CharSequence line) throws IOException {
				System.out.println(line);
				fileWriter.append(line).append('\n');
			}
		};
		writer.renderDot(individuals, name);
	}
	
	public static void main(String[] args) throws Exception {
		// create work dir
		File file = new File("out");
		file.mkdirs();
		write("examples/dauer-merged.owl", "out/dauer.dot", "dauer");
	}
}
