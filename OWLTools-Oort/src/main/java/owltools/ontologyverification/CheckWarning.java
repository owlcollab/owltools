package owltools.ontologyverification;

import java.util.Collections;
import java.util.List;

import org.semanticweb.owlapi.model.IRI;

/**
 * Information about an error or warning as a result of an ontology check. 
 *
 */
public class CheckWarning {

	private final String check;
	private final String message;
	private final boolean isFatal;
	
	private final List<IRI> iris;
	private final String field;

	/**
	 * @param check
	 * @param message
	 * @param isFatal
	 * @param iri
	 */
	public CheckWarning(String check, String message, boolean isFatal, IRI iri) {
		this(check, message, isFatal, iri == null ? null : Collections.singletonList(iri), null);
	}
	
	/**
	 * @param check
	 * @param message
	 * @param isFatal
	 * @param iri
	 * @param field
	 */
	public CheckWarning(String check, String message, boolean isFatal, IRI iri, String field) {
		this(check, message, isFatal, iri == null ? null : Collections.singletonList(iri), field);
	}
	
	/**
	 * @param check
	 * @param message
	 * @param isFatal
	 * @param iris
	 * @param field
	 */
	public CheckWarning(String check, String message, boolean isFatal, List<IRI> iris, String field) {
		this.check = check;
		this.message = message;
		this.isFatal = isFatal;
		this.iris = iris;
		this.field = field;
	}

	@Override
	public String toString() {
		return "CheckWarning" + (isFatal ? " (fatal)" : "") + ": " + message;
	}

	/**
	 * @return the check
	 */
	public String getCheck() {
		return check;
	}

	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @return the isFatal
	 */
	public boolean isFatal() {
		return isFatal;
	}

	/**
	 * @return the list of affected {@link IRI}s or null 
	 */
	public List<IRI> getIris() {
		return iris;
	}

	/**
	 * @return the field or null
	 */
	public String getField() {
		return field;
	}

	// generated
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((check == null) ? 0 : check.hashCode());
		result = prime * result + ((field == null) ? 0 : field.hashCode());
		result = prime * result + ((iris == null) ? 0 : iris.hashCode());
		result = prime * result + (isFatal ? 1231 : 1237);
		result = prime * result + ((message == null) ? 0 : message.hashCode());
		return result;
	}

	// generated
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CheckWarning other = (CheckWarning) obj;
		if (check == null) {
			if (other.check != null)
				return false;
		} else if (!check.equals(other.check))
			return false;
		if (field == null) {
			if (other.field != null)
				return false;
		} else if (!field.equals(other.field))
			return false;
		if (iris == null) {
			if (other.iris != null)
				return false;
		} else if (!iris.equals(other.iris))
			return false;
		if (isFatal != other.isFatal)
			return false;
		if (message == null) {
			if (other.message != null)
				return false;
		} else if (!message.equals(other.message))
			return false;
		return true;
	}


}
