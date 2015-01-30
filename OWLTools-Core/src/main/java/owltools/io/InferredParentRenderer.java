package owltools.io;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geneontology.reasoner.OWLExtendedReasoner;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;

import owltools.graph.OWLGraphWrapper;

/**
 
 Renders the most specific parent over a set of Objectproperties using an
  extended reasoner. Will also optionally "semi-materialize" GCIs.
  
 Base Format:
 
  - ID
  - Label
  - Rel1ID
  - Rel1Label
  - ..
  - ..
  - RelNID
  - RelNLabel
  
  The value V of the Rel column is the most specific inferred parent for that relation. i.e.
  
  ID SubClassOf Rel some V
  
  Must hold, and in addition, there should be no V' such that
  
  ID SubClassOf Rel some V'
  
  Also holds, where (Rel some V') SubClassOf (Rel some V)
 

  @author cjm

 */
public class InferredParentRenderer extends AbstractRenderer implements GraphRenderer {

	private static Logger LOG = Logger.getLogger(InferredParentRenderer.class);

	OWLExtendedReasoner reasoner;
	List<OWLObjectProperty> properties;
	OWLObjectProperty gciProperty;
	List<OWLClass> gciFillers = new ArrayList<OWLClass>();

	public InferredParentRenderer(PrintStream stream) {
		super(stream);
	}

	public InferredParentRenderer(String file) {
		super(file);
	}




	public OWLExtendedReasoner getReasoner() {
		return reasoner;
	}

	public void setReasoner(OWLExtendedReasoner reasoner) {
		this.reasoner = reasoner;
	}

	public List<OWLObjectProperty> getProperties() {
		return properties;
	}

	public void setProperties(List<OWLObjectProperty> properties) {
		this.properties = properties;
	}



	public OWLObjectProperty getGciProperty() {
		return gciProperty;
	}

	public void setGciProperty(OWLObjectProperty gciProperty) {
		this.gciProperty = gciProperty;
	}

	public List<OWLClass> getGciFillers() {
		return gciFillers;
	}

	public void setGciFillers(List<OWLClass> gciFillers) {
		this.gciFillers = gciFillers;
	}

	public void render(OWLGraphWrapper g) {
		graph = g;

		reasoner = (OWLExtendedReasoner) g.getReasoner();

		List<OWLClass> clist = new ArrayList<OWLClass>(g.getSourceOntology().getClassesInSignature(false));
		Collections.sort(clist);
		print("ClassID");
		sep();
		print("ClassLabel");
		if (gciFillers.size() > 0) {
			sep();
			print("In ID");
			sep();
			print("In Class");
		}
		for (OWLObjectProperty p : properties) {
			sep();
			String cn = p.getIRI().getFragment()+" "+graph.getLabel(p);
			print(cn+" ID");
			sep();
			print(cn+" Label");
		}		
		print("\n");
		for (OWLClass c : clist) {
			if (graph.isObsolete(c))
				continue;
			render(c);
			for (OWLClass fc : gciFillers) {
				render(c, fc);
			}
		}
		stream.close();
	}

	private void render(OWLClass c) {
		render(c, null);
	}

	// TODO - make this configurable
	private void render(OWLClass c, OWLClass filler) {
		if (c.isBottomEntity())
			return;
		if (c.isTopEntity())
			return;
		OWLClassExpression cx = c;
		if (filler != null) {
			OWLObjectSomeValuesFrom svf = graph.getDataFactory().getOWLObjectSomeValuesFrom(gciProperty, filler);
			cx = graph.getDataFactory().getOWLObjectIntersectionOf(c, svf);
		}
		if (!reasoner.isSatisfiable(cx)) {
			LOG.info("Ignore unsat: "+cx+" // base= "+c);
			return;
		}
		print(getId(c));
		sep();
		print(graph.getLabel(c));
		if (gciFillers.size() > 0) {
			if (filler != null) {
				sep();
				print(getId(filler));
				sep();
				print(graph.getLabel(filler));
			}
			else {
				sep();
				print("-");
				sep();
				print("-");				
			}
		}
		for (OWLObjectProperty p : properties) {
			Set<OWLClass> parents = reasoner.getSuperClassesOver(cx, p, true);
			List<OWLClass> lparents = new ArrayList<OWLClass>(parents);
			List<String> l1 = new ArrayList<String>();
			List<String> l2 = new ArrayList<String>();
			for (OWLClass pc : lparents) {
				l1.add(getId(pc));
				l2.add(graph.getLabel(pc));
			}

			sep();
			print(join(l1, "|"));
			sep();
			print(join(l2, "|"));
		}
		print("\n");
	}

	private String getId(OWLClass c) {
		return graph.getIdentifier(c);
	}

	static public String join(List<String> list, String conjunction)
	{
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String item : list)
		{
			if (first)
				first = false;
			else
				sb.append(conjunction);
			sb.append(item);
		}
		return sb.toString();
	}

}

