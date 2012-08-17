package org.geneontology.lego.dot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;

/**
 * Rudimentary implementation of a DOT writer for the LEGO annotations in OWL.
 * 
 * Uses the 'docs/lego-owl-mappings.txt' file as guide-line.
 */
public abstract class LegoDotWriter {

	private final OWLGraphWrapper graph;
	private final OWLReasoner reasoner;
	private final Set<OWLObjectProperty> occurs_in;
	private final Set<OWLObjectProperty> enabled_by;

	public static class UnExpectedStructureException extends Exception {

		// generated
		private static final long serialVersionUID = -3343544020570925182L;

		/**
		 * @param message
		 * @param cause
		 */
		public UnExpectedStructureException(String message, Throwable cause) {
			super(message, cause);
		}

		/**
		 * @param message
		 */
		public UnExpectedStructureException(String message) {
			super(message);
		}
		
	}
	
	/**
	 * Create a new writer for the given ontology and reasoner.
	 * 
	 * @param graph
	 * @param reasoner
	 */
	public LegoDotWriter(OWLGraphWrapper graph, OWLReasoner reasoner) {
		super();
		this.graph = graph;
		this.reasoner = reasoner;
		this.enabled_by = findProperties(graph, "http://purl.obolibrary.org/obo/enabled_by"); // enabled_by
		this.occurs_in = findProperties(graph, "http://purl.obolibrary.org/obo/BFO_0000066", "http://purl.obolibrary.org/obo/occurs_in"); // occurs_in
	}
	
	private static Set<OWLObjectProperty> findProperties(OWLGraphWrapper graph, String...iris) {
		Set<OWLObjectProperty> properties = new HashSet<OWLObjectProperty>();
		for (String iri : iris) {
			properties.add(graph.getDataFactory().getOWLObjectProperty(IRI.create(iri)));
		}
		return properties;
	}
	
	/**
	 * Render a dot file for the given individuals (aka set of annotations).
	 * 
	 * @param individuals
	 * @param name name of the graph to be used in the dot file (optional)
	 * @param renderKey
	 * @throws IOException
	 * @throws UnExpectedStructureException thrown, if there are unexpected axioms.
	 */
	public void renderDot(Collection<OWLNamedIndividual> individuals, String name, boolean renderKey)
			throws IOException, UnExpectedStructureException {

		try {
			open();
			// start dot
			if (name == null) {
				appendLine("digraph {");
			} else {
				appendLine("digraph " + quote(name) + " {");
			}

			// individual nodes
			Map<String, String> legend = new HashMap<String, String>();
			for (OWLNamedIndividual individual : individuals) {
				renderIndividualsNode(individual, legend);
			}
			if (!legend.isEmpty() && renderKey) {
				appendLine("");
				appendLine("// Key / Legend",1);
				appendLine("subgraph {", 1);
				List<String> legendKeys = new ArrayList<String>(legend.keySet());
				Collections.sort(legendKeys);
				CharSequence prev = null;
				for(String relName : legendKeys) {
					final CharSequence a = quote("legend_"+relName+"_A");
					final CharSequence b = quote("legend_"+relName+"_B");
					
					appendLine(a+"[shape=plaintext,label=\"\"];", 2);
					appendLine(b+"[shape=plaintext,label="+quote(relName)+"];", 2);
					appendLine(a+" -> "+b+" "+legend.get(relName)+";", 2);
					appendLine("");
					if (prev != null) {
						appendLine("// create invisibe edge for top down order", 2);
						appendLine(prev+" -> "+a+" [style=invis];", 2);
						appendLine("");
					}
					prev = b;
				}
				appendLine("}", 1);
			}

			// end dot
			appendLine("}");
		} finally {
			close();
		}
		
	}
	
