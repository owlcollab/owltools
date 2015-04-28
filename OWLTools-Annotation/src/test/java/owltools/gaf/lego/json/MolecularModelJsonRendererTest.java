package owltools.gaf.lego.json;

import static org.junit.Assert.*;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import owltools.gaf.lego.IdStringManager.AnnotationShorthand;
import owltools.gaf.lego.MolecularModelManager.UnknownIdentifierException;
import owltools.gaf.lego.json.JsonOwlObject.JsonOwlObjectType;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class MolecularModelJsonRendererTest {

	private static OWLGraphWrapper g = null;
	private static OWLOntologyManager m = null;
	private static OWLDataFactory f = null;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		File file = new File("src/test/resources/mgi-go.obo").getCanonicalFile();
		OWLOntology ont = pw.parseOWL(IRI.create(file));
		g = new OWLGraphWrapper(ont);
		f = g.getDataFactory();
		m = g.getManager();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		IOUtils.closeQuietly(g);
	}

	@Test
	public void testSimpleClass() throws Exception {
		testSimpleClassExpression(g.getOWLClassByIdentifier("GO:0000003"), "class");
	}
	
	@Test
	public void testSimpleSVF() throws Exception {
		OWLObjectSomeValuesFrom svf = f.getOWLObjectSomeValuesFrom(g.getOWLObjectPropertyByIdentifier("BFO:0000050"), g.getOWLClassByIdentifier("GO:0000003"));
		testSimpleClassExpression(svf, "svf");
	}
	
	@Test
	public void testSimpleUnion() throws Exception {
		OWLObjectSomeValuesFrom svf = f.getOWLObjectSomeValuesFrom(g.getOWLObjectPropertyByIdentifier("BFO:0000050"), g.getOWLClassByIdentifier("GO:0000003"));
		OWLClass cls = g.getOWLClassByIdentifier("GO:0000122");
		testSimpleClassExpression(f.getOWLObjectUnionOf(cls, svf), "union");
	}
	
	@Test
	public void testSimpleIntersection() throws Exception {
		OWLObjectSomeValuesFrom svf = f.getOWLObjectSomeValuesFrom(g.getOWLObjectPropertyByIdentifier("BFO:0000050"), g.getOWLClassByIdentifier("GO:0000003"));
		OWLClass cls = g.getOWLClassByIdentifier("GO:0000122");
		testSimpleClassExpression(f.getOWLObjectIntersectionOf(cls, svf), "intersection");
	}
	
	@Test
	public void testAnnotations() throws Exception {
		// setup test model/ontology
		OWLOntology o = m.createOntology();
		OWLImportsDeclaration importDeclaration = f.getOWLImportsDeclaration(g.getSourceOntology().getOntologyID().getOntologyIRI());
		m.applyChange(new AddImport(o, importDeclaration));
		
		final IRI i1IRI = IRI.generateDocumentIRI();
		final OWLNamedIndividual ni1 = f.getOWLNamedIndividual(i1IRI);
		// declare individual
		m.addAxiom(o, f.getOWLDeclarationAxiom(ni1));
		// add annotations
		m.addAxiom(o, f.getOWLAnnotationAssertionAxiom(i1IRI, 
				f.getOWLAnnotation(f.getOWLAnnotationProperty(
						AnnotationShorthand.comment.getAnnotationProperty()), 
						f.getOWLLiteral("Comment 1"))));
		m.addAxiom(o, f.getOWLAnnotationAssertionAxiom(i1IRI, 
				f.getOWLAnnotation(f.getOWLAnnotationProperty(
						AnnotationShorthand.comment.getAnnotationProperty()), 
						f.getOWLLiteral("Comment 2"))));
		// declare type
		m.addAxiom(o, f.getOWLClassAssertionAxiom(g.getOWLClassByIdentifier("GO:0000003"), ni1));
		
		MolecularModelJsonRenderer r = new MolecularModelJsonRenderer(o);
		
		JsonOwlIndividual jsonOwlIndividualOriginal = r.renderObject(ni1);
		assertEquals(2, jsonOwlIndividualOriginal.annotations.length);
		
		String json = MolecularModelJsonRenderer.renderToJson(jsonOwlIndividualOriginal, true);
		
		JsonOwlIndividual jsonOwlIndividualParse = MolecularModelJsonRenderer.parseFromJson(json, JsonOwlIndividual.class);
		
		assertNotNull(jsonOwlIndividualParse);
		assertEquals(jsonOwlIndividualOriginal, jsonOwlIndividualParse);
	}
	
	private void testSimpleClassExpression(OWLClassExpression ce, String expectedJsonType) throws Exception {
		// setup test model/ontology
		OWLOntology o = m.createOntology();
		OWLImportsDeclaration importDeclaration = f.getOWLImportsDeclaration(g.getSourceOntology().getOntologyID().getOntologyIRI());
		m.applyChange(new AddImport(o, importDeclaration));
		
		// create indivdual with a ce type
		final IRI i1IRI = IRI.generateDocumentIRI();
		final OWLNamedIndividual ni1 = f.getOWLNamedIndividual(i1IRI);
		// declare individual
		m.addAxiom(o, f.getOWLDeclarationAxiom(ni1));
		// declare type
		m.addAxiom(o, f.getOWLClassAssertionAxiom(ce, ni1));
		
		
		MolecularModelJsonRenderer r = new MolecularModelJsonRenderer(o);
		
		JsonOwlIndividual jsonOwlIndividualOriginal = r.renderObject(ni1);
		
		String json = MolecularModelJsonRenderer.renderToJson(jsonOwlIndividualOriginal, true);
		assertTrue(json, json.contains("\"type\": \""+expectedJsonType+"\""));
		
		JsonOwlIndividual jsonOwlIndividualParse = MolecularModelJsonRenderer.parseFromJson(json, JsonOwlIndividual.class);
		
		assertNotNull(jsonOwlIndividualParse);
		assertEquals(jsonOwlIndividualOriginal, jsonOwlIndividualParse);
		
		Set<OWLClassExpression> ces = TestJsonOwlObjectParser.parse(new OWLGraphWrapper(o), jsonOwlIndividualParse.type);
		assertEquals(1, ces.size());
		assertEquals(ce, ces.iterator().next());
	}

	
	static class TestJsonOwlObjectParser {
		static OWLClassExpression parse(OWLGraphWrapper g, JsonOwlObject expression)
				throws Exception {
			if (expression == null) {
				throw new Exception("Missing expression: null is not a valid expression.");
			}
			if (expression.type == null) {
				throw new Exception("An expression type is required.");
			}
			if (JsonOwlObjectType.Class == expression.type) {
				if (expression.id == null) {
					throw new Exception("Missing literal for expression of type 'class'");
				}
				if (StringUtils.containsWhitespace(expression.id)) {
					throw new Exception("Identifiers may not contain whitespaces: '"+expression.id+"'");
				}
				OWLClass cls = g.getOWLClassByIdentifier(expression.id);
				if (cls == null) {
					throw new Exception("Could not retrieve a class for id: "+expression.id);
				}
				return cls;
			}
			else if (JsonOwlObjectType.SomeValueFrom == expression.type) {
				if (expression.property == null) {
					throw new Exception("Missing property for expression of type 'svf'");
				}
				if (expression.property.type != JsonOwlObjectType.ObjectProperty) {
					throw new Exception("Unexpected type for Property in 'svf': "+expression.property.type);
				}
				if (expression.property.id == null) {
					throw new Exception("Missing property id for expression of type 'svf'");
				}
				OWLObjectProperty p = g.getOWLObjectPropertyByIdentifier(expression.property.id);
				if (p == null) {
					throw new UnknownIdentifierException("Could not find a property for: "+expression.property);
				}
				if (expression.filler != null) {
					OWLClassExpression ce = parse(g, expression.filler);
					return g.getDataFactory().getOWLObjectSomeValuesFrom(p, ce);
				}
				else {
					throw new Exception("Missing literal or expression for expression of type 'svf'.");
				}
			}
			else if (JsonOwlObjectType.IntersectionOf == expression.type) {
				return parse(g, expression.expressions, JsonOwlObjectType.IntersectionOf);
			}
			else if (JsonOwlObjectType.UnionOf == expression.type) {
				return parse(g, expression.expressions, JsonOwlObjectType.UnionOf);
			}
			else {
				throw new UnknownIdentifierException("Unknown expression type: "+expression.type);
			}
		}
		
		static OWLClassExpression parse(OWLGraphWrapper g, JsonOwlObject[] expressions, JsonOwlObjectType type)
				throws Exception {
			if (expressions.length == 0) {
				throw new Exception("Missing expressions: empty expression list is not allowed.");
			}
			if (expressions.length == 1) {
				return parse(g, expressions[0]);	
			}
			Set<OWLClassExpression> clsExpressions = new HashSet<OWLClassExpression>();
			for (JsonOwlObject m3Expression : expressions) {
				OWLClassExpression ce = parse(g, m3Expression);
				clsExpressions.add(ce);
			}
			if (type == JsonOwlObjectType.UnionOf) {
				return g.getDataFactory().getOWLObjectUnionOf(clsExpressions);
			}
			else if (type == JsonOwlObjectType.IntersectionOf) {
				return g.getDataFactory().getOWLObjectIntersectionOf(clsExpressions);
			}
			else {
				throw new UnknownIdentifierException("Unsupported expression type: "+type);
			}
		}
		
		static Set<OWLClassExpression> parse(OWLGraphWrapper g, JsonOwlObject[] expressions)
				throws Exception {
			if (expressions.length == 0) {
				throw new Exception("Missing expressions: empty expression list is not allowed.");
			}
			Set<OWLClassExpression> clsExpressions = new HashSet<OWLClassExpression>();
			for (JsonOwlObject m3Expression : expressions) {
				OWLClassExpression ce = parse(g, m3Expression);
				clsExpressions.add(ce);
			}
			return clsExpressions;
		}
	}
}
