package owltools.gaf.lego;

import org.apache.commons.lang3.StringUtils;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import com.google.gson.annotations.SerializedName;

import owltools.graph.OWLGraphWrapper;
import owltools.vocab.OBOUpperVocabulary;

/**
 * Handle IRI to id string conversions.
 */
public class IdStringManager {

	/**
	 * @param i
	 * @param graph 
	 * @return id
	 * 
	 * @see IdStringManager#getIRI
	 */
	public static String getId(OWLNamedObject i, OWLGraphWrapper graph) {
		if (i instanceof OWLObjectProperty) {
			String relId = graph.getIdentifier(i);
			return relId;
		}
		IRI iri = i.getIRI();
		return getId(iri);
	}

	/**
	 * @param iri
	 * @return id
	 */
	public static String getId(IRI iri) {
		String iriString = iri.toString();
		// remove obo prefix from IRI
		String full = StringUtils.removeStart(iriString, OBOUpperVocabulary.OBO);
		String replaced;
		if (full.startsWith("#")) {
			replaced = StringUtils.removeStart(full, "#");
		}
		else {
			// replace first '_' char with ':' char
			replaced = StringUtils.replaceOnce(full, "_", ":");
		}
		return replaced;
	}

	/**
	 * Inverse method to {@link #getId}
	 * 
	 * @param id
	 * @param graph
	 * @return IRI
	 * 
	 * @see IdStringManager#getId
	 */
	public static IRI getIRI(String id, OWLGraphWrapper graph) {
		if (id.indexOf(':') < 0) {
			return graph.getIRIByIdentifier(id);
		}
		if(id.startsWith(OBOUpperVocabulary.OBO) ){
			return IRI.create(id);
		}
		String fullIRI = OBOUpperVocabulary.OBO + StringUtils.replaceOnce(id, ":", "_");
		return IRI.create(fullIRI);
	}

	/**
	 * Inverse method to {@link #getId} for IRIs only!
	 * 
	 * @param id
	 * @return IRI
	 * 
	 * @see IdStringManager#getId
	 */
	public static IRI getIRI(String id) {
		if(id.startsWith(OBOUpperVocabulary.OBO) ){
			return IRI.create(id);
		}
		String fullIRI = OBOUpperVocabulary.OBO + StringUtils.replaceOnce(id, ":", "_");
		return IRI.create(fullIRI);
	}

	public IdStringManager() {
		super();
	}
	
	public enum AnnotationShorthand {
		
		@SerializedName("layout-hint-x")
		x(IRI.create("http://geneontology.org/lego/layout/hint/x")),
		
		@SerializedName("layout-hint-y")
		y(IRI.create("http://geneontology.org/lego/layout/hint/y")),
		comment(OWLRDFVocabulary.RDFS_COMMENT.getIRI()), // arbitrary String
		evidence(IRI.create("http://geneontology.org/lego/evidence")), // eco class iri
		date(IRI.create("http://purl.org/dc/elements/1.1/date")), // arbitrary string at the moment, define date format?
		// DC recommends http://www.w3.org/TR/NOTE-datetime, one example format is YYYY-MM-DD
		source(IRI.create("http://purl.org/dc/elements/1.1/source")), // arbitrary string, such as PMID:000000
		contributor(IRI.create("http://purl.org/dc/elements/1.1/contributor")), // who contributed to the annotation
		title(IRI.create("http://purl.org/dc/elements/1.1/title")), // title (of the model)
		deprecated(OWLRDFVocabulary.OWL_DEPRECATED.getIRI()); // model annotation to indicate deprecated models
		
		
		private final IRI annotationProperty;
		
		AnnotationShorthand(IRI annotationProperty) {
			this.annotationProperty = annotationProperty;
		}
		
		public IRI getAnnotationProperty() {
			return annotationProperty;
		}
		
		
		public static AnnotationShorthand getShorthand(IRI iri) {
			for (AnnotationShorthand type : AnnotationShorthand.values()) {
				if (type.annotationProperty.equals(iri)) {
					return type;
				}
			}
			return null;
		}
		
		public static AnnotationShorthand getShorthand(String name) {
			for (AnnotationShorthand type : AnnotationShorthand.values()) {
				if (type.name().equals(name)) {
					return type;
				}
			}
			return null;
		}
	}

}