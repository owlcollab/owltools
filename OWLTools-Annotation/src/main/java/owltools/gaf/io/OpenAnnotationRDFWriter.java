package owltools.gaf.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import owltools.gaf.Bioentity;
import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

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
	
	final String oa = "http://www.w3.org/ns/oa#";

	public OpenAnnotationRDFWriter() {
		super();
	}

	public OpenAnnotationRDFWriter(String outputFormat) {
		super();
		this.outputFormat = outputFormat;
	}


	/**
	 * @param gdoc
	 * @param out
	 */
	public void write(GafDocument gdoc, PrintStream out) {
		createModel(gdoc);		
		model.write(out, outputFormat);
	}

	public void write(GafDocument gdoc, File ttl) throws FileNotFoundException {
		write(gdoc, new PrintStream(ttl));
		
	}
	
	public void write(GafDocument gdoc, String path) throws FileNotFoundException {
		write(gdoc, new PrintStream(path));
	}

	/**
	 * Generate an RDF model from a GAF Document
	 * 
	 * @param gdoc
	 */
	public void createModel(GafDocument gdoc) {
		model = ModelFactory.createDefaultModel();
		model.setNsPrefix("oa", oa);

		addHeaderInfo(gdoc);
		for (GeneAnnotation ann: gdoc.getGeneAnnotations()) {
			add(ann);
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
	 * Adds a single {@link GeneAnnotation} to a model
	 *
	 * @param ann
	 */
	public void add(GeneAnnotation ann) {
		if (ann == null) {
			return;
		}
		final Bioentity bioentity = ann.getBioentityObject();
		final String isoForm = StringUtils.trimToNull(ann.getGeneProductForm());
		
		// TODO: don't use blank nodes
		Resource rAnn = model.createResource();
		Resource rBioentity = createResourceFromId(bioentity.getId());
		Resource rDescriptor = createResourceFromId(ann.getCls());
		Resource rEvidenceType = createResourceFromId(ann.getEcoEvidenceCls());
		
		model.add(rAnn, RDF.type, getAnnotationType());
		model.add(rAnn, getAnnotationToBioentityProperty(), rBioentity);
		model.add(rAnn, getAnnotationToDescriptorProperty(), rDescriptor);
		model.add(rAnn, getAnnotationToEvidenceProperty(), rEvidenceType);
		
		for (String refId : ann.getReferenceIds()) {
			//model.add(rAnn, getAnnotationToBioentityProperty(), rDescriptor);
		}

	}
	
	private Property getAnnotationToEvidenceProperty() {
		return model.getProperty(oa + "motivatedBy"); // <-- this is pretty wacky...
		// may be better to use motivatedBy for WITH column
	}


	// the following methods below should be made configurable
	
	private Resource getAnnotationType() {
		return model.getResource(oa + "Annotation");
	}

	private Property getAnnotationToBioentityProperty() {
		return model.getProperty(oa + "hasBody");
	}
	
	private Property getAnnotationToDescriptorProperty() {
		return model.getProperty(oa + "hasTarget");
	}

	// TODO - mapping to URIs for different ID types
	private Resource createResourceFromId(String id) {
		return model.createResource(id);
	}

	




	
}
