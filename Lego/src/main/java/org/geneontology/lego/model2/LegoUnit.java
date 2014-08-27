package org.geneontology.lego.model2;

import java.util.List;

import org.geneontology.lego.model.LegoMetadata;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;

public class LegoUnit extends LegoMetadata {

	private final IRI id;
	
	private final OWLClass enabledBy;
	private final OWLClass activity; // molecular function
	private final OWLClass process;  // part_of some process, optional
	private final List<OWLClass> location;

	/**
	 * @param id
	 * @param enabledBy
	 * @param activity
	 * @param process
	 * @param location
	 */
	public LegoUnit(IRI id, OWLClass enabledBy, OWLClass activity,
			OWLClass process, List<OWLClass> location) {
		this.id = id;
		this.enabledBy = enabledBy;
		this.activity = activity;
		this.process = process;
		this.location = location;
	}

	/**
	 * @return the id
	 */
	public IRI getId() {
		return id;
	}

	/**
	 * @return the enabledBy
	 */
	public OWLClass getEnabledBy() {
		return enabledBy;
	}

	/**
	 * @return the activity
	 */
	public OWLClass getActivity() {
		return activity;
	}

	/**
	 * @return the process
	 */
	public OWLClass getProcess() {
		return process;
	}

	/**
	 * @return the location
	 */
	public List<OWLClass> getLocation() {
		return location;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("LegoUnit [id=");
		builder.append(id);
		builder.append(", enabledBy=");
		builder.append(enabledBy);
		builder.append(", activity=");
		builder.append(activity);
		builder.append(", process=");
		builder.append(process);
		builder.append(", location=");
		builder.append(location);
		builder.append("]");
		return builder.toString();
	} 
	
}
