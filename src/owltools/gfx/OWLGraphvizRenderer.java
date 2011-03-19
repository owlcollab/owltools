package owltools.gfx;

import javax.imageio.*;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObject;

import owltools.gfx.GraphStyle;
import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLQuantifiedProperty;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import uk.ac.ebi.interpro.graphdraw.*;

// todo - make a common abstract class for both of these
public class OWLGraphvizRenderer extends OWLGraphLayoutRenderer {

	public OWLGraphvizRenderer(OWLGraphWrapper owlGraphWrapper) {
		super(owlGraphWrapper);
	}


	public String renderDot() {
		StringBuilder s = new StringBuilder();
		s.append("digraph g {\n");

		for (OWLGraphLayoutNode node : g.nodes) {
			s.append("  "+safe(node)+" [");
			s.append(props(node));
			s.append("];\n");
		}
		for (OWLGraphStrokeEdge edge : g.edges) {
			s.append("  "+safe(edge.getChild())+" -> "+safe(edge.getParent())+" [");
			s.append("];\n");
		}


		s.append("}\n");



		return s.toString();
	}

	private String safe(OWLGraphLayoutNode node) {
		return node.getOwlObject().toString().replaceAll("[^abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_]", "");
	}

	private String props(OWLGraphLayoutNode n) {
		Vector<String> ps = new Vector<String>();
		OWLObject ob = n.getOwlObject();
		ps.add(prop("label",owlGraphWrapper.getLabel(ob)));
		StringBuilder s = new StringBuilder();
		for (String p : ps) {
			s.append(p+", ");
		}

		return s.toString();
	}

	private String props(OWLGraphStrokeEdge se) {
		Vector<String> ps = new Vector<String>();
		OWLGraphEdge e = se.owlGraphEdge;
		//ps.add(prop("label",owlGraphWrapper.getLabel(e.getSingleQuantifiedProperty().)));
		StringBuilder s = new StringBuilder();
		for (String p : ps) {
			s.append(p+", ");
		}

		return s.toString();
	}


	private String prop(String p, String v) {
		return p+"=\""+ (v==null?"":v) + "\"";
	}


	public String renderImage(String fmt, OutputStream fos) {
		String dot = renderDot();

		String dotpath = "dot";
		try {
			File tmp = File.createTempFile("img", "dot");
			String infile = tmp.getAbsolutePath();
			String cmd = "dot -T"+fmt+" -Grankdir=BT "+infile;
			Process p = Runtime.getRuntime().exec(cmd);

			p.waitFor();
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(p.getInputStream()));
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// TODO





		return "TODO";

	}

}



