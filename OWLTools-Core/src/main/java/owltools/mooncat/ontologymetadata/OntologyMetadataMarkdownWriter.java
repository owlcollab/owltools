package owltools.mooncat.ontologymetadata;

import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import owltools.graph.OWLGraphWrapper;

public class OntologyMetadataMarkdownWriter {

	//public static void writeMarkdown(OWLGraphWrapper g, String baseDir, boolean isIncludeImage, File) {
	//}
	
	public static String renderMarkdown(OWLGraphWrapper g, String baseDir, boolean isIncludeImage) {

		OWLOntology ont = g.getSourceOntology();
		StringBuilder out = new StringBuilder();

		String ontId = g.getOntologyId();

		Set<OWLAnnotation> oAnns = ont.getAnnotations();
		System.err.println("NUM1:"+oAnns.size());

		String title = getVal("title", oAnns);

		out.append("## Ontology: "+title+"\n\n");
		out.append("IRI: "+rurl(ont)+"\n\n");

		String desc = getVal("description", oAnns);
		out.append("### Description\n\n");
		out.append(desc+"\n\n");

		Set<OWLOntology> imports = ont.getImports();
		if (imports.size() > 0) {
			out.append("### Imports\n\n");
			for (OWLOntology im : imports) {
				out.append(" * "+rurl(im)+"\n");
			}
		}
		if (isIncludeImage) {
			String imgFn = baseDir + "/" + ontId + ".png";
			out.append("![]("+imgFn+")\n\n");
		}

		System.err.println("NUM:"+oAnns.size());
		if (oAnns.size() > 0) {
			out.append("### Annotations:\n\n");
			for (OWLAnnotation ann : oAnns) {
				String annLabel = g.getLabelOrDisplayId(ann.getProperty());
				OWLAnnotationValue v = ann.getValue();
				String dv = v.toString();
				if (v instanceof OWLLiteral) {
					OWLLiteral lv = ((OWLLiteral)v);
					dv = lv.getLiteral();
					IRI dt = lv.getDatatype().getIRI();
					//System.out.println("DT = "+dt);
					if (dt.equals(OWL2Datatype.XSD_ANY_URI.getIRI())) {
						dv = href(lv.getLiteral());
					}
				}
				out.append(" * "+href(ann.getProperty().getIRI().toString(),annLabel)+" : "+dv+"\n");
			}
		}

		return out.toString();
	}

	private static String href(String url) {
		return href(url,url);
	}

	private static String href(String url, String label) {
		return "["+label+"]("+url+")";
	}

	private static String rurl(OWLOntology im) {
		return rurl(im.getOntologyID().getOntologyIRI().orElse(null));
	}

	private static String rurl(IRI iri) {
		return rurl(iri.toString());
	}
	private static String rurl(String iriStr) {
		return "["+iriStr+"]("+iriStr+")";
	}

	private static String getVal(String p, Set<OWLAnnotation> oAnns) {
		Set<String> vs = getVals(p, oAnns);
		if (vs.size() > 1)
			System.err.println("multiple val for "+p);
		if (vs.size() > 0)
			return vs.iterator().next();
		return null;
	}

	private static Set<String> getVals(String p, Set<OWLAnnotation> oAnns) {
		Set<OWLAnnotation> rmAnns = new HashSet<OWLAnnotation>();
		Set<String> vs = new HashSet<String>();
		System.err.println(" L: "+p);
		for (OWLAnnotation ann : oAnns) {
			String ps = ann.getProperty().getIRI().toString();
			ps = ps.replaceAll(".*/", "");
			if (ps.equals(p)) {
				String v = (ann.getValue() instanceof OWLLiteral) ? ((OWLLiteral)ann.getValue()).getLiteral() : ann.getValue().toString();
				//String v = ann.getValue().toString();
				vs.add(v);
				System.err.println("  P: "+ps+"="+v);
				rmAnns.add(ann);
			}
		}
		oAnns.removeAll(rmAnns);
		return vs;
	}

}
