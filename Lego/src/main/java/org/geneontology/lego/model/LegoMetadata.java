package org.geneontology.lego.model;

import java.util.Set;

public class LegoMetadata {

	private Set<String> dates = null;
	private Set<String> contributors = null;
	private Set<String> sources = null;
	private Set<String> evidence = null;

	public LegoMetadata() {
		super();
	}

	/**
	 * @return the contributors
	 */
	public Set<String> getContributors() {
		return contributors;
	}

	/**
	 * @param contributors the contributors to set
	 */
	public void setContributors(Set<String> contributors) {
		this.contributors = contributors;
	}

	/**
	 * @return the dates
	 */
	public Set<String> getDates() {
		return dates;
	}

	/**
	 * @param dates the dates to set
	 */
	public void setDates(Set<String> dates) {
		this.dates = dates;
	}

	/**
	 * @return the sources
	 */
	public Set<String> getSources() {
		return sources;
	}

	/**
	 * @param sources the sources to set
	 */
	public void setSources(Set<String> sources) {
		this.sources = sources;
	}

	/**
	 * @return the evidence
	 */
	public Set<String> getEvidence() {
		return evidence;
	}

	/**
	 * @param evidence the evidence to set
	 */
	public void setEvidence(Set<String> evidence) {
		this.evidence = evidence;
	}

}