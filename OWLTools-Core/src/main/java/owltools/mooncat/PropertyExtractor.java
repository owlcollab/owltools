package owltools.mooncat;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.obolibrary.obo2owl.Owl2Obo;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLProperty;

import owltools.graph.OWLGraphWrapper;

/**
 * @author cjm
 *
 * Extracts a set of properties from an ontology O
 * 
 * The extracted properties will also include follow the reference closure of those properties, along
 * axioms such as InverseOf, SubPropertyOf.
 * 
 * Annotation assertion axioms are also collected
 * 
 *
 */
public class PropertyExtractor {

	OWLOntology propertyOntology;
	private Logger LOG = Logger.getLogger(PropertyExtractor.class);
	OWLOntology mainOntology;
	boolean isExpansive = true;
	public boolean isCreateShorthand = true;

	public PropertyExtractor(OWLOntology propertyOntology) {
		super();
		this.propertyOntology = propertyOntology;
	}
	/**
	 * @param propertyOntology
	 * @param mainOntology
	 */
	public PropertyExtractor(OWLOntology propertyOntology,
			OWLOntology mainOntology) {
		super();
		this.propertyOntology = propertyOntology;
		this.mainOntology = mainOntology;
	}
	/**
	 */
	public OWLOntology extractPropertyOntology() throws OWLOntologyCreationException {
		UUID uuid = UUID.randomUUID();
		IRI iri = IRI.create("http://purl.obolibrary.org/obo/temporary/"+uuid.toString());
		return extractPropertyOntology(iri);
	}

	/**
	 */
	public OWLOntology extractPropertyOntology(IRI newIRI) throws OWLOntologyCreationException {
		Set<OWLProperty> props = new HashSet<OWLProperty>();
		for (OWLObjectProperty p : mainOntology.getObjectPropertiesInSignature(true)) {
			LOG.info("mainOntology contains: "+p);
			props.add(p);
		}
		return extractPropertyOntology(newIRI, props);
	}
	
	public OWLOntology extractPropertyOntology(IRI newIRI, OWLOntology ont) throws OWLOntologyCreationException {
		mainOntology = ont;
		return extractPropertyOntology(newIRI);
	}
	
	/**
	 */
	public OWLOntology extractPropertyOntology(IRI newIRI, Set<OWLProperty> props) throws OWLOntologyCreationException {

		OWLGraphWrapper g = new OWLGraphWrapper(propertyOntology);
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();

		OWLDataFactory df = propertyOntology.getOWLOntologyManager().getOWLDataFactory();

		Set<OWLProperty> visited = new HashSet<OWLProperty>();
		while (props.size() > 0) {
			OWLProperty prop = props.iterator().next();
			props.remove(prop);
			LOG.info("Adding: "+prop);
			if (visited.contains(prop))
				continue;
			visited.add(prop);
			axioms.add(df.getOWLDeclarationAxiom(prop));

			axioms.addAll(propertyOntology.getAnnotationAssertionAxioms(prop.getIRI()));
			if (prop instanceof OWLObjectProperty) {
				OWLObjectProperty op = (OWLObjectProperty) prop;
				Set<OWLObjectPropertyAxiom> refAxioms = propertyOntology.getAxioms(op);
				axioms.addAll(refAxioms);
				for (OWLObjectPropertyExpression sp : op.getSuperProperties(propertyOntology)) {
					if (sp instanceof OWLObjectProperty) {
						props.add((OWLObjectProperty) sp);
					}
					else {
						props.addAll(sp.getObjectPropertiesInSignature());
					}
				}
				if (isExpansive) {
					for (OWLAxiom ra : refAxioms) {
						props.addAll(ra.getObjectPropertiesInSignature());
					}
				}
			}
			if (prop instanceof OWLAnnotationProperty) {
				OWLAnnotationProperty ap = (OWLAnnotationProperty) prop;
				axioms.addAll(propertyOntology.getAxioms(ap));
			}
			else {

			}

			// shorthand
			if (isCreateShorthand) {
				String id = g.getLabel(prop);
				if (id != null) {
					id = id.replaceAll(" ", "_");
					OWLAxiom ax = df.getOWLAnnotationAssertionAxiom(
							df.getOWLAnnotationProperty(IRI.create("http://www.geneontology.org/formats/oboInOwl#shorthand")),
							prop.getIRI(), 
							df.getOWLLiteral(id));
					axioms.add(ax);
					LOG.info(ax);
					String pid = Owl2Obo.getIdentifier(prop.getIRI());
					axioms.add(
							df.getOWLAnnotationAssertionAxiom(
									df.getOWLAnnotationProperty(IRI.create("http://www.geneontology.org/formats/oboInOwl#hasDbXref")),
									prop.getIRI(), 
									df.getOWLLiteral(pid))
					);
				}
				else {
					LOG.error("No label: "+prop);
				}
			}
		}
		OWLOntology xo;
		if (newIRI != null)
			xo = propertyOntology.getOWLOntologyManager().createOntology(newIRI);
		else
			xo = propertyOntology.getOWLOntologyManager().createOntology();
		xo.getOWLOntologyManager().addAxioms(xo, axioms);


		return xo;
	}



}
