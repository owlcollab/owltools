package owltools.sim2.io;

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;
import owltools.sim2.SimpleOwlSim.ScoreAttributePair;
import owltools.sim2.scores.AttributePairScores;
import owltools.sim2.scores.ElementPairScores;

public class DelimitedLineRenderer extends AbstractRenderer implements SimResultRenderer {
	
	private static NumberFormat doubleRenderer = new DecimalFormat("#.##########");
	
	boolean isHeaderLine = true;

	private final String separator;
	private final String commentPrefix;
	
	public DelimitedLineRenderer(PrintStream resultOutStream, String separator, String commentPrefix) {
		this.resultOutStream = resultOutStream;
		this.separator = separator;
		this.commentPrefix = commentPrefix;
	}
	
	public DelimitedLineRenderer(PrintStream resultOutStream) {
		this(resultOutStream, "\t", "# ");
	}

	/* (non-Javadoc)
	 * @see owltools.sim.io.SimResultRenderer#printComment(java.lang.CharSequence)
	 */
	@Override
	public void printComment(CharSequence comment) {
		resultOutStream.print(commentPrefix);
		resultOutStream.println(comment);
	}
	
	/* (non-Javadoc)
	 * @see owltools.sim.io.SimResultRenderer#printSim(owltools.sim.io.Foobar.SimScores, org.semanticweb.owlapi.model.OWLClass, org.semanticweb.owlapi.model.OWLClass, owltools.graph.OWLGraphWrapper)
	 */
	@Override
	public void printAttributeSim(AttributesSimScores simScores, OWLGraphWrapper graph, OWLPrettyPrinter owlpp)
	{
		OWLClass a = simScores.a;
		OWLClass b = simScores.b;
		
		//scores
		renderAttScore(simScores.simjScoreLabel,simScores.simJScore,a,b,graph,owlpp);
		renderAttScore(simScores.AsymSimJScoreLabel,simScores.AsymSimJScore,a,b,graph,owlpp);
		
		ScoreAttributePair lcs = simScores.lcsScore;

		resultOutStream.print(simScores.lcsScorePrefix);
		resultOutStream.print(separator);
		resultOutStream.print(owlpp.render(a));
		resultOutStream.print(separator);
		resultOutStream.print(owlpp.render(b));
		resultOutStream.print(separator);
		resultOutStream.print(doubleRenderer.format(lcs.score));
		resultOutStream.print(separator);
		resultOutStream.print(graph.getIdentifier(lcs.attributeClass));
		resultOutStream.print(separator);
		resultOutStream.print(graph.getLabel(lcs.attributeClass));
		resultOutStream.println();
		
	}

	@Override
	public void printIndividualPairSim(IndividualSimScores scores, OWLPrettyPrinter owlpp, OWLGraphWrapper graph) {
		OWLNamedIndividual i = scores.i;
		OWLNamedIndividual j = scores.j;
		
		resultOutStream.println("NumAnnots\t"+renderPair(i,j, owlpp)+"\t"+scores.numberOfElementsI+"\t"+scores.numberOfElementsJ);

		if (scores.bmaAsymIC != null) {
			renderIndividualScore(scores.bmaAsymICLabel, scores.bmaAsymIC.score, scores.bmaAsymIC.attributeClassSet, i, j, owlpp);
		}
		if (scores.bmaSymIC != null) {
			renderIndividualScore(scores.bmaSymICLabel, scores.bmaSymIC.score, scores.bmaSymIC.attributeClassSet, i, j, owlpp);			
		}
		if (scores.bmaAsymJ != null) {
			renderIndividualScore(scores.bmaAsymJLabel, scores.bmaAsymJ.score, scores.bmaAsymJ.attributeClassSet, i, j, owlpp);
		}
		if (scores.bmaSymJ != null) {
			renderIndividualScore(scores.bmaSymJLabel, scores.bmaSymJ.score, scores.bmaSymJ.attributeClassSet, i, j, owlpp);
		}
		if (scores.simGIC != null) {
			resultOutStream.print(scores.simGICLabel);
			resultOutStream.print(separator);
			resultOutStream.print(renderPair(i,j, owlpp));
			resultOutStream.print(separator);
			resultOutStream.print(doubleRenderer.format(scores.simGIC));
			resultOutStream.println();
		}
		if (scores.maxIC != null) {
			renderIndividualScore(scores.maxICLabel, scores.maxIC.score, scores.maxIC.attributeClassSet, i, j, owlpp);
		}
		if (scores.simjScore != null) {
			resultOutStream.print(scores.simjScoreLabel);
			resultOutStream.print(separator);
			resultOutStream.print(renderPair(i,j, owlpp));
			resultOutStream.print(separator);
			resultOutStream.print(doubleRenderer.format(scores.simjScore));
			resultOutStream.println();
		}
		
	}

	protected void renderIndividualScore(String label, double score, Set<OWLClass> attributeClassSet, OWLNamedIndividual i, OWLNamedIndividual j, OWLPrettyPrinter owlpp) {
		resultOutStream.print(label);
		resultOutStream.print(separator);
		resultOutStream.print(renderPair(i,j, owlpp));
		resultOutStream.print(separator);
		resultOutStream.print(score);
		resultOutStream.print(separator);
		resultOutStream.print(show(attributeClassSet, owlpp));
		resultOutStream.println();
	}
	
	private CharSequence show(Set<OWLClass> attributeClassSet, OWLPrettyPrinter owlpp) {
		StringBuffer sb = new StringBuffer();
		for (OWLClass c : attributeClassSet) {
			sb.append(owlpp.render(c)).append('\t');
		}
		return sb;
	}

