package owltools.ontologyverification.impl;

import owltools.ontologyverification.OntologyCheck;

/**
 * Helper to reduce boiler plate code for an {@link OntologyCheck}.
 */
public abstract class AbstractCheck implements OntologyCheck {

	private final String id;
	private final String label;
	
	private boolean fatal = false;
	
	/**
	 * @param id
	 * @param label
	 * @param fatal
	 */
	protected AbstractCheck(String id, String label, boolean fatal) {
		this.id = id;
		this.label = label;
		this.fatal = fatal;
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
}
