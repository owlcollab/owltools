package owltools.sim.io;

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;
import owltools.sim2.OwlSim.ScoreAttributeSetPair;
import owltools.sim2.scores.AttributePairScores;
import owltools.sim2.scores.ElementPairScores;

public class FormattedRenderer extends AbstractRenderer implements SimResultRenderer {

	private static NumberFormat doubleRenderer = new DecimalFormat("#.##########");

	boolean isHeaderLine = true;

	public FormattedRenderer(PrintStream resultOutStream, OWLPrettyPrinter owlpp) {
		this.resultOutStream = resultOutStream;
		this.owlpp = owlpp;
	}




	public FormattedRenderer(OWLPrettyPrinter owlpp) {
		this.resultOutStream = System.out;
		this.owlpp = owlpp;
	}
	public FormattedRenderer(OWLGraphWrapper g) {
		this.resultOutStream = System.out;
		this.owlpp = new OWLPrettyPrinter(g);
	}
	public FormattedRenderer(OWLGraphWrapper g, PrintStream resultOutStream) {
		this.resultOutStream = resultOutStream;
		this.owlpp = new OWLPrettyPrinter(g);
	}




	@Override
	public void printAttributeSim(AttributesSimScores simScores,
			OWLGraphWrapper graph, OWLPrettyPrinter owlpp) {
		// TODO Auto-generated method stub

	}


	/* (non-Javadoc)
	 * @see owltools.sim.io.SimResultRenderer#printPairScores(owltools.sim2.scores.PairScores, owltools.io.OWLPrettyPrinter, owltools.graph.OWLGraphWrapper)
	 */
	@Override
	public void printPairScores(ElementPairScores ijscores) {
		resultOutStream.println("\n## PAIR:");
		resultOutStream.println(" * I="+render(ijscores.getA()));
		resultOutStream.println(" * J="+render(ijscores.getB()));
		resultOutStream.println("### Summary");
		resultOutStream.println(" * DirectAtts(i) = "+ijscores.getNumberOfAttributesForI());
		resultOutStream.println(" * DirectAtts(j) = "+ijscores.getNumberOfAttributesForJ());
		resultOutStream.println("### Direct Attributes");
		resultOutStream.println(" * Atts(i)");
		for (OWLClass c : ijscores.cs) {
			resultOutStream.println("     * "+render(c));
		}
		resultOutStream.println(" * Atts(j)");
		for (OWLClass d : ijscores.ds) {
			resultOutStream.println("     * "+render(d));
		}
		resultOutStream.println("### SimJ");
		resultOutStream.println(" * SimJ(i,j) = "+ijscores.simjScore);
		resultOutStream.println(" * Asymmetric SimJ(i < j) = "+ijscores.asymmetricSimjScore);
		resultOutStream.println(" * Asymmetric SimJ(i > j) = "+ijscores.inverseAsymmetricSimjScore);
		resultOutStream.println("### IC");
		resultOutStream.println(" * MaxIC(i,j) = "+ijscores.maxIC);
		resultOutStream.println(" * MaxIC(i,j)[term] = "+render(ijscores.maxICwitness));
		resultOutStream.println(" * AvgIC(i,j) = "+ijscores.avgIC);
		resultOutStream.println(" * SimGIC(i,j) = "+ijscores.simGIC);
		resultOutStream.println(" * Asymmetric SimGIC(i < j) = "+ijscores.asymmetricSimGIC);
		resultOutStream.println(" * Asymmetric SimGIC(i > j) = "+ijscores.inverseAsymmetricSimGIC);
		resultOutStream.println(" * MaxIC(i,j)[term] = "+ijscores.maxICwitness);
		resultOutStream.println(" * Asymmetric SimJ(i < j) = "+ijscores.asymmetricSimjScore);
		resultOutStream.println(" * Asymmetric SimJ(i > j) = "+ijscores.inverseAsymmetricSimjScore);
		resultOutStream.println("### Matrix");
		resultOutStream.println(" * Attributes(i), Best matches in j");
		for (int n=0; n<ijscores.cs.size(); n++) {
			OWLClass c = ijscores.cs.get(n);
			resultOutStream.println("     * "+render(c)+" "+render(ijscores.iclcsMatrix.bestForC[n]));
		}
		resultOutStream.println(" * Attributes(j), Best matches in i");
		for (int n=0; n<ijscores.ds.size(); n++) {
			OWLClass d = ijscores.ds.get(n);
			resultOutStream.println("     * "+render(d)+" "+render(ijscores.iclcsMatrix.bestForD[n]));
		}



	}

	// NEW
	/* (non-Javadoc)
	 * @see owltools.sim.io.SimResultRenderer#printPairScores(owltools.sim2.scores.AttributePairScores, owltools.io.OWLPrettyPrinter, owltools.graph.OWLGraphWrapper)
	 */
	@Override
	public void printPairScores(AttributePairScores scores) {
		resultOutStream.println("\n## PAIR:");
		resultOutStream.println(" * C="+render(scores.getA()));
		resultOutStream.println(" * D="+render(scores.getB()));
		resultOutStream.println("### Rank");
		resultOutStream.println(" * is D best match for C? "+scores.isBestMatchForI);
		resultOutStream.println("### SimJ");
		resultOutStream.println(" * SimJ(c,d) = "+scores.simjScore);
		resultOutStream.println(" * Asymmetric SimJ(c < d) = "+scores.asymmetricSimjScore);
		resultOutStream.println(" * Asymmetric SimJ(d > c) = "+scores.inverseAsymmetricSimjScore);
		resultOutStream.println("### SimGIC");
		resultOutStream.println(" * SimGIC(c,d) = "+scores.simGIC);
		//resultOutStream.println(" * Asymmetric SimJ(c < d) = "+scores.asymmetricSimjScore);
		//resultOutStream.println(" * Asymmetric SimJ(d > c) = "+scores.inverseAsymmetricSimjScore);
		resultOutStream.println("### LCS");
		resultOutStream.println(" * LCS(c,d).IC = "+scores.lcsIC);
		resultOutStream.println(" * LCS(c,d).term = "+render(scores.lcsSet));
	}
	private String render(ScoreAttributeSetPair sap) {
		return sap.score + " "+render(sap.attributeClassSet);
	}

	protected CharSequence render(Set<OWLClass> cset) {
		StringBuffer sb = new StringBuffer();
		if (cset == null)
			return "";
		for (OWLClassExpression c : cset) {
			sb.append(owlpp.render(c)).append("; ");
		}
		return sb;
	}
	protected CharSequence render(OWLClass c) {
		return owlpp.render(c);
	}
	protected CharSequence render(OWLNamedIndividual c) {
		return owlpp.render(c);
	}





	@Override
	public void printComment(CharSequence comment) {
		// TODO Auto-generated method stub

	}




	@Override
	public void printAttributeSim(AttributesSimScores simScores,
			OWLGraphWrapper graph) {
		// TODO Auto-generated method stub

	}




	@Override
	public void printAttributeSimWithIndividuals(AttributesSimScores simScores,
			OWLPrettyPrinter owlpp, OWLGraphWrapper g, OWLNamedIndividual i,
			OWLNamedIndividual j) {
		// TODO Auto-generated method stub

	}




	@Override
	public void printIndividualPairSim(IndividualSimScores scores,
			OWLPrettyPrinter owlpp, OWLGraphWrapper graph) {
		// TODO Auto-generated method stub

	}

}

