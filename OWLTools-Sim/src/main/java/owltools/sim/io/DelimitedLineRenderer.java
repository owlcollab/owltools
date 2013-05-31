package owltools.sim.io;

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.semanticweb.owlapi.model.OWLClass;

import owltools.graph.OWLGraphWrapper;
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

	public static class SimScores {
		
		public Double simJScore = null;
		public String simjScoreLabel = null;
		
		public Double AsymSimJScore = null;
		public String AsymSimJScoreLabel = null;
		
		public ScoreAttributePair lcsScore = null;
		public String lcsScorePrefix = null;
		
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
	public void printAttributeSim(SimScores simScores, OWLClass a, OWLClass b, OWLGraphWrapper graph)
	{
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
	}

}
