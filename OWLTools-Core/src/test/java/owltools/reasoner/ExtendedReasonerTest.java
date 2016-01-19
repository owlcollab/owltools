package owltools.reasoner;

import static org.junit.Assert.*;

import java.util.Set;

import org.geneontology.reasoner.ExpressionMaterializingReasoner;
import org.geneontology.reasoner.OWLExtendedReasoner;
import org.junit.Test;
import org.obolibrary.macro.ManchesterSyntaxTool;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.io.OWLParserException;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.reasoner.BufferingMode;
import org.semanticweb.owlapi.reasoner.ClassExpressionNotInProfileException;
import org.semanticweb.owlapi.reasoner.FreshEntitiesException;
import org.semanticweb.owlapi.reasoner.InconsistentOntologyException;
import org.semanticweb.owlapi.reasoner.ReasonerInterruptedException;
import org.semanticweb.owlapi.reasoner.TimeOutException;

import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;

/**
 * tests getLeastCommonSubsumerSimpleClassExpression
 * 
 * @author cjm
 *
 */
public class ExtendedReasonerTest extends AbstractReasonerTest {

	OWLGraphWrapper g;
	OWLPrettyPrinter pp;
	OWLExtendedReasoner reasoner;

	protected Set<OWLClass> findAncestors(String expr, 
			OWLObjectProperty p, boolean direct, Integer numExpected) throws TimeOutException, FreshEntitiesException, InconsistentOntologyException, ClassExpressionNotInProfileException, ReasonerInterruptedException, OWLParserException {
		System.out.println("Query: "+expr+" "+p+" Direct="+direct);
		OWLClassExpression qc = parseOMN(expr);
		System.out.println("QueryC: "+qc);
		//Set<OWLClassExpression> ces = reasoner.getSuperClassExpressions(qc, false);
		//System.out.println("NumX:"+ces.size());
		Set<OWLClass> clzs = reasoner.getSuperClassesOver(qc, p, direct);
		System.out.println("NumD:"+clzs.size());
		for (OWLClass c : clzs) {
			System.out.println("  D:"+c);
		}
		if (numExpected != null) {
			assertEquals(numExpected.intValue(), clzs.size());
		}
		return clzs;
	}
	
	@Test
	public void testQuery() throws Exception{
		g =  getOntologyWrapper("extended-reasoner-test.omn");
		parser = new ManchesterSyntaxTool(g.getSourceOntology(), g.getSupportOntologySet());
		reasoner = new ExpressionMaterializingReasoner(g.getSourceOntology(),
				new ElkReasonerFactory(), BufferingMode.NON_BUFFERING);
		reasoner.flush();
		IRI  piri;
		piri = IRI.create("http://x.org/part_of");
		OWLObjectProperty p = 
				g.getDataFactory().getOWLObjectProperty(piri);
		
		findAncestors("digit", p, true, 1);
		findAncestors("digit", p, false, 2);
		findAncestors("toe", p, true, 1);
		findAncestors("toe", p, false, 4);
		findAncestors("phalanx", p, true, 1);
		findAncestors("phalanx", p, false, 4);
		findAncestors("mouse_phalanx", p, true, 2);
		findAncestors("brachialis", p, true, 1);
		

	}
}
