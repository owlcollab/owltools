package org.geneontology.lego.model2;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LegoGraph {
	
	List<LegoUnit> units = new ArrayList<LegoUnit>();
	Set<LegoLink> links = new HashSet<LegoLink>();

	/**
	 * @return the units
	 */
	public List<LegoUnit> getUnits() {
		return units;
	}

	/**
	 * @return the links
	 */
	public Set<LegoLink> getLinks() {
		return links;
	}
	
}
