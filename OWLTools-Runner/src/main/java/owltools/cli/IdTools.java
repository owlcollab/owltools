package owltools.cli;

/**
 * Convenience tool to convert IRI style Id suffixes into OBO style identifier.
 * For example BFO_0000050 is converted to BFO:0000050. 
 * It should not change ids like 'part_of'.
 */
public class IdTools {

	/**
	 * @param id
	 * @return true, if the id looks like an IRI style ID-suffix 
	 */
	static boolean isIRIStyleIdSuffix(String id) {
		// is BFO_0000050?
		// check for uppercase prefix until you find an underscore
		int underScoreIndex = -1;
		final int length = id.length();
		for (int i = 0; i < length; i++) {
			char c = id.charAt(i);
			if (c == '_') {
				underScoreIndex = i;
				break;
			}
			if (!Character.isLetter(c) || !Character.isUpperCase(c)) {
				return false;
			}
		}
		// check that there is a underscore
		if (underScoreIndex <= 1) {
			return false;
		}
		
		// check that the rest is only numbers
		if (length < underScoreIndex + 1) {
			return false;
		}
		for (int i = underScoreIndex + 1; i < length; i++) {
			char c = id.charAt(i);
			if (!Character.isDigit(c)) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Convert an IRI style suffix to an OBO-style identifier with a colon.
	 * Only input string which have been checked by {@link #isIRIStyleIdSuffix(String)}.
	 * 
	 * @param iriStyleId
	 * @return obo-style id
	 * 
	 * @see #isIRIStyleIdSuffix(String)
	 */
	static String convertToOboStyleId(String iriStyleId) {
		// BFO_0000050
		// assume that no other style of identifiers is input here
		// run no checks
		
		// find index of underscore
		int index = iriStyleId.indexOf('_');
		
		// compose
		StringBuilder sb = new StringBuilder();
		sb.append(iriStyleId.substring(0, index));
		sb.append(':');
		sb.append(iriStyleId.substring(index+1));
		
		return sb.toString();
	}
	
}
