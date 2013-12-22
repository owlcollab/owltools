package owltools.renderer.markdown;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.obolibrary.obo2owl.Obo2OWLConstants.Obo2OWLVocabulary;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import owltools.util.MinimalModelGenerator;

public class MarkdownRenderer {

	private static Logger LOG = Logger.getLogger(MarkdownRenderer.class);

	OWLOntology ontology;
	String directoryPath = "target/.";
	PrintStream io;

	public void render(OWLOntology o, String path) throws IOException {
		directoryPath = path;
		render(o);
	}

	public void render(OWLOntology o) throws IOException {
		ontology = o;
		FileUtils.forceMkdir(new File(directoryPath));
		for (OWLClass c : o.getClassesInSignature()) {
			render(c);
		}
	}

	public void render(OWLClass c) throws IOException {
		IRI iri = c.getIRI();
		String id = getId(c);

		try {
			tell(id);

			Set<OWLClassAxiom> logicalAxioms = 
					ontology.getAxioms(c);
			Set<OWLAnnotationAssertionAxiom> annotationAxioms = 
					ontology.getAnnotationAssertionAxioms(c.getIRI());
			//LOG.info("#annotationAxioms="+annotationAxioms.size());

			renderSection("Class : "+getLabel(c));

			renderTagValue("IRI", iri);

			renderAnnotationAxiom("Label", OWLRDFVocabulary.RDFS_LABEL.getIRI(), annotationAxioms);
			renderAnnotationAxiom("Definition", Obo2OWLVocabulary.IRI_IAO_0000115.getIRI(), annotationAxioms);
			renderAnnotationAxiom("Comment", OWLRDFVocabulary.RDFS_COMMENT.getIRI(), annotationAxioms);

			renderSection("Synonyms");

			renderAnnotationAxioms("", 
					Obo2OWLVocabulary.IRI_OIO_hasExactSynonym.getIRI(), 
					annotationAxioms);
			renderAnnotationAxioms("", 
					Obo2OWLVocabulary.IRI_OIO_hasBroadSynonym.getIRI(), 
					annotationAxioms);
			renderAnnotationAxioms("", 
					Obo2OWLVocabulary.IRI_OIO_hasNarrowSynonym.getIRI(), 
					annotationAxioms);
			renderAnnotationAxioms("", 
					Obo2OWLVocabulary.IRI_OIO_hasRelatedSynonym.getIRI(), 
					annotationAxioms);

			renderSection("Cross-references");

			renderAnnotationAxioms("", 
					Obo2OWLVocabulary.IRI_OIO_hasDbXref.getIRI(), 
					annotationAxioms);

			renderSection("Subsets");

			renderAnnotationAxioms("", 
					Obo2OWLVocabulary.IRI_OIO_inSubset.getIRI(), 
					annotationAxioms);

			renderSection("Superclasses");

			Set<OWLClassAxiom> lConsumed  = new HashSet<OWLClassAxiom>();
			List<OWLClassExpression> lEquiv = new ArrayList<OWLClassExpression>();
			List<OWLClass> lSuper = new ArrayList<OWLClass>();
			Map<OWLObjectPropertyExpression,List<OWLClassExpression>> mParents =
					new HashMap<OWLObjectPropertyExpression,List<OWLClassExpression>>();
			for (OWLClassAxiom ax : logicalAxioms) {
				if (ax instanceof OWLSubClassOfAxiom) {
					OWLSubClassOfAxiom scax = (OWLSubClassOfAxiom)ax;
					OWLClassExpression sup = scax.getSuperClass();
					if (!sup.isAnonymous()) {
						lConsumed.add(ax);
						lSuper.add((OWLClass) scax.getSuperClass());
					}
					else if (sup instanceof OWLObjectSomeValuesFrom) {
						OWLObjectSomeValuesFrom svf = (OWLObjectSomeValuesFrom)sup;
						OWLObjectPropertyExpression p = svf.getProperty();
						if (!mParents.containsKey(p)) {
							mParents.put(p, new ArrayList<OWLClassExpression>());
						}
						mParents.get(p).add(svf.getFiller());
						lConsumed.add(ax);
					}
				}
				else if (ax instanceof OWLEquivalentClassesAxiom) {
					lEquiv.addAll(((OWLEquivalentClassesAxiom)ax).getClassExpressionsMinus(c));
					lConsumed.add(ax);
				}
			}
			logicalAxioms.removeAll(lConsumed);

			renderObjectTagValues("", lSuper);		

			List<OWLObjectPropertyExpression> plist = 
					new ArrayList<OWLObjectPropertyExpression>(mParents.keySet());
			Collections.sort(plist);
			for (OWLObjectPropertyExpression p : plist) {
				List<OWLClassExpression> pcs = mParents.get(p);
				Collections.sort(pcs);
				for (OWLClassExpression pc : pcs) {
					renderTagValue("", cvt(p)+" some "+cvt(pc));
				}
			}

			renderSection("Equivalencies");
			Collections.sort(lEquiv);
			for (OWLClassExpression x : lEquiv) {
				renderTagValue("", cvt(x));
			}


			renderSection("Other Logical Axioms");

			renderSection("Other Annotations");

			Set<OWLAnnotationProperty> props = new HashSet<OWLAnnotationProperty>();
			for (OWLAnnotationAssertionAxiom aaa : annotationAxioms) {
				props.add(aaa.getProperty());
			}
			List<OWLAnnotationProperty> lProp = new ArrayList<OWLAnnotationProperty>(props);
			Collections.sort(lProp);
			for (OWLAnnotationProperty prop : lProp) {
				List<String> vs = new ArrayList<String>();
				for (OWLAnnotationAssertionAxiom aaa : annotationAxioms) {
					if (aaa.getProperty().equals(prop)) {
						vs.add(cvt(aaa.getValue()));
					}
				}	
				renderTagValues(cvt(prop), vs);
			}


			renderSection("External Comments");
		}
		finally {
			try {
				told();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private String getId(OWLNamedObject ob) {
		String id = ob.getIRI().toString();
		id = id.replaceAll(".*/", "");
		return id;
	}

	private String getLabelOrId(OWLNamedObject ob) {
		String label = getLabel(ob);
		if (label == null) {
			return getId(ob);
		}
		else {
			return label;
		}
	}
	private String getLabel(OWLNamedObject ob) {
		String label = null;
		for (OWLAnnotationAssertionAxiom aaa : ontology.getAnnotationAssertionAxioms(ob.getIRI())) {
			if (aaa.getProperty().isLabel()) {
				label = cvt(aaa.getValue());
				break;
			}
		}
		return label;

	}

	private void told() throws IOException {
		io.close();

	}

	private void tell(String id) throws IOException {
		String path = directoryPath + "/" + this.getRelativePath(id);
		FileUtils.forceMkdir(new File(path));
		io = new PrintStream(
				FileUtils.openOutputStream(new File(path + "/" + id + ".md"))
				);
	}

	private void renderAnnotationAxioms(String tag, IRI piri,
			Set<OWLAnnotationAssertionAxiom> annotationAxioms) {

		List<String> vs = new ArrayList<String>();
		Set<OWLAnnotationAssertionAxiom> consumed = new HashSet<OWLAnnotationAssertionAxiom>();
		for (OWLAnnotationAssertionAxiom aaa : annotationAxioms) {
			if (aaa.getProperty().getIRI().equals(piri)) {
				StringBuffer v = 
						new StringBuffer(cvt(aaa.getValue()));
				if (aaa.getAnnotations().size() > 0) {
					List<String> avs = new ArrayList<String>();
					for (OWLAnnotation ann : aaa.getAnnotations()) {
						avs.add(cvt(ann));
					}
					Collections.sort(avs);
					v.append(" [ "+StringUtils.join(avs, ", ")+" ]");
				}
				vs.add(v.toString());
				consumed.add(aaa);
			}
		}
		annotationAxioms.removeAll(consumed);
		renderTagValues(tag, vs);

	}

	private void renderAnnotationAxiom(String tag, IRI piri,
			Set<OWLAnnotationAssertionAxiom> annotationAxioms) {
		// TODO - max 1
		renderAnnotationAxioms(tag, piri, annotationAxioms);
	}

	private void renderTagValues(String t, List<String> vs) {
		Collections.sort(vs);
		for (String v : vs) {
			renderTagValue(t, v);
		}
	}


	private void renderObjectTagValues(String tag, List<OWLClass> lSuper) {
		Collections.sort(lSuper);
		for (OWLClass s : lSuper) {
			renderTagValue(tag, cvt(s));
		}
	}

	private void renderTagValue(String t, Object v) {
		if (t == null || t.equals("")) {
			render(" * "+v);
		}
		else {
			render(" * *"+t+"* = "+v);
		}
	}

	private void render(String s) {
		io.println(s);
	}

	private void renderSection(String s) {
		render("");
		render("## "+s);
		render("");

	}
	private void renderSubSection(String s) {
		render("");
		render("### "+s);
		render("");
	}

	private String cvt(OWLObject x) {
		if (x instanceof OWLNamedObject) {
			OWLNamedObject s = (OWLNamedObject)x;
			return getMarkdownLink(s);
		}
		else if (x instanceof OWLLiteral) {
			OWLLiteral lx = (OWLLiteral)x;
			return lx.getLiteral();
		}
		else if (x instanceof OWLAnnotation) {
			OWLAnnotation a = (OWLAnnotation)x;
			return (cvt(a.getProperty()) + " = " + cvt(a.getValue()));
		}
		else {
			// TODO
			return x.toString();
		}
	}

	private String getMarkdownLink(OWLNamedObject s) {
		String id = getId(s);
		return "["+getLabelOrId(s)+"](../../"+getRelativePath(id)+"/"+id+".md)";
	}

	private String getRelativePath(String id) {
		int pos = id.indexOf("_");
		String sp = null;
		String num = id;
		if (pos > -1) {
			sp = id.substring(0, pos);
			num = id.substring(pos+1);
		}

		String code = id.substring(id.length()-2);
		//String end = code + "/"+id+".md";
		if (sp == null) {
			return code;
		}
		else {
			return sp+"/"+code;
		}
	}


}
