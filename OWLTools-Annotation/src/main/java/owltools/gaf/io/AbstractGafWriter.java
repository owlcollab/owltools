package owltools.gaf.io;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import owltools.gaf.Bioentity;
import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;

/**
 * General utility to write a {@link GafDocument} or {@link GeneAnnotation}.
 */
public abstract class AbstractGafWriter  {

	/**
	 * Write a full GAF.
	 * 
	 * @param gdoc
	 */
	public void write(GafDocument gdoc) {
		try {
			writeHeader(gdoc);
			for (GeneAnnotation ann: gdoc.getGeneAnnotations()) {
				write(ann);
			}
		}
		finally {
			end();
		}
	}
	

	/**
	 * Write a header of a GAF, use the comments from the {@link GafDocument}.
	 * 
	 * @param gdoc
	 */
	public void writeHeader(GafDocument gdoc) {
		writeHeader(gdoc.getComments());
	}
	
	/**
	 * Write a header for a GAF, header comments are optional.
	 * 
	 * @param comments
	 */
	public void writeHeader(List<String> comments) {
		print("!gaf-version: 2.0\n");
		if (comments != null && !comments.isEmpty()) {
			for (String comment : comments) {
				print("! "+comment+"\n");
			}
		}
	}

	/**
	 * Write a single {@link GeneAnnotation}.
	 * 
	 * @param ann
	 */
	public void write(GeneAnnotation ann) {
		if (ann == null) {
			return;
		}
		Bioentity e = ann.getBioentityObject();
		// c1
		print(e.getDb());
		sep();
		
		// c2
		print(e.getLocalId());
		sep();
		
		// c3
		print(e.getSymbol());
		sep();
		
		// c4
		print(ann.getQualifierString());
		sep();
		
		// c5
		print(ann.getCls());
		sep();
		
		// c6
		List<String> referenceIds = ann.getReferenceIds();
		if (referenceIds != null && !referenceIds.isEmpty()) {
			print(StringUtils.join(referenceIds, '|'));
		}
		sep();
		
		// c7
		print(ann.getEvidenceCls());
		sep();
		
		// c8
		print(ann.getWithExpression());
		sep();
		
		// c9
		print(ann.getAspect());
		sep();
		
		// c10
		print(e.getFullName());
		sep();
		
		// c11
		List<String> synonyms = e.getSynonyms();
		if (synonyms != null && !synonyms.isEmpty()) {
			String combined = StringUtils.join(synonyms, '|'); 
			print(combined);
		}
		sep();
		
		// c12
		print(e.getTypeCls());
		sep();
		
		// c13
		print(createTaxonString(ann, e));
		sep();
		
		// c14
		print(ann.getLastUpdateDate());
		sep();
		
		// c15
		print(ann.getAssignedBy());
		sep();
		
		// c16
		print(ann.getExtensionExpression());
		sep();
		
		// c17
		print(ann.getGeneProductForm());
		nl();
	}
	
	private String createTaxonString(GeneAnnotation ann, Bioentity e) {
		StringBuilder sb = new StringBuilder();
		sb.append(StringUtils.replace(e.getNcbiTaxonId(), "NCBITaxon:", "taxon:"));
		String actsOnTaxonId = StringUtils.trimToNull(ann.getActsOnTaxonId());
		if (actsOnTaxonId != null) {
			sb.append('|').append(StringUtils.replace(actsOnTaxonId, "NCBITaxon:", "taxon:"));
		}
		return sb.toString();
	}

	/**
	 * Append an arbitrary string.
	 * 
	 * @param s
	 */
	protected abstract void print(String s);

	/**
	 * Called after the writing of a {@link GafDocument} has been finished.
	 */
	protected abstract void end();

	/**
	 * Append a the separator between columns.
	 */
	protected void sep() {
		print("\t");
	}

	/**
	 * Append the separator between rows.
	 */
	protected void nl() {
		print("\n");
	}

}