	private void renderIndividualsNode(OWLNamedIndividual individual, Map<String, String> legend) throws IOException, UnExpectedStructureException {
		
		OWLPrettyPrinter owlpp = new OWLPrettyPrinter(graph);
		OWLOntology sourceOntology = graph.getSourceOntology();
		
		Set<OWLClassAssertionAxiom> axioms = sourceOntology.getClassAssertionAxioms(individual);
		LegoIndividualType type = getIndividualType(individual, axioms);
		if (LegoIndividualType.Unknown == type) {
			throw new UnExpectedStructureException("Could not determine lego type for individual: "+individual+" with Axioms: "+axioms);
		}
		else if (LegoIndividualType.MolecularAnnotation == type) {
			// annoton
			
			OWLClass typeClass = getType(individual);
			OWLClass molecularFunction = typeClass;
			OWLClass activeEntity = null;
			List<OWLClassExpression> cellularLocations = new ArrayList<OWLClassExpression>();
			
			for (OWLClassAssertionAxiom axiom : axioms) {
				OWLClassExpression expression = axiom.getClassExpression();
				if (expression.isClassExpressionLiteral()) {
					// assume it's molecularFunction
					// ignore, use reasoner to retrieve type
				}
				else if (expression instanceof OWLObjectSomeValuesFrom) {
					OWLObjectSomeValuesFrom object = (OWLObjectSomeValuesFrom) expression;
					OWLObjectPropertyExpression property = object.getProperty();
					OWLClassExpression clsExp = object.getFiller();
					if (enabled_by.contains(property) && !clsExp.isAnonymous()) {
						// active entity
						if (activeEntity != null) {
							throw new UnExpectedStructureException("The individual: "+owlpp.render(individual)+" has multiple 'enabled_by' declarations.");
						}
						activeEntity = clsExp.asOWLClass();
					}
					else if (occurs_in.contains(property)) {
						// cellular location
						cellularLocations.add(clsExp);
					}
				}
			}
			
			if (cellularLocations.isEmpty()) {
				// check super classes for cellular location information
				OWLClassExpression cellularLocation = searchCellularLocation(typeClass);
				if (cellularLocation != null) {
					cellularLocations.add(cellularLocation);
				}
			}
			
			String label;
			// render node
			if (molecularFunction == null) {
				label="?";
			}
			else {
				label = graph.getLabelOrDisplayId(molecularFunction);
			}
			
			StringBuilder line = new StringBuilder(nodeId(individual));
			line.append(" [shape=plaintext,label=");
			line.append('<'); // start HTML markup
			line.append("<TABLE BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\"><TR>");
			
			if (activeEntity != null) {
				// render activeEntity as box on top of the activity 
				line.append("<TD>").append(graph.getLabelOrDisplayId(activeEntity)).append("</TD></TR><TR>");
			}
			
			line.append("<TD BGCOLOR=\"lightblue\" COLSPAN=\"2\">").append(label).append("</TD>");
			for(OWLClassExpression cellularLocation : cellularLocations) {
				String location;
				if (!cellularLocation.isAnonymous()) {
					location = graph.getLabelOrDisplayId(cellularLocation.asOWLClass());
				}
				else {
					location = owlpp.render(cellularLocation);
				}
				line.append("<TD BGCOLOR=\"yellow\">").append(location).append("</TD>");
			}
			line.append("</TR></TABLE>");
			line.append('>'); // end HTML markup
			line.append("];");
			
			appendLine("");
			appendLine("// annoton", 1);
			appendLine(line, 1);
			
		}
		else if (LegoIndividualType.MolecularContext == type) {
			// context
			
			OWLClass parentClass = getType(individual);
//			OWLClass cellularLocation = null;
//			OWLClass cellType = null;
//			OWLClass grossAnatomy = null;
//			OWLClass organism = null;
//			
//			
//			for (OWLClassAssertionAxiom axiom : axioms) {
//				OWLClassExpression expression = axiom.getClassExpression();
//				if (expression.isClassExpressionLiteral()) {
//					// assume it's parentClass
//					// ignore, use reasoner to retrieve type
//				}
//				else if (expression instanceof OWLObjectSomeValuesFrom) {
//					OWLObjectSomeValuesFrom object = (OWLObjectSomeValuesFrom) expression;
//					OWLObjectPropertyExpression property = object.getProperty();
//					if (occurs_in.contains(property)) {
//						OWLClassExpression clsExp = object.getFiller();
//						if (clsExp.isAnonymous()) {
//							// TODO implement this part of the spec and test with a new example
//							/*
//							 * ClassAssertion(
//								 :occurs_in SOME (L AND
//								                   :part_of SOME (CT AND
//								                                     :part_of SOME (A AND
//								                                                      :part_of some O)))
//								 I)
//							 */
//						}
//					}
//				}
//			}
			
			String label;
			// render node
			if (parentClass == null) {
				label="?";
			}
			else {
				label = graph.getLabelOrDisplayId(parentClass);
			}
			
			StringBuilder line = new StringBuilder(nodeId(individual));
			line.append(" [shape=plaintext,label=");
			line.append('<'); // start HTML markup
			line.append("<TABLE BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\">");
			line.append("<TR><TD>").append(label).append("</TD></TR>");
			line.append("</TABLE>");
			line.append('>'); // end HTML markup
			line.append("];");
			
			appendLine("");
			appendLine("// context", 1);
			appendLine(line, 1);
		}
		
		
		// render links
		Set<OWLObjectPropertyAssertionAxiom> propertyAxioms = sourceOntology.getObjectPropertyAssertionAxioms(individual);
		for (OWLObjectPropertyAssertionAxiom propertyAxiom : propertyAxioms) {
			OWLIndividual object = propertyAxiom.getObject();
			if (object instanceof OWLNamedIndividual == false) {
				throw new UnExpectedStructureException("Expected a named individual for a link: "+propertyAxiom);
			}
			OWLNamedIndividual namedTarget = (OWLNamedIndividual) object;
			OWLObjectPropertyExpression property = propertyAxiom.getProperty();
			String linkLabel = graph.getLabelOrDisplayId(property);
			if ("directly_inhibits".equals(linkLabel)) {
				appendLine("");
				appendLine("// edge", 1);
				appendLine(nodeId(individual)+" -> "+nodeId(namedTarget)+" [arrowhead=tee];", 1);
				if (!legend.containsKey("directly_inhibits")) {
					legend.put("directly_inhibits", "[arrowhead=tee]");
				}
			}
			else if ("part_of".equals(linkLabel) || "part of".equals(linkLabel)) {
				appendLine("");
				appendLine("// edge", 1);
				appendLine(nodeId(individual)+" -> "+nodeId(namedTarget)+" [color=\"#C0C0C0\"];", 1);
				if (!legend.containsKey("part_of")) {
					legend.put("part_of", "[color=\"#C0C0C0\"]");
				}
			}
			else if ("directly_activates".equals(linkLabel)) {
				appendLine("");
				appendLine("// edge", 1);
				appendLine(nodeId(individual)+" -> "+nodeId(namedTarget)+" [arrowhead=open];", 1);
				if (!legend.containsKey("directly_activates")) {
					legend.put("directly_activates", "[arrowhead=open]");
				}
			}
			else {
				appendLine("");
				appendLine("// edge", 1);
				appendLine(nodeId(individual)+" -> "+nodeId(namedTarget)+" [label="+quote(linkLabel)+"];", 1);
			}
		}
	}
	
