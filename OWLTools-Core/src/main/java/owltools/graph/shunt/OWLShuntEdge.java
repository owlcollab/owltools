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
}
