package owltools.renderer.markdown;

import java.io.File;
import java.io.IOException;
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
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLInverseObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLNaryBooleanClassExpression;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyCharacteristicAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import owltools.util.OwlHelper;

/**
 * Fairly hacky md renderer, geared towards storing md in github.
 * 
 * Generates ones page per Object (so far just classes, OPs and APs), plus
 * an index page.
 * 
 * The idea is to store the md in github and make use of github's automatic
 * rendering. To avoid large flat directories we use a directory structure,
 * first divided by ontology ID space (warning: OBO hardcode alert), and then
 * by the last two digits (or other characters) of the ID. For example:
 * 
 * https://github.com/obophenotype/cephalopod-ontology/blob/master/src/ontology/md/CEPH/59/CEPH_0000259.md
 * 
 * It's not really clear at all whether this is a good idea. There are some nice aspects:
 * 
 * - the ontology is searchable via github search
 * - we have another fallback diff mechanism (the rendered md is generally sorted to avoid spurious diffs)
 * - it seems to be easier to customize and much less work than making a bona fide web interface
 * - localized docs, e.g. for forked repos
 * - universal md currency. E.g. easy to paste into tracker items on github
 * - possibly extensible to some future framework that allows web-based editing...
 * 
 * Some bad aspects
 * 
 *  - everything is very wired to github
 *  - the rendering is incomplete. So far the rule is if it's an axiom type I care about it will probably be rendered
 *  - until the rendering procedure is stable spurious diffs will be generated
 *  - it's just a bit peculiar
 *  - ultimately pointless will be subsumed into the Ultimate web ontology framework that has all the features of Protege, a Wiki and GitHub, .. WebProtege?
 *  
 * Existing features:
 * 
 * is semi-aware of some OBO annotation properties; renders synonyms in a special section
 * 
 * aware of depictions (see http://douroucouli.wordpress.com/2012/07/03/45/)
 * Example: https://github.com/obophenotype/cephalopod-ontology/blob/master/src/ontology/md/CEPH/30/CEPH_0000130.md
 * 
 * Manchester-esque recursive rendering of some axioms and expressions (falls back to ofn)
 * 
 * 
 * 
 * 
 * @author cjm
 *
 */
public class MarkdownRenderer {

	private static Logger LOG = Logger.getLogger(MarkdownRenderer.class);
	public int MAX_REFERENCING_AXIOMS = 1000;

	OWLOntology ontology;
	String directoryPath = "target/.";
	PrintStream io;
	private int chunkLevel = 2;
	
	

	public int getChunkLevel() {
		return chunkLevel;
	}

	public void setChunkLevel(int chunkLevel) {
		this.chunkLevel = chunkLevel;
	}

	public void render(OWLOntology o, String path) throws IOException {
		directoryPath = path;
		render(o);
	}

	public void render(OWLOntology o) throws IOException {
		ontology = o;
		FileUtils.forceMkdir(new File(directoryPath));

		// note: each object gets rendered to its own file;
		// if editing be careful; each render(..) operation sets the global filehandle io
		// object, and is expected to close it when finished
		for (OWLClass c : o.getClassesInSignature()) {
			render(c);
		}
		for (OWLObjectProperty p : o.getObjectPropertiesInSignature()) {
			render(p);
		}
		for (OWLAnnotationProperty p : o.getAnnotationPropertiesInSignature()) {
			render(p);
		}

		tellToIndex();
		renderSection("Ontology");
		IRI oidValue = null;
		IRI versionValue = null;
		if(o.getOntologyID().getOntologyIRI().isPresent()) {
			oidValue = o.getOntologyID().getOntologyIRI().get();
		}
		if (o.getOntologyID().getVersionIRI().isPresent()) {
			versionValue = o.getOntologyID().getVersionIRI().get();
		}
		renderTagValue("OID", oidValue);
		renderTagValue("Version", versionValue);

		renderSection("Classes");
		for (OWLClass c : o.getClassesInSignature()) {
			renderTagValue("", getMarkdownLink(c,""));
		}
		renderSection("ObjectProperties");
		for (OWLObjectProperty p : o.getObjectPropertiesInSignature()) {
			renderTagValue("", getMarkdownLink(p,""));
		}
		renderSection("AnnotationProperties");
		for (OWLAnnotationProperty p : o.getAnnotationPropertiesInSignature()) {
			renderTagValue("", getMarkdownLink(p,""));
		}
		told();
	}

