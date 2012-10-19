package owltools.mooncat.ontologymetadata;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import owltools.graph.OWLGraphWrapper;

public class ImportChainExtractor {
	
	private static Logger LOG = Logger.getLogger(ImportChainExtractor.class);
	static IRI fakeImportsPropertyIRI = IRI.create("http://www.geneontology.org/formats/oboInOwl#imports");

	public static OWLOntology extractOntologyMetadata(OWLGraphWrapper g, String mdoIRI) throws OWLOntologyCreationException {
		OWLGraphWrapper mdg  = new OWLGraphWrapper(mdoIRI);
		OWLOntology mdo = mdg.getSourceOntology();
		for (OWLOntology o : g.getAllOntologies()) {
			LOG.info("Ontology:"+o);
			IRI oi = o.getOntologyID().getOntologyIRI();
			// Note: using the owl:imports property triggers the OWLAPI to load ths import
			for (OWLAnnotation ann : o.getAnnotations()) {
				OWLAnnotationAssertionAxiom aaa = mdg.getDataFactory().getOWLAnnotationAssertionAxiom(oi, ann);
				LOG.info("  adding ontology metadata assertion:"+aaa);
				mdg.getManager().addAxiom(mdo,aaa); 
			}
			for (OWLImportsDeclaration oid : o.getImportsDeclarations()) {
				// TODO - change vocabulary
				OWLAnnotationAssertionAxiom aaa = 
					mdg.getDataFactory().getOWLAnnotationAssertionAxiom(g.getDataFactory().getOWLAnnotationProperty(fakeImportsPropertyIRI),
							oi, oid.getIRI());
				mdg.getManager().addAxiom(mdo, aaa);
			}
			IRI v = o.getOntologyID().getVersionIRI();
			if (v != null) {
				OWLAnnotationAssertionAxiom aaa = 
					mdg.getDataFactory().getOWLAnnotationAssertionAxiom(g.getDataFactory().getOWLAnnotationProperty(OWLRDFVocabulary.OWL_VERSION_IRI.getIRI()),
							oi, v);
				mdg.getManager().addAxiom(mdo, aaa);

			}

		}
		return mdo;
	}

}
