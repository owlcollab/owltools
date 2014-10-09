package owltools.gaf.lego.server.handler;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.semanticweb.owlapi.expression.ParserException;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLObjectProperty;

import owltools.gaf.lego.ManchesterSyntaxTool;
import owltools.gaf.lego.MolecularModelManager;
import owltools.gaf.lego.MolecularModelManager.UnknownIdentifierException;
import owltools.gaf.lego.server.external.ExternalLookupService;
import owltools.gaf.lego.server.external.ExternalLookupService.LookupEntry;
import owltools.gaf.lego.server.handler.JsonOrJsonpBatchHandler.MissingParameterException;
import owltools.gaf.lego.server.handler.M3BatchHandler.M3Expression;
import owltools.gaf.lego.server.handler.M3BatchHandler.M3ExpressionType;
import owltools.graph.OWLGraphWrapper;
import owltools.util.ModelContainer;

public class M3ExpressionParser {

	static OWLClassExpression parse(String modelId, M3Expression expression, 
			MolecularModelManager<?> m3,
			ExternalLookupService externalLookupService)
			throws MissingParameterException, UnknownIdentifierException, OWLException {
		ModelContainer model = m3.checkModelId(modelId);
		OWLGraphWrapper g = new OWLGraphWrapper(model.getAboxOntology());
		return parse(g, expression, externalLookupService);
	}
	
	static OWLClassExpression parse(OWLGraphWrapper g, M3Expression expression,
			ExternalLookupService externalLookupService)
			throws MissingParameterException, UnknownIdentifierException, OWLException {
		if (expression == null) {
			throw new MissingParameterException("Missing expression: null is not a valid expression.");
		}
		if (expression.type == null) {
			throw new MissingParameterException("An expression type is required.");
		}
		if (M3ExpressionType.clazz.getLbl().equals(expression.type)) {
			if (expression.literal == null) {
				throw new MissingParameterException("Missing literal for expression of type 'class'");
			}
			OWLClass cls = g.getOWLClassByIdentifier(expression.literal);
			if (cls == null) {
					throw new UnknownIdentifierException("Could not retrieve a class for literal: "+expression.literal);
			}
			return cls;
		}
		else if (M3ExpressionType.svf.getLbl().equals(expression.type)) {
			if (expression.onProp == null) {
				throw new MissingParameterException("Missing onProp for expression of type 'svf'");
			}
			OWLObjectProperty p = g.getOWLObjectPropertyByIdentifier(expression.onProp);
			if (p == null) {
				throw new UnknownIdentifierException("Could not find a property for: "+expression.onProp);
			}
			if (expression.expressions != null && expression.expressions.length > 0) {
				OWLClassExpression ce = parse(g, expression.expressions, externalLookupService, M3ExpressionType.intersection);
				return g.getDataFactory().getOWLObjectSomeValuesFrom(p, ce);
			}
			else if (expression.literal != null) {
				OWLClassExpression ce;
				if (expression.literal.contains(" ")) {
					ce = parseClassExpression(expression.literal, g, true , externalLookupService);
				}
				else {
					ce = g.getOWLClassByIdentifier(expression.literal);
					if (ce == null) {
						if (externalLookupService != null) {
							List<LookupEntry> lookup = externalLookupService.lookup(expression.literal);
							if (lookup == null || lookup.isEmpty()) {
								throw new UnknownIdentifierException("Could not validate the id: "+expression.literal);
							}
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
		else if (M3ExpressionType.intersection.getLbl().equals(expression.type)) {
			return parse(g, expression.expressions, externalLookupService, M3ExpressionType.intersection);
		}
		else if (M3ExpressionType.union.getLbl().equals(expression.type)) {
			return parse(g, expression.expressions, externalLookupService, M3ExpressionType.union);
		}
		else {
			throw new UnknownIdentifierException("Unknown expression type: "+expression.type);
		}
	}
	
	private static OWLClassExpression parse(OWLGraphWrapper g, M3Expression[] expressions, 
			ExternalLookupService externalLookupService, M3ExpressionType type)
			throws MissingParameterException, UnknownIdentifierException, OWLException {
		if (expressions.length == 0) {
			throw new MissingParameterException("Missing expressions: empty expression list is not allowed.");
		}
		if (expressions.length == 1) {
			return parse(g, expressions[0], externalLookupService);	
		}
		Set<OWLClassExpression> clsExpressions = new HashSet<OWLClassExpression>();
		for (M3Expression m3Expression : expressions) {
			OWLClassExpression ce = parse(g, m3Expression, externalLookupService);
			clsExpressions.add(ce);
		}
		if (type == M3ExpressionType.union) {
			return g.getDataFactory().getOWLObjectUnionOf(clsExpressions);
		}
		return g.getDataFactory().getOWLObjectIntersectionOf(clsExpressions);
	}
	
	private static OWLClass createClass(String id, OWLGraphWrapper g) {
		IRI iri = g.getIRIByIdentifier(id);
		return g.getDataFactory().getOWLClass(iri);
	}
	
	static OWLClassExpression parseClassExpression(String expression, OWLGraphWrapper g, 
			boolean createClasses, ExternalLookupService externalLookupService) 
			throws OWLException, UnknownIdentifierException {
		try {
			ManchesterSyntaxTool syntaxTool = new ManchesterSyntaxTool(g, createClasses);
			OWLClassExpression clsExpr = syntaxTool.parseManchesterExpression(expression);
			if (externalLookupService != null) {
				Map<String, OWLClass> createdClasses = syntaxTool.getCreatedClasses();
				for (Entry<String, OWLClass> createdEntry : createdClasses.entrySet()) {
					String id = createdEntry.getKey();
					List<LookupEntry> lookup = externalLookupService.lookup(id); // TODO taxon
					if (lookup == null || lookup.isEmpty()) {
						throw new UnknownIdentifierException("Could not validate the id: "+id+" in expression: "+expression);
					}
				}
			}
			return clsExpr;
		}
		catch (ParserException e) {
			// wrap in an Exception (not RuntimeException) to enable proper error handling
			throw new OWLException("Could not parse expression: \""+expression+"\"", e) {

				private static final long serialVersionUID = -9158071212925724138L;
			};
		}
	}
}
