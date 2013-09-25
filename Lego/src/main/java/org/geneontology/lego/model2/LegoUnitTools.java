package org.geneontology.lego.model2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.geneontology.lego.model.CellularLocationTools;
import org.geneontology.lego.model.LegoNode;
import org.geneontology.lego.model.LegoTools;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.graph.OWLGraphWrapper;

public class LegoUnitTools extends LegoTools {
	
	public LegoUnitTools(OWLGraphWrapper graph, OWLReasoner reasoner) {
		super(graph, reasoner);
	}

	
	public LegoGraph createLegoGraph(Collection<OWLNamedIndividual> individuals) throws UnExpectedStructureException {
		List<LegoUnit> units = new ArrayList<LegoUnit>();
		for (OWLNamedIndividual individual : individuals) {
			final LegoUnit unit = createUnit(individual);
			if (unit != null) {
				units.add(unit);
			}
		}
		List<LegoNode> nodes = createLegoNodes(individuals);
		
		LegoGraph legoGraph = new LegoGraph(units, nodes);
		return legoGraph;
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
