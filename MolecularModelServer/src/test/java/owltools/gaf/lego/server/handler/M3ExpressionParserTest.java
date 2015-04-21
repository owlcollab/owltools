package owltools.gaf.lego.server.handler;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import owltools.gaf.lego.MolecularModelManager.UnknownIdentifierException;
import owltools.gaf.lego.json.JsonOwlObject;
import owltools.gaf.lego.json.JsonOwlObject.JsonOwlObjectType;
import owltools.gaf.lego.server.external.ProteinToolService;
import owltools.gaf.lego.server.handler.OperationsTools.MissingParameterException;
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
		JsonOwlObject expression = null;
		new M3ExpressionParser().parse(graph, expression, proteinService);
	}
	
	@Test(expected=MissingParameterException.class)
	public void testMissing1() throws Exception {
		JsonOwlObject expression = new JsonOwlObject();
		new M3ExpressionParser().parse(graph, expression, proteinService);
	}
	
	@Test(expected=MissingParameterException.class)
	public void testMissing2() throws Exception {
		JsonOwlObject expression = new JsonOwlObject();
		expression.type = JsonOwlObjectType.Class;
		new M3ExpressionParser().parse(graph, expression, proteinService);
	}
	
	@Test(expected=MissingParameterException.class)
	public void testMissing3() throws Exception {
		JsonOwlObject expression = new JsonOwlObject();
		expression.type = JsonOwlObjectType.SomeValueFrom;
		expression.property = "BFO:0000066"; // occurs_in
		new M3ExpressionParser().parse(graph, expression, proteinService);
	}
	
	@Test(expected=MissingParameterException.class)
	public void testMissing4() throws Exception {
		JsonOwlObject expression = new JsonOwlObject();
		expression.type = JsonOwlObjectType.SomeValueFrom;
		expression.property = "BFO:0000066"; // occurs_in
		expression.filler = new JsonOwlObject();
		new M3ExpressionParser().parse(graph, expression, proteinService);
	}
	
	@Test(expected=MissingParameterException.class)
	public void testMissing5() throws Exception {
		JsonOwlObject expression = new JsonOwlObject();
		expression.type = JsonOwlObjectType.SomeValueFrom;
		expression.property = "BFO:0000066"; // occurs_in
		expression.filler = new JsonOwlObject();
		expression.filler.type = JsonOwlObjectType.Class;
		new M3ExpressionParser().parse(graph, expression, proteinService);
	}
	
	@Test(expected=MissingParameterException.class)
	public void testMissing6() throws Exception {
		JsonOwlObject expression = new JsonOwlObject();
		expression.type = JsonOwlObjectType.SomeValueFrom;
		expression.property = "BFO:0000066"; // occurs_in
		expression.filler = new JsonOwlObject();
		expression.filler.id = "GO:0006915";
		new M3ExpressionParser().parse(graph, expression, proteinService);
	}
	
	@Test
	public void testParseClazz() throws Exception {
		
		JsonOwlObject expression = new JsonOwlObject();
		expression.type = JsonOwlObjectType.Class;
		expression.id = "GO:0006915";
		
		OWLClassExpression ce = new M3ExpressionParser().parse(graph, expression, proteinService);
		assertEquals(graph.getOWLClassByIdentifier("GO:0006915"), ce);
	}
	
	@Test(expected=UnknownIdentifierException.class)
	public void testParseClazzFail() throws Exception {
		
		JsonOwlObject expression = new JsonOwlObject();
		expression.type = JsonOwlObjectType.Class;
		expression.id = "FO:0006915";
		
		new M3ExpressionParser().parse(graph, expression, proteinService);
	}
	
	@Test
	public void testParseSvf() throws Exception {
		
		JsonOwlObject expression = new JsonOwlObject();
		expression.type = JsonOwlObjectType.SomeValueFrom;
		expression.property = "BFO:0000066"; // occurs_in
		expression.filler = new JsonOwlObject();
		expression.filler.type = JsonOwlObjectType.Class;
		expression.filler.id = "GO:0005623"; // cell
		
		OWLClassExpression ce = new M3ExpressionParser().parse(graph, expression, proteinService);
		assertNotNull(ce);
	}
	
	@Test(expected=UnknownIdentifierException.class)
	public void testParseSvfFail1() throws Exception {
		
		JsonOwlObject expression = new JsonOwlObject();
		expression.type = JsonOwlObjectType.SomeValueFrom;
		expression.property = "BFO:0000066"; // occurs_in
		expression.filler = new JsonOwlObject();
		expression.filler.type = JsonOwlObjectType.Class;
		expression.filler.id = "FO:0005623"; // error
		
		new M3ExpressionParser().parse(graph, expression, proteinService);
	}
	
	@Test(expected=UnknownIdentifierException.class)
	public void testParseSvfFail2() throws Exception {
		
		JsonOwlObject expression = new JsonOwlObject();
		expression.type = JsonOwlObjectType.SomeValueFrom;
		expression.property = "FFO:0000066"; // error
		expression.filler = new JsonOwlObject();
		expression.filler.type = JsonOwlObjectType.Class;
		expression.filler.id = "GO:0005623"; // cell
		
		new M3ExpressionParser().parse(graph, expression, proteinService);
	}
	
}