	private OWLClassExpression searchCellularLocation(OWLClass cls) {
		Queue<OWLClass> queue = new Queue<OWLClass>();
		queue.add(cls);
		return searchCellularLocation(queue);
	}
	
	private OWLClassExpression searchCellularLocation(Queue<OWLClass> queue) {
		if (queue.isEmpty()) {
			return null;
		}
		List<OWLClass> nextLevel = new ArrayList<OWLClass>();
		while(!queue.isEmpty()) {
			OWLClass cls = queue.pop();
			for (OWLOntology ontology : graph.getAllOntologies()) {
				
				// equivalent classes
				Set<OWLEquivalentClassesAxiom> eqAxioms = ontology.getEquivalentClassesAxioms(cls);
				for (OWLEquivalentClassesAxiom axiom : eqAxioms) {
					Set<OWLClassExpression> expressions = axiom.getClassExpressionsMinus(cls);
					for (OWLClassExpression ce : expressions) {
						if (!ce.isAnonymous()) {
							nextLevel.add(ce.asOWLClass());
						}
						else if (ce instanceof OWLObjectSomeValuesFrom) {
							OWLObjectSomeValuesFrom expr = (OWLObjectSomeValuesFrom) ce;
							OWLObjectPropertyExpression propertyExpression = expr.getProperty();
							OWLClassExpression filler = expr.getFiller();
							if (occurs_in.contains(propertyExpression)) {
								return filler;
							}
							if (!filler.isAnonymous()) {
								nextLevel.add(filler.asOWLClass());
							}
						}
					}
				}
				
				// super classes
				Set<OWLSubClassOfAxiom> subAxioms = ontology.getSubClassAxiomsForSubClass(cls);
				for (OWLSubClassOfAxiom axiom : subAxioms) {
					OWLClassExpression ce = axiom.getSuperClass();
					if (!ce.isAnonymous()) {
						nextLevel.add(ce.asOWLClass());
					}
					else if (ce instanceof OWLObjectSomeValuesFrom) {
						OWLObjectSomeValuesFrom expr = (OWLObjectSomeValuesFrom) ce;
						OWLObjectPropertyExpression propertyExpression = expr.getProperty();
						OWLClassExpression filler = expr.getFiller();
						if (occurs_in.contains(propertyExpression)) {
							return filler;
						}
						if (!filler.isAnonymous()) {
							nextLevel.add(filler.asOWLClass());
						}
					}
				}
			}
		}
		queue.addAll(nextLevel);
		return searchCellularLocation(queue);
	}
	
