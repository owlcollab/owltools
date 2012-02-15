package owltools.mooncat;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.obolibrary.macro.ManchesterSyntaxTool;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.expression.ParserException;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import owltools.graph.OWLGraphWrapper;

/**
 * Tools for running DL queries on {@link OWLOntology}.
 * 
 * TODO decide on final package and name
 */
public class DLQueryTool {

	private static final Logger LOG = Logger.getLogger(DLQueryTool.class);
	
	/**
	 * Execute the DL query on the given ontology graph. Uses the factory to create 
	 * the {@link OWLReasoner} for an internal query ontology.
	 * 
	 * @param dlQuery
	 * @param graph
	 * @param reasonerFactory
	 * @return set of {@link OWLClass} which 
	 * @throws ParserException
	 * @throws OWLOntologyCreationException
	 */
	public static Set<OWLClass> executeDLQuery(String dlQuery, OWLGraphWrapper graph, 
			OWLReasonerFactory reasonerFactory) throws ParserException, OWLOntologyCreationException 
	{
		// create parser and parse DL query string
		ManchesterSyntaxTool parser = null;
		
		OWLClassExpression ce;
		try {
			parser = new ManchesterSyntaxTool(graph.getSourceOntology(), graph.getSupportOntologySet());
			ce = parser.parseManchesterExpression(dlQuery);
		} finally {
			// always dispose parser to avoid a memory leak
			if (parser != null) {
				parser.dispose();
			}
		}
		
		// create query ontology
		OWLOntologyManager m = OWLManager.createOWLOntologyManager();
		IRI tempIRI = new OWLOntologyID().getOntologyIRI();
		OWLOntology queryOntology = m.createOntology(tempIRI, graph.getAllOntologies());
		OWLDataFactory f = m.getOWLDataFactory();
		OWLClass qc = f.getOWLClass(IRI.create("http://owltools.org/Q"));
		OWLEquivalentClassesAxiom ax = f.getOWLEquivalentClassesAxiom(ce, qc);
		m.addAxiom(queryOntology, ax);
		
		Set<OWLClass> subset = executeQuery(ce, queryOntology, reasonerFactory);
		if(subset.isEmpty()) {
			LOG.warn("No classes found for query subclass of:"+dlQuery);
		}
		return subset;
	}
	
	/**
	 * Execute the DL query on the given ontology graph. Uses the factory to create 
	 * the {@link OWLReasoner} for an internal query ontology.
	 * 
	 * @param queryObject
	 * @param ontology
	 * @param reasonerFactory
	 * @return set of {@link OWLClass} which 
	 */
	static Set<OWLClass> executeQuery(OWLClassExpression queryObject, OWLOntology ontology, 
			OWLReasonerFactory reasonerFactory) 
	{
		Set<OWLClass> subset = new HashSet<OWLClass>();
		
		LOG.info("Create reasoner for query ontology.");
		OWLReasoner reasoner = createReasoner(ontology, reasonerFactory);
		LOG.info("Start evaluation for DL query subclass of: "+queryObject);
		NodeSet<OWLClass> node = reasoner.getSubClasses(queryObject, false);
		if (node != null) {
			Set<OWLClass> classes = node.getFlattened();
			for (OWLClass owlClass : classes) {
				if (!owlClass.isBottomEntity() && !owlClass.isTopEntity()) {
					subset.add(owlClass);
				}
			}
			LOG.info("Number of found classes for dl query subclass of: "+classes.size());
		}
		return subset;
	}

	static OWLReasoner createReasoner(OWLOntology ontology, OWLReasonerFactory reasonerFactory) {
		// Create an instance of an OWL API reasoner
		OWLReasoner reasoner = reasonerFactory.createReasoner(ontology);
		
		// Optionally let the reasoner compute the most relevant inferences in
		// advance
		reasoner.precomputeInferences(InferenceType.values());
		return reasoner;
	}

}
