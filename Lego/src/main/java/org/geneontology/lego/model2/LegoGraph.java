package org.geneontology.lego.model2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.geneontology.lego.model.LegoLink;
import org.geneontology.lego.model.LegoNode;

public class LegoGraph {
	
	// for display purposes only, that is a lossy model
	private final List<LegoUnit> units;
	
	private final List<LegoNode> nodes;

	/**
	 * @param units
	 * @param nodes
	 */
	public LegoGraph(List<LegoUnit> units, List<LegoNode> nodes) {
		this.units = units;
		this.nodes = nodes;
	}

	/**
	 * The units are for display purposes only.<br>
	 * For the full model use nodes and links.
	 * 
	 * @return the units
	 */
	public List<LegoUnit> getUnits() {
		return units;
	}

	/**
	 * @return the nodes
	 */
	public List<LegoNode> getNodes() {
		return nodes;
	}

	/**
	 * Extract the whole list of links form the nodes
	 * 
	 * @return list of nodes
	 */
	public List<LegoLink> getLinks() {
		List<LegoLink> links = new ArrayList<LegoLink>();
		for(LegoNode node : nodes) {
			Collection<LegoLink> current = node.getLinks();
			if (current != null) {
				links.addAll(current);
			}
		}
		return links;
	}
}
