package owltools.gaf.lego.server.handler;

import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLObjectProperty;

import owltools.gaf.lego.LegoModelGenerator;
import owltools.gaf.lego.MolecularModelManager;
import owltools.gaf.lego.MolecularModelManager.UnknownIdentifierException;
import owltools.gaf.lego.server.handler.JsonOrJsonpBatchHandler.MissingParameterException;
import owltools.gaf.lego.server.handler.M3BatchHandler.M3Expression;
import owltools.graph.OWLGraphWrapper;
import owltools.vocab.OBOUpperVocabulary;


public class M3ExpressionParser {

	static OWLClassExpression parse(String modelId, M3Expression expression, MolecularModelManager m3)
			throws MissingParameterException, UnknownIdentifierException, OWLException {
		LegoModelGenerator model = m3.getModel(modelId);
		if (model == null) {
			throw new UnknownIdentifierException("Could not find a model for id: "+modelId);
		}
		OWLGraphWrapper g = new OWLGraphWrapper(model.getAboxOntology());
		return parse(g, expression, false);
	}
	
	private static OWLClassExpression parse(OWLGraphWrapper g, M3Expression expression, boolean createClasses)
			throws MissingParameterException, UnknownIdentifierException, OWLException {
		if (expression == null) {
			throw new MissingParameterException("Missing expression: null is not a valid expression.");
		}
		if ("class".equals(expression.type)) {
			if (expression.literal == null) {
				throw new MissingParameterException("Missing literal for expression of type 'class'");
			}
			OWLClass cls = g.getOWLClassByIdentifier(expression.literal);
			if (cls == null) {
				if (!createClasses) {
					throw new UnknownIdentifierException("Could not retrieve a class for literal: "+expression.literal);
				}
				cls = createClass(expression.literal, g);
			}
			return cls;
		}
		else if ("svf".equals(expression.type)) {
			if (expression.onProp == null) {
				throw new MissingParameterException("Missing onProp for expression of type 'svf'");
			}
			OWLObjectProperty p = g.getOWLObjectPropertyByIdentifier(expression.onProp);
			if (p == null) {
				throw new UnknownIdentifierException("Could not find a property for: "+expression.onProp);
			}
			if (p.getIRI().equals(OBOUpperVocabulary.GOREL_enabled_by.getIRI())) {
				// allow the creation of classes (i.e. Genes) for enabled_by
				createClasses = true;
			}
			if (expression.expressions != null && expression.expressions.length > 0) {
				OWLClassExpression ce = parse(g, expression.expressions, createClasses);
				return g.getDataFactory().getOWLObjectSomeValuesFrom(p, ce);
			}
			else if (expression.literal != null) {
				OWLClassExpression ce;
				if (expression.literal.contains(" ")) {
					ce = MolecularModelManager.parseClassExpression(expression.literal, g);
				}
				else {
					ce = g.getOWLClassByIdentifier(expression.literal);
					if (ce == null) {
						if (!createClasses) {
							throw new MissingParameterException("Could not retrieve a class for literal: "+expression.literal);
						}
						ce = createClass(expression.literal, g);
					}
				}
				return g.getDataFactory().getOWLObjectSomeValuesFrom(p, ce);
			}
			else {
				throw new MissingParameterException("Missing literal or expression for expression of type 'svf'.");
			}
		}
		else {
			throw new UnknownIdentifierException("Unknown expression type: "+expression.type);
		}
	}
	
	private static OWLClassExpression parse(OWLGraphWrapper g, M3Expression[] expressions, boolean createClasses)
			throws MissingParameterException, UnknownIdentifierException, OWLException {
		if (expressions.length == 0) {
			throw new MissingParameterException("Missing expressions: empty expression list are not allowed.");
		}
		if (expressions.length == 1) {
			return parse(g, expressions[0], createClasses);	
		}
		Set<OWLClassExpression> clsExpressions = new HashSet<OWLClassExpression>();
		for (M3Expression m3Expression : expressions) {
			OWLClassExpression ce = parse(g, m3Expression, createClasses);
			clsExpressions.add(ce);
		}
		return g.getDataFactory().getOWLObjectIntersectionOf(clsExpressions);
	}
	
	private static OWLClass createClass(String id, OWLGraphWrapper g) {
		IRI iri = g.getIRIByIdentifier(id);
		return g.getDataFactory().getOWLClass(iri);
	}
}