	public void renderAbout(OWLClass c) throws IOException {
		Set<OWLAxiom> axioms = ontology.getReferencingAxioms(c, Imports.INCLUDED);
	}

	private void renderGenericAnnotations(OWLNamedObject c) {
		Set<OWLAnnotationAssertionAxiom> annotationAxioms = 
				ontology.getAnnotationAssertionAxioms(c.getIRI());
		renderGenericAnnotations(annotationAxioms);
	}

	private void renderGenericAnnotations(Set<OWLAnnotationAssertionAxiom> annotationAxioms ) {
		renderSection("Other Annotations");

		Set<OWLAnnotationProperty> props = new HashSet<OWLAnnotationProperty>();
		for (OWLAnnotationAssertionAxiom aaa : annotationAxioms) {
			props.add(aaa.getProperty());
		}
		List<OWLAnnotationProperty> lProp = new ArrayList<OWLAnnotationProperty>(props);
		Collections.sort(lProp);
		for (OWLAnnotationProperty prop : lProp) {
			if (prop.getIRI().equals(Obo2OWLVocabulary.IRI_OIO_hasOboNamespace.getIRI())) {
				continue;
			}
			if (prop.getIRI().getFragment().equals("id")) {
				continue;
			}
			List<String> vs = new ArrayList<String>();
			for (OWLAnnotationAssertionAxiom aaa : annotationAxioms) {
				if (aaa.getProperty().equals(prop)) {
					vs.add(generateText(aaa.getValue()));
				}
			}	
			renderTagValues(generateText(prop), vs);
		}
	}
	private void renderControlledAnnotations(OWLNamedObject c) {
		Set<OWLAnnotationAssertionAxiom> annotationAxioms = 
				ontology.getAnnotationAssertionAxioms(c.getIRI());
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
	}
	
	private void renderUsage(Set<OWLAxiom> refAxs) {
		List<String> axstrs = new ArrayList<String>();


		renderSection("Usage");
		int n = 0;
		for (OWLAxiom ax : refAxs) {
			axstrs.add(generateText(ax));
			n++;
			if (n > MAX_REFERENCING_AXIOMS) {
				renderTagValue("", "...TRUNCATED REMAINING AXIOMS");
			}
		}
		Collections.sort(axstrs);
		for (String axstr : axstrs) {
			renderTagValue("", axstr);
		}

	}

