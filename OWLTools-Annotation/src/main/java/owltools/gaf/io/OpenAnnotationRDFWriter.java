package owltools.gaf.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import owltools.gaf.Bioentity;
import owltools.gaf.ExtensionExpression;
import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.vocab.OBOUpperVocabulary;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * Generates and writes RDF Models conforming to http://www.openannotation.org/spec/core/core.html
 * from {@link GeneAnnotation} objects.
 * 
 * TODOs:
 *  - use subProperties of hasBody and hasTarget?
 *  - use subClass of oa:Annotation
 *  - use own vocabulary for evidence
 *  - fix evidence
 *  - decide on policy for isoforms
 *  - vocabulary for GPAD relation
 *  - decide on model for annotation extensions
 *  - add all GPAD fields
 *  - include option to have labels and other data from bioentities (tho expected to come from elsewhere)
 *  - decide on URI policy for bioentities, entities in WITH column
 *  - many more things...
 * 
 * @author cjm
 *
 */
public class OpenAnnotationRDFWriter {

	Model model;
	String outputFormat = "TURTLE";
	boolean isAddBioentities = false;
	boolean isAddAnnotations = true;

	/**
	 * maps identifiers and shortform labels to URIs.
	 * 
	 * This can be set in advance to control
	 * - mapping relation labels to URIs in annotation extensions
	 * - mapping DBs to URIs (e.g. in assignedBy)
	 */
	Map<String,String> idToUrlLookup = new HashMap<String,String>();

	final String oa = "http://www.w3.org/ns/oa#";
	final String dc = "http://purl.org/dc/elements/1.1/";
	final String idorg = "http://identifiers.org/";

	/**
	 * Creates a new writer, using the default output format (turtle)
	 */
	public OpenAnnotationRDFWriter() {
		super();
	}

	/**
	 * @param outputFormat - any format accepted by Jena
	 */
	public OpenAnnotationRDFWriter(String outputFormat) {
		super();
		this.outputFormat = outputFormat;
	}

	/**
	 * If set, calling {{@link #write(GafDocument, File)} generates bioentities plus annotations (as in a GAF file)
	 */
	public void setGafMode() {
		isAddBioentities = true;
		isAddAnnotations = true;
	}
	/**
	 * If set, calling {{@link #write(GafDocument, File)} generates annotations only (as in a GPAD file)
	 * 
	 * This is the default
	 */
	public void setGpadfMode() {
		isAddBioentities = false;
		isAddAnnotations = true;
	}
	/**
	 * If set, calling {{@link #write(GafDocument, File)} generates bioentities only (as in a GPI file)
	 */
	public void setGpifMode() {
		isAddBioentities = true;
		isAddAnnotations = false;
	}


	/**
	 * Writes associations from GAF document as RDF on out
	 * 
	 * @param gdoc
	 * @param out
	 */
	public void write(GafDocument gdoc, PrintStream out) {
		createModel(gdoc);		
		model.write(out, outputFormat);
	}

	public void write(GafDocument gdoc, File ttl) throws FileNotFoundException {
		createModel(gdoc);		
		model.write(new PrintStream(ttl), outputFormat);

	}

	public void write(GafDocument gdoc, String path) throws FileNotFoundException {
		createModel(gdoc);		
		model.write(new PrintStream(path), outputFormat);
	}

	/**
	 * Generate an RDF model from a GAF Document
	 * 
	 * @param gdoc
	 */
	public void createModel(GafDocument gdoc) {
		model = ModelFactory.createDefaultModel();
		model.setNsPrefix("oa", oa);
		model.setNsPrefix("id", idorg);
		model.setNsPrefix("GO", OBOUpperVocabulary.OBO + "GO_");

		addHeaderInfo(gdoc);
		if (isAddAnnotations) {
			for (GeneAnnotation ann: gdoc.getGeneAnnotations()) {
				add(ann);
			}
		}
		if (isAddBioentities) {
			for (Bioentity e: gdoc.getBioentities()) {
				add(e);
			}
		}
	}


	/**
	 * Adds header info from a GAF, use the comments from the {@link GafDocument}.
	 * 
	 * @param gdoc
	 */
	public void addHeaderInfo(GafDocument gdoc) {
		List<String> comments = gdoc.getComments();
		if (comments != null && !comments.isEmpty()) {
			for (String comment : comments) {
				//
			}
		}
	}


	/**
	 * Adds a single {@link Bioentity} to a model
	 * @param e
	 */
	public void add(Bioentity bioentity) {
		Resource rBioentity = createResourceFromId(bioentity.getId());
		model.add(rBioentity, RDFS.label, bioentity.getSymbol());

	}

