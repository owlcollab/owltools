package owltools.sim2.io;

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
import owltools.sim2.OwlSim.ScoreAttributeSetPair;
import owltools.sim2.SimpleOwlSim.ScoreAttributePair;
import owltools.sim2.scores.AttributePairScores;
import owltools.sim2.scores.ElementPairScores;
import owltools.sim2.scores.PairScores;

public class TabularRenderer extends AbstractRenderer implements SimResultRenderer {

	private static NumberFormat doubleRenderer = new DecimalFormat("#.##########");

	boolean isHeaderLine = true;

	private final String separator;
	private final String commentPrefix;

	public TabularRenderer(PrintStream resultOutStream, String separator, String commentPrefix) {
		this.resultOutStream = resultOutStream;
		this.separator = separator;
		this.commentPrefix = commentPrefix;
	}

	public TabularRenderer(PrintStream resultOutStream) {
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
		String bestMarker = "";
		if (simScores.isBestMatch)
			bestMarker = "*";
		// elements
		cols.add("A_ID");
		vals.add(bestMarker+graph.getIdentifier(a));
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

	@Override
	public void printIndividualPairSim(IndividualSimScores scores, OWLPrettyPrinter owlpp, OWLGraphWrapper graph) {
		OWLNamedIndividual i = scores.i;
		OWLNamedIndividual j = scores.j;
		Integer numannotsI = scores.numberOfElementsI;
		Integer numannotsJ = scores.numberOfElementsJ;


		List<String> vals = new ArrayList<String>();
		List<String> cols = new ArrayList<String>();

		// elements
		cols.add("A");
		vals.add(graph.getIdentifier(i));
		cols.add("A_Label");
		vals.add(graph.getLabel(i));
		cols.add("B_ID");
		vals.add(graph.getIdentifier(j));
		cols.add("B_Label");
		vals.add(graph.getLabel(j));

		cols.add("NumAnnots A");
		vals.add(numannotsI.toString());

		cols.add("NumAnnots B");
		vals.add(numannotsJ.toString());

		cols.add(scores.maxICLabel);
		cols.add(scores.maxICLabel + " Term");
		if (scores.maxIC != null) {
			vals.add(doubleRenderer.format(scores.maxIC.score));
			//should just be a single term with the max(maxIC)
			vals.add(show(scores.maxIC.attributeClassSet, owlpp).toString());
		}	else {
			vals.add("");
			vals.add("");
		}

		cols.add(scores.bmaAsymICLabel);
		cols.add(scores.bmaAsymICLabel + " Terms");
		if (scores.bmaAsymIC != null) {			
			vals.add(doubleRenderer.format(scores.bmaAsymIC.score));
			//terms in common
			vals.add(show(scores.bmaAsymIC.attributeClassSet, owlpp).toString());
		} else {
			vals.add("");
			vals.add("");
		}

		if (scores.bmaSymIC != null) {
			vals.add(doubleRenderer.format(scores.bmaSymIC.score));
		} else {
			vals.add("");
		}

		cols.add(scores.simjScoreLabel);
		if (scores.simjScore != null) {
			vals.add(doubleRenderer.format(scores.simjScore));
		}
		else {
			vals.add("");
		}

		cols.add(scores.bmaAsymJLabel);
		if (scores.bmaAsymJ != null) {
			vals.add(doubleRenderer.format(scores.bmaAsymJ.score));
		}	else {
			vals.add("");
		}

		cols.add(scores.bmaSymJLabel);
		if (scores.bmaSymJ != null) {
			vals.add(doubleRenderer.format(scores.bmaSymJ.score));
		} else {
			vals.add("");
		}

		cols.add(scores.simGICLabel);
		if (scores.simGIC != null) {
			vals.add(doubleRenderer.format(scores.simGIC));
		} else {
			vals.add("");
		}		

		if (isHeaderLine) {
			resultOutStream.println(StringUtils.join(cols, separator));
			isHeaderLine = false;
		}

		resultOutStream.println(StringUtils.join(vals, separator));

	}


	protected CharSequence renderPair(OWLNamedIndividual i, OWLNamedIndividual j, OWLPrettyPrinter owlpp) {
		StringBuilder sb = new StringBuilder();
		sb.append(owlpp.render(i)).append(this.separator).append(owlpp.render(j));
		return sb;
	}

	protected CharSequence show(Set<OWLClass> cset, OWLPrettyPrinter owlpp) {
		StringBuffer sb = new StringBuffer();
		for (OWLClassExpression c : cset) {
			sb.append(owlpp.render(c)).append('\t');
		}
		return sb;
	}

	@Override
	public void printAttributeSimWithIndividuals(AttributesSimScores simScores, OWLPrettyPrinter owlpp,
			OWLGraphWrapper g, OWLNamedIndividual i, OWLNamedIndividual j) {

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
			OWLGraphWrapper graph, OWLPrettyPrinter owlpp) {
		// TODO Auto-generated method stub
		
	}



	// NEW
	@Override
	public void printPairScores(ElementPairScores scores) {
		OWLNamedIndividual i = scores.i;
		OWLNamedIndividual j = scores.j;

		
		List<String> vals = new ArrayList<String>();
		List<String> cols = new ArrayList<String>();

		// elements
		cols.add("A");
		vals.add(graph.getIdentifier(i));
		cols.add("A_Label");
		vals.add(graph.getLabel(i));
		cols.add("B_ID");
		vals.add(graph.getIdentifier(j));
		cols.add("B_Label");
		vals.add(graph.getLabel(j));

		cols.add("NumAnnots A");
		vals.add(scores.cs.size()+"");

		cols.add("NumAnnots B");
		vals.add(scores.ds.size()+"");

		cols.add("Combined");
		if (scores.combinedScore != null) {
			vals.add(doubleRenderer.format(scores.combinedScore));
		}
		else {
			vals.add("");
		}

		cols.add("SimJ");
		if (scores.simjScore != null) {
			vals.add(doubleRenderer.format(scores.simjScore));
		}
		else {
			vals.add("");
		}

		cols.add("MaxIC");
		cols.add("MaxIC Term");
		if (scores.maxIC != null) {
			vals.add(doubleRenderer.format(scores.maxIC));
			//should just be a single term with the max(maxIC)
			vals.add(show(scores.maxICwitness, owlpp).toString());
		}	else {
			vals.add("");
			vals.add("");
		}

		cols.add("AsymBMA_IC");
		cols.add("AsymBMC_IC Terms");
		if (scores.bmaAsymIC != null) {			
			vals.add(doubleRenderer.format(scores.bmaAsymIC));
			//terms in common
			//vals.add(show(scores.bmaAsymIC.attributeClassSet, owlpp).toString());
			ScoreAttributeSetPair[] saps = scores.iclcsMatrix.bestForC;
			StringBuffer sb = new StringBuffer();
			for (ScoreAttributeSetPair sap : saps) {
				if (sap == null)
					continue;
				sb.append(show(sap.attributeClassSet, owlpp)+";");
			}
			vals.add(sb.toString()); // TODO
		} else {
			vals.add("");
			vals.add("");
		}

		if (scores.bmaSymIC != null) {
			vals.add(doubleRenderer.format(scores.bmaSymIC));
		} else {
			vals.add("");
		}


//		cols.add("ASym");
//		if (scores.bmaAsymJ != null) {
//			vals.add(doubleRenderer.format(scores.bmaAsymJ.score));
//		}	else {
//			vals.add("");
//		}
//
//		cols.add(scores.bmaSymJLabel);
//		if (scores.bmaSymJ != null) {
//			vals.add(doubleRenderer.format(scores.bmaSymJ.score));
//		} else {
//			vals.add("");
//		}
//
//		cols.add(scores.simGICLabel);
//		if (scores.simGIC != null) {
//			vals.add(doubleRenderer.format(scores.simGIC));
//		} else {
//			vals.add("");
//		}		

		if (isHeaderLine) {
			resultOutStream.println(StringUtils.join(cols, separator));
			isHeaderLine = false;
		}

		resultOutStream.println(StringUtils.join(vals, separator));
	}
	
	@Override
	public void printPairScores(AttributePairScores simScores) {
		OWLClass a = simScores.getA();
		OWLClass b = simScores.getB();

		List<String> vals = new ArrayList<String>();
		List<String> cols = new ArrayList<String>();
		String bestMarker = "";
		if (simScores.isBestMatchForI)
			bestMarker = "*";
		// elements
		cols.add("A_ID");
		vals.add(bestMarker+graph.getIdentifier(a));
		cols.add("A_Label");
		vals.add(graph.getLabel(a));
		cols.add("B_ID");
		vals.add(graph.getIdentifier(b));
		cols.add("B_Label");
		vals.add(graph.getLabel(b));

		//scores
		cols.add("SimJ");
		if (simScores.simjScore != null) {
			vals.add(doubleRenderer.format(simScores.simjScore));
		}
		else {
			vals.add("");
		}

		cols.add("AsymSymJ");
		if (simScores.asymmetricSimjScore != null) {
			vals.add(doubleRenderer.format(simScores.asymmetricSimjScore));
		}
		else {
			vals.add("");
		}

		cols.add("IC-of-LCS");
		cols.add("LCS-ID");
		cols.add("LCS_Label");

		if (simScores.lcsSet != null) {
			OWLClass lcs = simScores.lcsSet.iterator().next();
			vals.add(doubleRenderer.format(simScores.lcsIC));
			vals.add(graph.getIdentifier(lcs));
			vals.add(graph.getLabel(lcs));
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

