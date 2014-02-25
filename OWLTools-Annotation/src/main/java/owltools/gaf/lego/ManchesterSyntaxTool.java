package owltools.gaf.lego;

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

	private OWLDataFactory dataFactory;
	private OWLEntityChecker entityChecker;

	/**
	 * Create new instance.
	 * 
	 * @param graph
	 * @param createClasses if set to true, classes are generated even if they are not declared.
	 */
	public ManchesterSyntaxTool(OWLGraphWrapper graph, boolean createClasses) {
		super();
		this.dataFactory = graph.getDataFactory();
		entityChecker = new AdvancedEntityChecker(graph, createClasses);
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

		AdvancedEntityChecker(OWLGraphWrapper graph, boolean createClasses) {
			super();
			this.graph = graph;
			this.createClasses = createClasses;
		}

		public OWLClass getOWLClass(String name) {
			if (name.length() < 2) {
				return null;
			}
			name = trimQuotes(name);
			OWLObject owlObject = graph.getOWLObjectByIdentifier(name);
			if (owlObject == null) {
				owlObject = graph.getOWLObjectByLabel(name);
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
					c = graph.getDataFactory().getOWLClass(iri);
				}
				return c;
			}
			else {
				if (createClasses && !StringUtils.contains(name, ' ')) {
					return graph.getDataFactory().getOWLClass(graph.getIRIByIdentifier(name));
				}
			}
			return null;
		}

		public OWLObjectProperty getOWLObjectProperty(String name) {
			if (name.length() < 2) {
				return null;
			}
			name = trimQuotes(name);
			OWLObjectProperty p = graph.getOWLObjectPropertyByIdentifier(name);
			if (p == null) {
				p = graph.getOWLObjectProperty(name);
			}
			if (p == null) {
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
