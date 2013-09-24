package org.geneontology.lego.model2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.geneontology.lego.model.CellularLocationTools;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.graph.OWLGraphWrapper;

public class LegoTools {
	
	private final OWLGraphWrapper graph;
	private final OWLReasoner reasoner;
	private final Set<OWLObjectProperty> enabled_by;
	private final Set<OWLObjectProperty> occurs_in;
	private final Set<OWLObjectProperty> part_of;
	private final OWLClass mf;
	private final OWLClass bp;
	
	public LegoTools(OWLGraphWrapper graph, OWLReasoner reasoner) {
		this(graph, reasoner,
			findProperties(graph, "http://purl.obolibrary.org/obo/enabled_by"), // enabled_by
			findProperties(graph, 
					"http://purl.obolibrary.org/obo/BFO_0000066", // occurs_in 
					"http://purl.obolibrary.org/obo/occurs_in"),
			findProperties(graph, 
					"http://purl.obolibrary.org/obo/BFO_0000050", // part_of
					"http://purl.obolibrary.org/obo/part_of"),
			graph.getOWLClassByIdentifier("GO:0003674"), // molecular function 
			graph.getOWLClassByIdentifier("GO:0008150")); // biological process
	}

	protected LegoTools(OWLGraphWrapper graph, OWLReasoner reasoner,
			Set<OWLObjectProperty> enabled_by, Set<OWLObjectProperty> occurs_in,
			Set<OWLObjectProperty> part_of,
			OWLClass mf,
			OWLClass bp)
	{
		this.graph = graph;
		this.reasoner = reasoner;
		this.enabled_by = enabled_by;
		this.occurs_in = occurs_in;
		this.part_of = part_of;
		this.mf = mf;
		this.bp = bp;
	}
	
	private static Set<OWLObjectProperty> findProperties(OWLGraphWrapper graph, String...iris) {
		Set<OWLObjectProperty> properties = new HashSet<OWLObjectProperty>();
		for (String iri : iris) {
			properties.add(graph.getDataFactory().getOWLObjectProperty(IRI.create(iri)));
		}
		return properties;
	}

	public LegoGraph createLegoGraph(Collection<OWLNamedIndividual> individuals) {
		LegoGraph legoGraph = new LegoGraph();
		Map<IRI, OWLNamedIndividual> validNodes = new HashMap<IRI, OWLNamedIndividual>();
		for (OWLNamedIndividual individual : individuals) {
			final LegoUnit unit = createUnit(individual);
			if (unit != null) {
				legoGraph.units.add(unit);
				validNodes.put(unit.getId(), individual);
			}
		}
		
		for(LegoUnit unit : legoGraph.units) {
			Set<LegoLink> links = createLinks(unit, validNodes);
			legoGraph.links.addAll(links);
		}
		return legoGraph;
	}
	
	private Set<LegoLink> createLinks(LegoUnit unit, Map<IRI, OWLNamedIndividual> validNodes) {
		OWLNamedIndividual individiual = validNodes.get(unit.getId());
		Set<LegoLink> links = new HashSet<LegoLink>();
		Set<OWLObjectPropertyAssertionAxiom> propertyAxioms = getPropertyAxioms(individiual);
		for (OWLObjectPropertyAssertionAxiom axiom : propertyAxioms) {
			OWLIndividual object = axiom.getObject();
			if (object.isNamed() && validNodes.containsKey(object.asOWLNamedIndividual().getIRI())) {
				OWLObjectPropertyExpression property = axiom.getProperty();
				if (property.isAnonymous() == false) {
					OWLNamedIndividual namedTarget = object.asOWLNamedIndividual();
					OWLObjectProperty relation = property.asOWLObjectProperty();
					links.add(new LegoLink(unit.getId(), namedTarget.getIRI(), relation));
				}
			}
		}
		return links;
	}