	protected void renderAttScore(String label, double score, OWLClass a, OWLClass b, OWLGraphWrapper g, OWLPrettyPrinter owlpp) {
		resultOutStream.print(label);
		resultOutStream.print(separator);
		resultOutStream.print(owlpp.render(a));
		resultOutStream.print(separator);
		resultOutStream.print(owlpp.render(b));
		resultOutStream.print(separator);
		resultOutStream.print(doubleRenderer.format(score));
		resultOutStream.print(separator);
		resultOutStream.println();
	}
	
	protected void renderAttScoreWithIndividuals(String label, double score, OWLClass a, OWLClass b, OWLNamedIndividual i, OWLNamedIndividual j, OWLGraphWrapper g, OWLPrettyPrinter owlpp) {
		resultOutStream.print(label);
		resultOutStream.print(separator);
		resultOutStream.print(renderPair(i,j, owlpp));
		resultOutStream.print(separator);
		resultOutStream.print(owlpp.render(a));
		resultOutStream.print(separator);
		resultOutStream.print(owlpp.render(b));
		resultOutStream.print(separator);
		resultOutStream.print(doubleRenderer.format(score));
		resultOutStream.print(separator);
		resultOutStream.println();
	}

	
	protected CharSequence renderPair(OWLNamedIndividual i, OWLNamedIndividual j, OWLPrettyPrinter owlpp) {
		StringBuilder sb = new StringBuilder();
		sb.append(owlpp.render(i)).append(this.separator).append(owlpp.render(j));
		return sb;
	}
	

	@Override
	public void printAttributeSimWithIndividuals(AttributesSimScores simScores, OWLPrettyPrinter owlpp,
			OWLGraphWrapper g, OWLNamedIndividual i, OWLNamedIndividual j) {
		// TODO Auto-generated method stub
		OWLClass a = simScores.a;
		OWLClass b = simScores.b;
		List<String> vals = new ArrayList<String>();
		List<String> cols = new ArrayList<String>();

		//Individuals
		cols.add("A");
		vals.add(owlpp.render(i));
		cols.add("B");
		vals.add(owlpp.render(j));

		// elements
		cols.add("A_annot_ID");
		vals.add(g.getIdentifier(a));
		cols.add("A_annot_Label");
		vals.add(g.getLabel(a));
		cols.add("B_annot_ID");
		vals.add(g.getIdentifier(b));
		cols.add("B_annot_Label");
		vals.add(g.getLabel(b));

		//scores
		cols.add(simScores.simjScoreLabel);
		if (simScores.simJScore != null) {
			vals.add(doubleRenderer.format(simScores.simJScore));
		}
		else {
			vals.add("");
		}

		cols.add(simScores.AsymSimJScoreLabel);
		if (simScores.AsymSimJScore != null) {
			vals.add(doubleRenderer.format(simScores.AsymSimJScore));
		}
		else {
			vals.add("");
		}

		cols.add(simScores.lcsScorePrefix+"_Score");
		cols.add(simScores.lcsScorePrefix);
		cols.add(simScores.lcsScorePrefix+"_Label");

		ScoreAttributePair lcs = simScores.lcsScore;
		if (lcs != null) {
			vals.add(doubleRenderer.format(lcs.score));
			vals.add(g.getIdentifier(lcs.attributeClass));
			vals.add(g.getLabel(lcs.attributeClass));
		}
		else {
			vals.add("");
			vals.add("");
			vals.add("");
		}

		if (isHeaderLine) {
			resultOutStream.println(StringUtils.join(cols, separator));
			isHeaderLine = false;
		}
		resultOutStream.println(StringUtils.join(vals, separator));
	}

	@Override
	public void printAttributeSim(AttributesSimScores simScores,
			OWLGraphWrapper graph) {
		// TODO Auto-generated method stub
		
	}

	// NEW
	/* (non-Javadoc)
	 * @see owltools.sim.io.SimResultRenderer#printPairScores(owltools.sim2.scores.ElementPairScores, owltools.io.OWLPrettyPrinter, owltools.graph.OWLGraphWrapper)
	 */
	@Override
	public void printPairScores(ElementPairScores scores) {
		// TODO
	}
	/* (non-Javadoc)
	 * @see owltools.sim.io.SimResultRenderer#printPairScores(owltools.sim2.scores.AttributePairScores, owltools.io.OWLPrettyPrinter, owltools.graph.OWLGraphWrapper)
	 */
	@Override
	public void printPairScores(AttributePairScores simScores) {
		OWLClass a = simScores.getA();
		OWLClass b = simScores.getB();
		
		//scores
		renderAttScore("SimJ",
				simScores.simjScore,a,b,
				graph,owlpp);
		renderAttScore("AsymSimJ",simScores.asymmetricSimjScore,a,b,
				graph,
				owlpp);
		
		//ScoreAttributePair lcs = simScores.;

		OWLClass lcs = simScores.lcsSet.iterator().next();
		resultOutStream.print("LCS");
		resultOutStream.print(separator);
		resultOutStream.print(owlpp.render(a));
		resultOutStream.print(separator);
		resultOutStream.print(owlpp.render(b));
		resultOutStream.print(separator);
		resultOutStream.print(doubleRenderer.format(simScores.lcsIC));
		resultOutStream.print(separator);
		resultOutStream.print(graph.getIdentifier(lcs));
		resultOutStream.print(separator);
		resultOutStream.print(graph.getLabel(lcs));
		resultOutStream.println();
	}

}

