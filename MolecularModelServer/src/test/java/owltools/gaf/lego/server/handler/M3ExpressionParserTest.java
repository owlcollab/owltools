package owltools.gaf.lego.server.handler;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import owltools.gaf.lego.MolecularModelManager.UnknownIdentifierException;
import owltools.gaf.lego.server.external.ProteinToolService;
import owltools.gaf.lego.server.handler.JsonOrJsonpBatchHandler.MissingParameterException;
import owltools.gaf.lego.server.handler.M3BatchHandler.M3Expression;
import owltools.gaf.lego.server.handler.M3BatchHandler.M3ExpressionType;
import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class M3ExpressionParserTest {

	private static ProteinToolService proteinService;
	private static OWLGraphWrapper graph;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		init(new ParserWrapper());
	}

	static void init(ParserWrapper pw) throws OWLOntologyCreationException, IOException {
		graph = pw.parseToOWLGraph("http://purl.obolibrary.org/obo/go.owl");
		graph.mergeOntology(pw.parse("http://purl.obolibrary.org/obo/ro.owl"));
		proteinService = new ProteinToolService("src/test/resources/ontology/protein/subset");
	}

	@Test(expected=MissingParameterException.class)
	public void testMissing0() throws Exception {
		M3Expression expression = null;
		new M3ExpressionParser().parse(graph, expression, proteinService);
	}
	
	@Test(expected=MissingParameterException.class)
	public void testMissing1() throws Exception {
		M3Expression expression = new M3Expression();
		new M3ExpressionParser().parse(graph, expression, proteinService);
	}
	
	@Test(expected=MissingParameterException.class)
	public void testMissing2() throws Exception {
		M3Expression expression = new M3Expression();
		expression.type = M3ExpressionType.clazz.getLbl();
		new M3ExpressionParser().parse(graph, expression, proteinService);
	}
	
	@Test(expected=MissingParameterException.class)
	public void testMissing3() throws Exception {
		M3Expression expression = new M3Expression();
		expression.type = M3ExpressionType.svf.getLbl();
		expression.literal = "GO:0005623"; // cell
		new M3ExpressionParser().parse(graph, expression, proteinService);
	}
	
	@Test(expected=MissingParameterException.class)
	public void testMissing4() throws Exception {
		M3Expression expression = new M3Expression();
		expression.type = M3ExpressionType.svf.getLbl();
		expression.onProp = "BFO:0000066"; // occurs_in
		new M3ExpressionParser().parse(graph, expression, proteinService);
	}
	
	@Test
	public void testParseClazz() throws Exception {
		
		M3Expression expression = new M3Expression();
		expression.type = M3ExpressionType.clazz.getLbl();
		expression.literal = "GO:0006915";
		
		OWLClassExpression ce = new M3ExpressionParser().parse(graph, expression, proteinService);
		assertEquals(graph.getOWLClassByIdentifier("GO:0006915"), ce);
	}
	
	@Test(expected=UnknownIdentifierException.class)
	public void testParseClazzFail() throws Exception {
		
		M3Expression expression = new M3Expression();
		expression.type = M3ExpressionType.clazz.getLbl();
		expression.literal = "FO:0006915";
		
		new M3ExpressionParser().parse(graph, expression, proteinService);
	}
	
	@Test
	public void testParseSvf() throws Exception {
		
		M3Expression expression = new M3Expression();
		expression.type = M3ExpressionType.svf.getLbl();
		expression.onProp = "BFO:0000066"; // occurs_in
		expression.literal = "GO:0005623"; // cell
		
		OWLClassExpression ce = new M3ExpressionParser().parse(graph, expression, proteinService);
		assertNotNull(ce);
	}
	
	@Test(expected=UnknownIdentifierException.class)
	public void testParseSvfFail1() throws Exception {
		
		M3Expression expression = new M3Expression();
		expression.type = M3ExpressionType.svf.getLbl();
		expression.onProp = "BFO:0000066"; // occurs_in
		expression.literal = "FO:0005623"; // error
		
		new M3ExpressionParser().parse(graph, expression, proteinService);
	}
	
	@Test(expected=UnknownIdentifierException.class)
	public void testParseSvfFail2() throws Exception {
		
		M3Expression expression = new M3Expression();
		expression.type = M3ExpressionType.svf.getLbl();
		expression.onProp = "FFO:0000066"; // error
		expression.literal = "GO:0005623"; // cell
		
		new M3ExpressionParser().parse(graph, expression, proteinService);
	}
	
	@Test
	public void testParseComplexOr() throws Exception {
		
		M3Expression expression = new M3Expression();
		expression.type = M3ExpressionType.svf.getLbl();
		expression.onProp = "RO:0002333"; // enabled_by
		expression.literal = "('has part' some UniProtKB:F1NGQ9) or ('has part' some UniProtKB:F1NH29)";
		
		OWLClassExpression ce = new M3ExpressionParser().parse(graph, expression, proteinService);
		assertNotNull(ce);
	}
	
	@Test(expected=UnknownIdentifierException.class)
	public void testParseComplexFail1() throws Exception {
		
		M3Expression expression = new M3Expression();
		expression.type = M3ExpressionType.svf.getLbl();
		expression.onProp = "RO:0002333"; // enabled_by
		expression.literal = "('has part' some UniProtKB:F000F1) or ('has part' some UniProtKB:F000F2)";
		
		new M3ExpressionParser().parse(graph, expression, proteinService);
	}
	
	@Test
	public void testParseComplex1() throws Exception {
		
		M3Expression expression = new M3Expression();
		expression.type = M3ExpressionType.svf.getLbl();
		expression.onProp = "BFO:0000066"; // occurs_in
		expression.literal = "'has part' some GO:0005791"; // rough endoplasmic reticulum
		
		new M3ExpressionParser().parse(graph, expression, proteinService);
	}
	
	@Test
	public void testParseComplex2() throws Exception {
		
		M3Expression expression = new M3Expression();
		expression.type = M3ExpressionType.svf.getLbl();
		expression.onProp = "BFO:0000066"; // occurs_in
		expression.literal = "'has part' some 'rough endoplasmic reticulum'"; // GO:0005791
		
		new M3ExpressionParser().parse(graph, expression, proteinService);
	}
	
	@Test(expected=UnknownIdentifierException.class)
	public void testParseComplexFail2() throws Exception {
		
		M3Expression expression = new M3Expression();
		expression.type = M3ExpressionType.svf.getLbl();
		expression.onProp = "BFO:0000066"; // occurs_in
		expression.literal = "'has part' some FO:0005791"; // error
		
		new M3ExpressionParser().parse(graph, expression, proteinService);
	}

}
