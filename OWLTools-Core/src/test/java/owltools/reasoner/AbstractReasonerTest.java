package owltools.reasoner;

import java.io.IOException;
import java.util.Set;

import static org.junit.Assert.*;

import org.obolibrary.macro.ManchesterSyntaxTool;
import org.obolibrary.oboformat.parser.OBOFormatParserException;
import org.semanticweb.owlapi.io.OWLParserException;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.reasoner.ClassExpressionNotInProfileException;
import org.semanticweb.owlapi.reasoner.FreshEntitiesException;
import org.semanticweb.owlapi.reasoner.InconsistentOntologyException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.ReasonerInterruptedException;
import org.semanticweb.owlapi.reasoner.TimeOutException;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;
import owltools.io.ParserWrapper;

public abstract class AbstractReasonerTest extends OWLToolsTestBasics {
	ManchesterSyntaxTool parser;
	OWLPrettyPrinter pp;


	
	protected OWLGraphWrapper getOntologyWrapper(String file) throws OWLOntologyCreationException, IOException, OBOFormatParserException {
		ParserWrapper pw = new ParserWrapper();
		return pw.parseToOWLGraph(getResourceIRIString(file));
	}
	
	protected OWLGraphWrapper getOntologyWrapperFromURL(String url) throws OWLOntologyCreationException, IOException, OBOFormatParserException {
		ParserWrapper pw = new ParserWrapper();
		return pw.parseToOWLGraph(url);
	}
	
	protected OWLReasoner getGraphReasoner(OWLGraphWrapper g) {
		parser = new ManchesterSyntaxTool(g.getSourceOntology(), g.getSupportOntologySet());
		pp = new OWLPrettyPrinter(g);
		GraphReasonerFactory rf = new GraphReasonerFactory();
		return rf.createReasoner(g.getSourceOntology());
	}
	
	protected OWLClassExpression parseOMN(String expr) throws OWLParserException {
		return parser.parseManchesterExpression(expr);
	}
	
	protected Set<OWLClass> findDescendants(OWLReasoner r, String expr) throws TimeOutException, FreshEntitiesException, InconsistentOntologyException, ClassExpressionNotInProfileException, ReasonerInterruptedException, OWLParserException {
		return findDescendants(r, expr, null);
	}
	
	protected Set<OWLClass> findDescendants(OWLReasoner r, String expr, Integer numExpected) throws TimeOutException, FreshEntitiesException, InconsistentOntologyException, ClassExpressionNotInProfileException, ReasonerInterruptedException, OWLParserException {
		System.out.println("Query: "+expr);
		OWLClassExpression qc = parseOMN(expr);
		Set<OWLClass> clzs = r.getSubClasses(qc, false).getFlattened();
		clzs.remove(r.getRootOntology().getOWLOntologyManager().getOWLDataFactory().getOWLNothing());
		if (!qc.isAnonymous())
			clzs.add((OWLClass) qc);
		System.out.println("NumD:"+clzs.size());
		for (OWLClass c : clzs) {
			System.out.println("  D:"+c);
		}
		if (numExpected != null) {
			assertEquals(numExpected.intValue(), clzs.size());
		}
		return clzs;
	}
	


	
	protected Set<OWLNamedIndividual> findIndividuals(OWLReasoner r, String expr, Integer numExpected) throws TimeOutException, FreshEntitiesException, InconsistentOntologyException, ClassExpressionNotInProfileException, ReasonerInterruptedException, OWLParserException {
		System.out.println("Query: "+expr);
		Set<OWLNamedIndividual> inds = r.getInstances(parseOMN(expr), false).getFlattened();
		for (OWLNamedIndividual i : inds) {
			System.out.println("  I:"+i);
		}
		if (numExpected != null) {
			assertEquals(numExpected.intValue(), inds.size());
		}
		return inds;
	}

	
}