	public void render(OWLClass c) throws IOException {
		IRI iri = c.getIRI();
		String id = getId(c);

		try {
			tell(id);

			Set<IRI> imgs = new HashSet<IRI>();
			
			Set<OWLClassAxiom> logicalAxioms = 
					ontology.getAxioms(c, Imports.EXCLUDED);
			Set<OWLAnnotationAssertionAxiom> annotationAxioms = 
					ontology.getAnnotationAssertionAxioms(c.getIRI());
			//LOG.info("#annotationAxioms="+annotationAxioms.size());

			renderSection("Class : "+getLabel(c));

			renderTagValue("IRI", iri);
			
			for (OWLAxiom ax : ontology.getReferencingAxioms(c)) {
				if (ax instanceof OWLClassAssertionAxiom) {
					LOG.info("Inspecting CAX:"+ax);
					// try and extract depiction axioms
					OWLClassAssertionAxiom cax = (OWLClassAssertionAxiom)ax;
					if (cax.getClassExpression() instanceof OWLObjectSomeValuesFrom) {
						OWLObjectSomeValuesFrom svf = (OWLObjectSomeValuesFrom)(cax.getClassExpression());
						if (!svf.getProperty().isAnonymous()) {
							OWLObjectProperty p = (OWLObjectProperty)svf.getProperty();
							LOG.info("SVF CAX:"+cax);
							if (p.getIRI().equals(IRI.create("http://xmlns.com/foaf/0.1/depicts"))) {
								if (!cax.getIndividual().isAnonymous()) {
									imgs .add(cax.getIndividual().asOWLNamedIndividual().getIRI());
									LOG.info("Extracted img:"+imgs);
								}
							}
						}
					}
				}

			}
			
			if (imgs.size() > 0) {
				renderSection("Depictions");
				for (IRI img : imgs) {
					render("![Depiction]("+img+")");
				}
			}


			renderControlledAnnotations(c);

			
			// extracting superclasses
			Set<OWLClassAxiom> lConsumed  = new HashSet<OWLClassAxiom>();
			List<OWLClassExpression> lEquiv = new ArrayList<OWLClassExpression>();
			List<OWLClass> lSuper = new ArrayList<OWLClass>();
			Map<OWLObjectPropertyExpression,List<OWLClassExpression>> mParents =
					new HashMap<OWLObjectPropertyExpression,List<OWLClassExpression>>();
			for (OWLClassAxiom ax : logicalAxioms) {
				//LOG.info("LAX:"+ax);

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
				else {
					// will be rendered later
				}
			}
			logicalAxioms.removeAll(lConsumed);

			renderSection("Superclasses");
			renderObjectTagValues("", lSuper);		

			
			List<OWLObjectPropertyExpression> plist = 
					new ArrayList<OWLObjectPropertyExpression>(mParents.keySet());
			Collections.sort(plist);
			for (OWLObjectPropertyExpression p : plist) {
				List<OWLClassExpression> pcs = mParents.get(p);
				Collections.sort(pcs);
				for (OWLClassExpression pc : pcs) {
					renderTagValue("", generateText(p)+" some "+generateText(pc));
				}
			}

			renderSection("Equivalencies");
			Collections.sort(lEquiv);
			for (OWLClassExpression x : lEquiv) {
				renderTagValue("", generateText(x));
			}

			renderSection("Other Logical Axioms");

			List<String> axstrs = new ArrayList<String>();
			for (OWLAxiom ax : logicalAxioms) {
				axstrs.add(generateText(ax));
			}
			Collections.sort(axstrs);
			for (String axstr : axstrs) {
				renderTagValue("", axstr);
			}

			renderGenericAnnotations(c);


			
			renderUsage(ontology.getReferencingAxioms(c));
			


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

	public void render(OWLObjectProperty p) throws IOException {
		IRI iri = p.getIRI();
		String id = getId(p);

		try {
			tell(id);

			Set<OWLObjectPropertyAxiom> logicalAxioms = 
					ontology.getAxioms(p, Imports.EXCLUDED);

			renderSection("Class : "+getLabel(p));

			renderTagValue("IRI", iri);

			renderControlledAnnotations(p);


			Set<OWLObjectPropertyExpression> sups = new HashSet<OWLObjectPropertyExpression>();
			Set<OWLObjectPropertyExpression> subs = new HashSet<OWLObjectPropertyExpression>();

			for (OWLObjectPropertyAxiom ax : logicalAxioms) {
				if (ax instanceof OWLSubObjectPropertyOfAxiom) {
					OWLSubObjectPropertyOfAxiom scax = (OWLSubObjectPropertyOfAxiom)ax;
					OWLObjectPropertyExpression sup = scax.getSuperProperty();
					OWLObjectPropertyExpression sub = scax.getSubProperty();
					if (sub.equals(p)) {
						sups.add(sup);
					}
					if (sup.equals(p)) {
						subs.add(sub);
					}
				}
			}

			renderSection("SuperProperties");
			for (OWLObjectPropertyExpression pe : sups) {
				renderTagValue("", getMarkdownLink(pe));
			}
			renderSection("SubProperties");
			for (OWLObjectPropertyExpression pe : subs) {
				renderTagValue("", pe);
			}

			renderSection("Other Logical Axioms");

			List<String> axstrs = new ArrayList<String>();
			for (OWLAxiom ax : logicalAxioms) {
				axstrs.add(generateText(ax));
			}
			Collections.sort(axstrs);
			for (String axstr : axstrs) {
				renderTagValue("", axstr);
			}

			renderGenericAnnotations(p);


			renderUsage(ontology.getReferencingAxioms(p));

			

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

	public void render(OWLAnnotationProperty p) throws IOException {
		IRI iri = p.getIRI();
		String id = getId(p);

		try {
			tell(id);


			renderSection("Class : "+getLabel(p));

			renderTagValue("IRI", iri);

			renderControlledAnnotations(p);

			renderSection("SuperProperties");
			for (OWLAnnotationProperty pe : OwlHelper.getSuperProperties(p, ontology)) {
				renderTagValue("", getMarkdownLink(pe));
			}
			renderSection("SubProperties");
			for (OWLAnnotationProperty pe : OwlHelper.getSubProperties(p, ontology)) {
				renderTagValue("", getMarkdownLink(pe));
			}

			renderSection("Other Logical Axioms");

			renderGenericAnnotations(p);


			renderUsage(ontology.getReferencingAxioms(p));
			

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
	
	/**
	 * TODO: rename getFragment?
	 * 
	 * Fetches the part after the "/"
	 * 
	 * @param object
	 * @return
	 */
	private String getId(OWLNamedObject ob) {
		String id = ob.getIRI().toString();
		id = id.replaceAll(".*/", "");
		id = id.replaceAll("#", "-");
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
				label = generateText(aaa.getValue());
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

	private void tellToIndex() throws IOException {
		String path = directoryPath; 
		FileUtils.forceMkdir(new File(path));
		// todo - make configurable
		io = new PrintStream(
				FileUtils.openOutputStream(new File(path + "/index.md")) 
				);
	}
	private void renderAnnotationAxioms(String tag, IRI piri,
			Set<OWLAnnotationAssertionAxiom> annotationAxioms) {

		List<String> vs = new ArrayList<String>();
		Set<OWLAnnotationAssertionAxiom> consumed = new HashSet<OWLAnnotationAssertionAxiom>();
		for (OWLAnnotationAssertionAxiom aaa : annotationAxioms) {
			if (aaa.getProperty().getIRI().equals(piri)) {
				StringBuffer v = 
						new StringBuffer(generateText(aaa.getValue()));
				if (aaa.getAnnotations().size() > 0) {
					List<String> avs = new ArrayList<String>();
					for (OWLAnnotation ann : aaa.getAnnotations()) {
						avs.add(generateText(ann));
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
			renderTagValue(tag, generateText(s));
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

	private String generateText(OWLObject x) {
		if (x instanceof OWLNamedObject) {
			OWLNamedObject s = (OWLNamedObject)x;
			return getMarkdownLink(s);
		}
		else if (x instanceof OWLLiteral) {
			OWLLiteral lx = (OWLLiteral)x;
			return lx.getLiteral();
		}
		else if (x instanceof IRI) {
			return x.toString();
		}
		else if (x instanceof OWLDeclarationAxiom) {
			return "-";
		}
		else if (x instanceof OWLAnnotation) {
			OWLAnnotation a = (OWLAnnotation)x;
			return (generateText(a.getProperty()) + " = " + generateText(a.getValue()));
		}
		else if (x instanceof OWLNaryBooleanClassExpression) {
			List<String> vs = new ArrayList<String>();
			for (OWLClassExpression y : ((OWLNaryBooleanClassExpression)x).getOperands()) {
				vs.add(generateText(y));
			}
			Collections.sort(vs);
			String op = " and ";
			if (x instanceof OWLObjectUnionOf) {
				op = "or";
			}
			return StringUtils.join(vs.iterator(), op);
		}
		else if (x instanceof OWLObjectSomeValuesFrom) {
			OWLObjectSomeValuesFrom svf = (OWLObjectSomeValuesFrom)x;
			return generateText(svf.getProperty()) + " some " + generateText(svf.getFiller());
		}
		else if (x instanceof OWLObjectAllValuesFrom) {
			OWLObjectAllValuesFrom svf = (OWLObjectAllValuesFrom)x;
			return generateText(svf.getProperty()) + " only " + generateText(svf.getFiller());
		}
		else if (x instanceof OWLAxiom) {
			// TODO - render axiom annotations
			if (x instanceof OWLSubClassOfAxiom) {
				OWLSubClassOfAxiom ax = (OWLSubClassOfAxiom)x;
				return generateText(ax.getSubClass()) + " SubClassOf " + generateText(ax.getSuperClass());
			}
			else if (x instanceof OWLSubObjectPropertyOfAxiom) {
				OWLSubObjectPropertyOfAxiom ax = (OWLSubObjectPropertyOfAxiom)x;
				return generateText(ax.getSubProperty()) + " SubPropertyOf " + generateText(ax.getSuperProperty());
			}
			else if (x instanceof OWLInverseObjectPropertiesAxiom) {
				OWLInverseObjectPropertiesAxiom ax = (OWLInverseObjectPropertiesAxiom)x;
				return generateText(ax.getFirstProperty()) + " InverseOf " + generateText(ax.getSecondProperty());
			}
			else if (x instanceof OWLObjectPropertyCharacteristicAxiom) {
				OWLObjectPropertyCharacteristicAxiom ax = (OWLObjectPropertyCharacteristicAxiom)x;
				return ax.getAxiomType() + " " + generateText(ax.getProperty());
			}
			else if (x instanceof OWLObjectPropertyDomainAxiom) {
				OWLObjectPropertyDomainAxiom ax = (OWLObjectPropertyDomainAxiom)x;
				return generateText(ax.getProperty()) + " Domain " + generateText(ax.getDomain());
			}
			else if (x instanceof OWLObjectPropertyRangeAxiom) {
				OWLObjectPropertyRangeAxiom ax = (OWLObjectPropertyRangeAxiom)x;
				return generateText(ax.getProperty()) + " Range " + generateText(ax.getRange());
			}
			else if (x instanceof OWLAnnotationAssertionAxiom) {
				OWLAnnotationAssertionAxiom ax = (OWLAnnotationAssertionAxiom)x;
				return generateText(ax.getSubject()) + " " + generateText(ax.getProperty()) +
						" " + generateText(ax.getValue());
			}
			else if (x instanceof OWLClassAssertionAxiom) {
				OWLClassAssertionAxiom ax = (OWLClassAssertionAxiom)x;
				return generateText(ax.getIndividual()) + " InstanceOf" + generateText(ax.getClassExpression());
			}
			else if (x instanceof OWLEquivalentClassesAxiom) {
				OWLEquivalentClassesAxiom ax = (OWLEquivalentClassesAxiom)x;
				StringBuffer sb = null;
				for (OWLClassExpression cex : ax.getClassExpressions()) {
					if (sb == null)
						sb = new StringBuffer("");
					else
						sb.append(" == ");
					sb.append(generateText(cex));
				}
				return sb.toString();
			}
			else {
				LOG.warn("Using default for unhandled axiom type "+x+" "+x.getClass());
			}
		}
		else {
			LOG.warn("Using default for "+x+" "+x.getClass());
			// TODO

		}
		return x.toString();
	}

	private String getMarkdownLink(OWLObject s) {
		String prefix = "../../";
		if (chunkLevel == 1) {
			prefix = "../";
		}
		else if (chunkLevel == 0) {
			prefix = "";
		}
		return getMarkdownLink(s, prefix);
	}

	private String getMarkdownLink(OWLObject obj, String pathToRoot) {
		if (obj instanceof OWLNamedObject) {
			OWLNamedObject s = (OWLNamedObject)obj;
			String id = getId(s);
			String rpath = pathToRoot+getRelativePath(id);
			if (rpath.length() == 0) {
				// everything is in same directory; no need for "/"
				return "["+getLabelOrId(s)+"]("+id+".md)";
			}
			return "["+getLabelOrId(s)+"]("+rpath+"/"+id+".md)";
		}
		else {
			return generateText(obj);
		}
	}

	/**
	 * @param id of format {IDSPACE}_{LOCALID}
	 * @return path of form IDSPACE/CODE
	 */
	private String getRelativePath(String id) {
		if (chunkLevel == 0) {
			return "";
		}
		
		// we assume OBO-style IRIs, which are of the form IDSPACE_LOCALID;
		// these tr
		int pos = id.indexOf("_");
		String sp = null;
		String num = id;
		if (pos > -1) {
			sp = id.substring(0, pos);
			num = id.substring(pos+1);
		}
		if (chunkLevel == 1) {
			return sp;
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
