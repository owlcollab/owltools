package owltools.gaf.io;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import owltools.gaf.Bioentity;
import owltools.gaf.GafDocument;
import owltools.gaf.parser.BuilderTools;

public class GpiWriter {

	private final PrintWriter pw;
	private final double version;

	/**
	 * @param pw
	 * @param version 
	 */
	public GpiWriter(PrintWriter pw, double version) {
		this.pw = pw;
		this.version = version;
	}


	/**
	 * Write a full GAF.
	 * 
	 * @param bioentities
	 */
	public void write(List<Bioentity> bioentities) {
		try {
			writeHeader();
			for (Bioentity entity: bioentities) {
				write(entity);
			}
		}
		finally {
			end();
		}
	}
	

	/**
	 * Write a header of a GPI.
	 */
	public void writeHeader() {
		writeHeader(null);
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
	 * Write a single {@link Bioentity}.
	 * 
	 * @param bioentity
	 */
	public void write(Bioentity bioentity) {
		if (bioentity == null) {
			return;
		}
		
		if (version >= 1.2) { 
			// c1 DB required
			print(bioentity.getDb());
			sep();
		}

		// c2 DB_Object_ID required
		print(bioentity.getLocalId());
		sep();
		
		// c3 DB_Object_Symbol
		print(bioentity.getSymbol());
		sep();
		
		// c4 DB_Object_Name
		print(bioentity.getFullName());
		sep();
		
		// c5 DB_Object_Synonym(s)
		print(bioentity.getSynonyms(), '|');
		sep();
		
		// c6 DB_Object_Type
		print(bioentity.getTypeCls());
		sep();
		
		// c7 Taxon
		// TODO print(bioentity.getNcbiTaxonId());
		sep();
		
		// c8 Parent_Object_ID
		print(bioentity.getParentObjectId());
		sep();
		
		// c9 DB_Xrefs
		print(bioentity.getDbXrefs(), '|');
		sep();
		
		// c10 Gene_Product_Properties
		print(BuilderTools.buildPropertyExpression(bioentity.getProperties()));
		nl();
	}
	
	private void print(Collection<String> list, char separator) {
		if (list != null && !list.isEmpty()) {
			print(StringUtils.join(list, separator));
		}
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
