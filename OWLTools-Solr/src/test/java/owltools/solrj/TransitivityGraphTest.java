package owltools.solrj;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.util.OWLClassExpressionVisitorAdapter;

import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;
import owltools.reasoner.ExpressionMaterializingReasoner;

/**
 * Test case for owltools bug https://code.google.com/p/owltools/issues/detail?id=105
 */
public class TransitivityGraphTest {
	
	static OWLGraphWrapper graph = null;

	@BeforeClass
	public static void beforeClass() throws Exception {
		ParserWrapper pw = new ParserWrapper();
		File testOntology = new File("src/test/resources/topology-test.obo").getCanonicalFile();
		graph = new OWLGraphWrapper(pw.parseOWL(IRI.create(testOntology)));
	}
	
	/**
	 * Use the {@link ExpressionMaterializingReasoner} to also infer 
	 * 'REL some value from TARGET' unnamed super classes.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testExpectedRelations() throws Exception {
		final OWLClass in = graph.getOWLClassByIdentifier("GO:0030414"); // GO:0030414 ! peptidase inhibitor activity
		final OWLClass ac = graph.getOWLClassByIdentifier("GO:0008233"); // GO:0008233 ! peptidase activity
		List<String> propIds = Arrays.asList("BFO:0000050", 
				"BFO:0000066", "RO:0002211", 
				"RO:0002212", "RO:0002213", 
				"RO:0002215", "RO:0002216");
		Set<OWLObjectProperty> props = graph.relationshipIDsToPropertySet(propIds);
		
		ExpressionMaterializingReasoner r = new ExpressionMaterializingReasoner(graph.getSourceOntology());
		Logger.getLogger(ExpressionMaterializingReasoner.class).setLevel(Level.ERROR);
		Logger.getLogger("org.semanticweb.elk").setLevel(Level.ERROR);
		r.materializeExpressions(props);
		
		Set<OWLClassExpression> superClassExpressions = r.getSuperClassExpressions(in, false);
		final Set<String> foundProperties = new HashSet<String>();
		for (OWLClassExpression ce : superClassExpressions) {
			ce.accept(new OWLClassExpressionVisitorAdapter(){

				@Override
				public void visit(OWLClass cls) {
					if (cls.isBuiltIn() == false && ac.equals(cls)) {
						foundProperties.add("is_a");
					}
				}

				@Override
				public void visit(OWLObjectSomeValuesFrom svf) {
					if (ac.equals(svf.getFiller())) {
						foundProperties.add(graph.getIdentifier(svf.getProperty()));
					}
				}
				
			});
		}
		assertEquals(2, foundProperties.size());
		assertTrue(foundProperties.contains("regulates"));
		assertTrue(foundProperties.contains("negatively_regulates"));
	}
	
}
