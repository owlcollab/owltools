package owltools.gaf.io;

import java.io.PrintWriter;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import owltools.gaf.Bioentity;
import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.gaf.parser.BuilderTools;

public class GpadWriter {
	
	private final PrintWriter pw;
	private final double version;

	/**
	 * @param pw
	 * @param version 
	 */
	public GpadWriter(PrintWriter pw, double version) {
		this.pw = pw;
		this.version = version;
	}


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
		if (version < 1.2) {
			print("!gpad-version: 1.1");
		}
		else {
			print("!gpad-version: 1.2"); //TODO use a number format to write the actual format version
		}
		nl();
		if (comments != null && !comments.isEmpty()) {
			for (String comment : comments) {
				print("! "+comment);
				nl();
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
		final Bioentity bioentity = ann.getBioentityObject();
		if (bioentity == null) {
			return;
		}
		
		// c1 DB required
		print(bioentity.getDb());
		sep();
		
		// c2 DB_Object_ID required
		print(bioentity.getLocalId());
		sep();
		
		// c3 Qualifier required
		print(BuilderTools.buildQualifierString(ann.getCompositeQualifiers()));
		sep();
		
		// c4 GO ID required
		print(ann.getCls());
		sep();
		
		// c5 DB:Reference(s) required
		print(StringUtils.join(ann.getReferenceIds(), '|'));
		sep();
		
		// c6 Evidence code required
		print(ann.getEcoEvidenceCls());
		sep();
		
		// c7 With (or) From optional
		print(BuilderTools.buildWithString(ann.getWithInfos()));
		sep();
		
		// c8 Interacting taxon ID (for multi-organism processes) optional
		print(BuilderTools.buildTaxonString(ann.getActsOnTaxonId()));
		sep();
		
		// c9 Date required
		print(ann.getLastUpdateDate());
		sep();
		
		// c10 Assigned_by required
		print(ann.getAssignedBy());
		sep();
		
		// c11 Annotation Extension optional
		print(BuilderTools.buildExtensionExpression(ann.getExtensionExpressions()));
		sep();
		
		// c12 Annotation Properties optional
		print(BuilderTools.buildPropertyExpression(ann.getProperties()));
		nl();
	}
	
	/**
	 * Append an arbitrary string.
	 * 
	 * @param s
	 */
	protected void print(String s) {
		s = StringUtils.trimToNull(s);
		if (s != null) {
			pw.print(s);
		}
	}

	/**
	 * Called after the writing of a {@link GafDocument} has been finished.
	 */
	protected void end() {
		pw.close();
	}

	/**
	 * Append a the separator between columns.
	 */
	protected void sep() {
		pw.print('\t');
	}

	/**
	 * Append the separator between rows.
	 */
	protected void nl() {
		pw.println();
	}
}
