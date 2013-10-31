package owltools.sim.io;

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;
import owltools.sim2.SimpleOwlSim.ScoreAttributePair;
import owltools.sim2.scores.AttributePairScores;
import owltools.sim2.scores.ElementPairScores;
import owltools.sim2.scores.PairScores;

public class JSONRenderer extends AbstractRenderer implements SimResultRenderer {

	private static NumberFormat doubleRenderer = new DecimalFormat("#.##########");

	boolean isHeaderLine = true;

	private final String separator;
	private final String commentPrefix;

	public JSONRenderer(PrintStream resultOutStream, String separator, String commentPrefix) {
		this.resultOutStream = resultOutStream;
		this.separator = separator;
		this.commentPrefix = commentPrefix;
	}

	public JSONRenderer(PrintStream resultOutStream) {
		//TODO: comments not allowed in JSON.  though, perhaps we could use: _comment: <blah>
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

	
	/* {match: "MGI:12345",
  match_label: "shh",
  simj: 0.5,
  maxIC: 8.7,
  lcs: ["MP:1", "MP:2"],
  attribute_matches: [
    {query_attribute: "HP:1",
     match_attribute: "MP:5",
     lcs: "MP:1"
     ic: ""
    },
    {..},
  ],
#    label_map: [ {id: "MP:1", label: "abnormal brain morphology", ... } ]
#    ic_map: [ {id: "MP:1", ic: "5.4", ... } ]
  obj_map [     {id: "MP:1", label: "abnormal brain morphology", ic: ...} ]
  		*/
	@Override
	public void printIndividualPairSim(IndividualSimScores scores, OWLPrettyPrinter owlpp, OWLGraphWrapper graph) {
		OWLNamedIndividual i = scores.i;
		OWLNamedIndividual j = scores.j;
		Integer numannotsI = scores.numberOfElementsI;
		Integer numannotsJ = scores.numberOfElementsJ;

		//store the match and score details
		HashMap<String,String> keyValPairs = new HashMap<String, String>();

		//store properties of the attributes
		//array of elements - maybe i don't even need to make this
		class ObjMap {
			private String id;
			private String label;
			private Double ic;
			
			ObjMap(String id, String label, Double ic) {
				this.id = id;
				this.label = label;
				this.ic = ic;
			}
		}

		Set<ObjMap> objMapSet = new HashSet<ObjMap>();
		
		keyValPairs.put("match", graph.getIdentifier(j));
		keyValPairs.put("match_label", graph.getLabel(j));
		keyValPairs.put("n", numannotsJ.toString());

		if (scores.simjScore != null) {
			keyValPairs.put(scores.simjScoreLabel, doubleRenderer.format(scores.simjScore));
		}
		if (scores.maxIC != null) {
			keyValPairs.put(scores.maxICLabel, doubleRenderer.format(scores.maxIC.score));
			keyValPairs.put(scores.maxICLabel + " Term", "[maxic term ids here]");
			//this is dumb - iterate instead
			//OWLClassExpression o = scores.maxIC.attributeClassSet.iterator().next();
			//ObjMap om = new ObjMap(graph.getIdentifier(o), graph.getLabel(o), scores.maxIC.score);
			//objMapSet.add(om);
		}
		//TODO: alter show method to render a list of terms.
		keyValPairs.put("lcs", "comma,separated,list,of lcs,ids,above,threshold");
		//revise show(scores.bmaAsymIC.attributeClassSet, owlpp).toString()
		//TODO: add the lcs terms to the objmap set - get 
		
		//TODO: wrap the whole thing in a query  "J", "n"

		if (scores.bmaSymIC != null) {
			keyValPairs.put(scores.bmaSymICLabel, doubleRenderer.format(scores.bmaSymIC));
		}
		if (scores.bmaAsymIC != null) {
			keyValPairs.put(scores.bmaAsymICLabel, doubleRenderer.format(scores.bmaAsymIC));
		}

		if (scores.bmaAsymJ != null) {
			keyValPairs.put(scores.bmaAsymJLabel, doubleRenderer.format(scores.bmaAsymJ.score));
		}

		if (scores.bmaSymJ != null) {
			keyValPairs.put(scores.bmaSymJLabel, doubleRenderer.format(scores.bmaSymJ.score));
		}

		if (scores.simGIC != null) {
			keyValPairs.put(scores.simGICLabel, doubleRenderer.format(scores.simGIC));
		}

		
		resultOutStream.print("{");
		//will end up with a trailing comma - will it break?
		for (String k : keyValPairs.keySet()) {
			resultOutStream.println(quote(k) + ": " + quote(keyValPairs.get(k)) + ",");
		}
		resultOutStream.print("}");

		resultOutStream.flush();
	}
	
	private String quote(String s) {
		return ("\"" + s + "\"");
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
		resultOutStream.flush();
	}

	@Override
	public void printAttributeSim(AttributesSimScores simScores,
			OWLGraphWrapper graph, OWLPrettyPrinter owlpp) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void dispose() {
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
	public void printPairScores(AttributePairScores scores) {
		// TODO
	}


}