	private static class Queue<T> {
		
		private final Set<T> visited = new HashSet<T>();
		private final LinkedList<T> list = new LinkedList<T>();
		
		public synchronized T pop() {
			return list.removeFirst();
		}
		
		public synchronized boolean isEmpty() {
			return list.isEmpty();
		}
		
		public synchronized void addAll(Collection<T> c) {
			for (T t : c) {
				if (!visited.contains(t)) {
					list.add(t);
					visited.add(t);
				}
			}
		}
		
		public synchronized void add(T t) {
			if (!visited.contains(t)) {
				list.add(t);
				visited.add(t);
			}
		}
	}
	
	private OWLClass getType(OWLNamedIndividual individual) throws UnExpectedStructureException {
		NodeSet<OWLClass> types = reasoner.getTypes(individual, true);
		if (types.isEmpty() || types.isBottomSingleton() || types.isTopSingleton()) {
			return null;
		}
		Set<OWLClass> set = types.getFlattened();
		if (set.size() != 1) {
			throw new UnExpectedStructureException("Too many types for the Individual: "+individual+" types: "+types);
		}
		return set.iterator().next();
		
	}
	
	private CharSequence nodeId(OWLNamedIndividual individual) {
		return nodeId(individual.getIRI());
	}
	
	private CharSequence nodeId(IRI iri) {
		String iriString = iri.toString();
		return quote(iriString);
	}
	
	private static enum LegoIndividualType {
		MolecularAnnotation,
		MolecularContext,
		Unknown
	}
	
	private LegoIndividualType getIndividualType(OWLNamedIndividual individual, Set<OWLClassAssertionAxiom> axioms) {
		boolean hasEnabledBy = false;
		boolean hasLiteral = false;
		for (OWLClassAssertionAxiom axiom : axioms) {
			OWLClassExpression expression = axiom.getClassExpression();
			if (expression.isClassExpressionLiteral()) {
				hasLiteral = true;
			} else if (expression instanceof OWLObjectSomeValuesFrom) {
				OWLObjectSomeValuesFrom object = (OWLObjectSomeValuesFrom) expression;
				OWLObjectPropertyExpression property = object.getProperty();
				if (enabled_by.contains(property)) {
					hasEnabledBy = true;
				}
			}
		}
		if (hasLiteral == false ) {
			// literal is required
			return LegoIndividualType.Unknown;
		}
		if (hasEnabledBy) {
			return LegoIndividualType.MolecularAnnotation;
		}
		return LegoIndividualType.MolecularContext;
	}

	private CharSequence quote(CharSequence cs) {
		StringBuilder sb = new StringBuilder();
		sb.append('"');
		sb.append(cs);
		sb.append('"');
		return sb;
	}
	
	private void appendLine(CharSequence line, int indent) throws IOException {
		if (indent <= 0) {
			appendLine(line);
		} else {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < indent; i++) {
				sb.append("    ");
			}
			sb.append(line);
			appendLine(sb);
		}
	}

	/**
	 * Overwrite this method to implement a custom open() routine for the writer.
	 * 
	 * @throws IOException
	 */
	protected void open() throws IOException {
		// default: do nothing
	}
	
	/**
	 * Write a line to the file. It is expected that the writer appends the
	 * appropriate newlines. They are not part of the line.
	 * 
	 * @param line
	 * @throws IOException
	 */
	protected abstract void appendLine(CharSequence line) throws IOException;
	
	
	/**
	 * Overwrite this method to implement a custom close() function for the writer.
	 * Guaranteed to be called, also in case of an Exception (try-finally pattern).
	 */
	protected void close() {
		// default: do nothing
	}

}