	/**
	 * Adds a single {@link GeneAnnotation} to a model
	 *
	 * @param ann
	 */
	public void add(GeneAnnotation ann) {
		if (ann == null) {
			return;
		}
		final Bioentity bioentity = ann.getBioentityObject();
		// TODO - why are we trimming here?
		final String isoForm = StringUtils.trimToNull(ann.getGeneProductForm());

		// TODO: don't use blank nodes
		Resource rAnn = model.createResource();
		Resource rBioentity = createResourceFromId(bioentity.getId());
		Resource rDescriptor = createResourceFromId(ann.getCls());

		model.add(rAnn, RDF.type, getAnnotationType());
		model.add(rAnn, getAnnotationToBioentityProperty(), rBioentity);
		model.add(rAnn, getAnnotationToDescriptorProperty(), rDescriptor);

		// TODO
		if (isoForm != null) {
			//Resource x = createResourceFromId(isoForm);
			//model.add(rAnn, getAnnotationToIsoformProperty(), x);

		}

		if (ann.getEcoEvidenceCls() != null) {
			Resource rEvidenceType = createResourceFromId(ann.getEcoEvidenceCls());
			model.add(rAnn, getAnnotationToEvidenceProperty(), rEvidenceType);
		}

		for (String refId : ann.getReferenceIds()) {
			//model.add(rAnn, getAnnotationToReferencesProperty(), rDescriptor);
		}

		/// TODO
		if (ann.isNegated()) {
		}


		for (List<ExtensionExpression> xl : ann.getExtensionExpressions()) {
			for (ExtensionExpression x : xl) {
				if (false) {
					// TODO - relations ontology needs loaded for mapping
					Resource xn = model.createResource(); // TODO - alternative to blank nodes?
					model.add(xn, 
							createPropertyFromId(x.getRelation()), 
							createResourceFromId(x.getCls()));
					model.add(rAnn, getAnnotationToExtensionProperty(), xn);
				}
			}
		}

		Pair<String, String> actsOnTaxon = ann.getActsOnTaxonId();
		if (actsOnTaxon != null) {
			// TODO
		}

		String date = ann.getLastUpdateDate();
		if (date != null) {
			// TODO - convert to xsd:date if use oa
			String isoDate = 
					date.substring(0, 4) + "-" + date.substring(4, 6) + "-" + date.substring(6, 8);
			Literal xsdDate = ResourceFactory.createTypedLiteral (isoDate, XSDDatatype.XSDdate);
			model.add(rAnn, getAnnotationDateProperty(), xsdDate);
		}

		if (ann.getAssignedBy() != null) {
			// TODO - use ORCID?
			model.add(rAnn, getAssignedByProperty(), ann.getAssignedBy());
		}

		for (String w : ann.getWithInfos()) {
			model.add(rAnn, getAnnotationToWithProperty(), createResourceFromId(w));

		}

	}

	// TODO: decide on property
	private Property getAnnotationToEvidenceProperty() {
		return model.getProperty(oa + "motivatedBy"); // <-- this is pretty wacky...
		// may be better to use motivatedBy for WITH column
	}

	private Property getAnnotationToWithProperty() {
		return model.getProperty(oa + "motivatedBy"); // <-- this is pretty wacky...
	}


	// the following methods below should be made configurable

	private Resource getAnnotationType() {
		return model.getResource(oa + "Annotation");
	}

	// TODO: decide whether to use subproperty in own model
	private Property getAnnotationToBioentityProperty() {
		return model.getProperty(oa + "hasBody");
	}

	// TODO: should we model as subtype of hasBody?
	private Property getAnnotationToIsoformProperty() {
		return model.getProperty(oa + "hasSpecificBody"); // TODO <-- this is not in OA
	}

	// TODO: decide whether to use subproperty in own model
	private Property getAnnotationToDescriptorProperty() {
		return model.getProperty(oa + "hasTarget");
	}

	// TODO: decide whether to use subproperty in own model
	private Property getAnnotationToExtensionProperty() {
		return model.getProperty(oa + "hasAnnotationExtension"); // TODO <-- this is not in OA!
	}

	private Property getAssignedByProperty() {
		//return model.getProperty(dc + "source");
		return model.getProperty(oa + "annotatedBy");
	}

	private Property getAnnotationDateProperty() {
		return model.getProperty(oa + "annotatedAt"); // must be XSD
	}


	private Property createPropertyFromId(String id) {
		Resource r = createResourceFromId(id);
		return model.getProperty(r.getURI()); // DUMB
	}

	// TODO - mapping to URIs for different ID types
	// this should be refactored; rely on prefix mapping to do the work here?
	private Resource createResourceFromId(String id) {
		if (idToUrlLookup.containsKey(id)) {
			return model.createResource(idToUrlLookup.get(id));
		}
		String[] parts = id.split(":");
		String pfx = parts[0];
		String frag;
		if (parts.length == 1) {
			System.err.println("NO_SEP: "+id);
			pfx = "";
			frag = parts[0];
			return model.createResource("http://geneontology.org/data/"+frag); // TODO
		}
		else {
			frag = parts[1];
		}
		if (parts.length != 2) {
			if (parts.length == 3 && parts[0].equals("MGI") && parts[1].equals("MGI")) {
				// dumb MGI IDs
				frag = parts[2];
			}
			else {
				// TODO: throw exception? There are too many of these
				System.err.println("BAD:"+id);
				if (parts.length == 3 && parts[0].equals(parts[1])) {
					frag = parts[2];
				}
				else if (parts.length == 3 ) {
					// TODO - fix data upstream for special PRO rule
					if (parts[0].startsWith("UniProt") &&
							parts[2].startsWith("PRO_")) {
						return model.createResource("http://purl.uniprot.org/annotation/" + parts[2]);
					}

					frag = parts[1]+"-"+parts[2];
				}
				else {
					return null;
				}
			}
		}
		String uriBase = model.getNsPrefixURI(pfx);
		String uri;
		if (uriBase == null) {
			//uri = OBOUpperVocabulary.OBO + pfx +"_" + frag;
			uri = idorg + pfx + ":" + frag;
		}
		else {
			uri = uriBase + frag;
		}
		return model.createResource(uri);
	}







}
