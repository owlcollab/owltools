package owltools.sim.io;

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;
import owltools.sim2.SimpleOwlSim.ScoreAttributePair;

public class DelimitedLineRenderer implements SimResultRenderer {
	
	private static NumberFormat doubleRenderer = new DecimalFormat("#.##########");
	
	boolean isHeaderLine = true;
	
	private final PrintStream resultOutStream;
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
	public void printAttributeSim(AttributesSimScores simScores, OWLGraphWrapper graph)
	{
		OWLClass a = simScores.a;
		OWLClass b = simScores.b;
		List<String> vals = new ArrayList<String>();
		List<String> cols = new ArrayList<String>();
		// elements
		cols.add("A_ID");
		vals.add(graph.getIdentifier(a));
		cols.add("A_Label");
		vals.add(graph.getLabel(a));
		cols.add("B_ID");
		vals.add(graph.getIdentifier(b));
		cols.add("B_Label");
		vals.add(graph.getLabel(b));

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
			vals.add(graph.getIdentifier(lcs.attributeClass));
			vals.add(graph.getLabel(lcs.attributeClass));
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
		resultOutStream.flush();
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
		
		resultOutStream.flush();
	}

	protected void renderIndividualScore(String label, double score, Set<OWLClassExpression> attributeClassSet, OWLNamedIndividual i, OWLNamedIndividual j, OWLPrettyPrinter owlpp) {
		resultOutStream.print(label);
		resultOutStream.print(separator);
		resultOutStream.print(renderPair(i,j, owlpp));
		resultOutStream.print(separator);
		resultOutStream.print(score);
		resultOutStream.print(separator);
		resultOutStream.print(show(attributeClassSet, owlpp));
		resultOutStream.println();
	}
	
	protected CharSequence renderPair(OWLNamedIndividual i, OWLNamedIndividual j, OWLPrettyPrinter owlpp) {
		StringBuilder sb = new StringBuilder();
		sb.append(owlpp.render(i)).append(this.separator).append(owlpp.render(j));
		return sb;
	}
	
	protected CharSequence show(Set<OWLClassExpression> cset, OWLPrettyPrinter owlpp) {
		StringBuffer sb = new StringBuffer();
		for (OWLClassExpression c : cset) {
			sb.append(owlpp.render(c)).append('\t');
		}
		return sb;
	}
	
}