	private LegoUnit createUnit(OWLNamedIndividual individual) {
		// check that there is a type
		OWLClass cls = getType(individual);
		if (cls == null) {
			return null;
		}
		
		// check that the type is a molecular function (activity)
		if (isMf(cls) == false) {
			return null;
		}
		OWLClass activity = cls;
		
		OWLClass enabledBy = null;
		OWLClass process = null;
		List<OWLClass> locations = new ArrayList<OWLClass>();
		
		// enabled_by and occurs_in
		Set<OWLClassAssertionAxiom> axioms = getClassAxioms(individual);
		for (OWLClassAssertionAxiom axiom : axioms) {
			OWLClassExpression expression = axiom.getClassExpression();
			if (expression instanceof OWLObjectSomeValuesFrom) {
				OWLObjectSomeValuesFrom object = (OWLObjectSomeValuesFrom) expression;
				OWLObjectPropertyExpression property = object.getProperty();
				OWLClassExpression clsExp = object.getFiller();
				if (enabled_by.contains(property) && !clsExp.isAnonymous()) {
					// active entity
					enabledBy = clsExp.asOWLClass();
				}
				else if (occurs_in.contains(property) && !clsExp.isAnonymous()) {
					// cellular location
					locations.add(clsExp.asOWLClass());
				}
			}
		}
		
		// part_of
		Set<OWLObjectPropertyAssertionAxiom> propertyAxioms = getPropertyAxioms(individual);
		for (OWLObjectPropertyAssertionAxiom axiom : propertyAxioms) {
			final OWLObjectPropertyExpression property = axiom.getProperty();
			if (part_of.contains(property)) {
				final OWLIndividual object = axiom.getObject();
				OWLClass type = getType(object);
				if (type != null && isBp(type)) {
					process = type;
					Set<OWLClassAssertionAxiom> processAxioms = getClassAxioms(object.asOWLNamedIndividual());
					for (OWLClassAssertionAxiom processAxiom : processAxioms) {
						OWLClassExpression expression = processAxiom.getClassExpression();
						if (expression instanceof OWLObjectSomeValuesFrom) {
							OWLObjectSomeValuesFrom someValuesFrom = (OWLObjectSomeValuesFrom) expression;
							OWLObjectPropertyExpression otherProperty = someValuesFrom.getProperty();
							OWLClassExpression clsExp = someValuesFrom.getFiller();
							if (occurs_in.contains(otherProperty) && !clsExp.isAnonymous()) {
								// cellular location
								locations.add(clsExp.asOWLClass());
							}
						}
					}
				}
			}
		}
		if (locations.isEmpty()) {
			OWLClassExpression searched = CellularLocationTools.searchCellularLocation(process, graph, occurs_in);
			if (searched != null && searched.isAnonymous() == false) {
				locations.add(searched.asOWLClass());
			}
		}
		
		LegoUnit unit = new LegoUnit(individual.getIRI(), enabledBy, activity , process, locations);
		return unit;
	}

	private OWLClass getType(OWLIndividual individual) {
		OWLClass type = null;
		if (individual.isNamed()) {
			Set<OWLClass> set = reasoner.getTypes(individual.asOWLNamedIndividual(), true).getFlattened();
			if (set.size() == 1) {
				OWLClass cls = set.iterator().next();
				if (cls.isBuiltIn() == false) {
					type = cls;
				}
			}
		}
		return type;
	}
	
	private boolean isMf(OWLClass cls) {
		final NodeSet<OWLClass> superClasses = reasoner.getSuperClasses(cls, false);
		return superClasses.containsEntity(mf) || mf.equals(cls);
	}
	
	private boolean isBp(OWLClass cls) {
		final NodeSet<OWLClass> superClasses = reasoner.getSuperClasses(cls, false);
		return superClasses.containsEntity(bp) || bp.equals(cls);
	}
	
	private Set<OWLClassAssertionAxiom> getClassAxioms(OWLNamedIndividual individual) {
		Set<OWLClassAssertionAxiom> allAxioms = new HashSet<OWLClassAssertionAxiom>();
		for(OWLOntology o : graph.getAllOntologies()) {
			allAxioms.addAll(o.getClassAssertionAxioms(individual));
		}
		return allAxioms;
	}
	
	private Set<OWLObjectPropertyAssertionAxiom> getPropertyAxioms(OWLNamedIndividual individual) {
		Set<OWLObjectPropertyAssertionAxiom> propertyAxioms = new HashSet<OWLObjectPropertyAssertionAxiom>();
		for(OWLOntology o : graph.getAllOntologies()) {
			propertyAxioms.addAll(o.getObjectPropertyAssertionAxioms(individual));
		}
		return propertyAxioms;
	}
	
}
