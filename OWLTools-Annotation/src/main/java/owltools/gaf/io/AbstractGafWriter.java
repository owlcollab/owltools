package owltools.gaf.io;

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import owltools.gaf.Bioentity;
import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.gaf.parser.BuilderTools;

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
		printSingle(e.getDb());
		sep();
		
		// c2
		printSingle(e.getLocalId());
		sep();
		
		// c3
		printSingle(e.getSymbol());
		sep();
		
		// c4
		print(ann.getCompositeQualifiers(), '|');
		sep();
		
		// c5
		printSingle(ann.getCls());
		sep();
		
		// c6
		print(ann.getReferenceIds(), '|');
		sep();
		
		// c7
		printSingle(ann.getShortEvidence());
		sep();
		
		// c8
		print(ann.getWithInfos(), '|');
		sep();
		
		// c9
		printSingle(ann.getAspect());
		sep();
		
		// c10
		printSingle(e.getFullName());
		sep();
		
		// c11
		print(e.getSynonyms(), '|'); 
		sep();
		
		// c12
		printSingle(e.getTypeCls());
		sep();
		
		// c13
		printSingle(createTaxonString(ann, e));
		sep();
		
		// c14
		printSingle(ann.getLastUpdateDate());
		sep();
		
		// c15
		printSingle(ann.getAssignedBy());
		sep();
		
		// c16
		printSingle(BuilderTools.buildExtensionExpression(ann.getExtensionExpressions()));
		sep();
		
		// c17
		printSingle(ann.getGeneProductForm());
		nl();
	}
	
	private String createTaxonString(GeneAnnotation ann, Bioentity e) {
		StringBuilder sb = new StringBuilder();
		sb.append(StringUtils.replace(e.getNcbiTaxonId(), "NCBITaxon:", "taxon:"));
		Pair<String, String> actsOnTaxonId = ann.getActsOnTaxonId();
		if (actsOnTaxonId != null && actsOnTaxonId.getLeft() != null) {
			String taxId = BuilderTools.removePrefix(actsOnTaxonId.getLeft(), ':');
			sb.append('|').append("taxon:").append(taxId);
		}
		return sb.toString();
	}

	private void print(Collection<String> list, char separator) {
		if (list != null && !list.isEmpty()) {
			print(StringUtils.join(list, separator));
		}
	}
	
	private void printSingle(String s) {
		s = StringUtils.trimToNull(s);
		if (s != null) {
			print(s);
		}
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
