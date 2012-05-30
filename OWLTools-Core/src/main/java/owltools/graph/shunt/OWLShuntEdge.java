package owltools.graph.shunt;

/**
 * An edge reduced to nothing; for use with OWLShuntGraph.
 * 
 * TODO: As we are just testing/doing POC right now, the predicate is being ignored
 * (and until I figure out how to squeeze it out of OWL--see Chris).
 * 
 * See: {@link owltools.graph.shunt.OWLShuntGraph} and {@link owltools.graph.shunt.OWLShuntNode}.
 */
public class OWLShuntEdge {
	
	public String subject_id = null;
	public String object_id = null;
	public String predicate_id = null;
	
	/**
	 * @param subject_id
	 * @param object_id
	 */
	public OWLShuntEdge(String subject_id, String object_id) {
		this.subject_id = subject_id;
		this.object_id = object_id;
	}
	
	/**
	 * @param subject_id
	 * @param object_id
	 * @param predicate_id
	 */
	public OWLShuntEdge(String subject_id, String object_id, String predicate_id) {
		this.subject_id = subject_id;
		this.object_id = object_id;
		this.predicate_id = predicate_id;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((object_id == null) ? 0 : object_id.hashCode());
		result = prime * result
				+ ((predicate_id == null) ? 0 : predicate_id.hashCode());
		result = prime * result
				+ ((subject_id == null) ? 0 : subject_id.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OWLShuntEdge other = (OWLShuntEdge) obj;
		if (object_id == null) {
			if (other.object_id != null)
				return false;
		} else if (!object_id.equals(other.object_id))
			return false;
		if (predicate_id == null) {
			if (other.predicate_id != null)
				return false;
		} else if (!predicate_id.equals(other.predicate_id))
			return false;
		if (subject_id == null) {
			if (other.subject_id != null)
				return false;
		} else if (!subject_id.equals(other.subject_id))
			return false;
		return true;
	}
}
