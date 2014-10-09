package owltools.gaf.lego.server.external;

import java.util.List;

/**
 * Interface for wrapping a service to lookup information for a given identifier.
 */
public interface ExternalLookupService {

	/**
	 * Result of an id lookup.
	 */
	public static class LookupEntry {

		public final String id;
		public final String label;
		public final String type;
		public final String taxon;
		
		/**
		 * @param id
		 * @param label
		 * @param type
		 * @param taxon
		 */
		public LookupEntry(String id, String label, String type, String taxon) {
			this.id = id;
			this.label = label;
			this.type = type;
			this.taxon = taxon;
		}
	}
	
	/**
	 * Lookup the information for the given identifier. This is not a search.
	 * 
	 * @param id
	 * @return entries
	 */
	public List<LookupEntry> lookup(String id);
	
	/**
	 * Lookup the information for the given identifier and taxon. This is not a
	 * search.
	 * 
	 * @param id
	 * @param taxon
	 * @return entry
	 */
	public LookupEntry lookup(String id, String taxon);
	
}
