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
	
	public String sub = null;
	public String obj = null;
	public String pred = null;
	public String meta = null;

	/**
	 * @param subject_id
	 * @param object_id
	 */
	public OWLShuntEdge(String subject_id, String object_id) {
		this.sub = subject_id;
		this.obj = object_id;
	}
	
	/**
	 * @param subject_id
	 * @param object_id
	 * @param predicate_id
	 */
	public OWLShuntEdge(String subject_id, String object_id, String predicate_id) {
		this.sub = subject_id;
		this.obj = object_id;
		this.pred = predicate_id;
	}

	/**
	 * @return the metadata
	 */
	public String getMetadata() {
		return meta;
	}
	
	/**
	 * @param metadata the metadata to set
	 */
	public void setMetadata(String metadata) {
		this.meta = metadata;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((obj == null) ? 0 : obj.hashCode());
		result = prime * result
				+ ((pred == null) ? 0 : pred.hashCode());
		result = prime * result
				+ ((sub == null) ? 0 : sub.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object thing) {
		if (this == thing)
			return true;
		if (thing == null)
			return false;
		if (getClass() != thing.getClass())
			return false;
		OWLShuntEdge other = (OWLShuntEdge) thing;
		if (obj == null) {
			if (other.obj != null)
				return false;
		} else if (!obj.equals(other.obj))
			return false;
		if (pred == null) {
			if (other.pred != null)
				return false;
		} else if (!pred.equals(other.pred))
			return false;
		if (sub == null) {
			if (other.sub != null)
				return false;
		} else if (!sub.equals(other.sub))
			return false;
		return true;
	}
}
