package owltools.gaf.lego;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxEditorParser;
import org.semanticweb.owlapi.expression.OWLEntityChecker;
import org.semanticweb.owlapi.expression.ParserException;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;

import owltools.graph.OWLGraphWrapper;


/**
 * Wrapper for parsing OWL Manchester Syntax using a {@link OWLGraphWrapper}.
 * This is a simplified re-implementation.
 */
public class ManchesterSyntaxTool {

	private final OWLDataFactory dataFactory;
	private final OWLEntityChecker entityChecker;
	private final Map<String, OWLClass> createdClassesMap;

	/**
	 * Create new instance.
	 * 
	 * @param graph
	 * @param createClasses if set to true, classes are generated even if they are not declared.
	 */
	public ManchesterSyntaxTool(OWLGraphWrapper graph, boolean createClasses) {
		super();
		this.dataFactory = graph.getDataFactory();
		createdClassesMap = new HashMap<String, OWLClass>();
		entityChecker = new AdvancedEntityChecker(graph, createClasses, createdClassesMap);
	}

	/**
	 * @return the createdClasses
	 */
	public Map<String, OWLClass> getCreatedClasses() {
		return Collections.unmodifiableMap(createdClassesMap);
	}

	/**
	 * Parse a class expression in Manchester syntax. 
	 * 
	 * @param expression
	 * @return {@link OWLClassExpression}
	 * @throws ParserException
	 */
	public OWLClassExpression parseManchesterExpression(String expression) throws ParserException {
		ManchesterOWLSyntaxEditorParser parser = createParser(expression);
		OWLClassExpression ce = parser.parseClassExpression();
		return ce;
	}


	private ManchesterOWLSyntaxEditorParser createParser(String expression) {
		ManchesterOWLSyntaxEditorParser parser = 
			new ManchesterOWLSyntaxEditorParser(dataFactory, expression);

		parser.setOWLEntityChecker(entityChecker);

		return parser;
	}

	static class AdvancedEntityChecker implements OWLEntityChecker {

		private final OWLGraphWrapper graph;
		private final boolean createClasses;
		private final Map<String, OWLClass> createdClassesMap;

		AdvancedEntityChecker(OWLGraphWrapper graph, boolean createClasses, 
				Map<String, OWLClass> createdClassesMap) {
			super();
			this.graph = graph;
			this.createClasses = createClasses;
			this.createdClassesMap = createdClassesMap;
		}

		public OWLClass getOWLClass(String name) {
			if (name.length() < 2) {
				return null;
			}
			OWLObject owlObject;
			if (name.charAt(0) == '\'') {
				name = trimQuotes(name);
				owlObject = graph.getOWLObjectByLabel(name);
			}
			else {
				owlObject = graph.getOWLObjectByIdentifier(name);
				if (owlObject == null) {
					owlObject = graph.getOWLObjectByLabel(name);
				}
			}
			if (owlObject != null) {
				if (owlObject instanceof OWLClass) {
					return (OWLClass) owlObject;
				}
				return null;
			}
			if (name.startsWith("http:")) {
				IRI iri = IRI.create(name);
				owlObject = graph.getOWLObject(iri);
				if (owlObject != null) {
					if (owlObject instanceof OWLClass) {
						return (OWLClass) owlObject;
					}
					return null;
				}
				OWLClass c = null;
				if (createClasses) {
					String id = graph.getIdentifier(iri);
					c = createdClassesMap.get(id);
					if (c == null) {
						c = graph.getDataFactory().getOWLClass(iri);
						createdClassesMap.put(name, c);
					}
				}
				return c;
			}
			else {
				if (createClasses && !StringUtils.contains(name, ' ')) {
					OWLClass c = createdClassesMap.get(name);
					if (c == null) {
						IRI iri = graph.getIRIByIdentifier(name);
						c = graph.getDataFactory().getOWLClass(iri);
						createdClassesMap.put(name, c);
					}
					return c;
				}
			}
			return null;
		}

		public OWLObjectProperty getOWLObjectProperty(String name) {
			if (name.length() < 2) {
				return null;
			}
			name = trimQuotes(name);
			OWLObjectProperty p = null;
			if (StringUtils.contains(name, ' ') == false) {
				p = graph.getOWLObjectPropertyByIdentifier(name);
				if (p == null) {
					p = graph.getOWLObjectProperty(name);
				}
			}
			if (p == null) {
				graph.getIRIByLabel(name);
				OWLObject owlObject = graph.getOWLObjectByLabel(name);
				if (owlObject != null && owlObject instanceof OWLObjectProperty) {
					p = (OWLObjectProperty) owlObject;
				}
			}
			return p;
		}
		
		private String trimQuotes(String s) {
			if (s.startsWith("'") && s.endsWith("'")) {
				s = s.substring(1, s.length() - 1);
			}
			return s;
		}

		public OWLDataProperty getOWLDataProperty(String name) {
			return null;
		}

		public OWLNamedIndividual getOWLIndividual(String name) {
			OWLNamedIndividual i = graph.getOWLIndividualByIdentifier(name);
			if (i == null) {
				i = graph.getOWLIndividual(name);
			}
			return i;
		}

		public OWLDatatype getOWLDatatype(String name) {
			return null;
		}

		public OWLAnnotationProperty getOWLAnnotationProperty(String name) {
			return null;
		}

	}
}
