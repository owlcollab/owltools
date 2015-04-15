package owltools.gaf.lego.server.handler;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLObjectProperty;

import owltools.gaf.lego.MolecularModelManager;
import owltools.gaf.lego.MolecularModelManager.UnknownIdentifierException;
import owltools.gaf.lego.json.JsonOwlObject;
import owltools.gaf.lego.json.JsonOwlObject.JsonOwlObjectType;
import owltools.gaf.lego.server.external.ExternalLookupService;
import owltools.gaf.lego.server.external.ExternalLookupService.LookupEntry;
import owltools.gaf.lego.server.handler.OperationsImpl.MissingParameterException;
import owltools.graph.OWLGraphWrapper;
import owltools.util.ModelContainer;

public class M3ExpressionParser {
	
	private final boolean checkLiteralIds;
	
	M3ExpressionParser(boolean checkLiteralIds) {
		this.checkLiteralIds = checkLiteralIds;
	}
	
	M3ExpressionParser() {
		this(true);
	}

	OWLClassExpression parse(String modelId, JsonOwlObject expression, 
			MolecularModelManager<?> m3,
			ExternalLookupService externalLookupService)
			throws MissingParameterException, UnknownIdentifierException, OWLException {
		ModelContainer model = m3.checkModelId(modelId);
		OWLGraphWrapper g = new OWLGraphWrapper(model.getAboxOntology());
		return parse(g, expression, externalLookupService);
	}
	
	OWLClassExpression parse(OWLGraphWrapper g, JsonOwlObject expression,
			ExternalLookupService externalLookupService)
			throws MissingParameterException, UnknownIdentifierException, OWLException {
		if (expression == null) {
			throw new MissingParameterException("Missing expression: null is not a valid expression.");
		}
		if (expression.type == null) {
			throw new MissingParameterException("An expression type is required.");
		}
		if (JsonOwlObjectType.Class == expression.type) {
			if (expression.id == null) {
				throw new MissingParameterException("Missing literal for expression of type 'class'");
			}
			if (StringUtils.containsWhitespace(expression.id)) {
				throw new UnknownIdentifierException("Identifiers may not contain whitespaces: '"+expression.id+"'");
			}
			OWLClass cls;
			if (checkLiteralIds) {
				cls = g.getOWLClassByIdentifier(expression.id);
				if (cls == null && externalLookupService != null) {
					List<LookupEntry> lookup = externalLookupService.lookup(expression.id);
					if (lookup == null || lookup.isEmpty()) {
						throw new UnknownIdentifierException("Could not validate the id: "+expression.id);
					}
					cls = createClass(expression.id, g);
				}
				if (cls == null) {
					throw new UnknownIdentifierException("Could not retrieve a class for id: "+expression.id);
				}
			}
			else {
				cls = createClass(expression.id, g);
			}
			return cls;
		}
		else if (JsonOwlObjectType.SomeValueFrom == expression.type) {
			if (expression.property == null) {
				throw new MissingParameterException("Missing onProperty for expression of type 'svf'");
			}
			OWLObjectProperty p = g.getOWLObjectPropertyByIdentifier(expression.property);
			if (p == null) {
				throw new UnknownIdentifierException("Could not find a property for: "+expression.property);
			}
			if (expression.filler != null) {
				OWLClassExpression ce = parse(g, expression.filler, externalLookupService);
				return g.getDataFactory().getOWLObjectSomeValuesFrom(p, ce);
			}
			else {
				throw new MissingParameterException("Missing literal or expression for expression of type 'svf'.");
			}
		}
		else if (JsonOwlObjectType.IntersectionOf == expression.type) {
			return parse(g, expression.expressions, externalLookupService, JsonOwlObjectType.IntersectionOf);
		}
		else if (JsonOwlObjectType.UnionOf == expression.type) {
			return parse(g, expression.expressions, externalLookupService, JsonOwlObjectType.UnionOf);
		}
		else {
			throw new UnknownIdentifierException("Unknown expression type: "+expression.type);
		}
	}
	
	private OWLClassExpression parse(OWLGraphWrapper g, JsonOwlObject[] expressions, 
			ExternalLookupService externalLookupService, JsonOwlObjectType type)
			throws MissingParameterException, UnknownIdentifierException, OWLException {
		if (expressions.length == 0) {
			throw new MissingParameterException("Missing expressions: empty expression list is not allowed.");
		}
		if (expressions.length == 1) {
			return parse(g, expressions[0], externalLookupService);	
		}
		Set<OWLClassExpression> clsExpressions = new HashSet<OWLClassExpression>();
		for (JsonOwlObject m3Expression : expressions) {
			OWLClassExpression ce = parse(g, m3Expression, externalLookupService);
			clsExpressions.add(ce);
		}
		if (type == JsonOwlObjectType.UnionOf) {
			return g.getDataFactory().getOWLObjectUnionOf(clsExpressions);
		}
		else if (type == JsonOwlObjectType.IntersectionOf) {
			return g.getDataFactory().getOWLObjectIntersectionOf(clsExpressions);
		}
		else {
			throw new UnknownIdentifierException("Unsupported expression type: "+type.getLbl());
		}
	}
	
	private OWLClass createClass(String id, OWLGraphWrapper g) {
		IRI iri = g.getIRIByIdentifier(id);
		return g.getDataFactory().getOWLClass(iri);
	}
	
//	OWLClassExpression parseClassExpression(String expression, OWLGraphWrapper g, 
//			boolean createClasses, ExternalLookupService externalLookupService) 
//			throws OWLException, UnknownIdentifierException {
//		try {
//			ManchesterSyntaxTool syntaxTool = new ManchesterSyntaxTool(g, createClasses);
//			OWLClassExpression clsExpr = syntaxTool.parseManchesterExpression(expression);
//			if (checkLiteralIds && externalLookupService != null) {
//				Map<String, OWLClass> createdClasses = syntaxTool.getCreatedClasses();
//				for (Entry<String, OWLClass> createdEntry : createdClasses.entrySet()) {
//					String id = createdEntry.getKey();
//					List<LookupEntry> lookup = externalLookupService.lookup(id); // TODO taxon
//					if (lookup == null || lookup.isEmpty()) {
//						throw new UnknownIdentifierException("Could not validate the id: "+id+" in expression: "+expression);
//					}
//				}
//			}
//			return clsExpr;
//		}
//		catch (ParserException e) {
//			// wrap in an Exception (not RuntimeException) to enable proper error handling
//			throw new OWLException("Could not parse expression: \""+expression+"\"", e) {
//
//				private static final long serialVersionUID = -9158071212925724138L;
//			};
//		}
//	}
}
