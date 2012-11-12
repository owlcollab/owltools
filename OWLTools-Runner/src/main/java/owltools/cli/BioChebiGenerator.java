package owltools.cli;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

/**
 * Methods for creating the BioChEBI wrapper file for ChEBI. The wrapper creates
 * the GCIs equivalences from ChEBI in GO.
 */
public class BioChebiGenerator {

	private final Map<OWLObjectProperty, Set<OWLObjectProperty>> expansionMap;
	
	/**
	 * @param expansionMap
	 */
	public BioChebiGenerator(Map<OWLObjectProperty, Set<OWLObjectProperty>> expansionMap) {
		this.expansionMap = expansionMap;
	}
	
	/**
	 * Create the GCIs for BioChEBI. Add the axioms into the given ontology.
	 * 
	 * @param ontology
	 */
	public void expand(OWLOntology ontology) {
		final OWLOntologyManager manager = ontology.getOWLOntologyManager();
		final OWLDataFactory factory = manager.getOWLDataFactory();
		
		// scan axioms
		Set<OWLSubClassOfAxiom> axioms = ontology.getAxioms(AxiomType.SUBCLASS_OF, true);
		for (OWLSubClassOfAxiom axiom : axioms) {
			OWLClassExpression superCE = axiom.getSuperClass();
			OWLClassExpression subCE = axiom.getSubClass();
			if (subCE.isAnonymous()) {
				// sub class needs to be an named OWLClass
				continue;
			}

			if (superCE instanceof OWLObjectSomeValuesFrom == false) {
				continue;
			}
			OWLObjectSomeValuesFrom some = (OWLObjectSomeValuesFrom) superCE;

			OWLObjectPropertyExpression expression = some.getProperty();
			if (expression.isAnonymous()) {
				// object property expression needs to be a named OWLObjectProperty 
				continue;
			}

			OWLObjectProperty p = (OWLObjectProperty) expression;
			
			Set<OWLObjectProperty> expansions = expansionMap.get(p);
			if (expansions == null) {
				continue;
			}

			// get content for GCI
			OWLClassExpression y = some.getFiller();
			OWLClass x = subCE.asOWLClass();
			for (OWLObjectProperty createProperty : expansions) {
				OWLClassExpression ce1 = factory.getOWLObjectSomeValuesFrom(createProperty, x);
				OWLClassExpression ce2 = factory.getOWLObjectSomeValuesFrom(createProperty, y);
				OWLEquivalentClassesAxiom eq = factory.getOWLEquivalentClassesAxiom(ce1, ce2);
				manager.addAxiom(ontology, eq);
			}
		}
	}

	/**
	 * Create the GCIs for BioChEBI. Use the given ontology file as template.
	 * 
	 * @param bioChebiTemplateFile
	 * @return ontology
	 * @throws OWLOntologyCreationException
	 */
	public static OWLOntology createBioChebi(File bioChebiTemplateFile) throws OWLOntologyCreationException {
		// load
		ParserWrapper pw = new ParserWrapper();
		OWLOntology ontology = pw.parseOWL(IRI.create(bioChebiTemplateFile));
		
		OWLGraphWrapper graph = new OWLGraphWrapper(ontology);
		createBioChebi(graph);
		return ontology;
	}
	
	/**
	 * Create the GCIs for BioChEBI. Add the axioms into the given ontology graph.
	 * 
	 * @param graph
	 */
	public static void createBioChebi(OWLGraphWrapper graph) {
		// find properties
		Set<OWLObjectProperty> searchProperties = getPropertiesByLabels(graph, 
				"is conjugate acid of", 
				"is conjugate base of");
		Set<OWLObjectProperty> createProperties = getPropertiesByIRI(graph,
				"http://purl.obolibrary.org/obo/BFO_0000057",    // has participant
				"http://purl.obolibrary.org/obo/RO_0002313",     // transports or maintains localization of
				"http://purl.obolibrary.org/obo/RO_0002233",     // has input
				"http://purl.obolibrary.org/obo/RO_0002234",     // has output
				"http://purl.obolibrary.org/obo/GOREL_0000034",  // imports
				"http://purl.obolibrary.org/obo/GOREL_0000035",  // exports
				"http://purl.obolibrary.org/obo/GOREL_0000036"   // regulates level of
				); 

		// create GCI expansion map
		Map<OWLObjectProperty, Set<OWLObjectProperty>> expansionMap = new HashMap<OWLObjectProperty, Set<OWLObjectProperty>>();
		for (OWLObjectProperty p : searchProperties) {
			expansionMap.put(p, createProperties);
		}
		BioChebiGenerator generator = new BioChebiGenerator(expansionMap);
		OWLOntology ontology = graph.getSourceOntology();
		generator.expand(ontology);
	}
	
	/**
	 * Main method. For testing purposes ONLY!
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		
		OWLOntology ontology = createBioChebi(new File("src/main/resources/bio-chebi-input.owl"));
		
		// save
		File outFile = new File("bio-chebi.owl");
		ontology.getOWLOntologyManager().saveOntology(ontology, IRI.create(outFile));
	}

	private static Set<OWLObjectProperty> getPropertiesByIRI(OWLGraphWrapper graph, String...iris) {
		Set<OWLObjectProperty> set = new HashSet<OWLObjectProperty>();
		for (String iri : iris) {
			OWLObjectProperty property = graph.getOWLObjectProperty(iri);
			if (property == null) {
				throw new RuntimeException("No property found for IRI: "+iri);
			}
			set.add(property);
		}
		return set;
	}

	private static Set<OWLObjectProperty> getPropertiesByLabels(OWLGraphWrapper graph, String...labels) {
		Set<OWLObjectProperty> set = new HashSet<OWLObjectProperty>();
		for (String label : labels) {
			OWLObject obj = graph.getOWLObjectByLabel(label);
			if (obj == null) {
				throw new RuntimeException("No property found for label: "+label);
			}
			if (obj instanceof OWLObjectProperty) {
				set.add((OWLObjectProperty) obj);
			}
			else {
				throw new RuntimeException("No obj is wrong type: '"+obj.getClass().getName()+"' + for label: "+label);
			}
		}
		return set;
	}
}
