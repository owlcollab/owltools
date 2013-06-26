package owltools.gaf.io;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.List;

import org.apache.commons.io.IOUtils;

import owltools.gaf.Bioentity;
import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.io.OWLPrettyPrinter;

public class GafWriter  {

	protected OWLPrettyPrinter prettyPrinter;
	protected PrintStream stream;

	

	public GafWriter() {
		super();
	}
	
	public PrintStream getStream() {
		return stream;
	}

	public void setStream(PrintStream stream) {
		this.stream = stream;
	}
	
	public void setStream(String file) {
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(file);
			this.stream = new PrintStream(new BufferedOutputStream(fos));
		} catch (FileNotFoundException e) {
			throw new RuntimeException("Could not open file: "+file, e);
		}
	}
	
	public void write(GafDocument gdoc) {
		try {
			writeHeader(gdoc);
			for (GeneAnnotation ann: gdoc.getGeneAnnotations()) {
				write(ann);
			}
		}
		finally {
			IOUtils.closeQuietly(stream);
		}
	}
	

	public void writeHeader(GafDocument gdoc) {
		writeHeader(gdoc.getComments());
	}
	
	public void writeHeader(List<String> comments) {
		print("!gaf-version: 2.0\n");
		if (comments != null && !comments.isEmpty()) {
			for (String comment : comments) {
				print("! "+comment+"\n");
			}
		}
	}

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
		print(ann.getCompositeQualifier());
		sep();
		
		// c5
		print(ann.getCls());
		sep();
		
		// c6
		print(ann.getReferenceId());
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
		StringBuilder synonymBuilder = new StringBuilder();
		List<String> synonyms = e.getSynonyms();
		if (synonyms != null && !synonyms.isEmpty()) {
			for (int i = 0; i < synonyms.size(); i++) {
				if (i > 0) {
					synonymBuilder.append('|');
				}
				synonymBuilder.append(synonyms.get(i));
			}
		}
		print(synonymBuilder.toString());
		sep();
		
		// c12
		print(e.getTypeCls());
		sep();
		
		// c13
		String taxon = e.getNcbiTaxonId().replaceAll("NCBITaxon", "taxon");
		print(taxon);
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

	protected void print(String s) {
		stream.print(s);
	}


	protected void sep() {
		stream.print("\t");
	}

	protected void nl() {
		stream.print("\n");
	}

}
