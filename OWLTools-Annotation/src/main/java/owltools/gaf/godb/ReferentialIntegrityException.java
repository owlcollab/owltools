package owltools.gaf.godb;


public class ReferentialIntegrityException extends Exception {

	public ReferentialIntegrityException(String table, Object obj) {
		super("No entry for "+obj+" in "+table);
	}
	
}
