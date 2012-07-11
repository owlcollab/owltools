package owltools.oppl;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import org.coode.oppl.AnnotationBasedSymbolTableFactory;
import org.coode.oppl.ChangeExtractor;
import org.coode.oppl.OPPLParser;
import org.coode.oppl.OPPLScript;
import org.coode.oppl.ParserFactory;
import org.coode.oppl.exceptions.RuntimeExceptionHandler;
import org.coode.oppl.rendering.ManchesterSyntaxRenderer;
import org.coode.parsers.common.SystemErrorEcho;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.AddOntologyAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLAxiomChange;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChangeVisitor;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLRuntimeException;
import org.semanticweb.owlapi.model.RemoveAxiom;
import org.semanticweb.owlapi.model.RemoveImport;
import org.semanticweb.owlapi.model.RemoveOntologyAnnotation;
import org.semanticweb.owlapi.model.SetOntologyID;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

public class OpplRunner {

	public void feedOPPL(String script, OWLOntology ontology, OWLReasoner reasoner) {
		OWLOntologyManager ontologyManager = ontology.getOWLOntologyManager();

		ParserFactory parserFactory = new ParserFactory(ontologyManager, ontology, reasoner);
		
		AnnotationBasedSymbolTableFactory annotationBasedSymbolTableFactory = new AnnotationBasedSymbolTableFactory(
				ontologyManager,
				Arrays.asList(ontologyManager.getOWLDataFactory().getRDFSLabel().getIRI()));
		
		OPPLParser parser = parserFactory.build(
				new SystemErrorEcho(),
				annotationBasedSymbolTableFactory);
		
		
		OPPLScript parsed = parser.parse(script);
		ChangeExtractor extractor = new ChangeExtractor(new RuntimeExceptionHandler() {
			public void handlePatternSyntaxExcpetion(PatternSyntaxException e) {
				e.printStackTrace();
			}

			public void handleOWLRuntimeException(OWLRuntimeException e) {
				e.printStackTrace();
			}

			public void handleException(RuntimeException e) {
				e.printStackTrace();
			}
		}, true);
		List<OWLAxiomChange> changes = extractor.visit(parsed);
		for (OWLAxiomChange owlAxiomChange : changes) {
			final ManchesterSyntaxRenderer renderer = parserFactory.getOPPLFactory().getManchesterSyntaxRenderer(
					parsed.getConstraintSystem());
			
			owlAxiomChange.accept(new OWLOntologyChangeVisitor() {
				public void visit(RemoveOntologyAnnotation change) {
					System.out.println(change);
				}

				public void visit(AddOntologyAnnotation change) {
					System.out.println(change);
				}

				public void visit(RemoveImport change) {
					System.out.println(change);
				}

				public void visit(AddImport change) {
					System.out.println(change);
				}

				public void visit(SetOntologyID change) {
					System.out.println(change);
				}

				public void visit(RemoveAxiom change) {
					OWLAxiom axiom = change.getAxiom();
					axiom.accept(renderer);
					System.out.println(String.format("REMOVE %s", renderer));
				}

				public void visit(AddAxiom change) {
					OWLAxiom axiom = change.getAxiom();
					axiom.accept(renderer);
					System.out.println(String.format("ADD %s", renderer));
				}
			});
		}
	}

	public static void main(String[] args) throws Exception {
		// "oppl/t1.owl"
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new File("src/test/resources/oppl/t1.owl"));
		OpplRunner r = new OpplRunner();
		String query1 = "?A:CLASS SELECT ?A SubClassOf 'Heart disease (disorder)' BEGIN ADD ?A SubClassOf !Candidate END;";
		r.feedOPPL(query1, ontology, null);
	}

}
