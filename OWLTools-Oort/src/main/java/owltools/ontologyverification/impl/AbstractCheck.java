package owltools.ontologyverification.impl;

import owltools.ontologyverification.OntologyCheck;
import owltools.ontologyverification.OntologyCheckRunner.TimePoint;

/**
 * Helper to reduce boiler plate code for an {@link OntologyCheck}.
 */
public abstract class AbstractCheck implements OntologyCheck {

	private final String id;
	private final String label;
	private TimePoint timePoint;
	
	private boolean fatal = false;
	
	/**
	 * @param id
	 * @param label
	 * @param fatal
	 * @param timePoint
	 */
	protected AbstractCheck(String id, String label, boolean fatal, TimePoint timePoint) {
		this.id = id;
		this.label = label;
		this.fatal = fatal;
		this.timePoint = timePoint;
	}

	@Override
	public boolean isFatal() {
		return fatal;
	}

	@Override
	public void setFatal(boolean fatal) {
		this.fatal = fatal;
	}

	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public String getID() {
		return id;
	}

	@Override
	public TimePoint getTimePoint() {
		return timePoint;
	}

	@Override
	public void setTimePoint(TimePoint timePoint) {
		this.timePoint = timePoint;
	}

	// generated
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (fatal ? 1231 : 1237);
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		result = prime * result
				+ ((timePoint == null) ? 0 : timePoint.hashCode());
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
		AbstractCheck other = (AbstractCheck) obj;
		if (fatal != other.fatal)
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
			return false;
		if (timePoint != other.timePoint)
			return false;
		return true;
	}
	
}
